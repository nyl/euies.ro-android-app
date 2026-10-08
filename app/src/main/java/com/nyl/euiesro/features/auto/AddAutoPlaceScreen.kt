package com.nyl.euiesro.features.auto

import android.Manifest
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.nyl.euiesro.EuIesroTopAppBar
import com.nyl.euiesro.R
import com.nyl.euiesro.common.GroupedTypeCategory
import com.nyl.euiesro.common.GroupedTypeOption
import com.nyl.euiesro.common.WorkingHoursEntry
import com.nyl.euiesro.common.defaultWorkingHoursMap
import com.nyl.euiesro.common.workingHoursMapToJson
import com.nyl.euiesro.features.auto.components.AutoFormFields
import com.nyl.euiesro.features.auto.components.AutoLocationStatusDialog
import com.nyl.euiesro.features.launchspots.AddressSuggestion
import com.nyl.euiesro.features.launchspots.searchAddressSuggestions
import com.nyl.euiesro.resolveBestCurrentLocationSuspend
import kotlinx.coroutines.launch

@Composable
fun AddAutoPlaceScreen(
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
    var geocodingBusy by rememberSaveable { mutableStateOf(false) }
    var mainSuggestions by rememberSaveable { mutableStateOf<List<AddressSuggestion>>(emptyList()) }
    var lastSuggestionQuery by rememberSaveable { mutableStateOf("") }
    var selectedTypeIds by rememberSaveable { mutableStateOf(setOf<Int>()) }
    var workingHours by remember { mutableStateOf(defaultWorkingHoursMap()) }
    var showLocationDialog by rememberSaveable { mutableStateOf(false) }
    var locationDialogMessage by rememberSaveable { mutableStateOf("GPS-ul nu este disponibil momentan. Activează GPS-ul și verifică dacă telefonul are semnal.") }
    var submitting by rememberSaveable { mutableStateOf(false) }

    val typeGroups = remember { AutoApi.fetchAutoTypeGroupsSync() }
    val serviceGroups = typeGroups.map { group ->
        GroupedTypeCategory(
            category = group.category,
            options = group.options.map { option ->
                GroupedTypeOption(value = option.id.toString(), label = option.label)
            }
        )
    }
    val selectedServices = selectedTypeIds.map { it.toString() }.toSet()

    fun updateCoords(lat: Double, lng: Double) {
        latitude = lat.toString()
        longitude = lng.toString()
    }

    fun applySuggestion(suggestion: AddressSuggestion) {
        address = suggestion.displayName
        lastSuggestionQuery = suggestion.displayName
        updateCoords(suggestion.latitude, suggestion.longitude)
        mainSuggestions = emptyList()
    }

    fun resolveAddressToLocation(query: String) {
        val cleaned = query.trim()
        if (cleaned.isBlank()) {
            locationDialogMessage = "Introdu adresa pentru a localiza locul pe hartă."
            showLocationDialog = true
            return
        }

        geocodingBusy = true
        scope.launch {
            val result = com.nyl.euiesro.features.launchspots.geocodeAddress(cleaned)
            geocodingBusy = false
            if (result == null) {
                locationDialogMessage = "Nu am găsit adresa. Verifică textul și încearcă din nou."
                showLocationDialog = true
                return@launch
            }
            address = cleaned
            updateCoords(result.first, result.second)
            mainSuggestions = emptyList()
        }
    }

    fun useCurrentLocation() {
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!hasFine && !hasCoarse) {
            locationDialogMessage = "Pentru a folosi locația curentă, permite accesul la GPS și localizare."
            showLocationDialog = true
            return
        }

        scope.launch {
            val location = resolveBestCurrentLocationSuspend(context)
            if (location == null) {
                locationDialogMessage = "Nu am găsit un fix GPS suficient de precis. Verifică dacă GPS-ul este activ și dacă telefonul are semnal."
                showLocationDialog = true
                return@launch
            }
            updateCoords(location.latitude, location.longitude)
            if (address.isBlank()) {
                address = "Locație curentă"
            }
            showLocationDialog = false
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

    Scaffold(
        topBar = { EuIesroTopAppBar(title = "Add auto place", onBack = onBack) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AutoFormFields(
                title = title,
                onTitleChange = { title = it },
                description = description,
                onDescriptionChange = { description = it },
                address = address,
                onAddressChange = { address = it; mainSuggestions = emptyList() },
                suggestions = mainSuggestions,
                onSuggestionSelected = { applySuggestion(it) },
                onSearchAddress = { resolveAddressToLocation(address) },
                geocodingBusy = geocodingBusy,
                onUseCurrentLocation = { useCurrentLocation() },
                latitude = latitude,
                onLatitudeChange = { latitude = it },
                longitude = longitude,
                onLongitudeChange = { longitude = it },
                serviceGroups = serviceGroups,
                selectedServices = selectedServices,
                onToggleService = { key ->
                    val id = key.toIntOrNull() ?: return@AutoFormFields
                    selectedTypeIds = if (selectedTypeIds.contains(id)) {
                        selectedTypeIds - id
                    } else {
                        selectedTypeIds + id
                    }
                },
                workingHours = workingHours,
                onWorkingHoursChange = { day, entry ->
                    workingHours = LinkedHashMap(workingHours).apply { put(day, entry) }
                },
                titleLabel = "Title",
                descriptionLabel = "Description",
                addressLabel = "Address",
                searchLabel = "Search",
                currentLocationLabel = "Use current location",
                latitudeLabel = "Latitude",
                longitudeLabel = "Longitude",
                servicesLabel = "Services"
            )

            Button(
                onClick = {
                    if (submitting) return@Button
                    if (title.isBlank()) {
                        locationDialogMessage = "Completează titlul locației înainte de salvare."
                        showLocationDialog = true
                        return@Button
                    }

                    submitting = true
                    scope.launch {
                        val created = AutoApi.createAutoPlace(
                            title = title,
                            description = description.ifBlank { null },
                            address = address.ifBlank { null },
                            latitude = latitude.toDoubleOrNull() ?: 45.9432,
                            longitude = longitude.toDoubleOrNull() ?: 24.9668,
                            phone = null,
                            website = null,
                            email = null,
                            facebook = null,
                            instagram = null,
                            tiktok = null,
                            youtube = null,
                            typeIds = selectedTypeIds.toList().sorted(),
                            workingHours = workingHours
                        )
                        submitting = false
                        if (created != null) {
                            onLocationAdded()
                        } else {
                            locationDialogMessage = "Nu am putut salva locația. Verifică coordonatele și încearcă din nou."
                            showLocationDialog = true
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !submitting
            ) {
                Text(if (submitting) "Saving..." else "Create place")
            }
        }
    }

    AutoLocationStatusDialog(
        show = showLocationDialog,
        message = locationDialogMessage,
        onDismiss = { showLocationDialog = false }
    )
}
