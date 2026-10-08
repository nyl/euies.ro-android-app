package com.nyl.euiesro

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.nyl.euiesro.common.LocationUiItem
import com.nyl.euiesro.common.ModuleSection
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.util.Locale
import kotlin.math.cos
import kotlin.math.pow

object SiteMarkerIconResolver {
    private val motoIconMap = mapOf(
        "magazine" to "🏍️",
        "vulcanizare" to "🛞",
        "moto" to "🛠️",
        "itp" to "🧾",
        "popas" to "🍽️",
        "diverse" to "🧩",
        "adunari" to "🤘"
    )

    fun resolveMotoMarkerIcons(services: List<String>): List<String> {
        val icons = services.mapNotNull { service ->
            val normalized = service.trim()
            if (normalized.isBlank()) null else motoIconMap[normalized]
        }
        return if (icons.isNotEmpty()) icons.distinct() else listOf("📍")
    }

    fun resolveRoadAssistanceMarkerIcons(services: List<String>): List<String> {
        val icons = services.mapNotNull { service ->
            val normalized = service.trim().lowercase(Locale.getDefault())
            when {
                normalized.contains("service mobil") || normalized.contains("mobil") -> "🚑"
                normalized.contains("vulcanizare") -> "🛠️"
                normalized.contains("transport") || normalized.contains("vehicule") -> "🚚"
                else -> null
            }
        }
        return if (icons.isNotEmpty()) icons.distinct() else listOf("📍")
    }

    fun resolveLaunchSpotMarkerIcons(iconValues: List<String>): List<String> {
        return iconValues.filter { it.isNotBlank() }.distinct().ifEmpty { listOf("📍") }
    }

    fun resolveAutoMarkerIcons(services: List<String>, markerIcons: List<String> = emptyList()): List<String> {
        val normalizedServices = services.map { it.trim().lowercase(Locale.getDefault()) }
        val autoSignals = normalizedServices.mapNotNull { service ->
            when {
                service.contains("vulcanizare") || service.contains("service") -> "🛠️"
                service.contains("piese") || service.contains("caroserie") -> "🚘"
                service.contains("electric") || service.contains("diagnostic") -> "🔧"
                service.contains("detailing") || service.contains("wash") -> "✨"
                else -> null
            }
        }
        val explicitIcons = markerIcons.filter { it.isNotBlank() }
        return (autoSignals + explicitIcons).distinct().ifEmpty { listOf("🚗") }
    }

    fun isImageIcon(iconValue: String): Boolean {
        val value = iconValue.trim()
        return value.startsWith("http://", true) ||
            value.startsWith("https://", true) ||
            value.startsWith("/", true)
    }
}

private const val MAP_LAYER_STANDARD = "standard"
private const val MAP_LAYER_SATELLITE = "satellite"

@Composable
fun InteractiveLocationMap(
    items: List<LocationUiItem>,
    modifier: Modifier = Modifier,
    interactive: Boolean = false,
    showUserLocation: Boolean = false,
    tileSource: String = MAP_LAYER_STANDARD,
    onMapReady: (MapView) -> Unit = {},
    onMarkerClick: ((LocationUiItem) -> Unit)? = null
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            Configuration.getInstance().load(
                context.applicationContext,
                PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
            )
            buildMapView(context, interactive).also { map ->
                onMapReady(map)
            }
        },
        update = { map ->
            applyMapTileSource(map, tileSource)
            applyInteractionMode(map, interactive)
            renderMap(map, items, interactive, showUserLocation, onMarkerClick)
        }
    )
}

private fun applyMapTileSource(map: MapView, tileSource: String) {
    val resolvedTileSource = when (tileSource) {
        MAP_LAYER_SATELLITE -> object : OnlineTileSourceBase(
            "EsriWorldImagery",
            0,
            20,
            256,
            ".jpg",
            arrayOf("https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/"),
            "Tiles © Esri"
        ) {
            override fun getTileURLString(pMapTileIndex: Long): String {
                val zoom = MapTileIndex.getZoom(pMapTileIndex)
                val x = MapTileIndex.getX(pMapTileIndex)
                val y = MapTileIndex.getY(pMapTileIndex)
                return "${getBaseUrl()}$zoom/$y/$x"
            }
        }
        else -> TileSourceFactory.MAPNIK
    }
    map.setTileSource(resolvedTileSource)
}

