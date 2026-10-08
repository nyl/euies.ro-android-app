package com.nyl.euiesro.features.launchspots

import android.Manifest
import android.content.Context
import android.location.LocationManager
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.ButtonDefaults
import com.nyl.euiesro.features.launchspots.components.AddSpotMapCard
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.nyl.euiesro.FilterChip
import com.nyl.euiesro.app.screens.EuIesroTopAppBar
import com.nyl.euiesro.common.GroupedTypeCategory
import com.nyl.euiesro.common.GroupedTypeOption
import com.nyl.euiesro.common.GroupedTypeSelector
import com.nyl.euiesro.resolveBestCurrentLocation
import com.nyl.euiesro.resolveBestCurrentLocationSuspend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

private data class AddLaunchSpotDraft(
    val title: String,
    val description: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val launchSurface: String?,
    val waterDepth: String?,
    val accessibility: String?,
    val typeIds: List<Int>
)

data class AddressSuggestion(
    val displayName: String,
    val latitude: Double,
    val longitude: Double
)

@Composable
fun AddLaunchSpotsScreen(
    onBack: () -> Unit,
    onLocationAdded: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    var latitude by rememberSaveable { mutableStateOf("45.9432") }
    var longitude by rememberSaveable { mutableStateOf("24.9668") }
    var selectedSurface by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedDepth by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedAccessibility by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedTypeIds by rememberSaveable { mutableStateOf(setOf<Int>()) }
    var submitting by rememberSaveable { mutableStateOf(false) }
    var showResultDialog by rememberSaveable { mutableStateOf(false) }
    var submitError by rememberSaveable { mutableStateOf<String?>(null) }
    var showLocationDialog by rememberSaveable { mutableStateOf(false) }
    var locationDialogMessage by rememberSaveable { mutableStateOf("GPS-ul nu este disponibil momentan. Activează GPS-ul și verifică dacă telefonul are semnal.") }
    var geocodingBusy by rememberSaveable { mutableStateOf(false) }
    var showFullscreenMap by rememberSaveable { mutableStateOf(false) }
    var fullscreenSearch by rememberSaveable { mutableStateOf("") }
    var fullscreenSuggestions by rememberSaveable { mutableStateOf<List<AddressSuggestion>>(emptyList()) }
    var fullscreenLoading by rememberSaveable { mutableStateOf(false) }
    var mainSuggestions by rememberSaveable { mutableStateOf<List<AddressSuggestion>>(emptyList()) }
    var lastSuggestionQuery by rememberSaveable { mutableStateOf("") }
    var lastFullscreenSuggestionQuery by rememberSaveable { mutableStateOf("") }

    val mapCardColors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)

    fun updateCoords(lat: Double, lng: Double) {
        latitude = lat.toString()
        longitude = lng.toString()
    }

    fun applySuggestion(suggestion: AddressSuggestion, closePicker: Boolean = false) {
        address = suggestion.displayName
        updateCoords(suggestion.latitude, suggestion.longitude)
        fullscreenSearch = suggestion.displayName
        mainSuggestions = emptyList()
        fullscreenSuggestions = emptyList()
        if (closePicker) {
            showFullscreenMap = false
        }
    }

    LaunchedEffect(showFullscreenMap, fullscreenSearch) {
        if (!showFullscreenMap) {
            fullscreenSuggestions = emptyList()
            lastFullscreenSuggestionQuery = ""
            return@LaunchedEffect
        }
        val query = fullscreenSearch.trim()
        if (query.length < 2) {
            fullscreenSuggestions = emptyList()
            lastFullscreenSuggestionQuery = ""
            return@LaunchedEffect
        }
        if (query == lastFullscreenSuggestionQuery) {
            return@LaunchedEffect
        }
        lastFullscreenSuggestionQuery = query
        kotlinx.coroutines.delay(350)
        if (fullscreenSearch.trim() != query) return@LaunchedEffect
        fullscreenLoading = true
        try {
            fullscreenSuggestions = searchAddressSuggestions(query)
        } finally {
            fullscreenLoading = false
        }
    }

    LaunchedEffect(address) {
        val query = address.trim()
        val words = query.split(Regex("[,\\s]+")).filter { it.isNotBlank() }
        val shouldAutocomplete = when {
            words.isEmpty() -> false
            words.size == 1 -> words.first().length >= 4
            else -> true
        }

        if (!shouldAutocomplete) {
            mainSuggestions = emptyList()
            lastSuggestionQuery = ""
            return@LaunchedEffect
        }
        if (query == lastSuggestionQuery) {
            return@LaunchedEffect
        }
        lastSuggestionQuery = query
        kotlinx.coroutines.delay(650)
        if (address.trim() != query) return@LaunchedEffect
        mainSuggestions = searchAddressSuggestions(query).take(4)
    }

    val typeGroups = remember { LaunchSpotApi.fetchLaunchSpotTypeGroupsSync() }
    val typeOptions = typeGroups.flatMap { it.options }
    val groupedTypeGroups = typeGroups.map { group ->
        GroupedTypeCategory(
            category = group.category,
            options = group.options.map { option ->
                GroupedTypeOption(value = option.id.toString(), label = option.label)
            }
        )
    }
    val surfaceOptions = listOf(
        "concrete_ramp" to "Concrete ramp",
        "natural_shore" to "Natural shore",
        "floating_pontoon" to "Floating pontoon",
        "crane" to "Crane"
    )
    val depthOptions = listOf(
        "small" to "Small",
        "medium" to "Medium",
        "large" to "Large"
    )
    val accessOptions = listOf(
        "trailer_access" to "Trailer access",
        "pedestrian_only" to "Pedestrian only"
    )

    fun resolveAddressToLocation(query: String, setAddressField: Boolean = true) {
        val cleaned = query.trim()
        if (cleaned.isBlank()) {
            submitError = "Introdu adresa pentru a localiza punctul pe hartă."
            return
        }

        geocodingBusy = true
        submitError = null
        scope.launch {
            val result = geocodeAddress(cleaned)
            geocodingBusy = false
            if (result == null) {
                submitError = "Nu am găsit adresa. Verifică textul și încearcă din nou."
                return@launch
            }
            if (setAddressField) {
                address = cleaned
            }
            updateCoords(result.first, result.second)
            fullscreenSearch = cleaned
            fullscreenSuggestions = emptyList()
            mainSuggestions = emptyList()
        }
    }

    fun onSearchAddress() {
        resolveAddressToLocation(address)
    }

    fun useCurrentLocation() {
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!hasFine && !hasCoarse) {
            submitError = "Permite accesul la locație pentru a folosi poziția curentă."
            return
        }

        val location = resolveBestCurrentLocation(context)
        if (location == null) {
            submitError = "Locația GPS nu este suficient de precisă în acest moment. Verifică dacă semnalul GPS este activ și încearcă din nou."
            return
        }
        updateCoords(location.latitude, location.longitude)
        address = address.ifBlank { "Punct curent" }
        fullscreenSearch = address.ifBlank { "Punct curent" }
    }

    androidx.compose.material3.Scaffold(
        topBar = {
            EuIesroTopAppBar(title = "Add launch spot", onBack = onBack)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5
                    )

                    Text("Address", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it; mainSuggestions = emptyList() },
                            label = { Text("Address") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Button(onClick = { onSearchAddress() }, enabled = !geocodingBusy) {
                            Text(if (geocodingBusy) "Caut…" else "Search")
                        }
                    }

                    if (mainSuggestions.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                        ) {
                            mainSuggestions.forEach { suggestion ->
                                Text(
                                    text = suggestion.displayName,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            applySuggestion(suggestion)
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = { useCurrentLocation() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Use current location")
                    }

                    AddSpotMapCard(
                        latitude = latitude,
                        longitude = longitude,
                        onMarkerMoved = { lat, lng -> updateCoords(lat, lng) },
                        onFullscreenClick = { showFullscreenMap = true }
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = latitude,
                            onValueChange = { latitude = it },
                            label = { Text("Latitude") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = longitude,
                            onValueChange = { longitude = it },
                            label = { Text("Longitude") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    SectionChipGroup(title = "Launch surface") {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(surfaceOptions) { option ->
                                FilterChip(
                                    label = option.second,
                                    selected = selectedSurface == option.first,
                                    onClick = {
                                        selectedSurface = if (selectedSurface == option.first) null else option.first
                                    }
                                )
                            }
                        }
                    }

                    SectionChipGroup(title = "Water depth") {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(depthOptions) { option ->
                                FilterChip(
                                    label = option.second,
                                    selected = selectedDepth == option.first,
                                    onClick = {
                                        selectedDepth = if (selectedDepth == option.first) null else option.first
                                    }
                                )
                            }
                        }
                    }

                    SectionChipGroup(title = "Accessibility") {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(accessOptions) { option ->
                                FilterChip(
                                    label = option.second,
                                    selected = selectedAccessibility == option.first,
                                    onClick = {
                                        selectedAccessibility = if (selectedAccessibility == option.first) null else option.first
                                    }
                                )
                            }
                        }
                    }

                    GroupedTypeSelector(
                        title = "Types",
                        groups = groupedTypeGroups,
                        selectedValues = selectedTypeIds.map { it.toString() }.toSet(),
                        onToggle = { key ->
                            val id = key.toIntOrNull() ?: return@GroupedTypeSelector
                            selectedTypeIds = if (selectedTypeIds.contains(id)) {
                                selectedTypeIds - id
                            } else {
                                selectedTypeIds + id
                            }
                        }
                    )

                    submitError?.let {
                        Text(text = it, color = MaterialTheme.colorScheme.error)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(onClick = onBack) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.size(8.dp))
                        Button(
                            onClick = {
                                val lat = latitude.toDoubleOrNull()
                                val lng = longitude.toDoubleOrNull()
                                if (title.isBlank() || lat == null || lng == null) {
                                    submitError = "Title and valid coordinates are required."
                                    return@Button
                                }

                                submitting = true
                                submitError = null
                                scope.launch {
                                    val createdId = LaunchSpotApi.createLaunchSpot(
                                        title = title.trim(),
                                        description = description.trim(),
                                        address = address.trim(),
                                        latitude = lat,
                                        longitude = lng,
                                        launchSurface = selectedSurface,
                                        waterDepth = selectedDepth,
                                        accessibility = selectedAccessibility,
                                        typeIds = selectedTypeIds.toList().sorted()
                                    )
                                    submitting = false
                                    if (createdId != null) {
                                        showResultDialog = true
                                        onLocationAdded()
                                    } else {
                                        submitError = "Unable to create the launch spot. Please verify you're logged in and try again."
                                    }
                                }
                            },
                            enabled = !submitting
                        ) {
                            Text(if (submitting) "Saving…" else "Save location")
                        }
                    }
                }
            }
        }
    }

    if (showFullscreenMap) {
        Dialog(
            onDismissRequest = { showFullscreenMap = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Select location",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Button(
                                onClick = { showFullscreenMap = false },
                                shape = RoundedCornerShape(999.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Text("Done")
                            }
                        }

                        Spacer(modifier = Modifier.padding(top = 8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = fullscreenSearch,
                                onValueChange = { fullscreenSearch = it },
                                label = { Text("Search address") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            Button(
                                onClick = { resolveAddressToLocation(fullscreenSearch, setAddressField = true) },
                                enabled = !geocodingBusy
                            ) {
                                Text(if (geocodingBusy) "Caut…" else "Search")
                            }
                        }

                        if (fullscreenLoading) {
                            Text(
                                text = "Searching…",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        if (fullscreenSuggestions.isNotEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.65f), RoundedCornerShape(12.dp))
                            ) {
                                fullscreenSuggestions.take(5).forEach { suggestion ->
                                    Text(
                                        text = suggestion.displayName,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                applySuggestion(suggestion, closePicker = true)
                                            }
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.padding(top = 8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                    val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                    if (!hasFine && !hasCoarse) {
                                        submitError = "Permite accesul la locație pentru a folosi poziția curentă."
                                        return@OutlinedButton
                                    }
                                    val location = resolveBestCurrentLocation(context)
                                    if (location == null) {
                                        submitError = "Locația GPS nu este suficient de precisă în acest moment. Verifică dacă semnalul GPS este activ și încearcă din nou."
                                        return@OutlinedButton
                                    }
                                    updateCoords(location.latitude, location.longitude)
                                    address = "Punct curent"
                                    fullscreenSearch = "Punct curent"
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Use current location")
                            }
                        }

                        Spacer(modifier = Modifier.padding(top = 8.dp))

                        AddSpotMapCard(
                            latitude = latitude,
                            longitude = longitude,
                            onMarkerMoved = { lat, lng -> updateCoords(lat, lng) },
                            mapHeight = 520.dp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }

    if (showLocationDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showLocationDialog = false },
            title = { Text("Locație curentă") },
            text = { Text(locationDialogMessage) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { showLocationDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    if (showResultDialog) {
        Dialog(onDismissRequest = { showResultDialog = false; onBack() }) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Location added",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text("The launch spot was saved successfully.")
                    Button(
                        onClick = { showResultDialog = false; onBack() },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("OK")
                    }
                }
            }
        }
    }
}

internal suspend fun geocodeAddress(query: String): Pair<Double, Double>? = withContext(Dispatchers.IO) {
    try {
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        val url = URL("https://nominatim.openstreetmap.org/search?format=jsonv2&limit=1&q=$encoded")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        connection.setRequestProperty("User-Agent", "EuIesro/1.0")
        connection.setRequestProperty("Accept", "application/json")

        if (connection.responseCode !in 200..299) {
            Log.e("AddLaunchSpotsScreen", "Geocode failed: ${connection.responseCode}")
            return@withContext null
        }

        val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        val array = JSONArray(body)
        if (array.length() == 0) return@withContext null
        val item = array.getJSONObject(0)
        val lat = item.optDouble("lat", Double.NaN)
        val lon = item.optDouble("lon", Double.NaN)
        if (lat.isNaN() || lon.isNaN()) return@withContext null
        lat to lon
    } catch (e: Exception) {
        Log.e("AddLaunchSpotsScreen", "geocodeAddress failed", e)
        null
    }
}

internal suspend fun searchAddressSuggestions(query: String): List<AddressSuggestion> = withContext(Dispatchers.IO) {
    if (query.trim().length < 2) return@withContext emptyList()

    try {
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        val url = URL("https://nominatim.openstreetmap.org/search?format=jsonv2&limit=5&q=$encoded")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        connection.setRequestProperty("User-Agent", "EuIesro/1.0")
        connection.setRequestProperty("Accept", "application/json")

        if (connection.responseCode !in 200..299) {
            return@withContext emptyList()
        }

        val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        val array = JSONArray(body)
        val results = mutableListOf<AddressSuggestion>()
        for (i in 0 until array.length()) {
            val item = array.getJSONObject(i)
            val displayName = item.optString("display_name", "").trim()
            val lat = item.optDouble("lat", Double.NaN)
            val lon = item.optDouble("lon", Double.NaN)
            if (displayName.isNotBlank() && !lat.isNaN() && !lon.isNaN()) {
                results.add(AddressSuggestion(displayName, lat, lon))
            }
        }
        results
    } catch (e: Exception) {
        Log.e("AddLaunchSpotsScreen", "searchAddressSuggestions failed", e)
        emptyList()
    }
}

@Composable
internal fun SectionChipGroup(
    title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        content()
    }
}
