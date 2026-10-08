package com.nyl.euiesro.features.roadassistance.common

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nyl.euiesro.R
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.preference.PreferenceManager
import com.nyl.euiesro.resolveBestCurrentLocationForDisplay
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon

@Composable
fun RoadAssistanceFullscreenBoundaryMapDialog(
    latitude: String,
    longitude: String,
    coverageBoundary: String,
    onDismiss: () -> Unit,
    onBoundaryChanged: (String) -> Unit,
    onLocationChanged: (Double, Double) -> Unit,
) {
    val context = LocalContext.current
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues()
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues()
    val boundaryMarkerStates = remember { mutableStateOf<List<Marker>>(emptyList()) }
    val polygonState = remember { mutableStateOf<Polygon?>(null) }
    val localBoundaryPoints = rememberSaveable { mutableStateOf(RoadAssistanceBoundaryMapUtils.parseBoundary(coverageBoundary)) }
    var selectedBoundaryIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    var draggingBoundaryIndex by remember { mutableStateOf<Int?>(null) }
    var viewportInitialized by remember { mutableStateOf(false) }
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }

    fun updateLocalBoundary(points: List<Pair<Double, Double>>) {
        localBoundaryPoints.value = points
    }

    val flushBoundaryToParent: () -> Unit = {
        val serialized = RoadAssistanceBoundaryMapUtils.serializeBoundary(localBoundaryPoints.value)
        localBoundaryPoints.value = RoadAssistanceBoundaryMapUtils.parseBoundary(serialized)
        if (serialized.isNotBlank()) {
            onBoundaryChanged(serialized)
        } else {
            onBoundaryChanged("")
        }
    }

    fun deleteSelectedPoint() {
        val index = selectedBoundaryIndex ?: return
        val updated = RoadAssistanceBoundaryMapUtils.removeBoundaryPoint(localBoundaryPoints.value, index)
        updateLocalBoundary(updated)
        selectedBoundaryIndex = if (updated.isEmpty()) null else index.coerceAtMost(updated.lastIndex)
    }

    var showClearConfirmation by remember { mutableStateOf(false) }

    fun clearBoundary() {
        updateLocalBoundary(emptyList())
        selectedBoundaryIndex = null
        showClearConfirmation = false
    }

    fun zoomToCurrentLocation() {
        val currentLocation = resolveBestCurrentLocationForDisplay(context) ?: return
        mapViewRef?.controller?.animateTo(GeoPoint(currentLocation.latitude, currentLocation.longitude))
        mapViewRef?.controller?.setZoom(15.0)
    }

    fun closeEditor(saveChanges: Boolean = true) {
        if (saveChanges) {
            flushBoundaryToParent()
        }
        onDismiss()
    }

    LaunchedEffect(coverageBoundary) {
        val initialPoints = RoadAssistanceBoundaryMapUtils.parseBoundary(coverageBoundary)
        if (initialPoints.isNotEmpty() && localBoundaryPoints.value != initialPoints) {
            localBoundaryPoints.value = initialPoints
        }
    }

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(
                        top = statusBarPadding.calculateTopPadding(),
                        bottom = navBarPadding.calculateBottomPadding() + 8.dp
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = stringResource(R.string.road_assistance_boundary_editor),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                        Text(
                            text = if (localBoundaryPoints.value.size >= 3) {
                                stringResource(R.string.road_assistance_points_status, localBoundaryPoints.value.size)
                            } else {
                                stringResource(R.string.road_assistance_boundary_hint)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = { closeEditor(saveChanges = true) },
                            shape = RoundedCornerShape(999.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text(stringResource(R.string.road_assistance_done))
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (selectedBoundaryIndex != null && localBoundaryPoints.value.isNotEmpty()) {
                        Button(
                            onClick = { deleteSelectedPoint() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(999.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        ) {
                            Text(stringResource(R.string.road_assistance_delete_selected))
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    if (localBoundaryPoints.value.isNotEmpty()) {
                        Button(
                            onClick = { showClearConfirmation = true },
                            modifier = Modifier.weight(if (selectedBoundaryIndex != null && localBoundaryPoints.value.isNotEmpty()) 1f else 0.4f),
                            shape = RoundedCornerShape(999.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Text(stringResource(R.string.road_assistance_clear))
                        }
                    }
                }

                if (showClearConfirmation) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            tonalElevation = 2.dp,
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.96f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = { clearBoundary() },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(999.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.onError
                                    )
                                ) {
                                    Text(stringResource(R.string.road_assistance_confirm_clear))
                                }

                                Button(
                                    onClick = { showClearConfirmation = false },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(999.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    )
                                ) {
                                    Text(stringResource(R.string.road_assistance_cancel))
                                }
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            Configuration.getInstance().load(
                                ctx.applicationContext,
                                PreferenceManager.getDefaultSharedPreferences(ctx.applicationContext)
                            )

                            val map = MapView(ctx).apply {
                                setTileSource(TileSourceFactory.MAPNIK)
                                setMultiTouchControls(true)
                                setHorizontalMapRepetitionEnabled(false)
                                setVerticalMapRepetitionEnabled(false)
                                minZoomLevel = 3.0
                                maxZoomLevel = 20.0
                                zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)
                                controller.setZoom(10.0)
                                controller.setCenter(GeoPoint(45.9432, 24.9668))
                                setScrollableAreaLimitDouble(BoundingBox(85.0, 180.0, -85.0, -180.0))
                            }

                            mapViewRef = map

                            val mapEventsReceiver = object : MapEventsReceiver {
                                override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                                    if (p == null) return false
                                    val updated = RoadAssistanceBoundaryMapUtils.addBoundaryPoint(
                                        localBoundaryPoints.value,
                                        p.latitude,
                                        p.longitude
                                    )
                                    updateLocalBoundary(updated)
                                    selectedBoundaryIndex = updated.lastIndex
                                    return true
                                }

                                override fun longPressHelper(p: GeoPoint?): Boolean {
                                    if (p == null) return false
                                    val nearestIndex = localBoundaryPoints.value
                                        .withIndex()
                                        .minByOrNull { (_, point) ->
                                            val latDiff = point.first - p.latitude
                                            val lonDiff = point.second - p.longitude
                                            latDiff * latDiff + lonDiff * lonDiff
                                        }
                                        ?.index ?: return false
                                    val updated = RoadAssistanceBoundaryMapUtils.removeBoundaryPoint(localBoundaryPoints.value, nearestIndex)
                                    updateLocalBoundary(updated)
                                    selectedBoundaryIndex = nearestIndex.coerceAtMost((updated.size - 1).coerceAtLeast(0))
                                    return true
                                }
                            }
                            map.overlays.add(MapEventsOverlay(mapEventsReceiver))

                            map
                        },
                        update = { map ->
                            mapViewRef = map
                            val targetLat = latitude.toDoubleOrNull() ?: 45.9432
                            val targetLng = longitude.toDoubleOrNull() ?: 24.9668
                            val targetPoint = GeoPoint(targetLat, targetLng)

                            val currentBoundaryPoints = localBoundaryPoints.value
                            if (boundaryMarkerStates.value.size != currentBoundaryPoints.size) {
                                boundaryMarkerStates.value.forEach { oldMarker -> map.overlays.remove(oldMarker) }
                                boundaryMarkerStates.value = currentBoundaryPoints.mapIndexed { index, point ->
                                    Marker(map).apply {
                                        position = GeoPoint(point.first, point.second)
                                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                                        icon = createBoundaryMarkerDrawable(map.context, isStart = index == 0, isSelected = selectedBoundaryIndex == index)
                                        isDraggable = true
                                        infoWindow = null
                                        setOnMarkerClickListener { _, _ ->
                                            selectedBoundaryIndex = index
                                            true
                                        }
                                        setOnMarkerDragListener(object : Marker.OnMarkerDragListener {
                                            override fun onMarkerDrag(marker: Marker) {
                                                val updated = localBoundaryPoints.value.toMutableList()
                                                updated[index] = marker.position.latitude to marker.position.longitude
                                                localBoundaryPoints.value = updated
                                                selectedBoundaryIndex = index
                                                draggingBoundaryIndex = index
                                            }
                                            override fun onMarkerDragEnd(marker: Marker) {
                                                draggingBoundaryIndex = null
                                                val updated = localBoundaryPoints.value.toMutableList()
                                                updated[index] = marker.position.latitude to marker.position.longitude
                                                selectedBoundaryIndex = index
                                                updateLocalBoundary(updated)
                                            }
                                            override fun onMarkerDragStart(marker: Marker) {
                                                draggingBoundaryIndex = index
                                            }
                                        })
                                    }
                                }
                                boundaryMarkerStates.value.forEach { marker -> map.overlays.add(marker) }
                            } else {
                                currentBoundaryPoints.forEachIndexed { index, point ->
                                    val marker = boundaryMarkerStates.value.getOrNull(index) ?: return@forEachIndexed
                                    if (draggingBoundaryIndex != index) {
                                        marker.position = GeoPoint(point.first, point.second)
                                    }
                                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                                    marker.icon = createBoundaryMarkerDrawable(map.context, isStart = index == 0, isSelected = selectedBoundaryIndex == index)
                                    marker.isDraggable = true
                                    marker.infoWindow = null
                                }
                            }

                            if (currentBoundaryPoints.size >= 3) {
                                val polygon = Polygon(map).apply {
                                    fillPaint.color = 0x4080A9FF.toInt()
                                    outlinePaint.color = 0xFF2563EB.toInt()
                                    outlinePaint.strokeWidth = 2f
                                    points = currentBoundaryPoints.map { GeoPoint(it.first, it.second) }
                                }
                                polygonState.value?.let { oldPolygon -> map.overlays.remove(oldPolygon) }
                                polygonState.value = polygon
                                map.overlays.add(polygon)
                            } else {
                                polygonState.value?.let { oldPolygon -> map.overlays.remove(oldPolygon) }
                                polygonState.value = null
                            }

                            if (!viewportInitialized) {
                                val viewportPoints = currentBoundaryPoints.map { GeoPoint(it.first, it.second) } + targetPoint
                                if (viewportPoints.size > 1) {
                                    val bounds = BoundingBox.fromGeoPoints(viewportPoints)
                                    val latSpan = (bounds.latNorth - bounds.latSouth).coerceAtLeast(0.02)
                                    val lonSpan = (bounds.lonEast - bounds.lonWest).coerceAtLeast(0.02)
                                    map.controller.setCenter(
                                        GeoPoint(
                                            (bounds.latNorth + bounds.latSouth) / 2.0,
                                            (bounds.lonEast + bounds.lonWest) / 2.0
                                        )
                                    )
                                    map.controller.zoomToSpan(latSpan, lonSpan)
                                } else {
                                    map.controller.setCenter(targetPoint)
                                    map.controller.setZoom(14.0)
                                }
                                viewportInitialized = true
                            }

                            map.invalidate()
                        }
                    )

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 14.dp, bottom = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            FloatingActionButton(
                                onClick = { mapViewRef?.controller?.zoomIn() },
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Zoom in")
                            }
                            FloatingActionButton(
                                onClick = { mapViewRef?.controller?.zoomOut() },
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Zoom out")
                            }
                        }

                        FloatingActionButton(
                            onClick = { zoomToCurrentLocation() },
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(Icons.Default.MyLocation, contentDescription = "Zoom to my location")
                        }
                    }
                }

            }
        }
    }
}

