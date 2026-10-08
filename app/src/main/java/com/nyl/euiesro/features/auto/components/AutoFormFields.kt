package com.nyl.euiesro.features.auto.components

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
import androidx.compose.ui.unit.dp
import com.nyl.euiesro.common.GroupedTypeCategory
import com.nyl.euiesro.common.GroupedTypeSelector
import com.nyl.euiesro.common.WorkingHoursEditor
import com.nyl.euiesro.common.WorkingHoursEntry
import com.nyl.euiesro.features.launchspots.AddressSuggestion
import com.nyl.euiesro.features.launchspots.components.AddSpotMapCard

@Composable
fun AutoFormFields(
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
    latitude: String,
    onLatitudeChange: (String) -> Unit,
    longitude: String,
    onLongitudeChange: (String) -> Unit,
    serviceGroups: List<GroupedTypeCategory>,
    selectedServices: Set<String>,
    onToggleService: (String) -> Unit,
    workingHours: Map<String, WorkingHoursEntry>,
    onWorkingHoursChange: (String, WorkingHoursEntry) -> Unit,
    titleLabel: String,
    descriptionLabel: String,
    addressLabel: String,
    searchLabel: String,
    currentLocationLabel: String,
    latitudeLabel: String,
    longitudeLabel: String,
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
                    Text(if (geocodingBusy) "Searching..." else searchLabel)
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

            AddSpotMapCard(
                latitude = latitude,
                longitude = longitude,
                onMarkerMoved = { lat, lng ->
                    onLatitudeChange(lat.toString())
                    onLongitudeChange(lng.toString())
                },
                onFullscreenClick = null
            )

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

            WorkingHoursEditor(
                title = "Program de lucru",
                workingHours = workingHours,
                onEntryChange = onWorkingHoursChange
            )

            GroupedTypeSelector(
                title = servicesLabel,
                groups = serviceGroups,
                selectedValues = selectedServices,
                onToggle = onToggleService
            )
        }
    }
}
