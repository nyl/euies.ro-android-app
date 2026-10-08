package com.nyl.euiesro.features.launchspots

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
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
import android.Manifest
import androidx.core.content.ContextCompat
import com.nyl.euiesro.FilterChip
import com.nyl.euiesro.common.GroupedTypeCategory
import com.nyl.euiesro.common.GroupedTypeOption
import com.nyl.euiesro.common.GroupedTypeSelector
import com.nyl.euiesro.common.LocationUiItem
import com.nyl.euiesro.app.screens.EuIesroTopAppBar
import com.nyl.euiesro.resolveBestCurrentLocationSuspend
import com.nyl.euiesro.features.launchspots.components.AddSpotMapCard
import kotlinx.coroutines.launch

@Composable
fun SuggestEditScreen(
    location: LocationUiItem,
    onBack: () -> Unit,
    onSuggestionSubmitted: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var title by rememberSaveable(location.id) { mutableStateOf(location.title) }
    var description by rememberSaveable(location.id) { mutableStateOf(location.description.orEmpty()) }
    var address by rememberSaveable(location.id) { mutableStateOf(location.address.orEmpty()) }
    var latitude by rememberSaveable(location.id) { mutableStateOf(location.latitude.toString()) }
    var longitude by rememberSaveable(location.id) { mutableStateOf(location.longitude.toString()) }
    var selectedSurface by rememberSaveable(location.id) { mutableStateOf<String?>(null) }
    var selectedDepth by rememberSaveable(location.id) { mutableStateOf<String?>(null) }
    var selectedAccessibility by rememberSaveable(location.id) { mutableStateOf<String?>(null) }
    var selectedTypeIds by rememberSaveable(location.id) { mutableStateOf(location.typeIds.toSet()) }
    var submitting by rememberSaveable { mutableStateOf(false) }
    var submitError by rememberSaveable { mutableStateOf<String?>(null) }
    var geocodingBusy by rememberSaveable { mutableStateOf(false) }
    var showLocationDialog by rememberSaveable { mutableStateOf(false) }
    var locationDialogMessage by rememberSaveable { mutableStateOf("GPS-ul nu este disponibil momentan. Activează GPS-ul și verifică dacă telefonul are semnal.") }
    var showFullscreenMap by rememberSaveable { mutableStateOf(false) }
    var fullscreenSearch by rememberSaveable { mutableStateOf(address) }
    var fullscreenSuggestions by rememberSaveable { mutableStateOf<List<AddressSuggestion>>(emptyList()) }
    var fullscreenLoading by rememberSaveable { mutableStateOf(false) }
    var mainSuggestions by rememberSaveable { mutableStateOf<List<AddressSuggestion>>(emptyList()) }
    var lastSuggestionQuery by rememberSaveable { mutableStateOf("") }
    var lastFullscreenSuggestionQuery by rememberSaveable { mutableStateOf("") }

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

    LaunchedEffect(location.id) {
        val detail = LaunchSpotApi.fetchLaunchSpotDetail(location.id)
        if (detail != null) {
            title = detail.title
            description = detail.description.orEmpty()
            address = detail.address.orEmpty()
            latitude = detail.latitude.toString()
            longitude = detail.longitude.toString()
            selectedSurface = detail.surface
            selectedDepth = detail.depth
            selectedAccessibility = detail.accessibility
            selectedTypeIds = detail.typeIds.toSet()
            fullscreenSearch = detail.address.orEmpty()
        }
    }

    fun updateCoords(lat: Double, lng: Double) {
        latitude = lat.toString()
        longitude = lng.toString()
    }

    fun applySuggestion(suggestion: AddressSuggestion, closePicker: Boolean = false) {
        address = suggestion.displayName
        lastSuggestionQuery = suggestion.displayName
        lastFullscreenSuggestionQuery = suggestion.displayName
        updateCoords(suggestion.latitude, suggestion.longitude)
        fullscreenSearch = suggestion.displayName
        mainSuggestions = emptyList()
        fullscreenSuggestions = emptyList()
        if (closePicker) showFullscreenMap = false
    }

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
            if (setAddressField) address = cleaned
            updateCoords(result.first, result.second)
            fullscreenSearch = cleaned
            fullscreenSuggestions = emptyList()
            mainSuggestions = emptyList()
        }
    }

    fun useCurrentLocation() {
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!hasFine && !hasCoarse) {
            submitError = "Permite accesul la locație pentru a folosi poziția curentă."
            locationDialogMessage = "Pentru a folosi locația curentă, permite accesul la GPS și localizare."
            showLocationDialog = true
            return
        }

        scope.launch {
            val location = resolveBestCurrentLocationSuspend(context)
            if (location == null) {
                submitError = "Locația GPS nu este suficient de precisă în acest moment. Verifică dacă semnalul GPS este activ și încearcă din nou."
                locationDialogMessage = "Nu am găsit un fix GPS suficient de precis. Verifică dacă GPS-ul este activ și dacă telefonul are semnal."
                showLocationDialog = true
                return@launch
            }
            updateCoords(location.latitude, location.longitude)
            if (address.isBlank()) address = "Punct curent"
            showLocationDialog = false
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

    if (showLocationDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showLocationDialog = false },
            title = { Text("Locație curentă") },
            text = { Text(locationDialogMessage) },
            confirmButton = {
                TextButton(onClick = { showLocationDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    androidx.compose.material3.Scaffold(
        topBar = { EuIesroTopAppBar(title = "Suggest edit", onBack = onBack) }
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
                        Button(onClick = { resolveAddressToLocation(address) }, enabled = !geocodingBusy) {
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
                                        .clickable { applySuggestion(suggestion) }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = { useCurrentLocation() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Use current location") }

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
                                    onClick = { selectedSurface = if (selectedSurface == option.first) null else option.first }
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
                                    onClick = { selectedDepth = if (selectedDepth == option.first) null else option.first }
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
                                    onClick = { selectedAccessibility = if (selectedAccessibility == option.first) null else option.first }
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

                    submitError?.let { Text(text = it, color = MaterialTheme.colorScheme.error) }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(onClick = onBack) { Text("Cancel") }
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
                                    val success = LaunchSpotApi.submitLaunchSpotEditSuggestion(
                                        spotId = location.id,
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
                                    if (success) {
                                        onSuggestionSubmitted()
                                    } else {
                                        submitError = "Unable to submit an edit suggestion. Please try again."
                                    }
                                }
                            },
                            enabled = !submitting
                        ) {
                            Text(if (submitting) "Sending…" else "Submit suggestion")
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
                modifier = Modifier.fillMaxSize().padding(12.dp)
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
                            Text("Select location", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Button(onClick = { showFullscreenMap = false }) { Text("Done") }
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
                            Button(onClick = { resolveAddressToLocation(fullscreenSearch, setAddressField = true) }, enabled = !geocodingBusy) {
                                Text(if (geocodingBusy) "Caut…" else "Search")
                            }
                        }
                        if (fullscreenLoading) {
                            Text("Searching…", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
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
                                            .clickable { applySuggestion(suggestion, closePicker = true) }
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
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
}
