package com.nyl.euiesro.features.auto.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nyl.euiesro.common.LocationUiItem
import com.nyl.euiesro.common.map.MapBottomSheetLayout
import com.nyl.euiesro.common.map.MapFilterRow
import com.nyl.euiesro.features.auto.AutoPlaceSummary
import org.osmdroid.views.MapView

@Composable
fun AutoMapOverlayLayout(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onToggleFilters: () -> Unit,
    filterOptions: List<Pair<String, String>>,
    selectedType: Int?,
    onFilterSelected: (Int?) -> Unit,
    showFilters: Boolean,
    items: List<LocationUiItem>,
    places: List<AutoPlaceSummary>,
    userLocation: android.location.Location? = null,
    onOpenLocation: (LocationUiItem) -> Unit,
    onAddAutoPlace: () -> Unit,
    onMapReady: (MapView) -> Unit,
    onCenterLocation: (Float) -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    MapBottomSheetLayout(
        searchQuery = searchQuery,
        onSearchQueryChange = onSearchQueryChange,
        searchPlaceholder = "Caută auto",
        onFilterClick = if (filterOptions.isNotEmpty()) onToggleFilters else null,
        filterContent = if (showFilters) {
            {
                MapFilterRow(
                    items = filterOptions,
                    selectedKey = selectedType?.toString(),
                    onSelect = { selectedTypeKey ->
                        onFilterSelected(selectedTypeKey?.toIntOrNull())
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else null,
        items = items,
        onMapReady = onMapReady,
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
                        AutoLocationSheetItem(
                            place = place,
                            distanceText = distanceText,
                            onClick = { onOpenLocation(item) }
                        )
                    }
                }
            }
        },
        onAddAction = onAddAutoPlace,
        modifier = modifier.fillMaxSize()
    )
}
