package com.nyl.euiesro.features.events

import android.Manifest
import android.content.Context
import android.location.LocationManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.nyl.euiesro.EuIesroTopAppBar
import com.nyl.euiesro.resolveBestCurrentLocation
import com.nyl.euiesro.resolveBestCurrentLocationSuspend
import com.nyl.euiesro.features.launchspots.AddressSuggestion
import com.nyl.euiesro.features.launchspots.components.AddSpotMapCard
import com.nyl.euiesro.features.launchspots.geocodeAddress
import com.nyl.euiesro.features.launchspots.searchAddressSuggestions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditEventScreen(
    event: EventSummary,
    onBack: () -> Unit,
    onEventUpdated: (EventSummary) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var title by rememberSaveable(event.id) { mutableStateOf(event.title) }
    var description by rememberSaveable(event.id) { mutableStateOf(event.description ?: "") }
    var startAt by rememberSaveable(event.id) { mutableStateOf(event.startAt ?: "") }
    var endAt by rememberSaveable(event.id) { mutableStateOf(event.endAt ?: "") }
    var timezone by rememberSaveable(event.id) { mutableStateOf(java.util.TimeZone.getDefault().id) }
    var locationName by rememberSaveable(event.id) { mutableStateOf(event.locationName ?: "") }
    var locationAddress by rememberSaveable(event.id) { mutableStateOf(event.locationAddress ?: "") }
    var latitude by rememberSaveable(event.id) { mutableStateOf((event.latitude ?: 45.9432).toString()) }
    var longitude by rememberSaveable(event.id) { mutableStateOf((event.longitude ?: 24.9668).toString()) }
    var whatsappGroupUrl by rememberSaveable(event.id) { mutableStateOf(event.whatsappGroupUrl ?: "") }
    var organizerName by rememberSaveable(event.id) { mutableStateOf(event.organizerName ?: "") }
    var organizerWhatsappCountryCode by rememberSaveable(event.id) { mutableStateOf(event.organizerWhatsappCountryCode ?: "") }
    var organizerWhatsappNumber by rememberSaveable(event.id) { mutableStateOf(event.organizerWhatsappNumber ?: "") }
    var organizerIsNotUser by rememberSaveable(event.id) { mutableStateOf(event.organizerIsNotUser) }
    var coverImageUrl by rememberSaveable(event.id) { mutableStateOf(event.coverImageUrl ?: "") }
    var selectedCoverUriString by rememberSaveable(event.id) { mutableStateOf("") }
    var showStartDatePicker by rememberSaveable(event.id) { mutableStateOf(false) }
    var showStartTimePicker by rememberSaveable(event.id) { mutableStateOf(false) }
    var showEndDatePicker by rememberSaveable(event.id) { mutableStateOf(false) }
    var showEndTimePicker by rememberSaveable(event.id) { mutableStateOf(false) }
    var geocodingBusy by rememberSaveable(event.id) { mutableStateOf(false) }
    var mainSuggestions by rememberSaveable(event.id) { mutableStateOf<List<AddressSuggestion>>(emptyList()) }
    var lastSuggestionQuery by rememberSaveable(event.id) { mutableStateOf("") }
    var submitting by rememberSaveable(event.id) { mutableStateOf(false) }
    var error by rememberSaveable(event.id) { mutableStateOf<String?>(null) }
    var success by rememberSaveable(event.id) { mutableStateOf(false) }
    var showLocationDialog by rememberSaveable(event.id) { mutableStateOf(false) }
    var locationDialogMessage by rememberSaveable(event.id) { mutableStateOf("GPS-ul nu este disponibil momentan. Activează GPS-ul și verifică dacă telefonul are semnal.") }

    val startDateState = rememberDatePickerState(initialSelectedDateMillis = parseDateTimeValue(startAt) ?: System.currentTimeMillis())
    val endDateState = rememberDatePickerState(initialSelectedDateMillis = parseDateTimeValue(endAt) ?: System.currentTimeMillis())
    val startTimeState = rememberTimePickerState(initialHour = 18, initialMinute = 0)
    val endTimeState = rememberTimePickerState(initialHour = 20, initialMinute = 0)
    val selectedCoverUri = selectedCoverUriString.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }

    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedCoverUriString = uri.toString()
            coverImageUrl = ""
            error = null
        }
    }

    fun updateCoords(lat: Double, lng: Double) {
        latitude = lat.toString()
        longitude = lng.toString()
    }

    fun applySuggestion(suggestion: AddressSuggestion) {
        locationAddress = suggestion.displayName
        lastSuggestionQuery = suggestion.displayName
        updateCoords(suggestion.latitude, suggestion.longitude)
        mainSuggestions = emptyList()
    }

    fun useCurrentLocation() {
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!hasFine && !hasCoarse) {
            error = "Permite accesul la locație pentru a folosi poziția curentă."
            locationDialogMessage = "Pentru a folosi locația curentă, permite accesul la GPS și localizare."
            showLocationDialog = true
            return
        }

        scope.launch {
            val location = resolveBestCurrentLocationSuspend(context)
            if (location == null) {
                error = "Locația GPS nu este suficient de precisă în acest moment. Verifică dacă semnalul GPS este activ și încearcă din nou."
                locationDialogMessage = "Nu am găsit un fix GPS suficient de precis. Verifică dacă GPS-ul este activ și dacă telefonul are semnal."
                showLocationDialog = true
                return@launch
            }

            updateCoords(location.latitude, location.longitude)
            if (locationAddress.isBlank()) {
                locationAddress = "Punct curent"
            }
            showLocationDialog = false
        }
    }

    fun resolveAddressToLocation(query: String) {
        val cleaned = query.trim()
        if (cleaned.isBlank()) {
            error = "Introdu adresa pentru a localiza punctul pe hartă."
            return
        }

        geocodingBusy = true
        error = null
        scope.launch {
            val result = geocodeAddress(cleaned)
            geocodingBusy = false
            if (result == null) {
                error = "Nu am găsit adresa. Verifică textul și încearcă din nou."
                return@launch
            }
            locationAddress = cleaned
            updateCoords(result.first, result.second)
            mainSuggestions = emptyList()
        }
    }

    LaunchedEffect(locationAddress) {
        val query = locationAddress.trim()
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
        delay(650)
        if (locationAddress.trim() != query) return@LaunchedEffect
        mainSuggestions = searchAddressSuggestions(query).take(4)
    }

    Scaffold(
        topBar = { EuIesroTopAppBar(title = "Editează eveniment", onBack = onBack) }
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
                        label = { Text("Titlu") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Descriere") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4,
                        maxLines = 6
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = startAt,
                            onValueChange = { startAt = it },
                            label = { Text("Începe") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            readOnly = true
                        )
                        OutlinedTextField(
                            value = endAt,
                            onValueChange = { endAt = it },
                            label = { Text("Terminare") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            readOnly = true
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { showStartDatePicker = true }, modifier = Modifier.weight(1f)) { Text("Data începe") }
                        Button(onClick = { showEndDatePicker = true }, modifier = Modifier.weight(1f)) { Text("Data terminare") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { showStartTimePicker = true }, modifier = Modifier.weight(1f)) { Text("Ora începe") }
                        Button(onClick = { showEndTimePicker = true }, modifier = Modifier.weight(1f)) { Text("Ora terminare") }
                    }

                    OutlinedTextField(
                        value = locationName,
                        onValueChange = { locationName = it },
                        label = { Text("Nume locație") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = locationAddress,
                            onValueChange = { locationAddress = it; mainSuggestions = emptyList() },
                            label = { Text("Adresă") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Button(onClick = { resolveAddressToLocation(locationAddress) }, enabled = !geocodingBusy) {
                            Text(if (geocodingBusy) "Caut…" else "Search")
                        }
                    }

                    if (mainSuggestions.isNotEmpty()) {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
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
                    }

                    OutlinedButton(onClick = { useCurrentLocation() }, modifier = Modifier.fillMaxWidth()) {
                        Text("Use my location")
                    }

                    AddSpotMapCard(
                        latitude = latitude,
                        longitude = longitude,
                        onMarkerMoved = { lat, lng -> updateCoords(lat, lng) },
                        mapHeight = 260.dp,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = latitude,
                            onValueChange = { latitude = it },
                            label = { Text("Latitudine") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = longitude,
                            onValueChange = { longitude = it },
                            label = { Text("Longitudine") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    OutlinedTextField(
                        value = whatsappGroupUrl,
                        onValueChange = { whatsappGroupUrl = it },
                        label = { Text("WhatsApp group URL") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            checked = organizerIsNotUser,
                            onCheckedChange = { organizerIsNotUser = it }
                        )
                        Text("Nu sunt eu organizatorul")
                    }

                    if (organizerIsNotUser) {
                        OutlinedTextField(
                            value = organizerName,
                            onValueChange = { organizerName = it },
                            label = { Text("Nume organizator") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = organizerWhatsappCountryCode,
                            onValueChange = { organizerWhatsappCountryCode = it },
                            label = { Text("Cod țară") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = organizerWhatsappNumber,
                            onValueChange = { organizerWhatsappNumber = it },
                            label = { Text("Număr WhatsApp") },
                            modifier = Modifier.weight(2f),
                            singleLine = true
                        )
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Cover image",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Button(
                                onClick = { imagePickerLauncher.launch("image/*") },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Alege imagine de copertă")
                            }
                            val previewModel = when {
                                coverImageUrl.isNotBlank() -> coverImageUrl
                                selectedCoverUri != null -> selectedCoverUri
                                else -> null
                            }
                            if (previewModel != null) {
                                AsyncImage(
                                    model = previewModel,
                                    contentDescription = "Event cover preview",
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                                )
                            }
                        }
                    }

                    if (error != null) {
                        Text(
                            text = error ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (success) {
                        Text(
                            text = "Evenimentul a fost actualizat.",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                if (title.isBlank() || startAt.isBlank()) {
                                    error = "Titlul și data de început sunt obligatorii."
                                    return@launch
                                }
                                if (endAt.isNotBlank()) {
                                    val startMillis = parseDateTimeValue(startAt) ?: 0L
                                    val endMillis = parseDateTimeValue(endAt) ?: 0L
                                    if (endMillis < startMillis) {
                                        error = "Data de final trebuie să fie după data de început."
                                        return@launch
                                    }
                                }
                                if (organizerWhatsappCountryCode.isNotBlank() && !organizerWhatsappCountryCode.matches(Regex("^\\+[0-9]{1,4}$"))) {
                                    error = "Codul de țară WhatsApp este invalid."
                                    return@launch
                                }
                                if (organizerWhatsappNumber.isNotBlank() && !organizerWhatsappNumber.matches(Regex("^[0-9\\s\\-]{6,20}$"))) {
                                    error = "Numărul WhatsApp este invalid."
                                    return@launch
                                }

                                submitting = true
                                error = null

                                val finalCoverUrl = if (selectedCoverUri != null) {
                                    EventsApi.uploadEventCoverImage(context, selectedCoverUri)
                                } else {
                                    coverImageUrl
                                }

                                if (selectedCoverUri != null && finalCoverUrl == null) {
                                    submitting = false
                                    error = "Nu s-a putut încărca imaginea de copertă."
                                    return@launch
                                }

                                val ok = EventsApi.updateEvent(
                                    event.id,
                                    EventFormData(
                                        title = title,
                                        description = description,
                                        startAt = startAt,
                                        endAt = endAt,
                                        timezone = timezone,
                                        locationName = locationName,
                                        locationAddress = locationAddress,
                                        latitude = latitude,
                                        longitude = longitude,
                                        whatsappGroupUrl = whatsappGroupUrl,
                                        organizerIsNotUser = organizerIsNotUser,
                                        organizerName = organizerName,
                                        organizerWhatsappCountryCode = organizerWhatsappCountryCode,
                                        organizerWhatsappNumber = organizerWhatsappNumber,
                                        coverImageUrl = finalCoverUrl.orEmpty()
                                    )
                                )
                                submitting = false
                                if (ok) {
                                    success = true
                                    onEventUpdated(
                                        event.copy(
                                            title = title,
                                            description = description,
                                            startAt = startAt,
                                            endAt = endAt,
                                            locationName = locationName,
                                            locationAddress = locationAddress,
                                            latitude = latitude.toDoubleOrNull(),
                                            longitude = longitude.toDoubleOrNull(),
                                            whatsappGroupUrl = whatsappGroupUrl,
                                            organizerName = organizerName,
                                            organizerWhatsappCountryCode = organizerWhatsappCountryCode,
                                            organizerWhatsappNumber = organizerWhatsappNumber,
                                            organizerIsNotUser = organizerIsNotUser,
                                            coverImageUrl = finalCoverUrl.orEmpty()
                                        )
                                    )
                                } else {
                                    error = "Nu s-a putut salva evenimentul."
                                }
                            }
                        },
                        enabled = !submitting,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (submitting) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(strokeWidth = 2.dp)
                                Text("Se salvează...")
                            }
                        } else {
                            Text("Salvează modificările")
                        }
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
                TextButton(onClick = { showLocationDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    if (showStartDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    startDateState.selectedDateMillis?.let { millis ->
                        val datePart = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(millis))
                        val timePart = if (startAt.contains('T')) {
                            startAt.substringAfter('T').takeIf { it.length >= 5 } ?: "18:00"
                        } else {
                            "18:00"
                        }
                        startAt = "${datePart}T${timePart}"
                    }
                    showStartDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) { Text("Anulează") }
            }
        ) {
            DatePicker(state = startDateState)
        }
    }

    if (showStartTimePicker) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showStartTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val datePart = startAt.substringBefore('T').ifBlank { "2026-01-01" }
                    val timeText = String.format(Locale.US, "%02d:%02d", startTimeState.hour, startTimeState.minute)
                    startAt = "${datePart}T${timeText}"
                    showStartTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showStartTimePicker = false }) { Text("Anulează") }
            },
            text = { TimePicker(state = startTimeState) }
        )
    }

    if (showEndDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    endDateState.selectedDateMillis?.let { millis ->
                        val datePart = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(millis))
                        val timePart = if (endAt.contains('T')) {
                            endAt.substringAfter('T').takeIf { it.length >= 5 } ?: "20:00"
                        } else {
                            "20:00"
                        }
                        endAt = "${datePart}T${timePart}"
                    }
                    showEndDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) { Text("Anulează") }
            }
        ) {
            DatePicker(state = endDateState)
        }
    }

    if (showEndTimePicker) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showEndTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val datePart = endAt.substringBefore('T').ifBlank { "2026-01-01" }
                    val timeText = String.format(Locale.US, "%02d:%02d", endTimeState.hour, endTimeState.minute)
                    endAt = "${datePart}T${timeText}"
                    showEndTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showEndTimePicker = false }) { Text("Anulează") }
            },
            text = { TimePicker(state = endTimeState) }
        )
    }
}
