package com.nyl.euiesro.features.moto

import android.Manifest
import android.content.Context
import android.location.LocationManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nyl.euiesro.R
import androidx.core.content.ContextCompat
import com.nyl.euiesro.EuIesroTopAppBar
import com.nyl.euiesro.common.WorkingHoursEditor
import com.nyl.euiesro.common.defaultWorkingHoursMap
import com.nyl.euiesro.resolveBestCurrentLocation
import com.nyl.euiesro.resolveBestCurrentLocationSuspend
import com.nyl.euiesro.features.launchspots.components.AddSpotMapCard
import kotlinx.coroutines.launch

@Composable
fun AddMotoPlaceScreen(
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
    var phone by rememberSaveable { mutableStateOf("") }
    var website by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var facebook by rememberSaveable { mutableStateOf("") }
    var instagram by rememberSaveable { mutableStateOf("") }
    var tiktok by rememberSaveable { mutableStateOf("") }
    var youtube by rememberSaveable { mutableStateOf("") }
    var selectedServices by rememberSaveable { mutableStateOf(setOf<String>()) }
    var workingHours by remember { mutableStateOf(defaultWorkingHoursMap()) }
    var submitting by rememberSaveable { mutableStateOf(false) }
    var submitError by rememberSaveable { mutableStateOf<String?>(null) }
    var successMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var showLocationDialog by rememberSaveable { mutableStateOf(false) }
    var locationDialogMessage by rememberSaveable { mutableStateOf("GPS-ul nu este disponibil momentan. Activează GPS-ul și verifică dacă telefonul are semnal.") }

    val serviceOptions = listOf(
        "magazine" to stringResource(R.string.moto_service_magazine),
        "vulcanizare" to stringResource(R.string.moto_service_vulcanizare),
        "moto" to stringResource(R.string.moto_service_service),
        "itp" to stringResource(R.string.moto_service_itp),
        "popas" to stringResource(R.string.moto_service_popas),
        "diverse" to stringResource(R.string.moto_service_diverse),
        "adunari" to stringResource(R.string.moto_service_adunari)
    )
    val titleCoordsError = stringResource(R.string.moto_error_title_coords)
    val createSuccessMessage = stringResource(R.string.moto_success_create)
    val createErrorMessage = stringResource(R.string.moto_error_create)
    val savingText = stringResource(R.string.moto_status_saving)
    val createActionText = stringResource(R.string.moto_action_create)

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
            submitError = context.getString(R.string.moto_error_address_required)
            return
        }

        geocodingBusy = true
        submitError = null
        scope.launch {
            val result = geocodeAddress(cleaned)
            geocodingBusy = false
            if (result == null) {
                submitError = context.getString(R.string.moto_error_address_not_found)
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
            submitError = context.getString(R.string.moto_error_location_permission)
            locationDialogMessage = "Pentru a folosi locația curentă, permite accesul la GPS și localizare."
            showLocationDialog = true
            return
        }

        scope.launch {
            val location = resolveBestCurrentLocationSuspend(context)
            if (location == null) {
                submitError = context.getString(R.string.moto_error_current_location_unavailable)
                locationDialogMessage = "Nu am găsit un fix GPS suficient de precis. Verifică dacă GPS-ul este activ și dacă telefonul are semnal."
                showLocationDialog = true
                return@launch
            }
            updateCoords(location.latitude, location.longitude)
            address = address.ifBlank { context.getString(R.string.moto_current_location_placeholder) }
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

    Scaffold(
        topBar = { EuIesroTopAppBar(title = stringResource(R.string.moto_add_title), onBack = onBack) }
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
                        label = { Text(stringResource(R.string.moto_label_title)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text(stringResource(R.string.moto_label_description)) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it; mainSuggestions = emptyList() },
                            label = { Text(stringResource(R.string.moto_label_address)) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Button(onClick = { resolveAddressToLocation(address) }, enabled = !geocodingBusy) {
                            Text(if (geocodingBusy) stringResource(R.string.moto_status_searching) else stringResource(R.string.moto_action_search))
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
                        Text(stringResource(R.string.moto_action_use_current_location))
                    }

                    AddSpotMapCard(
                        latitude = latitude,
                        longitude = longitude,
                        onMarkerMoved = { lat, lng ->
                            updateCoords(lat, lng)
                        },
                        onFullscreenClick = null
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = latitude,
                            onValueChange = { latitude = it },
                            label = { Text(stringResource(R.string.moto_label_latitude)) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = longitude,
                            onValueChange = { longitude = it },
                            label = { Text(stringResource(R.string.moto_label_longitude)) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    WorkingHoursEditor(
                        title = "Program de lucru",
                        workingHours = workingHours,
                        onEntryChange = { day, entry ->
                            workingHours = LinkedHashMap(workingHours).apply { put(day, entry) }
                        }
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text(stringResource(R.string.moto_label_phone)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = website,
                        onValueChange = { website = it },
                        label = { Text(stringResource(R.string.moto_label_website)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text(stringResource(R.string.moto_label_email)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = facebook,
                        onValueChange = { facebook = it },
                        label = { Text(stringResource(R.string.moto_label_facebook)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = instagram,
                        onValueChange = { instagram = it },
                        label = { Text(stringResource(R.string.moto_label_instagram)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = tiktok,
                        onValueChange = { tiktok = it },
                        label = { Text(stringResource(R.string.moto_label_tiktok)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = youtube,
                        onValueChange = { youtube = it },
                        label = { Text(stringResource(R.string.moto_label_youtube)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Text(stringResource(R.string.moto_label_services), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            serviceOptions.chunked(2).forEach { row ->
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    row.forEach { (key, label) ->
                                        val selected = key in selectedServices
                                        Button(
                                            onClick = {
                                                selectedServices = if (selected) selectedServices - key else selectedServices + key
                                            },
                                            shape = RoundedCornerShape(16.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                                contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(54.dp)
                                        ) {
                                            Text(label, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (submitError != null) {
                        Text(submitError ?: "", color = MaterialTheme.colorScheme.error)
                    }
                    if (successMessage != null) {
                        Text(successMessage ?: "", color = MaterialTheme.colorScheme.primary)
                    }

                    Button(
                        onClick = {
                            val lat = latitude.toDoubleOrNull()
                            val lng = longitude.toDoubleOrNull()
                            if (title.isBlank() || lat == null || lng == null) {
                                submitError = titleCoordsError
                                successMessage = null
                                return@Button
                            }

                            submitting = true
                            submitError = null
                            successMessage = null
                            scope.launch {
                                val createdId = MotoApi.createMotoPlace(
                                    title = title,
                                    description = description.ifBlank { null },
                                    address = address.ifBlank { null },
                                    latitude = lat,
                                    longitude = lng,
                                    phone = phone.ifBlank { null },
                                    website = website.ifBlank { null },
                                    email = email.ifBlank { null },
                                    facebook = facebook.ifBlank { null },
                                    instagram = instagram.ifBlank { null },
                                    tiktok = tiktok.ifBlank { null },
                                    youtube = youtube.ifBlank { null },
                                    services = selectedServices.toList(),
                                    isOwner = true,
                                    workingHours = workingHours
                                )
                                if (createdId != null) {
                                    successMessage = createSuccessMessage
                                    onLocationAdded()
                                    onBack()
                                } else {
                                    submitError = createErrorMessage
                                }
                                submitting = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !submitting
                    ) {
                        Text(if (submitting) savingText else createActionText)
                    }
                }
            }
        }
    }
}