private fun buildMapView(context: Context, interactive: Boolean): MapView {
    return MapView(context).apply {
        setTileSource(TileSourceFactory.MAPNIK)
        setBuiltInZoomControls(false)
        setHorizontalMapRepetitionEnabled(false)
        setVerticalMapRepetitionEnabled(false)
        // Software layer avoids black artifact flashes reported on some GPUs during drag gestures.
        setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)
        isTilesScaledToDpi = true
        minZoomLevel = 3.0
        maxZoomLevel = 20.0
        controller.setZoom(10.0)
        applyInteractionMode(this, interactive)
    }
}

private fun applyInteractionMode(map: MapView, interactive: Boolean) {
    map.setMultiTouchControls(interactive)
    map.isFocusable = interactive
    map.isClickable = interactive
    map.setOnTouchListener(if (interactive) null else android.view.View.OnTouchListener { _, _ -> true })
}

private fun renderMap(
    map: MapView,
    items: List<LocationUiItem>,
    interactive: Boolean,
    showUserLocation: Boolean,
    onMarkerClick: ((LocationUiItem) -> Unit)? = null
) {
    val userLocation = if (showUserLocation) resolveLastKnownGeoPoint(map.context) else null
    val mapPoints = items.map { GeoPoint(it.latitude, it.longitude) }.toMutableList()
    if (userLocation != null) {
        mapPoints.add(userLocation)
    }

    val viewportBounds = if (mapPoints.isEmpty()) {
        BoundingBox(45.5, 27.5, 43.2, 24.3)
    } else {
        BoundingBox.fromGeoPoints(mapPoints)
    }
    val paddedBounds = expandBoundingBox(viewportBounds, if (mapPoints.size > 1) 0.18 else 0.35)

    val dataSignature = buildViewportSignature(items, userLocation, includeUserLocation = !interactive)
    val shouldRefit = map.tag != dataSignature
    if (shouldRefit) {
        map.overlays.clear()
        for (item in items) {
            map.overlays.add(createLocationMarker(map, item, onMarkerClick))
        }

        if (userLocation != null) {
            map.overlays.add(createUserLocationMarker(map, userLocation))
        }

        fitMapToBounds(map, paddedBounds, mapPoints.size)
        if (interactive) {
            map.setScrollableAreaLimitDouble(BoundingBox(85.0, 180.0, -85.0, -180.0))
        } else {
            map.setScrollableAreaLimitDouble(paddedBounds)
        }
        map.tag = dataSignature
    }

    if (!interactive) {
        map.setScrollableAreaLimitDouble(paddedBounds)
    }

    if (shouldRefit) {
        map.invalidate()
    }
}

private fun buildViewportSignature(
    items: List<LocationUiItem>,
    userLocation: GeoPoint?,
    includeUserLocation: Boolean
): String {
    val itemKey = items.joinToString(";") { item ->
        "${item.id}:${item.latitude}:${item.longitude}:${item.section.name}"
    }
    val userKey = if (!includeUserLocation || userLocation == null) {
        "none"
    } else {
        "${"%.5f".format(Locale.US, userLocation.latitude)},${"%.5f".format(Locale.US, userLocation.longitude)}"
    }
    return "$itemKey|$userKey"
}

private fun fitMapToBounds(map: MapView, bounds: BoundingBox, pointsCount: Int) {
    val center = GeoPoint(
        (bounds.latNorth + bounds.latSouth) / 2.0,
        (bounds.lonEast + bounds.lonWest) / 2.0
    )
    map.controller.setCenter(center)

    if (pointsCount <= 1) {
        map.controller.setZoom(14.0)
        return
    }

    val latSpan = (bounds.latNorth - bounds.latSouth).coerceAtLeast(0.01)
    val lonSpan = (bounds.lonEast - bounds.lonWest).coerceAtLeast(0.01)
    map.controller.zoomToSpan(latSpan, lonSpan)
}

