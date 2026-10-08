package com.nyl.euiesro.features.moto

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.nyl.euiesro.R
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import coil.compose.AsyncImage
import com.nyl.euiesro.FilterChip
import com.nyl.euiesro.InteractiveLocationMap
import com.nyl.euiesro.LocationMapHeader
import com.nyl.euiesro.MapCenterLocationButton
import com.nyl.euiesro.SearchUtils
import com.nyl.euiesro.SelectedLocationInfoCard
import com.nyl.euiesro.centerMapOnUserLocation
import com.nyl.euiesro.common.CompactFilterRow
import com.nyl.euiesro.resolveBestCurrentLocationForDisplay
import com.nyl.euiesro.sortLocationsByDistance
import com.nyl.euiesro.common.CompactLocationListItem
import com.nyl.euiesro.common.CompactSearchField
import com.nyl.euiesro.common.LocationUiItem
import com.nyl.euiesro.common.ModuleSection
import com.nyl.euiesro.features.moto.components.MotoMapOverlayLayout
import org.osmdroid.views.MapView

@Composable
fun MotoSection(
    innerPadding: PaddingValues,
    onOpenLocation: (LocationUiItem) -> Unit,
    onAddMotoPlace: () -> Unit = {}
) {
    val context = LocalContext.current
    var motoPlaces by remember { mutableStateOf<List<MotoPlaceSummary>>(emptyList()) }
    var loading by rememberSaveable { mutableStateOf(true) }
    var error by rememberSaveable { mutableStateOf("") }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedService by rememberSaveable { mutableStateOf<String?>(null) }
    var showFilters by rememberSaveable { mutableStateOf(false) }
    var selectedMapLocation by remember { mutableStateOf<LocationUiItem?>(null) }
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var didCenterOnUserLocation by rememberSaveable { mutableStateOf(false) }
    var hasLocationPermission by rememberSaveable {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    val noMotoPlacesText = stringResource(R.string.moto_no_places)
    val serviceOptions = listOf(
        "magazine" to stringResource(R.string.moto_service_magazine),
        "vulcanizare" to stringResource(R.string.moto_service_vulcanizare),
        "moto" to stringResource(R.string.moto_service_service),
        "itp" to stringResource(R.string.moto_service_itp),
        "popas" to stringResource(R.string.moto_service_popas),
        "diverse" to stringResource(R.string.moto_service_diverse),
        "adunari" to stringResource(R.string.moto_service_adunari)
    )

    LaunchedEffect(hasLocationPermission, mapView) {
        if (hasLocationPermission && mapView != null && !didCenterOnUserLocation) {
            centerMapOnUserLocation(mapView)
            didCenterOnUserLocation = true
        }
    }

    LaunchedEffect(Unit) {
        loading = true
        error = ""
        motoPlaces = MotoApi.fetchMotoPlaces(limit = 500)
        loading = false
        if (motoPlaces.isEmpty()) error = noMotoPlacesText
    }

    val filteredPlaces = remember(motoPlaces, searchQuery, selectedService) {
        motoPlaces.filter { place ->
            val matchesService = selectedService == null ||
                place.services.any { service ->
                    service.equals(selectedService, ignoreCase = true)
                }
            val matchesSearch = SearchUtils.matchesSearch(
                searchQuery,
                place.title,
                place.address,
                place.description
            )
            matchesService && matchesSearch
        }
    }

    val userLocation = remember(context) { resolveBestCurrentLocationForDisplay(context) }
    val items = sortLocationsByDistance(
        filteredPlaces.map { place ->
            LocationUiItem(
                id = place.id,
                section = ModuleSection.MOTO,
                title = place.title,
                description = place.description,
                address = place.address,
                latitude = place.latitude,
                longitude = place.longitude,
                phone = place.phone,
                website = place.website,
                email = place.email,
                services = place.services,
                imageUrls = listOfNotNull(place.primaryImage),
                typeIds = emptyList(),
                markerIcons = com.nyl.euiesro.SiteMarkerIconResolver.resolveMotoMarkerIcons(place.services)
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
            MotoMapOverlayLayout(
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                onToggleFilters = { showFilters = !showFilters },
                filterOptions = serviceOptions,
                selectedService = selectedService,
                onFilterSelected = { selectedService = it },
                showFilters = showFilters,
                items = items,
                places = filteredPlaces,
                userLocation = userLocation,
                onOpenLocation = onOpenLocation,
                onAddMotoPlace = onAddMotoPlace,
                onMapReady = { mapView = it },
                onCenterLocation = { sheetRatio ->
                    if (!hasLocationPermission) {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                        return@MotoMapOverlayLayout
                    }
                    centerMapOnUserLocation(mapView, sheetRatio)
                },
                onZoomIn = { mapView?.controller?.zoomIn() },
                onZoomOut = { mapView?.controller?.zoomOut() },
                modifier = Modifier.fillMaxSize()
            )
        }
    }

}

@Composable
private fun MotoLocationCard(place: MotoPlaceSummary, onClick: () -> Unit) {
    val statusLabel = if (place.isOpenNow()) "Deschis" else "Închis"
    val serviceChips = place.services.take(2)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = place.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp
                )

                if (!place.address.isNullOrBlank()) {
                    Text(
                        text = place.address,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "★ ${"%.1f".format(place.averageRating)}",
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.sp
                    )

                    Text(
                        text = "(${place.reviewsCount} recenzii)",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 9.sp
                    )
                }

                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = if (place.isOpenNow()) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                        tonalElevation = 0.dp
                    ) {
                        Text(
                            text = statusLabel,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            color = if (place.isOpenNow()) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Medium,
                            fontSize = 9.sp
                        )
                    }

                    serviceChips.forEach { chip ->
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            tonalElevation = 0.dp
                        ) {
                            Text(
                                text = chip,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontWeight = FontWeight.Medium,
                                fontSize = 9.sp
                            )
                        }
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primary,
                tonalElevation = 0.dp,
                modifier = Modifier.size(36.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
