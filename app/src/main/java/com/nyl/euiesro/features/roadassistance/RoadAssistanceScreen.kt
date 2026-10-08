package com.nyl.euiesro.features.roadassistance

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nyl.euiesro.R
import coil.compose.AsyncImage
import com.nyl.euiesro.ApiClient
import com.nyl.euiesro.FilterChip
import com.nyl.euiesro.resolveBestCurrentLocationForDisplay
import com.nyl.euiesro.sortLocationsByDistance
import com.nyl.euiesro.LocationMapHeader
import com.nyl.euiesro.SearchUtils
import com.nyl.euiesro.common.GroupedTypeCategory
import com.nyl.euiesro.common.GroupedTypeOption
import com.nyl.euiesro.common.GroupedTypeSelector
import com.nyl.euiesro.common.LocationUiItem
import com.nyl.euiesro.common.ModuleSection

@Composable
fun RoadAssistanceSection(innerPadding: androidx.compose.foundation.layout.PaddingValues, onOpenLocation: (LocationUiItem) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var places by remember { mutableStateOf<List<RoadAssistanceSummary>>(emptyList()) }
    var loading by rememberSaveable { mutableStateOf(true) }
    var error by rememberSaveable { mutableStateOf("") }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedService by rememberSaveable { mutableStateOf<String?>(null) }
    var mapView by remember { mutableStateOf<org.osmdroid.views.MapView?>(null) }
    var hasLocationPermission by rememberSaveable {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
                androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    val locationPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    val serviceFilterGroups = listOf(
        GroupedTypeCategory(
            category = stringResource(R.string.road_assistance_service_mobile),
            options = listOf(
                GroupedTypeOption(value = "service_service_mobile_moto", label = stringResource(R.string.road_assistance_vehicle_moto)),
                GroupedTypeOption(value = "service_service_mobile_auto", label = stringResource(R.string.road_assistance_vehicle_auto)),
                GroupedTypeOption(value = "service_service_mobile_atv", label = stringResource(R.string.road_assistance_vehicle_atv))
            )
        ),
        GroupedTypeCategory(
            category = stringResource(R.string.road_assistance_vulcanizare_mobile),
            options = listOf(
                GroupedTypeOption(value = "service_vulcanizare_mobile_moto", label = stringResource(R.string.road_assistance_vehicle_moto)),
                GroupedTypeOption(value = "service_vulcanizare_mobile_auto", label = stringResource(R.string.road_assistance_vehicle_auto)),
                GroupedTypeOption(value = "service_vulcanizare_mobile_atv", label = stringResource(R.string.road_assistance_vehicle_atv))
            )
        ),
        GroupedTypeCategory(
            category = stringResource(R.string.road_assistance_transport_vehicule),
            options = listOf(
                GroupedTypeOption(value = "service_transport_vehicule_moto", label = stringResource(R.string.road_assistance_vehicle_moto)),
                GroupedTypeOption(value = "service_transport_vehicule_auto", label = stringResource(R.string.road_assistance_vehicle_auto)),
                GroupedTypeOption(value = "service_transport_vehicule_atv", label = stringResource(R.string.road_assistance_vehicle_atv))
            )
        )
    )

    LaunchedEffect(searchQuery, selectedService) {
        loading = true
        error = ""
        places = RoadAssistanceApi.fetchRoadAssistancePlaces(
            limit = 500,
            query = searchQuery,
            service = selectedService
        )
        loading = false
        if (places.isEmpty()) error = "No road assistance providers available right now."
    }

    val userLocation = remember(context) { resolveBestCurrentLocationForDisplay(context) }
    val items = sortLocationsByDistance(
        places.map { place ->
            LocationUiItem(
                id = place.id,
                section = ModuleSection.ROAD_ASSISTANCE,
                title = place.title,
                description = place.description,
                address = place.address,
                latitude = place.latitude,
                longitude = place.longitude,
                phone = place.phone,
                website = place.website,
                email = place.email,
                services = place.services,
                imageUrls = emptyList(),
                typeIds = emptyList(),
                markerIcons = com.nyl.euiesro.SiteMarkerIconResolver.resolveRoadAssistanceMarkerIcons(place.services),
                averageRating = place.averageRating,
                reviewsCount = place.reviewsCount
            )
        },
        userLocation
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
    ) {
        if (loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
        } else if (error.isNotBlank()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(error, color = MaterialTheme.colorScheme.error)
            }
        } else {
            com.nyl.euiesro.common.map.MapBottomSheetLayout(
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                searchPlaceholder = stringResource(R.string.search_provider),
                onFilterClick = if (serviceFilterGroups.isNotEmpty()) { { /* modal managed by shared shell */ } } else null,
                onCloseFilter = { },
                useFilterModal = true,
                filterContent = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        GroupedTypeSelector(
                            title = "",
                            groups = serviceFilterGroups,
                            selectedValues = selectedService?.let { setOf<String>(it) } ?: emptySet<String>(),
                            onToggle = { value ->
                                selectedService = if (selectedService == value) null else value
                            }
                        )
                    }
                },
                items = items,
                onMapReady = { mapView = it },
                onCenterLocation = { sheetRatio ->
                    if (!hasLocationPermission) {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                android.Manifest.permission.ACCESS_FINE_LOCATION,
                                android.Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                        return@MapBottomSheetLayout
                    }
                    com.nyl.euiesro.centerMapOnUserLocation(mapView, sheetRatio)
                },
                onZoomIn = { mapView?.controller?.zoomIn() },
                onZoomOut = { mapView?.controller?.zoomOut() },
                showUserLocation = hasLocationPermission,
                sheetHeader = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${items.size} de locații găsite",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                sheetContent = {
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(items) { place ->
                            val distanceText = com.nyl.euiesro.calculateDistanceMeters(
                                userLocation?.latitude,
                                userLocation?.longitude,
                                place.latitude,
                                place.longitude
                            )?.let { meters -> com.nyl.euiesro.formatDistanceLabel(meters) }
                            com.nyl.euiesro.common.CompactLocationListItem(
                                title = place.title,
                                subtitle = place.address ?: "",
                                badge = "",
                                chips = place.services.take(3).map { it.replace('_', ' ') },
                                rating = place.averageRating,
                                reviewCount = place.reviewsCount,
                                statusText = null,
                                distanceText = distanceText,
                                onClick = { onOpenLocation(place) }
                            )
                        }
                    }
                },
                onAddAction = null,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
