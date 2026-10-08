package com.nyl.euiesro.features.launchspots

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.nyl.euiesro.R
import com.nyl.euiesro.SearchUtils
import com.nyl.euiesro.centerMapOnUserLocation
import com.nyl.euiesro.common.GroupedTypeCategory
import com.nyl.euiesro.common.GroupedTypeOption
import com.nyl.euiesro.common.GroupedTypeSelector
import com.nyl.euiesro.common.LocationUiItem
import com.nyl.euiesro.common.ModuleSection
import com.nyl.euiesro.common.map.MapBottomSheetLayout
import org.osmdroid.views.MapView

@Composable
fun LaunchSpotsSection(
    innerPadding: androidx.compose.foundation.layout.PaddingValues,
    onOpenLocation: (LocationUiItem) -> Unit,
    onAddLaunchSpot: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var launchSpots by remember { mutableStateOf<List<LaunchSpotSummary>>(emptyList()) }
    var loading by rememberSaveable { mutableStateOf(true) }
    var error by rememberSaveable { mutableStateOf("") }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedTypes by rememberSaveable { mutableStateOf<Set<Int>>(emptySet()) }
    var selectedSurfaces by rememberSaveable { mutableStateOf<Set<String>>(emptySet()) }
    var selectedDepths by rememberSaveable { mutableStateOf<Set<String>>(emptySet()) }
    var selectedAccessibilities by rememberSaveable { mutableStateOf<Set<String>>(emptySet()) }
    var showFilters by rememberSaveable { mutableStateOf(false) }
    var showFullscreenMap by rememberSaveable { mutableStateOf(false) }
    var mapView by remember { mutableStateOf<MapView?>(null) }

    LaunchedEffect(Unit) {
        loading = true
        error = ""
        launchSpots = LaunchSpotApi.fetchLaunchSpots(limit = 500)
        loading = false
        if (launchSpots.isEmpty()) error = "No launch spots available right now."
    }

    val filteredLaunchSpots = remember(launchSpots, searchQuery, selectedTypes, selectedSurfaces, selectedDepths, selectedAccessibilities) {
        launchSpots.filter { current ->
            val matchesQuery = SearchUtils.matchesSearch(searchQuery, current.title, current.address, current.description)
            val matchesType = selectedTypes.isEmpty() || current.typeIds.any { it in selectedTypes }
            val matchesSurface = selectedSurfaces.isEmpty() || (current.surface != null && current.surface in selectedSurfaces)
            val matchesDepth = selectedDepths.isEmpty() || (current.depth != null && current.depth in selectedDepths)
            val matchesAccessibility = selectedAccessibilities.isEmpty() || (current.accessibility != null && current.accessibility in selectedAccessibilities)
            matchesQuery && matchesType && matchesSurface && matchesDepth && matchesAccessibility
        }
    }

    val typeOptions = launchSpots
        .flatMap { it.typeOptions }
        .distinctBy { it.id }
        .sortedBy { it.label.lowercase() }

    val surfaceOptions = launchSpots
        .mapNotNull { it.surface }
        .distinct()
        .sortedBy { it.lowercase() }

    val depthOptions = launchSpots
        .mapNotNull { it.depth }
        .distinct()
        .sortedBy { it.lowercase() }

    val accessibilityOptions = launchSpots
        .mapNotNull { it.accessibility }
        .distinct()
        .sortedBy { it.lowercase() }

    val filterTypeGroups = remember { LaunchSpotApi.fetchLaunchSpotTypeGroupsSync() }
        .map { group ->
            GroupedTypeCategory(
                category = group.category,
                options = group.options.map { option ->
                    GroupedTypeOption(value = option.id.toString(), label = option.label)
                }
            )
        }
    val surfaceFilterGroups = listOf(
        GroupedTypeCategory(
            category = stringResource(R.string.surface_label),
            options = surfaceOptions.map { value ->
                GroupedTypeOption(value = value, label = humanizeLaunchSpotFilterValue(value))
            }
        )
    )
    val depthFilterGroups = listOf(
        GroupedTypeCategory(
            category = stringResource(R.string.depth_label),
            options = depthOptions.map { value ->
                GroupedTypeOption(value = value, label = humanizeLaunchSpotFilterValue(value))
            }
        )
    )
    val accessibilityFilterGroups = listOf(
        GroupedTypeCategory(
            category = stringResource(R.string.accessibility_label),
            options = accessibilityOptions.map { value ->
                GroupedTypeOption(value = value, label = humanizeLaunchSpotFilterValue(value))
            }
        )
    )

    val launchSpotMap = launchSpots.associateBy { it.id }

    val userLocation = remember(context) { com.nyl.euiesro.resolveBestCurrentLocationForDisplay(context) }
    val items = com.nyl.euiesro.sortLocationsByDistance(
        filteredLaunchSpots.map { place ->
            LocationUiItem(
                id = place.id,
                section = ModuleSection.LAUNCH_SPOTS,
                title = place.title,
                description = place.description,
                address = place.address,
                latitude = place.latitude,
                longitude = place.longitude,
                phone = null,
                website = null,
                email = null,
                services = emptyList(),
                imageUrls = place.images,
                typeIds = place.typeIds,
                markerIcons = com.nyl.euiesro.SiteMarkerIconResolver.resolveLaunchSpotMarkerIcons(place.markerIcons)
            )
        },
        userLocation
    )

    val hasActiveFilters = selectedTypes.isNotEmpty() || selectedSurfaces.isNotEmpty() || selectedDepths.isNotEmpty() || selectedAccessibilities.isNotEmpty()

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
            MapBottomSheetLayout(
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                searchPlaceholder = "Caută launch spot",
                onFilterClick = if (filterTypeGroups.isNotEmpty() || surfaceFilterGroups.first().options.isNotEmpty() || depthFilterGroups.first().options.isNotEmpty() || accessibilityFilterGroups.first().options.isNotEmpty()) { { showFilters = !showFilters } } else null,
                onCloseFilter = { showFilters = false },
                useFilterModal = true,
                filterContent = if (showFilters) {
                    {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            GroupedTypeSelector(
                                title = stringResource(R.string.type_label),
                                groups = filterTypeGroups,
                                selectedValues = selectedTypes.map { it.toString() }.toSet(),
                                onToggle = { value ->
                                    val nextValue = value.toIntOrNull() ?: return@GroupedTypeSelector
                                    selectedTypes = if (selectedTypes.contains(nextValue)) {
                                        selectedTypes - nextValue
                                    } else {
                                        selectedTypes + nextValue
                                    }
                                }
                            )

                            if (surfaceFilterGroups.first().options.isNotEmpty()) {
                                GroupedTypeSelector(
                                    title = stringResource(R.string.surface_label),
                                    groups = surfaceFilterGroups,
                                    selectedValues = selectedSurfaces,
                                    onToggle = { value ->
                                        selectedSurfaces = if (selectedSurfaces.contains(value)) {
                                            selectedSurfaces - value
                                        } else {
                                            selectedSurfaces + value
                                        }
                                    }
                                )
                            }

                            if (depthFilterGroups.first().options.isNotEmpty()) {
                                GroupedTypeSelector(
                                    title = stringResource(R.string.depth_label),
                                    groups = depthFilterGroups,
                                    selectedValues = selectedDepths,
                                    onToggle = { value ->
                                        selectedDepths = if (selectedDepths.contains(value)) {
                                            selectedDepths - value
                                        } else {
                                            selectedDepths + value
                                        }
                                    }
                                )
                            }

                            if (accessibilityFilterGroups.first().options.isNotEmpty()) {
                                GroupedTypeSelector(
                                    title = stringResource(R.string.accessibility_label),
                                    groups = accessibilityFilterGroups,
                                    selectedValues = selectedAccessibilities,
                                    onToggle = { value ->
                                        selectedAccessibilities = if (selectedAccessibilities.contains(value)) {
                                            selectedAccessibilities - value
                                        } else {
                                            selectedAccessibilities + value
                                        }
                                    }
                                )
                            }
                        }
                    }
                } else null,
                items = items,
                onMapReady = { mapView = it },
                onCenterLocation = { sheetRatio -> centerMapOnUserLocation(mapView, sheetRatio) },
                onZoomIn = { mapView?.controller?.zoomIn() },
                onZoomOut = { mapView?.controller?.zoomOut() },
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
                        if (hasActiveFilters) {
                            OutlinedButton(
                                onClick = {
                                    selectedTypes = emptySet()
                                    selectedSurfaces = emptySet()
                                    selectedDepths = emptySet()
                                    selectedAccessibilities = emptySet()
                                },
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text(stringResource(R.string.clear_action))
                            }
                        }
                    }
                },
                sheetContent = {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(items) { item ->
                            val place = filteredLaunchSpots.firstOrNull { it.id == item.id }
                            val distanceText = userLocation?.let {
                                com.nyl.euiesro.calculateDistanceMeters(
                                    userLocation.latitude,
                                    userLocation.longitude,
                                    item.latitude,
                                    item.longitude
                                )?.let { meters -> com.nyl.euiesro.formatDistanceLabel(meters) }
                            }
                            if (place != null) {
                                LaunchSpotMapItem(
                                    place = place,
                                    typeLabels = typeOptions.associate { it.id to it.label },
                                    distanceText = distanceText,
                                    onClick = { onOpenLocation(item) }
                                )
                            }
                        }
                    }
                },
                onAddAction = onAddLaunchSpot,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

