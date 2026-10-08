package com.nyl.euiesro.features.roadassistance.common

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.view.MotionEvent
import androidx.compose.foundation.background
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.preference.PreferenceManager
import com.nyl.euiesro.resolveBestCurrentLocationSuspend
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon

@Composable
fun RoadAssistanceBoundaryMapCard(
    latitude: String,
    longitude: String,
    boundaryPoints: List<Pair<Double, Double>>,
    onBoundaryChanged: (List<Pair<Double, Double>>) -> Unit,
    onLatitudeLongitudeChanged: (Double, Double) -> Unit,
    onFullscreenClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    mapHeight: Dp = 180.dp,
    title: String = "Locație și arie de acoperire",
    showControls: Boolean = true,
    showUserLocation: Boolean = false,
    isFullscreen: Boolean = false
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val mapState = remember { mutableStateOf<MapView?>(null) }
    val primaryMarkerState = remember { mutableStateOf<Marker?>(null) }
    val userMarkerState = remember { mutableStateOf<Marker?>(null) }
    val boundaryPointStates = remember(boundaryPoints.size) { mutableStateOf<List<Marker>>(emptyList()) }
    val polygonState = remember { mutableStateOf<Polygon?>(null) }
    val liveBoundaryPoints = remember(boundaryPoints) { mutableStateOf(boundaryPoints) }
    var drawingEnabled by remember { mutableStateOf(true) }
    var currentUserLocation by remember { mutableStateOf<GeoPoint?>(null) }

    LaunchedEffect(boundaryPoints) {
        if (boundaryPoints != liveBoundaryPoints.value) {
            liveBoundaryPoints.value = boundaryPoints
        }
    }

    LaunchedEffect(showUserLocation) {
        if (!showUserLocation) {
            currentUserLocation = null
            return@LaunchedEffect
        }
        currentUserLocation = resolveBestCurrentLocationSuspend(context)?.let {
            GeoPoint(it.latitude, it.longitude)
        }
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(if (isFullscreen) 0.dp else 12.dp)) {
            if (!isFullscreen) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (onFullscreenClick != null) {
                        Button(
                            onClick = onFullscreenClick,
                            shape = RoundedCornerShape(999.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text("Edit boundaries")
                        }
                    }
                }
            }

            Box(
                modifier = if (isFullscreen) {
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(top = 12.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surface)
                } else {
                    Modifier
                        .fillMaxWidth()
                        .height(mapHeight)
                        .padding(top = 12.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surface)
                }
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
                            setBuiltInZoomControls(true)
                            setMultiTouchControls(true)
                            setHorizontalMapRepetitionEnabled(false)
                            setVerticalMapRepetitionEnabled(false)
                            minZoomLevel = 3.0
                            maxZoomLevel = 20.0
                            controller.setZoom(10.0)
                            controller.setCenter(GeoPoint(45.9432, 24.9668))
                            setScrollableAreaLimitDouble(BoundingBox(85.0, 180.0, -85.0, -180.0))
                            setOnTouchListener { _, event ->
                                when (event.actionMasked) {
                                    MotionEvent.ACTION_DOWN -> {
                                        parent?.requestDisallowInterceptTouchEvent(true)
                                    }
                                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                        parent?.requestDisallowInterceptTouchEvent(false)
                                    }
                                }
                                false
                            }
                        }

                        val primaryMarker = Marker(map).apply {
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            isDraggable = showControls
                            setOnMarkerDragListener(object : Marker.OnMarkerDragListener {
                                override fun onMarkerDrag(marker: Marker) = Unit
                                override fun onMarkerDragEnd(marker: Marker) {
                                    onLatitudeLongitudeChanged(marker.position.latitude, marker.position.longitude)
                                }
                                override fun onMarkerDragStart(marker: Marker) = Unit
                            })
                        }

                        map.overlays.add(primaryMarker)
                        mapState.value = map
                        primaryMarkerState.value = primaryMarker
                        map
                    },
                    update = { map ->
                        val targetLat = latitude.toDoubleOrNull() ?: 45.9432
                        val targetLng = longitude.toDoubleOrNull() ?: 24.9668
                        val point = GeoPoint(targetLat, targetLng)
                        val currentBoundaryPoints = liveBoundaryPoints.value
                        val viewportSignature = listOf(
                            targetLat.toString(),
                            targetLng.toString(),
                            currentBoundaryPoints.joinToString("|") { "${it.first},${it.second}" }
                        ).joinToString("::")

                        primaryMarkerState.value?.position = point
                        primaryMarkerState.value?.isDraggable = showControls

                        if (showUserLocation) {
                            val userLocation = currentUserLocation
                            if (userLocation != null) {
                                val existingUserMarker = userMarkerState.value
                                if (existingUserMarker == null) {
                                    val marker = Marker(map).apply {
                                        position = userLocation
                                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                                        icon = createUserLocationMarkerDrawable(map.context)
                                    }
                                    userMarkerState.value = marker
                                    map.overlays.add(marker)
                                } else {
                                    existingUserMarker.position = userLocation
                                }
                            } else {
                                userMarkerState.value?.let { map.overlays.remove(it) }
                                userMarkerState.value = null
                            }
                        } else {
                            userMarkerState.value?.let { map.overlays.remove(it) }
                            userMarkerState.value = null
                        }

                        if (currentBoundaryPoints.size >= 3) {
                            val polygon = Polygon(map).apply {
                                fillColor = 0x4080A9FF.toInt()
                                outlinePaint.color = 0xFF2563EB.toInt()
                                outlinePaint.strokeWidth = 2f
                                points = currentBoundaryPoints.map { GeoPoint(it.first, it.second) }
                            }
                            if (polygonState.value != null) {
                                map.overlays.remove(polygonState.value)
                            }
                            map.overlays.add(polygon)
                            polygonState.value = polygon
                        } else {
                            polygonState.value?.let { map.overlays.remove(it) }
                            polygonState.value = null
                        }

                        if (showControls) {
                            val currentBoundaryMarkers = currentBoundaryPoints.mapIndexed { index, boundaryPoint ->
                                val marker = Marker(map).apply {
                                    position = GeoPoint(boundaryPoint.first, boundaryPoint.second)
                                    icon = createBoundaryMarkerDrawable(map.context, isStart = index == 0, isSelected = false)
                                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                                    isDraggable = true
                                    setOnMarkerDragListener(object : Marker.OnMarkerDragListener {
                                        override fun onMarkerDrag(marker: Marker) = Unit
                                        override fun onMarkerDragEnd(marker: Marker) {
                                            val updated = currentBoundaryPoints.toMutableList()
                                            updated[index] = marker.position.latitude to marker.position.longitude
                                            liveBoundaryPoints.value = updated
                                            onBoundaryChanged(updated)
                                        }
                                        override fun onMarkerDragStart(marker: Marker) = Unit
                                    })
                                }
                                marker
                            }
                            boundaryPointStates.value.forEach { existing -> map.overlays.remove(existing) }
                            boundaryPointStates.value = currentBoundaryMarkers
                            currentBoundaryMarkers.forEach { map.overlays.add(it) }
                        } else {
                            boundaryPointStates.value.forEach { existing -> map.overlays.remove(existing) }
                            boundaryPointStates.value = emptyList()
                        }

                        if (map.tag as? String != viewportSignature) {
                            val viewportPoints = currentBoundaryPoints.map { GeoPoint(it.first, it.second) } + point
                            if (viewportPoints.size > 1) {
                                val bounds = BoundingBox.fromGeoPoints(viewportPoints)
                                val latSpan = (bounds.latNorth - bounds.latSouth).coerceAtLeast(0.02)
                                val lonSpan = (bounds.lonEast - bounds.lonWest).coerceAtLeast(0.02)
                                map.controller.setCenter(GeoPoint(
                                    (bounds.latNorth + bounds.latSouth) / 2.0,
                                    (bounds.lonEast + bounds.lonWest) / 2.0
                                ))
                                map.controller.zoomToSpan(latSpan, lonSpan)
                            } else {
                                map.controller.setCenter(point)
                                map.controller.setZoom(14.0)
                            }
                            map.tag = viewportSignature
                        }

                        map.invalidate()
                    }
                )
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

private fun createUserLocationMarkerDrawable(context: Context): android.graphics.drawable.Drawable {
    val size = (22 * context.resources.displayMetrics.density).toInt().coerceAtLeast(22)
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val radius = size / 2f

    val outer = Paint().apply {
        isAntiAlias = true
        color = android.graphics.Color.argb(110, 59, 130, 246)
    }
    val inner = Paint().apply {
        isAntiAlias = true
        color = android.graphics.Color.argb(255, 37, 99, 235)
    }
    val core = Paint().apply {
        isAntiAlias = true
        color = android.graphics.Color.WHITE
    }

    canvas.drawCircle(radius, radius, radius, outer)
    canvas.drawCircle(radius, radius, radius * 0.62f, inner)
    canvas.drawCircle(radius, radius, radius * 0.22f, core)

    return BitmapDrawable(context.resources, bitmap)
}
