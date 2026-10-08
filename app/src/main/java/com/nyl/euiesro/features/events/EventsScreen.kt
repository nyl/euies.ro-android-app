package com.nyl.euiesro.features.events

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.nyl.euiesro.common.LocationUiItem
import com.nyl.euiesro.common.ModuleSection
import com.nyl.euiesro.ui.theme.EuIesroTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@Composable
fun EventsSection(
    innerPadding: PaddingValues,
    onOpenEvent: (EventSummary) -> Unit,
    onAddEvent: () -> Unit = {}
) {
    var events by remember { mutableStateOf<List<EventSummary>>(emptyList()) }
    var loading by rememberSaveable { mutableStateOf(true) }
    var error by rememberSaveable { mutableStateOf("") }
    var selectedTab by rememberSaveable { mutableStateOf("upcoming") }

    LaunchedEffect(selectedTab) {
        loading = true
        error = ""
        events = EventsApi.fetchUpcomingEvents(limit = 20, scope = selectedTab)
        loading = false
        if (events.isEmpty()) {
            error = if (selectedTab == "upcoming") {
                "Nu există ieșiri viitoare disponibile momentan."
            } else {
                "Nu există ieșiri trecute de afișat."
            }
        }
    }

    EventsSectionContent(
        innerPadding = innerPadding,
        events = events,
        loading = loading,
        error = error,
        selectedTab = selectedTab,
        onTabSelected = { selectedTab = it },
        onOpenEvent = onOpenEvent,
        onAddEvent = onAddEvent
    )
}

@Composable
private fun EventsSectionContent(
    innerPadding: PaddingValues,
    events: List<EventSummary>,
    loading: Boolean,
    error: String,
    selectedTab: String,
    onTabSelected: (String) -> Unit,
    onOpenEvent: (EventSummary) -> Unit,
    onAddEvent: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background.copy(alpha = 0.96f)
                    )
                )
            )
            .padding(innerPadding)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(18.dp)
                )
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val tabs = listOf("upcoming" to "Viitoare", "past" to "Trecute")
            tabs.forEach { (value, label) ->
                val selected = selectedTab == value
                val containerColor = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
                val contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

                Button(
                    onClick = { onTabSelected(value) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = containerColor,
                        contentColor = contentColor
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = if (selected) 4.dp else 0.dp,
                        pressedElevation = 0.dp
                    )
                ) {
                    Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        if (loading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp))
            }
        } else if (error.isNotBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.padding(top = 12.dp)
            ) {
                items(events) { event ->
                    EventDetailCard(event, onClick = { onOpenEvent(event) })
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }

                item {
                    Button(
                        onClick = onAddEvent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text("Organizeaza iesire / eveniment")
                    }
                }
            }
        }
    }
}

@Composable
fun EventDetailCard(event: EventSummary, onClick: () -> Unit = {}) {
    val organizerDisplayName = if (isAnonymousOrganizerName(event.organizerName)) "Anonymous" else event.organizerName ?: "Anonymous"
    val isAnonymous = isAnonymousOrganizerName(event.organizerName)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(24.dp), clip = false)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f)
                            )
                        )
                    )
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = if (isAnonymous) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isAnonymous) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Text(
                            text = organizerDisplayName.take(1).uppercase(Locale.getDefault()),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .padding(start = 10.dp)
                        .weight(1f)
                ) {
                    Text(
                        text = organizerDisplayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = formatEventDate(event.startAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = if ((event.status ?: "active").lowercase() == "active") MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = (event.status ?: "active").replaceFirstChar { it.uppercase() },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = if ((event.status ?: "active").lowercase() == "active") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                    )
                }
            }

            if (!event.coverImageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = event.coverImageUrl,
                    contentDescription = event.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentScale = ContentScale.Crop
                )
            }

            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp)) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                event.locationName?.let {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("📍", modifier = Modifier.padding(end = 6.dp))
                        Text(it, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                if (event.description != null && event.description.isNotBlank()) {
                    Text(
                        text = event.description.trim().replace("\n", " ").take(120) + if (event.description.length > 120) "…" else "",
                        modifier = Modifier.padding(top = 10.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                    ) {
                        Text(
                            text = "${event.interestedCount} interested",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f)
                    ) {
                        Text(
                            text = "${event.goingCount} going",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }
}

internal fun isAnonymousOrganizerName(name: String?): Boolean {
    val normalized = name?.trim().orEmpty()
    return normalized.isEmpty() || normalized.equals("Community", ignoreCase = true) || normalized.equals("Anonymous", ignoreCase = true)
}

internal fun buildWhatsappNumber(countryCode: String?, phoneNumber: String?): String? {
    val normalizedCountry = countryCode?.replace(Regex("[^0-9]"), "") ?: ""
    val normalizedNumber = phoneNumber?.replace(Regex("[^0-9]"), "") ?: ""
    return when {
        normalizedCountry.isNotBlank() && normalizedNumber.isNotBlank() -> "$normalizedCountry$normalizedNumber"
        normalizedNumber.isNotBlank() -> normalizedNumber
        else -> null
    }
}

internal fun formatEventDate(dateValue: String?): String {
    if (dateValue.isNullOrBlank()) return "Data nedisponibilă"
    val patterns = listOf(
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"
    )
    for (pattern in patterns) {
        try {
            val parser = SimpleDateFormat(pattern, Locale.getDefault())
            parser.isLenient = false
            val parsed = parser.parse(dateValue)
            if (parsed != null) {
                val formatter = SimpleDateFormat("d MMM • HH:mm", Locale("ro")).apply {
                    timeZone = TimeZone.getDefault()
                }
                return formatter.format(parsed)
            }
        } catch (_: Exception) {
        }
    }
    return dateValue
}

@Preview(showBackground = true)
@Composable
fun EventsSectionPreview() {
    EuIesroTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            EventsSectionContent(
                innerPadding = PaddingValues(0.dp),
                events = listOf(
                    EventSummary(
                        id = 1,
                        title = "Bucharest Moto Day",
                        startAt = "2024-05-20T10:00:00Z",
                        locationName = "Piața Constituției",
                        status = "active",
                        description = "O zi dedicată pasionaților de motociclism din București și împrejurimi. Vom avea parade, concursuri și multă distracție.",
                        organizerName = "EuIes.ro Team",
                        interestedCount = 150,
                        goingCount = 85,
                        coverImageUrl = null
                    ),
                    EventSummary(
                        id = 2,
                        title = "Transfăgărășan Ride",
                        startAt = "2024-06-15T08:00:00Z",
                        locationName = "Bâlea Lac",
                        status = "active",
                        description = "Experience the best driving road in the world with fellow riders.",
                        organizerName = "Mountain Riders",
                        interestedCount = 350,
                        goingCount = 150,
                        coverImageUrl = null
                    )
                ),
                loading = false,
                error = "",
                selectedTab = "upcoming",
                onTabSelected = {},
                onOpenEvent = {}
            )
        }
    }
}