private fun humanizeLaunchSpotFilterValue(value: String): String {
    return value
        .replace('_', ' ')
        .split(' ')
        .filter { it.isNotBlank() }
        .joinToString(" ") { segment ->
            segment.lowercase().replaceFirstChar { it.uppercase() }
        }
        .ifBlank { value }
}

private fun countFilters(
    selectedTypes: Set<Int>,
    selectedSurfaces: Set<String>,
    selectedDepths: Set<String>,
    selectedAccessibilities: Set<String>
): Int {
    return selectedTypes.size + selectedSurfaces.size + selectedDepths.size + selectedAccessibilities.size
}

@Composable
private fun LaunchSpotMapItem(
    place: LaunchSpotSummary,
    typeLabels: Map<Int, String> = emptyMap(),
    distanceText: String? = null,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (place.images.isNotEmpty()) {
                AsyncImage(
                    model = place.images.first(),
                    contentDescription = place.title,
                    modifier = Modifier
                        .size(58.dp)
                        .clickable(onClick = onClick),
                    contentScale = ContentScale.Crop
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = place.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp
                )

                place.address?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                distanceText?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "★ ${"%.1f".format(place.averageRating)}",
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "(${place.reviewsCount} recenzii)",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                }

                if (place.typeIds.isNotEmpty()) {
                    Row(
                        modifier = Modifier.padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        place.typeIds.take(2).forEach { typeId ->
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                tonalElevation = 0.dp
                            ) {
                                Text(
                                    text = typeLabels[typeId] ?: "Type $typeId",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
