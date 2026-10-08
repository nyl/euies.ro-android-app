package com.nyl.euiesro.features.auto

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.nyl.euiesro.R
import com.nyl.euiesro.SearchUtils
import com.nyl.euiesro.centerMapOnUserLocation
import com.nyl.euiesro.common.LocationUiItem
import com.nyl.euiesro.resolveBestCurrentLocationForDisplay
import com.nyl.euiesro.sortLocationsByDistance
import com.nyl.euiesro.common.ModuleSection
import com.nyl.euiesro.features.auto.components.AutoMapOverlayLayout
import org.osmdroid.views.MapView

@Composable
fun AutoListScreen(
    innerPadding: PaddingValues,
    onOpenLocation: (LocationUiItem) -> Unit,
    onAddAutoPlace: () -> Unit = {}
) {
    val context = LocalContext.current
    val userLocation = remember(context) { resolveBestCurrentLocationForDisplay(context) }
    var autoPlaces by remember { mutableStateOf<List<AutoPlaceSummary>>(emptyList()) }
    var loading by rememberSaveable { mutableStateOf(true) }
    var error by rememberSaveable { mutableStateOf("") }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedType by rememberSaveable { mutableStateOf<Int?>(null) }
    var showFilters by rememberSaveable { mutableStateOf(false) }
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var didCenterOnUserLocation by rememberSaveable { mutableStateOf(false) }
    val noAutoPlacesText = stringResource(R.string.no_auto_places)

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

    LaunchedEffect(Unit) {
        loading = true
        error = ""
        autoPlaces = AutoApi.fetchAutoPlaces(limit = 500)
        loading = false
        if (autoPlaces.isEmpty()) error = noAutoPlacesText
    }

    LaunchedEffect(hasLocationPermission, mapView) {
        if (hasLocationPermission && mapView != null && !didCenterOnUserLocation) {
            centerMapOnUserLocation(mapView)
            didCenterOnUserLocation = true
        }
    }

    val typeOptions = remember(autoPlaces) {
        autoPlaces
            .flatMap { it.typeOptions }
            .distinctBy { it.id }
            .sortedBy { it.label.lowercase() }
    }

    val filteredPlaces = remember(autoPlaces, searchQuery, selectedType) {
        autoPlaces.filter { place ->
            val matchesType = selectedType == null || place.typeIds.contains(selectedType)
            val matchesSearch = SearchUtils.matchesSearch(
                searchQuery,
                place.title,
                place.address,
                place.description
            )
            matchesType && matchesSearch
        }
    }

    val items = sortLocationsByDistance(
        filteredPlaces.map { place ->
            LocationUiItem(
                id = place.id,
                section = ModuleSection.AUTO,
                title = place.title,
                description = place.description,
                address = place.address,
                latitude = place.latitude,
                longitude = place.longitude,
                phone = place.phone,
                website = place.website,
                email = place.email,
                services = place.services,
                imageUrls = place.images,
                typeIds = place.typeIds,
                markerIcons = place.markerIcons
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
            AutoMapOverlayLayout(
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                onToggleFilters = { showFilters = !showFilters },
                filterOptions = typeOptions.map { it.id.toString() to it.label },
                selectedType = selectedType,
                onFilterSelected = { selectedType = it },
                showFilters = showFilters,
                items = items,
                places = filteredPlaces,
                userLocation = userLocation,
                onOpenLocation = onOpenLocation,
                onAddAutoPlace = onAddAutoPlace,
                onMapReady = { mapView = it },
                onCenterLocation = { sheetRatio ->
                    if (!hasLocationPermission) {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                        return@AutoMapOverlayLayout
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
