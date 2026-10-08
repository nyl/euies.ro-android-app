package com.nyl.euiesro.features.launchspots.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import android.view.MotionEvent
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.preference.PreferenceManager
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Composable
fun AddSpotMapCard(
    latitude: String,
    longitude: String,
    onMarkerMoved: (Double, Double) -> Unit,
    onFullscreenClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    mapHeight: Dp = 220.dp
) {
    val mapState = remember { mutableStateOf<MapView?>(null) }
    val markerState = remember { mutableStateOf<Marker?>(null) }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Locație",
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
                        Text("Ecran complet")
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(mapHeight)
                    .padding(top = 12.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surface)
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

                        val marker = Marker(map).apply {
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            isDraggable = true
                            setOnMarkerDragListener(object : Marker.OnMarkerDragListener {
                                override fun onMarkerDrag(marker: Marker) = Unit
                                override fun onMarkerDragEnd(marker: Marker) {
                                    onMarkerMoved(marker.position.latitude, marker.position.longitude)
                                }
                                override fun onMarkerDragStart(marker: Marker) = Unit
                            })
                        }

                        map.overlays.add(marker)
                        mapState.value = map
                        markerState.value = marker
                        map
                    },
                    update = { map ->
                        val targetLat = latitude.toDoubleOrNull() ?: 45.9432
                        val targetLng = longitude.toDoubleOrNull() ?: 24.9668
                        val point = GeoPoint(targetLat, targetLng)

                        markerState.value?.position = point
                        map.controller.setCenter(point)
                        map.controller.animateTo(point)
                        map.invalidate()
                    }
                )
            }
        }
    }
}
