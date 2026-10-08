package com.nyl.euiesro.features.roadassistance

import android.Manifest
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.nyl.euiesro.EuIesroTopAppBar
import com.nyl.euiesro.R
import com.nyl.euiesro.common.GroupedTypeCategory
import com.nyl.euiesro.common.GroupedTypeOption
import com.nyl.euiesro.common.LocationUiItem
import com.nyl.euiesro.features.launchspots.AddressSuggestion
import com.nyl.euiesro.features.launchspots.searchAddressSuggestions
import com.nyl.euiesro.features.roadassistance.common.RoadAssistanceBoundaryMapCard
import com.nyl.euiesro.features.roadassistance.common.RoadAssistanceBoundaryMapUtils
import com.nyl.euiesro.features.roadassistance.common.RoadAssistanceFormFields
import com.nyl.euiesro.features.roadassistance.common.RoadAssistanceFullscreenBoundaryMapDialog
import com.nyl.euiesro.resolveBestCurrentLocationSuspend
import kotlinx.coroutines.launch

@Composable
fun EditRoadAssistanceScreen(
    location: LocationUiItem,
    onBack: () -> Unit,
    onLocationUpdated: (LocationUiItem) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var title by rememberSaveable(location.id) { mutableStateOf(location.title) }
    var description by rememberSaveable(location.id) { mutableStateOf(location.description.orEmpty()) }
    var address by rememberSaveable(location.id) { mutableStateOf(location.address.orEmpty()) }
    var latitude by rememberSaveable(location.id) { mutableStateOf(location.latitude.toString()) }
    var longitude by rememberSaveable(location.id) { mutableStateOf(location.longitude.toString()) }
    var geocodingBusy by rememberSaveable { mutableStateOf(false) }
    var mainSuggestions by rememberSaveable { mutableStateOf<List<AddressSuggestion>>(emptyList()) }
    var lastSuggestionQuery by rememberSaveable { mutableStateOf("") }
    var coverageBoundary by rememberSaveable(location.id) { mutableStateOf("") }
    var hasLocalCoverageDraft by rememberSaveable(location.id) { mutableStateOf(false) }
    var phone by rememberSaveable(location.id) { mutableStateOf(location.phone.orEmpty()) }
    var email by rememberSaveable(location.id) { mutableStateOf(location.email.orEmpty()) }
    var whatsapp by rememberSaveable(location.id) { mutableStateOf("") }
    var website by rememberSaveable(location.id) { mutableStateOf(location.website.orEmpty()) }
    var facebook by rememberSaveable(location.id) { mutableStateOf("") }
    var instagram by rememberSaveable(location.id) { mutableStateOf("") }
    var selectedServices by rememberSaveable(location.id) { mutableStateOf(location.services.toSet()) }
    var submitting by rememberSaveable { mutableStateOf(false) }
    var showLocationDialog by rememberSaveable { mutableStateOf(false) }
    var showFullscreenMap by rememberSaveable { mutableStateOf(false) }
    var locationDialogMessage by rememberSaveable { mutableStateOf(context.getString(R.string.road_assistance_gps_unavailable)) }

    val serviceGroups = listOf(
        GroupedTypeCategory(
            category = stringResource(R.string.road_assistance_service_mobile),
            options = listOf(
                GroupedTypeOption(value = "service_service_mobile_moto", label = stringResource(R.string.road_assistance_vehicle_moto)),
                GroupedTypeOption(value = "service_service_mobile_auto", label = stringResource(R.string.road_assistance_vehicle_auto)),
                GroupedTypeOption(value = "service_service_mobile_atv", label = stringResource(R.string.road_assistance_vehicle_atv))
            )
        ),
        GroupedTypeCategory(
            category = stringResource(R.string.road_assistance_vulcanizare_mobile),
            options = listOf(
                GroupedTypeOption(value = "service_vulcanizare_mobile_moto", label = stringResource(R.string.road_assistance_vehicle_moto)),
                GroupedTypeOption(value = "service_vulcanizare_mobile_auto", label = stringResource(R.string.road_assistance_vehicle_auto)),
                GroupedTypeOption(value = "service_vulcanizare_mobile_atv", label = stringResource(R.string.road_assistance_vehicle_atv))
            )
        ),
        GroupedTypeCategory(
            category = stringResource(R.string.road_assistance_transport_vehicule),
            options = listOf(
                GroupedTypeOption(value = "service_transport_vehicule_moto", label = stringResource(R.string.road_assistance_vehicle_moto)),
                GroupedTypeOption(value = "service_transport_vehicule_auto", label = stringResource(R.string.road_assistance_vehicle_auto)),
                GroupedTypeOption(value = "service_transport_vehicule_atv", label = stringResource(R.string.road_assistance_vehicle_atv))
            )
        )
    )

    fun updateCoords(lat: Double, lng: Double) {
        latitude = lat.toString()
        longitude = lng.toString()
    }

    fun applyBoundaryDraft(nextBoundary: String) {
        coverageBoundary = nextBoundary
        hasLocalCoverageDraft = true
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
            locationDialogMessage = context.getString(R.string.road_assistance_error_address_required)
            showLocationDialog = true
            return
        }

        geocodingBusy = true
        scope.launch {
            val result = com.nyl.euiesro.features.launchspots.geocodeAddress(cleaned)
            geocodingBusy = false
            if (result == null) {
                locationDialogMessage = context.getString(R.string.road_assistance_error_address_not_found)
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
            locationDialogMessage = context.getString(R.string.road_assistance_error_location_permission)
            showLocationDialog = true
            return
        }

        scope.launch {
            val location = resolveBestCurrentLocationSuspend(context)
            if (location == null) {
                locationDialogMessage = context.getString(R.string.road_assistance_error_current_location_unavailable)
                showLocationDialog = true
                return@launch
            }
            updateCoords(location.latitude, location.longitude)
            if (address.isBlank()) address = context.getString(R.string.road_assistance_current_location_placeholder)
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
        if (query == lastSuggestionQuery) return@LaunchedEffect
        lastSuggestionQuery = query
        kotlinx.coroutines.delay(650)
        if (address.trim() != query) return@LaunchedEffect
        mainSuggestions = searchAddressSuggestions(query).take(4)
    }

    LaunchedEffect(location.id) {
        val detail = RoadAssistanceApi.fetchRoadAssistancePlaceDetail(location.id)
        if (detail != null) {
            title = detail.title
            description = detail.description.orEmpty()
            address = detail.address.orEmpty()
            latitude = detail.latitude.toString()
            longitude = detail.longitude.toString()
            if (!hasLocalCoverageDraft) {
                coverageBoundary = detail.coverageBoundary
            }
            phone = detail.phone.orEmpty()
            email = detail.email.orEmpty()
            whatsapp = ""
            website = detail.website.orEmpty()
            facebook = ""
            instagram = ""
            selectedServices = RoadAssistanceServiceKeys.normalize(detail.services)
        } else {
            title = location.title
            description = location.description.orEmpty()
            address = location.address.orEmpty()
            latitude = location.latitude.toString()
            longitude = location.longitude.toString()
            if (!hasLocalCoverageDraft) {
                coverageBoundary = ""
            }
            phone = location.phone.orEmpty()
            email = location.email.orEmpty()
            whatsapp = ""
            website = location.website.orEmpty()
            facebook = ""
            instagram = ""
            selectedServices = RoadAssistanceServiceKeys.normalize(location.services)
        }
    }

    Scaffold(
        topBar = { EuIesroTopAppBar(title = stringResource(R.string.road_assistance_edit_title), onBack = onBack) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RoadAssistanceFormFields(
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
                onFullscreenClick = { showFullscreenMap = true },
                latitude = latitude,
                onLatitudeChange = { latitude = it },
                longitude = longitude,
                onLongitudeChange = { longitude = it },
                coverageBoundary = coverageBoundary,
                onCoverageBoundaryChange = { applyBoundaryDraft(it) },
                phone = phone,
                onPhoneChange = { phone = it },
                email = email,
                onEmailChange = { email = it },
                whatsapp = whatsapp,
                onWhatsappChange = { whatsapp = it },
                website = website,
                onWebsiteChange = { website = it },
                facebook = facebook,
                onFacebookChange = { facebook = it },
                instagram = instagram,
                onInstagramChange = { instagram = it },
                serviceGroups = serviceGroups,
                selectedServices = selectedServices,
                onToggleService = { key ->
                    selectedServices = if (selectedServices.contains(key)) selectedServices - key else selectedServices + key
                },
                titleLabel = stringResource(R.string.road_assistance_title_label),
                descriptionLabel = stringResource(R.string.road_assistance_description_label),
                addressLabel = stringResource(R.string.road_assistance_address_label),
                searchLabel = stringResource(R.string.road_assistance_search_label),
                currentLocationLabel = stringResource(R.string.road_assistance_current_location),
                latitudeLabel = stringResource(R.string.road_assistance_latitude_label),
                longitudeLabel = stringResource(R.string.road_assistance_longitude_label),
                boundaryLabel = stringResource(R.string.road_assistance_boundary_label),
                servicesLabel = stringResource(R.string.road_assistance_services_label)
            )

            Button(
                onClick = {
                    if (title.isBlank()) {
                        locationDialogMessage = context.getString(R.string.road_assistance_error_title_required)
                        showLocationDialog = true
                        return@Button
                    }
                    if (!latitude.toDoubleOrNull().let { it != null } || !longitude.toDoubleOrNull().let { it != null }) {
                        locationDialogMessage = context.getString(R.string.road_assistance_error_invalid_coordinates)
                        showLocationDialog = true
                        return@Button
                    }

                    val safeBoundary = RoadAssistanceBoundaryMapUtils.sanitizeBoundaryString(coverageBoundary)
                    val parsedBoundary = RoadAssistanceBoundaryMapUtils.parseBoundary(coverageBoundary)
                    if (coverageBoundary.isNotBlank() && !RoadAssistanceBoundaryMapUtils.isBoundaryValid(parsedBoundary)) {
                        locationDialogMessage = context.getString(R.string.road_assistance_error_boundary_invalid)
                        showLocationDialog = true
                        return@Button
                    }

                    submitting = true
                    scope.launch {
                        val ok = RoadAssistanceApi.updateRoadAssistancePlace(
                            id = location.id,
                            title = title,
                            description = description.ifBlank { null },
                            address = address.ifBlank { null },
                            latitude = latitude.toDoubleOrNull() ?: 45.9432,
                            longitude = longitude.toDoubleOrNull() ?: 24.9668,
                            phone = phone.ifBlank { null },
                            email = email.ifBlank { null },
                            whatsapp = whatsapp.ifBlank { null },
                            website = website.ifBlank { null },
                            facebook = facebook.ifBlank { null },
                            instagram = instagram.ifBlank { null },
                            coverageBoundary = safeBoundary,
                            services = selectedServices.toList()
                        )
                        submitting = false
                        if (ok) {
                            hasLocalCoverageDraft = false
                            val updated = location.copy(
                                title = title,
                                description = description.ifBlank { null },
                                address = address.ifBlank { null },
                                latitude = latitude.toDoubleOrNull() ?: location.latitude,
                                longitude = longitude.toDoubleOrNull() ?: location.longitude,
                                services = selectedServices.toList()
                            )
                            onLocationUpdated(updated)
                        } else {
                            locationDialogMessage = context.getString(R.string.road_assistance_error_update_failed)
                            showLocationDialog = true
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !submitting
            ) {
                Text(if (submitting) "Saving..." else "Save changes")
            }
        }
    }

    if (showFullscreenMap) {
        RoadAssistanceFullscreenBoundaryMapDialog(
            latitude = latitude,
            longitude = longitude,
            coverageBoundary = coverageBoundary,
            onDismiss = { showFullscreenMap = false },
            onBoundaryChanged = { applyBoundaryDraft(it) },
            onLocationChanged = { lat, lng ->
                latitude = lat.toString()
                longitude = lng.toString()
            }
        )
    }

    if (showLocationDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showLocationDialog = false },
            title = { Text("Attention") },
            text = { Text(locationDialogMessage) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { showLocationDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}
