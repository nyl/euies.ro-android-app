package com.nyl.euiesro.common.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.nyl.euiesro.InteractiveLocationMap
import com.nyl.euiesro.common.LocationUiItem
import org.osmdroid.views.MapView

@Composable
fun MapBottomSheetLayout(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    searchPlaceholder: String = "Caută locație",
    onFilterClick: (() -> Unit)? = null,
    onCloseFilter: (() -> Unit)? = null,
    filterContent: (@Composable ColumnScope.() -> Unit)? = null,
    useFilterModal: Boolean = false,
    items: List<LocationUiItem>,
    onMapReady: (MapView) -> Unit,
    onCenterLocation: (Float) -> Unit,
    onZoomIn: () -> Unit = {},
    onZoomOut: () -> Unit = {},
    showUserLocation: Boolean = true,
    sheetHeader: (@Composable ColumnScope.() -> Unit)? = null,
    sheetContent: (@Composable ColumnScope.() -> Unit)? = null,
    onAddAction: (() -> Unit)? = null,
    extraActions: (@Composable ColumnScope.() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val minSheetRatio = 0.32f
    val midSheetRatio = 0.6f
    val maxSheetRatio = 0.98f
    var sheetRatio by remember { mutableFloatStateOf(midSheetRatio) }
    var isSatelliteView by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val maxSheetHeightPx = with(LocalDensity.current) { maxHeight.toPx() }
        val sheetHeight = (maxHeight * sheetRatio.coerceIn(minSheetRatio, maxSheetRatio)).coerceAtLeast(180.dp)

        Box(modifier = Modifier.fillMaxSize()) {
            InteractiveLocationMap(
                items = items,
                modifier = Modifier.fillMaxSize(),
                interactive = true,
                showUserLocation = showUserLocation,
                tileSource = if (isSatelliteView) "satellite" else "standard",
                onMapReady = onMapReady
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MapSearchField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = searchPlaceholder,
                    onFilterClick = onFilterClick
                )

                if (filterContent != null && !useFilterModal) {
                    filterContent()
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 102.dp, end = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MapControlButton(
                    icon = Icons.Default.MyLocation,
                    contentDescription = "Center location",
                    onClick = { onCenterLocation(sheetRatio) }
                )
                MapControlButton(
                    icon = Icons.Default.Add,
                    contentDescription = "Zoom in",
                    onClick = onZoomIn
                )
                MapControlButton(
                    icon = Icons.Default.Remove,
                    contentDescription = "Zoom out",
                    onClick = onZoomOut
                )
                MapControlButton(
                    icon = Icons.Default.Map,
                    contentDescription = "Toggle map layer",
                    onClick = { isSatelliteView = !isSatelliteView }
                )

                if (extraActions != null) {
                    extraActions()
                }
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(sheetHeight)
                    .padding(top = 2.dp),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                tonalElevation = 0.dp,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(30.dp)
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = {},
                                    onDragEnd = {
                                        val finalRatio = when {
                                            sheetRatio < 0.46f -> minSheetRatio
                                            sheetRatio > 0.76f -> maxSheetRatio
                                            else -> midSheetRatio
                                        }
                                        sheetRatio = finalRatio
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        val nextRatio = sheetRatio - (dragAmount.y / maxSheetHeightPx.coerceAtLeast(1f))
                                        sheetRatio = nextRatio.coerceIn(minSheetRatio, maxSheetRatio)
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        MapSheetHandle()
                    }

                    if (sheetHeader != null) {
                        sheetHeader()
                    }

                    if (sheetContent != null) {
                        sheetContent()
                    }
                }
            }

            AnimatedVisibility(
                visible = filterContent != null && useFilterModal,
                enter = slideInVertically(initialOffsetY = { it / 3 }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it / 3 }) + fadeOut(),
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.TopCenter)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 18.dp)
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.35f)
                    ) {}

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.82f)
                            .align(Alignment.TopCenter)
                            .padding(horizontal = 14.dp, vertical = 16.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopCenter),
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
                            tonalElevation = 8.dp,
                            shadowElevation = 12.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    IconButton(
                                        onClick = { onCloseFilter?.invoke() ?: onFilterClick?.invoke() },
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Close filters",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    filterContent?.invoke(this)
                                }
                            }
                        }
                    }
                }
            }

            if (onAddAction != null) {
                FloatingActionButton(
                    onClick = onAddAction,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 18.dp, bottom = 18.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Adaugă locație"
                    )
                }
            }
        }
    }
}