private fun createLocationMarker(
    map: MapView,
    item: LocationUiItem,
    onMarkerClick: ((LocationUiItem) -> Unit)? = null
): Marker {
    return Marker(map).apply {
        position = GeoPoint(item.latitude, item.longitude)
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        title = item.title
        snippet = item.address ?: item.description ?: "Location"
        icon = when (item.section) {
            ModuleSection.MOTO -> {
                val iconValue = SiteMarkerIconResolver.resolveMotoMarkerIcons(item.services).firstOrNull() ?: "📍"
                createMarkerDrawable(map.context, iconValue, android.graphics.Color.argb(255, 27, 94, 32))
            }
            ModuleSection.LAUNCH_SPOTS -> {
                val iconValue = SiteMarkerIconResolver.resolveLaunchSpotMarkerIcons(item.markerIcons).firstOrNull() ?: "📍"
                createMarkerDrawable(map.context, iconValue, android.graphics.Color.argb(255, 79, 70, 229))
            }
            ModuleSection.AUTO -> {
                val iconValue = SiteMarkerIconResolver.resolveAutoMarkerIcons(item.services, item.markerIcons).firstOrNull() ?: "🚗"
                createMarkerDrawable(map.context, iconValue, android.graphics.Color.argb(255, 8, 132, 255))
            }
            else -> ContextCompat.getDrawable(map.context, android.R.drawable.btn_star_big_on)
        }

        setOnMarkerClickListener { clickedMarker, _ ->
            map.controller.animateTo(clickedMarker.position)
            map.controller.setZoom(15.0)
            onMarkerClick?.invoke(item)
            true
        }
    }
}

fun centerMapOnLocation(map: MapView?, latitude: Double, longitude: Double, zoom: Double = 15.0) {
    val mapView = map ?: return
    mapView.controller.animateTo(GeoPoint(latitude, longitude))
    mapView.controller.setZoom(zoom)
}

private fun createUserLocationMarker(map: MapView, position: GeoPoint): Marker {
    return Marker(map).apply {
        this.position = position
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        title = "My location"
        snippet = "Current position"
        icon = createUserLocationDotDrawable(map.context)
    }
}

private fun expandBoundingBox(bounds: BoundingBox, paddingRatio: Double): BoundingBox {
    val latSpan = (bounds.latNorth - bounds.latSouth).coerceAtLeast(0.01)
    val lonSpan = (bounds.lonEast - bounds.lonWest).coerceAtLeast(0.01)
    val latPad = latSpan * paddingRatio
    val lonPad = lonSpan * paddingRatio

    val north = (bounds.latNorth + latPad).coerceAtMost(85.0)
    val south = (bounds.latSouth - latPad).coerceAtLeast(-85.0)
    val east = (bounds.lonEast + lonPad).coerceAtMost(180.0)
    val west = (bounds.lonWest - lonPad).coerceAtLeast(-180.0)

    return BoundingBox(north, east, south, west)
}

private fun createUserLocationDotDrawable(context: Context): android.graphics.drawable.Drawable {
    val size = (20 * context.resources.displayMetrics.density).toInt().coerceAtLeast(20)
    val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val radius = size / 2f

    val outer = android.graphics.Paint().apply {
        isAntiAlias = true
        color = android.graphics.Color.argb(115, 59, 130, 246)
    }
    val inner = android.graphics.Paint().apply {
        isAntiAlias = true
        color = android.graphics.Color.argb(255, 37, 99, 235)
    }
    val core = android.graphics.Paint().apply {
        isAntiAlias = true
        color = android.graphics.Color.WHITE
    }

    canvas.drawCircle(radius, radius, radius, outer)
    canvas.drawCircle(radius, radius, radius * 0.62f, inner)
    canvas.drawCircle(radius, radius, radius * 0.26f, core)

    return android.graphics.drawable.BitmapDrawable(context.resources, bitmap)
}

fun centerMapOnUserLocation(map: MapView?, sheetRatio: Float? = null) {
    val mapView = map ?: return
    val userLocation = resolveLastKnownGeoPoint(mapView.context) ?: return
    mapView.controller.setZoom(15.0)

    if (mapView.height <= 0) {
        mapView.controller.animateTo(userLocation)
        return
    }

    // Use a fixed anchor regardless of sheet state to keep centering consistent.
    val targetUserY = mapView.height * 0.40
    val mapMidY = mapView.height * 0.5
    val shiftUpPx = (mapMidY - targetUserY).coerceAtLeast(0.0)

    val zoom = mapView.zoomLevelDouble.takeIf { it > 0.0 } ?: 15.0
    val metersPerPixel = 156543.03392 * cos(Math.toRadians(userLocation.latitude)).coerceAtLeast(0.01) / 2.0.pow(zoom)
    val latitudeShiftDegrees = (shiftUpPx * metersPerPixel) / 111320.0
    val adjustedCenter = GeoPoint(
        (userLocation.latitude - latitudeShiftDegrees).coerceIn(-85.0, 85.0),
        userLocation.longitude.coerceIn(-180.0, 180.0)
    )
    mapView.controller.animateTo(adjustedCenter)
}

