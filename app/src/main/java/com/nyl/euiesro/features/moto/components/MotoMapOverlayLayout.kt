package com.nyl.euiesro.features.moto.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.nyl.euiesro.common.LocationUiItem
import com.nyl.euiesro.common.map.MapBottomSheetLayout
import com.nyl.euiesro.common.map.MapFilterRow
import com.nyl.euiesro.features.moto.MotoPlaceSummary
import org.osmdroid.views.MapView

@Composable
fun MotoMapOverlayLayout(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onToggleFilters: () -> Unit,
    filterOptions: List<Pair<String, String>>,
    selectedService: String?,
    onFilterSelected: (String?) -> Unit,
    showFilters: Boolean,
    items: List<LocationUiItem>,
    places: List<MotoPlaceSummary>,
    userLocation: android.location.Location? = null,
    onOpenLocation: (LocationUiItem) -> Unit,
    onAddMotoPlace: () -> Unit,
    onMapReady: (MapView) -> Unit,
    onCenterLocation: (Float) -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    MapBottomSheetLayout(
        searchQuery = searchQuery,
        onSearchQueryChange = onSearchQueryChange,
        searchPlaceholder = "Caută locație",
        onFilterClick = if (filterOptions.isNotEmpty()) onToggleFilters else null,
        filterContent = if (showFilters) {
            {
                MapFilterRow(
                    items = filterOptions,
                    selectedKey = selectedService,
                    onSelect = onFilterSelected,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else null,
        items = items,
        onMapReady = { map ->
            onMapReady(map)
        },
        onCenterLocation = onCenterLocation,
        onZoomIn = onZoomIn,
        onZoomOut = onZoomOut,
        sheetHeader = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
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
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(items) { item ->
                    val place = places.firstOrNull { it.id == item.id }
                    val distanceText = userLocation?.let {
                        com.nyl.euiesro.calculateDistanceMeters(
                            it.latitude,
                            it.longitude,
                            item.latitude,
                            item.longitude
                        )?.let { meters -> com.nyl.euiesro.formatDistanceLabel(meters) }
                    }
                    if (place != null) {
                        MotoLocationSheetItem(
                            place = place,
                            distanceText = distanceText,
                            onClick = { onOpenLocation(item) }
                        )
                    }
                }
            }
        },
        onAddAction = onAddMotoPlace,
        modifier = modifier.fillMaxSize()
    )
}

@Composable
private fun MotoLocationSheetItem(
    place: MotoPlaceSummary,
    distanceText: String? = null,
    onClick: () -> Unit
) {
    val status = place.openingStatus()
    val chipColors = when (status.state) {
        MotoPlaceSummary.OpeningStatus.State.OPEN -> MaterialTheme.colorScheme.primaryContainer
        MotoPlaceSummary.OpeningStatus.State.CLOSING_SOON -> MaterialTheme.colorScheme.tertiaryContainer
        MotoPlaceSummary.OpeningStatus.State.CLOSED -> MaterialTheme.colorScheme.errorContainer
    }
    val chipTextColors = when (status.state) {
        MotoPlaceSummary.OpeningStatus.State.OPEN -> MaterialTheme.colorScheme.onPrimaryContainer
        MotoPlaceSummary.OpeningStatus.State.CLOSING_SOON -> MaterialTheme.colorScheme.onTertiaryContainer
        MotoPlaceSummary.OpeningStatus.State.CLOSED -> MaterialTheme.colorScheme.onErrorContainer
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!place.primaryImage.isNullOrBlank()) {
                AsyncImage(
                    model = place.primaryImage,
                    contentDescription = place.title,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(14.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = place.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (!place.address.isNullOrBlank()) {
                    Text(
                        text = place.address,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                distanceText?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Row(
                    modifier = Modifier.padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "★ ${"%.1f".format(place.averageRating)}",
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "(${place.reviewsCount} recenzii)",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }

                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = chipColors,
                        tonalElevation = 0.dp
                    ) {
                        Text(
                            text = if (status.state == MotoPlaceSummary.OpeningStatus.State.CLOSING_SOON) "Deschis" else status.label,
                            color = chipTextColors,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }

                    place.services.take(2).forEach { chip ->
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            tonalElevation = 0.dp
                        ) {
                            Text(
                                text = chip,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                if (status.detail.isNotBlank()) {
                    Text(
                        text = status.detail,
                        modifier = Modifier.padding(top = 8.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Normal,
                        fontSize = 11.sp
                    )
                }
            }

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                tonalElevation = 0.dp,
                modifier = Modifier.size(40.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

