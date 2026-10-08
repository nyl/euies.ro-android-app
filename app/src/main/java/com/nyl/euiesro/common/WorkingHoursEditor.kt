package com.nyl.euiesro.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.json.JSONObject

val workingDayOrder = listOf("monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday")

fun defaultWorkingHoursMap(): LinkedHashMap<String, WorkingHoursEntry> = linkedMapOf(
    "monday" to WorkingHoursEntry(type = "hours", open = "09:00", close = "18:00"),
    "tuesday" to WorkingHoursEntry(type = "hours", open = "09:00", close = "18:00"),
    "wednesday" to WorkingHoursEntry(type = "hours", open = "09:00", close = "18:00"),
    "thursday" to WorkingHoursEntry(type = "hours", open = "09:00", close = "18:00"),
    "friday" to WorkingHoursEntry(type = "hours", open = "09:00", close = "18:00"),
    "saturday" to WorkingHoursEntry(type = "hours", open = "09:00", close = "18:00"),
    "sunday" to WorkingHoursEntry(type = "closed", open = "", close = "")
)

fun workingHoursMapToJson(data: Map<String, WorkingHoursEntry>): JSONObject {
    val json = JSONObject()
    workingDayOrder.forEach { day ->
        val entry = data[day] ?: defaultWorkingHoursMap()[day]!!
        val type = if (entry.type in setOf("hours", "closed", "nonstop")) entry.type else "hours"
        val normalizedOpen = if (type == "hours") entry.open else ""
        val normalizedClose = if (type == "hours") entry.close else ""
        json.put(day, JSONObject().put("type", type).put("open", normalizedOpen).put("close", normalizedClose))
    }
    return json
}

data class WorkingHoursEntry(
    val type: String = "hours",
    val open: String = "09:00",
    val close: String = "18:00"
)

@Composable
fun WorkingHoursEditor(
    title: String = "Program de lucru",
    workingHours: Map<String, WorkingHoursEntry>,
    onEntryChange: (String, WorkingHoursEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall)

            workingDayOrder.forEach { day ->
                val entry = workingHours[day] ?: WorkingHoursEntry()
                val dayLabel = day.replaceFirstChar { it.uppercase() }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(dayLabel, style = MaterialTheme.typography.labelMedium)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("hours" to "Ore", "closed" to "Închis", "nonstop" to "Non-stop").forEach { (type, label) ->
                            val selected = entry.type == type
                            val buttonModifier = Modifier.weight(1f)
                            if (selected) {
                                FilledTonalButton(
                                    onClick = {
                                        val nextEntry = when (type) {
                                            "closed" -> WorkingHoursEntry(type = "closed", open = "", close = "")
                                            "nonstop" -> WorkingHoursEntry(type = "nonstop", open = "", close = "")
                                            else -> WorkingHoursEntry(type = "hours", open = entry.open.ifBlank { "09:00" }, close = entry.close.ifBlank { "18:00" })
                                        }
                                        onEntryChange(day, nextEntry)
                                    },
                                    modifier = buttonModifier,
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(label, style = MaterialTheme.typography.labelSmall)
                                }
                            } else {
                                OutlinedButton(
                                    onClick = {
                                        val nextEntry = when (type) {
                                            "closed" -> WorkingHoursEntry(type = "closed", open = "", close = "")
                                            "nonstop" -> WorkingHoursEntry(type = "nonstop", open = "", close = "")
                                            else -> WorkingHoursEntry(type = "hours", open = entry.open.ifBlank { "09:00" }, close = entry.close.ifBlank { "18:00" })
                                        }
                                        onEntryChange(day, nextEntry)
                                    },
                                    modifier = buttonModifier,
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                                ) {
                                    Text(label, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }

                    if (entry.type == "hours") {
                        Text(
                            text = "${entry.open.ifBlank { "09:00" }} - ${entry.close.ifBlank { "18:00" }}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedTextField(
                                value = entry.open,
                                onValueChange = { onEntryChange(day, entry.copy(open = it)) },
                                label = { Text("Open") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = entry.close,
                                onValueChange = { onEntryChange(day, entry.copy(close = it)) },
                                label = { Text("Close") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                    } else {
                        Text(
                            text = when (entry.type) {
                                "closed" -> "Închis"
                                else -> "Non-stop"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