fun recenterMapForBottomSheet(map: MapView?, sheetRatio: Float, minSheetRatio: Float = 0.28f, maxSheetRatio: Float = 1f) {
    val mapView = map ?: return
    if (mapView.height <= 0) return

    val currentCenter = mapView.mapCenter ?: return
    val clampedRatio = sheetRatio.coerceIn(minSheetRatio, maxSheetRatio)
    val normalized = (clampedRatio - minSheetRatio) / (maxSheetRatio - minSheetRatio).coerceAtLeast(0.001f)
    val verticalOffsetPx = (mapView.height * (0.28f + normalized * 0.52f)).toInt()

    val screenPoint = android.graphics.Point()
    mapView.projection.toPixels(currentCenter, screenPoint)
    screenPoint.y = (screenPoint.y - verticalOffsetPx).coerceAtLeast(0)
    val adjustedCenter = mapView.projection.fromPixels(screenPoint.x, screenPoint.y)
    mapView.controller.animateTo(adjustedCenter)
}

private fun isGpsLocationUsable(location: Location?, maxAccuracyMeters: Float): Boolean {
    return location != null &&
        location.provider == LocationManager.GPS_PROVIDER &&
        location.accuracy > 0f &&
        location.accuracy <= maxAccuracyMeters
}

fun resolveBestCurrentLocation(context: Context, maxAccuracyMeters: Float = 100f): Location? {
    val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    if (!hasFine && !hasCoarse) {
        return null
    }

    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null

    val gpsLocation = try {
        locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
    } catch (_: SecurityException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }

    if (isGpsLocationUsable(gpsLocation, maxAccuracyMeters)) {
        return gpsLocation
    }

    return null
}

fun resolveBestCurrentLocationForDisplay(context: Context, maxAccuracyMeters: Float = 500f): Location? {
    val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    if (!hasFine && !hasCoarse) {
        return null
    }

    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null

    val candidates = listOfNotNull(
        try {
            locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        } catch (_: SecurityException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        },
        try {
            locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        } catch (_: SecurityException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        },
        try {
            locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
        } catch (_: SecurityException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
    )

    return candidates
        .filter { it.accuracy > 0f && it.accuracy <= maxAccuracyMeters }
        .maxByOrNull { it.time }
        ?: candidates.maxByOrNull { it.time }
}

fun resolveLastKnownGeoPointForDisplay(context: Context, maxAccuracyMeters: Float = 500f): GeoPoint? {
    return resolveBestCurrentLocationForDisplay(context, maxAccuracyMeters)?.let { GeoPoint(it.latitude, it.longitude) }
}

suspend fun resolveBestCurrentLocationSuspend(
    context: Context,
    maxAccuracyMeters: Float = 100f,
    timeoutMs: Long = 8000L
): Location? = withContext(Dispatchers.IO) {
    val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    if (!hasFine && !hasCoarse) {
        return@withContext null
    }

    val cachedLocation = resolveBestCurrentLocation(context, maxAccuracyMeters)
    if (cachedLocation != null) {
        return@withContext cachedLocation
    }

    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return@withContext null
    val result = CompletableDeferred<Location?>()
    val listener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            if (isGpsLocationUsable(location, maxAccuracyMeters)) {
                locationManager.removeUpdates(this)
                result.complete(location)
            }
        }

        override fun onProviderDisabled(provider: String) = Unit
        override fun onProviderEnabled(provider: String) = Unit
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
    }

    try {
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0L, 0f, listener, Looper.getMainLooper())
        withTimeoutOrNull(timeoutMs) { result.await() }
    } catch (_: SecurityException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    } finally {
        locationManager.removeUpdates(listener)
    }
}

fun resolveLastKnownGeoPoint(context: Context): GeoPoint? {
    return resolveBestCurrentLocation(context)?.let { GeoPoint(it.latitude, it.longitude) }
}

fun calculateDistanceMeters(fromLat: Double?, fromLng: Double?, toLat: Double, toLng: Double): Float? {
    if (fromLat == null || fromLng == null) {
        return null
    }

    val result = FloatArray(1)
    Location.distanceBetween(fromLat, fromLng, toLat, toLng, result)
    return result.firstOrNull()
}

fun formatDistanceLabel(distanceMeters: Float?): String? {
    if (distanceMeters == null || !distanceMeters.isFinite()) {
        return null
    }

    return if (distanceMeters >= 1000f) {
        val km = distanceMeters / 1000f
        "Distanță de la tine: ${"%.1f".format(Locale.US, km)} km"
    } else {
        "Distanță de la tine: ${distanceMeters.toInt()} m"
    }
}