private fun createBoundaryMarkerDrawable(
    context: Context,
    isStart: Boolean,
    isSelected: Boolean
): android.graphics.drawable.Drawable {
    val size = (28 * context.resources.displayMetrics.density).toInt().coerceAtLeast(28)
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val center = size / 2f
    val boundaryStrokeWidth = (2.5f * context.resources.displayMetrics.density).coerceAtLeast(2f)

    val halo = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = if (isStart) android.graphics.Color.argb(150, 16, 185, 129) else android.graphics.Color.argb(150, 8, 145, 178)
    }
    val fill = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = if (isStart) android.graphics.Color.argb(255, 34, 197, 94) else android.graphics.Color.argb(255, 255, 255, 255)
    }
    val border = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = boundaryStrokeWidth
        color = if (isStart) android.graphics.Color.argb(255, 22, 101, 52) else android.graphics.Color.argb(255, 15, 118, 110)
    }
    val inner = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = if (isStart) android.graphics.Color.argb(255, 255, 255, 255) else android.graphics.Color.argb(255, 24, 94, 83)
    }

    val outerRadius = size * 0.44f
    val innerRadius = size * 0.24f

    canvas.drawCircle(center, center, outerRadius, halo)
    canvas.drawCircle(center, center, outerRadius * 0.80f, fill)
    canvas.drawCircle(center, center, outerRadius * 0.80f, border)
    canvas.drawCircle(center, center, innerRadius, inner)

    if (isSelected) {
        val selection = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = boundaryStrokeWidth
            color = android.graphics.Color.argb(255, 59, 130, 246)
        }
        canvas.drawCircle(center, center, outerRadius + boundaryStrokeWidth * 2, selection)
    }

    return BitmapDrawable(context.resources, bitmap)
}
