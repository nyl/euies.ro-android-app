package com.nyl.euiesro.features.events

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.nyl.euiesro.EuIesroTopAppBar
import com.nyl.euiesro.InteractiveLocationMap
import com.nyl.euiesro.common.LocationUiItem
import com.nyl.euiesro.common.ModuleSection
import com.nyl.euiesro.common.ShareUrlBuilder
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@Composable
fun EventDetailScreen(
    event: EventSummary,
    currentUserName: String = "Guest",
    onBack: () -> Unit,
    onEditEvent: () -> Unit = {},
    onEventUpdated: (EventSummary) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var eventData by remember(event.id) { mutableStateOf(event) }
    val details = eventData
    var userReaction by remember(details.id) { mutableStateOf(details.userReaction) }
    var likeCount by remember(details.id) { mutableStateOf(details.likeCount) }
    var dislikeCount by remember(details.id) { mutableStateOf(details.dislikeCount) }
    var userRsvpStatus by remember(details.id) { mutableStateOf(details.userRsvpStatus) }
    var goingCount by remember(details.id) { mutableStateOf(details.goingCount) }
    var interestedCount by remember(details.id) { mutableStateOf(details.interestedCount) }
    var commentDraft by remember(details.id) { mutableStateOf("") }
    var updateDraft by remember(details.id) { mutableStateOf("") }

    LaunchedEffect(event.id) {
        val fresh = EventsApi.fetchEventDetail(event.id)
        if (fresh != null) {
            eventData = fresh
            onEventUpdated(fresh)
        }

        val state = EventsApi.fetchEventUserState(event.id)
        if (state.rsvpStatus != null || state.reaction != null) {
            userRsvpStatus = state.rsvpStatus
            userReaction = state.reaction
        }
    }
    val effectiveUserReaction = userReaction ?: details.userReaction
    val effectiveRsvpStatus = userRsvpStatus ?: details.userRsvpStatus

    suspend fun refreshEventDetail() {
        val fresh = EventsApi.fetchEventDetail(details.id)
        if (fresh != null) {
            eventData = fresh
            userReaction = fresh.userReaction
            likeCount = fresh.likeCount
            dislikeCount = fresh.dislikeCount
            userRsvpStatus = fresh.userRsvpStatus
            goingCount = fresh.goingCount
            interestedCount = fresh.interestedCount
            onEventUpdated(fresh)
        }

        val state = EventsApi.fetchEventUserState(details.id)
        if (state.rsvpStatus != null || state.reaction != null) {
            userRsvpStatus = state.rsvpStatus
            userReaction = state.reaction
        }
    }

    LaunchedEffect(
        event.id,
        details.userReaction,
        details.likeCount,
        details.dislikeCount,
        details.userRsvpStatus,
        details.goingCount,
        details.interestedCount
    ) {
        userReaction = details.userReaction
        likeCount = details.likeCount
        dislikeCount = details.dislikeCount
        userRsvpStatus = details.userRsvpStatus
        goingCount = details.goingCount
        interestedCount = details.interestedCount
    }

    fun onReactionSelected(reaction: String) {
        val previous = userReaction
        val nextReaction = if (previous == reaction) null else reaction
        val oldLikeCount = likeCount
        val oldDislikeCount = dislikeCount
        val oldUserReaction = userReaction

        userReaction = nextReaction
        when {
            previous == null && reaction == "like" -> likeCount += 1
            previous == null && reaction == "dislike" -> dislikeCount += 1
            previous == "like" && reaction == "like" -> likeCount = (likeCount - 1).coerceAtLeast(0)
            previous == "dislike" && reaction == "dislike" -> dislikeCount = (dislikeCount - 1).coerceAtLeast(0)
            previous == "like" && reaction == "dislike" -> {
                likeCount = (likeCount - 1).coerceAtLeast(0)
                dislikeCount += 1
            }
            previous == "dislike" && reaction == "like" -> {
                dislikeCount = (dislikeCount - 1).coerceAtLeast(0)
                likeCount += 1
            }
        }

        scope.launch {
            val success = EventsApi.submitReaction(details.id, nextReaction)
            if (!success) {
                userReaction = oldUserReaction
                likeCount = oldLikeCount
                dislikeCount = oldDislikeCount
            } else {
                refreshEventDetail()
            }
        }
    }

    fun onRsvpSelected(status: String?) {
        val previous = userRsvpStatus
        val oldGoingCount = goingCount
        val oldInterestedCount = interestedCount
        val oldUserRsvpStatus = userRsvpStatus
        val requestStatus = if (previous == status) null else status

        when {
            previous == status -> {
                userRsvpStatus = null
                when (status) {
                    "attending" -> goingCount = (goingCount - 1).coerceAtLeast(0)
                    "interested" -> interestedCount = (interestedCount - 1).coerceAtLeast(0)
                }
            }
            previous == null && status == "attending" -> {
                userRsvpStatus = "attending"
                goingCount += 1
            }
            previous == null && status == "interested" -> {
                userRsvpStatus = "interested"
                interestedCount += 1
            }
            previous == "attending" && status == "interested" -> {
                userRsvpStatus = "interested"
                goingCount = (goingCount - 1).coerceAtLeast(0)
                interestedCount += 1
            }
            previous == "interested" && status == "attending" -> {
                userRsvpStatus = "attending"
                interestedCount = (interestedCount - 1).coerceAtLeast(0)
                goingCount += 1
            }
            previous == "attending" && status == null -> {
                userRsvpStatus = null
                goingCount = (goingCount - 1).coerceAtLeast(0)
            }
            previous == "interested" && status == null -> {
                userRsvpStatus = null
                interestedCount = (interestedCount - 1).coerceAtLeast(0)
            }
            else -> {
                userRsvpStatus = status
            }
        }

        scope.launch {
            val success = EventsApi.submitRsvp(details.id, requestStatus)
            if (!success) {
                userRsvpStatus = oldUserRsvpStatus
                goingCount = oldGoingCount
                interestedCount = oldInterestedCount
            } else {
                refreshEventDetail()
            }
        }
    }

    fun submitComment() {
        val trimmed = commentDraft.trim()
        if (trimmed.isEmpty()) return

        scope.launch {
            val success = EventsApi.submitComment(details.id, trimmed)
            if (success) {
                commentDraft = ""
                refreshEventDetail()
            }
        }
    }

    fun submitUpdate() {
        val trimmed = updateDraft.trim()
        if (trimmed.isEmpty()) return

        scope.launch {
            val success = EventsApi.submitUpdate(details.id, trimmed)
            if (success) {
                updateDraft = ""
                refreshEventDetail()
            }
        }
    }

    val directionsUri = when {
        details.latitude != null && details.longitude != null -> Uri.parse("geo:${details.latitude},${details.longitude}?q=${Uri.encode(details.locationName ?: details.locationAddress ?: details.title)}")
        !details.locationName.isNullOrBlank() -> Uri.parse("geo:0,0?q=${Uri.encode(details.locationName)}")
        !details.locationAddress.isNullOrBlank() -> Uri.parse("geo:0,0?q=${Uri.encode(details.locationAddress)}")
        else -> null
    }

    val organizerDisplayName = when {
        isAnonymousOrganizerName(details.organizerName) -> "Anonymous"
        !details.organizerName.isNullOrBlank() -> details.organizerName
        !details.userName.isNullOrBlank() -> details.userName
        !details.username.isNullOrBlank() -> details.username
        else -> "Anonymous"
    }
    val isAnonymous = isAnonymousOrganizerName(details.organizerName)
    val currentUserNormalized = currentUserName.trim()
    val isEventOrganizer = details.userName?.trim().equals(currentUserNormalized, ignoreCase = true) == true ||
        details.username?.trim().equals(currentUserNormalized, ignoreCase = true) == true ||
        details.organizerName?.trim().equals(currentUserNormalized, ignoreCase = true) == true
    val whatsappNumber = buildWhatsappNumber(details.organizerWhatsappCountryCode, details.organizerWhatsappNumber)
    val whatsappUrl = when {
        !details.whatsappGroupUrl.isNullOrBlank() -> details.whatsappGroupUrl
        !whatsappNumber.isNullOrBlank() -> "https://wa.me/$whatsappNumber"
        else -> null
    }
    val shareUrl = ShareUrlBuilder.buildFor(ModuleSection.EVENTS, details.title, details.id)
    val locationUiItems = if (details.latitude != null && details.longitude != null) {
        listOf(
            LocationUiItem(
                id = details.id,
                section = ModuleSection.EVENTS,
                title = details.title,
                description = details.description,
                address = details.locationName ?: details.locationAddress,
                latitude = details.latitude,
                longitude = details.longitude,
                phone = null,
                website = null,
                email = null,
                services = emptyList(),
                imageUrls = listOfNotNull(details.coverImageUrl),
                typeIds = emptyList(),
                markerIcons = emptyList()
            )
        )
    } else {
        emptyList()
    }

    Scaffold(
        topBar = {
            EuIesroTopAppBar(
                title = "Eveniment",
                onBack = onBack,
                onShare = {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareUrl)
                        putExtra(Intent.EXTRA_SUBJECT, details.title)
                    }
                    context.startActivity(Intent.createChooser(intent, "Share"))
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Box(modifier = Modifier.fillMaxWidth()) {
                    if (!details.coverImageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = details.coverImageUrl,
                            contentDescription = details.title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(290.dp),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(290.dp)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0x00000000),
                                        Color(0x66000000)
                                    )
                                )
                            )
                    )

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 16.dp, end = 16.dp, bottom = 18.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.92f)
                        ) {
                            Text(
                                text = formatEventDate(details.startAt),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = details.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(top = 10.dp)
                        )

                        val metaItems = mutableListOf<String>()
                        if (!details.endAt.isNullOrBlank()) metaItems += "Până la: ${formatEventDate(details.endAt)}"
                        if (!details.locationName.isNullOrBlank()) metaItems += details.locationName
                        if (!details.organizerName.isNullOrBlank()) metaItems += "Organizator: $organizerDisplayName"
                        if (metaItems.isNotEmpty()) {
                            Text(
                                text = metaItems.joinToString(" • "),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isAnonymous) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            } else {
                                Text(
                                    text = organizerDisplayName.take(1).uppercase(Locale.getDefault()),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Column(modifier = Modifier.padding(start = 12.dp)) {
                            Text(
                                text = organizerDisplayName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isAnonymous) "Hidden organizer" else "Organizator",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Despre eveniment",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        details.locationName?.let {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 12.dp)) {
                                Text("📍", modifier = Modifier.padding(end = 8.dp))
                                Text(it, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }

                        if (!details.description.isNullOrBlank()) {
                            Text(
                                text = details.description,
                                modifier = Modifier.padding(top = 12.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                            )
                        }
                    }
                }
            }

            item {
                val attendanceState = effectiveRsvpStatus
                when (attendanceState) {
                    "attending" -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { },
                                modifier = Modifier.weight(1f),
                                enabled = false,
                                shape = RoundedCornerShape(999.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.primary,
                                    disabledContentColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                )
                            ) {
                                Text("Înregistrat")
                            }
                            OutlinedButton(
                                onClick = { onRsvpSelected(null) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(999.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text("Nu mai particip")
                            }
                        }
                    }
                    "interested" -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { },
                                modifier = Modifier.weight(1f),
                                enabled = false,
                                shape = RoundedCornerShape(999.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.primary,
                                    disabledContentColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                )
                            ) {
                                Text("Interesat")
                            }
                            OutlinedButton(
                                onClick = { onRsvpSelected(null) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(999.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text("Nu mai sunt interesat")
                            }
                        }
                    }
                    else -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onRsvpSelected("attending") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(999.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text("Particip")
                            }
                            OutlinedButton(
                                onClick = { onRsvpSelected("interested") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(999.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text("Interesat")
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Reacții",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onReactionSelected("like") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(999.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (effectiveUserReaction == "like") MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                    contentColor = if (effectiveUserReaction == "like") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text("Like ($likeCount)")
                            }
                            OutlinedButton(
                                onClick = { onReactionSelected("dislike") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(999.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (effectiveUserReaction == "dislike") MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                    contentColor = if (effectiveUserReaction == "dislike") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text("Dislike ($dislikeCount)")
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Actualizări",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        if (details.updates.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = "Nu există actualizări încă.",
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                                )
                            }
                        } else {
                            Column(
                                modifier = Modifier.padding(top = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                details.updates.forEach { update ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(
                                                text = "${update.userName ?: update.username ?: "Membru"} • ${update.createdAt ?: ""}",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = update.content,
                                                modifier = Modifier.padding(top = 4.dp),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (isEventOrganizer) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 12.dp)) {
                                Button(
                                    onClick = onEditEvent,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Editează eveniment")
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    androidx.compose.material3.TextField(
                                        value = updateDraft,
                                        onValueChange = { updateDraft = it },
                                        modifier = Modifier.weight(1f),
                                        placeholder = { Text("Lasa un update...") },
                                        singleLine = false,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    Button(
                                        onClick = { submitUpdate() },
                                        modifier = Modifier.padding(start = 8.dp),
                                        enabled = updateDraft.isNotBlank()
                                    ) {
                                        Text("Trimite")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Comentarii",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        if (details.comments.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = "Nu există comentarii încă.",
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                                )
                            }
                        } else {
                            Column(
                                modifier = Modifier.padding(top = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                details.comments.forEach { comment ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(
                                                text = "${comment.userName ?: comment.username ?: "Membru"} • ${comment.createdAt ?: ""}",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = comment.content,
                                                modifier = Modifier.padding(top = 4.dp),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.padding(top = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            androidx.compose.material3.TextField(
                                value = commentDraft,
                                onValueChange = { commentDraft = it },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("Lasa un comentariu...") },
                                singleLine = false,
                                shape = RoundedCornerShape(12.dp)
                            )
                            Button(
                                onClick = { submitComment() },
                                modifier = Modifier.padding(start = 8.dp),
                                enabled = commentDraft.isNotBlank()
                            ) {
                                Text("Trimite")
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Participanți",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Participanți: $goingCount",
                            modifier = Modifier.padding(top = 10.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "Interesați: $interestedCount",
                            modifier = Modifier.padding(top = 4.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )

                        if (details.joinedAttendees.isNotEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                details.joinedAttendees.forEach { attendee ->
                                    Surface(
                                        shape = RoundedCornerShape(999.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = attendee,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                        }

                        if (details.interestedAttendees.isNotEmpty()) {
                            Text(
                                text = "Interesați:",
                                modifier = Modifier.padding(top = 12.dp),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                details.interestedAttendees.forEach { attendee ->
                                    Surface(
                                        shape = RoundedCornerShape(999.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = attendee,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Contact",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        whatsappUrl?.let {
                            Button(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(it))
                                    context.startActivity(intent)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp)
                            ) {
                                Text(if (details.whatsappGroupUrl != null) "Open WhatsApp group" else "Open WhatsApp")
                            }
                        }

                        if (!whatsappNumber.isNullOrBlank()) {
                            Text(
                                text = whatsappNumber,
                                modifier = Modifier.padding(top = 10.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        }

                        if (whatsappUrl == null && details.locationName.isNullOrBlank() && details.locationAddress.isNullOrBlank()) {
                            Text(
                                text = "Nu există informații de contact disponibile.",
                                modifier = Modifier.padding(top = 10.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            if (!details.locationName.isNullOrBlank() || !details.locationAddress.isNullOrBlank() || details.latitude != null || details.longitude != null) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Map & directions",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = details.locationName ?: details.locationAddress ?: "Locație disponibilă",
                                modifier = Modifier.padding(top = 8.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )

                            if (locationUiItems.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(220.dp)
                                        .padding(top = 12.dp)
                                ) {
                                    InteractiveLocationMap(
                                        items = locationUiItems,
                                        modifier = Modifier.fillMaxSize(),
                                        interactive = false
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 12.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                        .padding(16.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Text(
                                        text = "Harta nu este disponibilă în acest moment. Folosește indicațiile pentru navigare.",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }

                            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    if (directionsUri != null) {
                                        val intent = Intent(Intent.ACTION_VIEW, directionsUri)
                                        context.startActivity(intent)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = directionsUri != null
                            ) {
                                Text("Directions")
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 20.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Informații",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Status: ${details.status ?: "active"}",
                            modifier = Modifier.padding(top = 10.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Data: ${formatEventDate(details.startAt)}",
                            modifier = Modifier.padding(top = 4.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Până la: ${details.endAt?.let { formatEventDate(it) } ?: "N/A"}",
                            modifier = Modifier.padding(top = 4.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Interesați: $interestedCount",
                            modifier = Modifier.padding(top = 4.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Participanți: $goingCount",
                            modifier = Modifier.padding(top = 4.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        details.locationName?.let {
                            Text(
                                text = "Locație: $it",
                                modifier = Modifier.padding(top = 4.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