fun sortLocationsByDistance(items: List<LocationUiItem>, userLocation: Location?): List<LocationUiItem> {
    if (userLocation == null) {
        return items
    }

    return items.sortedBy { item ->
        calculateDistanceMeters(userLocation.latitude, userLocation.longitude, item.latitude, item.longitude) ?: Float.MAX_VALUE
    }
}

private fun createMarkerDrawable(
    context: Context,
    iconValue: String,
    accentColor: Int = android.graphics.Color.argb(255, 8, 132, 255)
): android.graphics.drawable.Drawable {
    val normalized = iconValue.trim()
    if (normalized.isEmpty()) {
        return createTextMarker(context, "📍", accentColor)
    }
    if (SiteMarkerIconResolver.isImageIcon(normalized)) {
        val remoteUrl = if (normalized.startsWith("/")) {
            "${ApiClient.baseUrl.trimEnd('/')}$normalized"
        } else {
            normalized
        }
        return createImageMarker(context, remoteUrl) ?: createTextMarker(context, "📍", accentColor)
    }
    return createTextMarker(context, normalized, accentColor)
}

private fun createTextMarker(context: Context, iconText: String, accentColor: Int): android.graphics.drawable.Drawable {
    val size = (46 * context.resources.displayMetrics.density).toInt()
    val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val radius = size / 2f

    val bgPaint = android.graphics.Paint().apply {
        isAntiAlias = true
        color = android.graphics.Color.WHITE
    }
    val ringPaint = android.graphics.Paint().apply {
        isAntiAlias = true
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = (6 * context.resources.displayMetrics.density)
        color = accentColor
    }
    val shadowPaint = android.graphics.Paint().apply {
        isAntiAlias = true
        color = android.graphics.Color.argb(60, 0, 0, 0)
    }

    canvas.drawCircle(radius, radius, radius - 3f, shadowPaint)
    canvas.drawCircle(radius, radius, radius - 7f, bgPaint)
    canvas.drawCircle(radius, radius, radius - 7f, ringPaint)

    val textPaint = android.graphics.Paint().apply {
        isAntiAlias = true
        textAlign = android.graphics.Paint.Align.CENTER
        textSize = (22 * context.resources.displayMetrics.density)
        color = android.graphics.Color.DKGRAY
    }
    val textY = radius + (textPaint.descent() - textPaint.ascent()) / 2f - textPaint.descent()
    canvas.drawText(iconText, radius, textY, textPaint)

    return android.graphics.drawable.BitmapDrawable(context.resources, bitmap)
}

private fun createImageMarker(context: Context, imageUrl: String): android.graphics.drawable.Drawable? {
    return try {
        val connection = (java.net.URL(imageUrl).openConnection() as java.net.HttpURLConnection).apply {
            connectTimeout = 8000
            readTimeout = 8000
            doInput = true
            connect()
        }

        val bitmap = connection.inputStream.use { input ->
            android.graphics.BitmapFactory.decodeStream(input)
        }
        connection.disconnect()

        if (bitmap != null) android.graphics.drawable.BitmapDrawable(context.resources, bitmap) else null
    } catch (_: Exception) {
        null
    }
}

@Composable
fun SelectedLocationInfoCard(
    location: LocationUiItem,
    onOpenLocation: (LocationUiItem) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 10.dp,
        shadowElevation = 18.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = location.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close location card")
                }
            }

            location.address?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                    maxLines = 2,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Button(
                onClick = { onOpenLocation(location) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
            ) {
                Text(stringResource(R.string.view_details))
            }
        }
    }
}

@Composable
fun MapCenterLocationButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary
    ) {
        Icon(Icons.Default.MyLocation, contentDescription = "Zoom to my location")
    }
}

@Composable
fun LocationMapHeader(
    items: List<LocationUiItem>,
    isFullMap: Boolean = false,
    onToggleMap: (() -> Unit)? = null,
    onOpenFullscreen: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = stringResource(R.string.map_overview),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = stringResource(R.string.nearby_locations),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            InteractiveLocationMap(
                items = items,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                interactive = false,
                showUserLocation = false
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.locations_count, items.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
                if (onOpenFullscreen != null) {
                    Button(
                        onClick = onOpenFullscreen,
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            contentColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Text(stringResource(R.string.fullscreen))
                    }
                } else if (onToggleMap != null) {
                    Button(
                        onClick = onToggleMap,
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            contentColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Text(if (isFullMap) stringResource(R.string.mini_map) else stringResource(R.string.fullscreen))
                    }
                }
            }
        }
    }
}
