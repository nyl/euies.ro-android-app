package com.nyl.euiesro.features.roadassistance.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nyl.euiesro.R
import com.nyl.euiesro.common.GroupedTypeCategory
import com.nyl.euiesro.common.GroupedTypeSelector
import com.nyl.euiesro.features.launchspots.AddressSuggestion
import com.nyl.euiesro.features.launchspots.components.AddSpotMapCard

@Composable
fun RoadAssistanceFormFields(
    title: String,
    onTitleChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    address: String,
    onAddressChange: (String) -> Unit,
    suggestions: List<AddressSuggestion>,
    onSuggestionSelected: (AddressSuggestion) -> Unit,
    onSearchAddress: () -> Unit,
    geocodingBusy: Boolean,
    onUseCurrentLocation: () -> Unit,
    onFullscreenClick: (() -> Unit)? = null,
    latitude: String,
    onLatitudeChange: (String) -> Unit,
    longitude: String,
    onLongitudeChange: (String) -> Unit,
    coverageBoundary: String,
    onCoverageBoundaryChange: (String) -> Unit,
    phone: String,
    onPhoneChange: (String) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    whatsapp: String,
    onWhatsappChange: (String) -> Unit,
    website: String,
    onWebsiteChange: (String) -> Unit,
    facebook: String,
    onFacebookChange: (String) -> Unit,
    instagram: String,
    onInstagramChange: (String) -> Unit,
    serviceGroups: List<GroupedTypeCategory>,
    selectedServices: Set<String>,
    onToggleService: (String) -> Unit,
    titleLabel: String,
    descriptionLabel: String,
    addressLabel: String,
    searchLabel: String,
    currentLocationLabel: String,
    latitudeLabel: String,
    longitudeLabel: String,
    boundaryLabel: String,
    servicesLabel: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
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
                onValueChange = onTitleChange,
                label = { Text(titleLabel) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = description,
                onValueChange = onDescriptionChange,
                label = { Text(descriptionLabel) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = address,
                    onValueChange = onAddressChange,
                    label = { Text(addressLabel) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Button(onClick = onSearchAddress, enabled = !geocodingBusy) {
                    Text(if (geocodingBusy) stringResource(R.string.road_assistance_searching) else searchLabel)
                }
            }

            if (suggestions.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                ) {
                    suggestions.forEach { suggestion ->
                        Text(
                            text = suggestion.displayName,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSuggestionSelected(suggestion) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            OutlinedButton(
                onClick = onUseCurrentLocation,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(currentLocationLabel)
            }

            val boundaryPoints = RoadAssistanceBoundaryMapUtils.parseBoundary(coverageBoundary)
            RoadAssistanceBoundaryMapCard(
                latitude = latitude,
                longitude = longitude,
                boundaryPoints = boundaryPoints,
                onBoundaryChanged = { points ->
                    onCoverageBoundaryChange(RoadAssistanceBoundaryMapUtils.serializeBoundary(points))
                },
                onLatitudeLongitudeChanged = { lat, lng ->
                    onLatitudeChange(lat.toString())
                    onLongitudeChange(lng.toString())
                },
                onFullscreenClick = onFullscreenClick,
                showControls = false
            )

            OutlinedTextField(
                value = coverageBoundary,
                onValueChange = onCoverageBoundaryChange,
                label = { Text(boundaryLabel) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5,
                singleLine = false
            )

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.road_assistance_contact_details),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = phone,
                            onValueChange = onPhoneChange,
                            label = { Text(stringResource(R.string.road_assistance_phone)) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = email,
                            onValueChange = onEmailChange,
                            label = { Text(stringResource(R.string.road_assistance_email)) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = whatsapp,
                            onValueChange = onWhatsappChange,
                            label = { Text(stringResource(R.string.road_assistance_whatsapp)) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = website,
                            onValueChange = onWebsiteChange,
                            label = { Text(stringResource(R.string.road_assistance_website)) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = facebook,
                            onValueChange = onFacebookChange,
                            label = { Text(stringResource(R.string.road_assistance_facebook)) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = instagram,
                            onValueChange = onInstagramChange,
                            label = { Text(stringResource(R.string.road_assistance_instagram)) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = latitude,
                    onValueChange = onLatitudeChange,
                    label = { Text(latitudeLabel) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = longitude,
                    onValueChange = onLongitudeChange,
                    label = { Text(longitudeLabel) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            GroupedTypeSelector(
                title = servicesLabel,
                groups = serviceGroups,
                selectedValues = selectedServices,
                onToggle = onToggleService
            )
        }
    }
}
