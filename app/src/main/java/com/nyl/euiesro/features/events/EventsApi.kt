package com.nyl.euiesro.features.events

import android.content.Context
import android.net.Uri
import android.util.Log
import com.nyl.euiesro.ApiClient
import com.nyl.euiesro.EventOrganizerResolver
import com.nyl.euiesro.MediaUrlResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL

object EventsApi {
    private const val TAG = "EventsApi"

    private fun responseIndicatesUploadFailure(responseBody: String): Boolean {
        val cleaned = responseBody.lowercase()
        return listOf(
            "unauthorized",
            "forbidden",
            "invalid cover",
            "missing cover",
            "cover image",
            "upload failed",
            "unable to create"
        ).any { cleaned.contains(it) }
    }

    suspend fun uploadEventCoverImage(context: Context, imageUri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val url = URL("${ApiClient.baseUrl.trimEnd('/')}/api/events/upload-cover")
            val connection = ApiClient.prepareRequest(url, "POST")
            val boundary = "----EuIesroEventCoverUpload${System.currentTimeMillis()}"
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            connection.setRequestProperty("Accept", "application/json,text/plain,*/*")

            connection.outputStream.use { outputStream ->
                val fileName = "event_cover_${System.currentTimeMillis()}.jpg"
                val mimeType = context.contentResolver.getType(imageUri) ?: "image/jpeg"

                outputStream.write("--$boundary\r\n".toByteArray(Charsets.UTF_8))
                outputStream.write("Content-Disposition: form-data; name=\"cover_image\"; filename=\"$fileName\"\r\n".toByteArray(Charsets.UTF_8))
                outputStream.write("Content-Type: $mimeType\r\n\r\n".toByteArray(Charsets.UTF_8))
                context.contentResolver.openInputStream(imageUri)?.use { input ->
                    input.copyTo(outputStream)
                }
                outputStream.write("\r\n--$boundary--\r\n".toByteArray(Charsets.UTF_8))
                outputStream.flush()
            }

            val status = connection.responseCode
            val responseBody = ApiClient.readBody(connection)
            if (status !in 200..299) {
                Log.e(TAG, "uploadEventCoverImage failed: HTTP $status body=$responseBody")
                return@withContext null
            }
            if (responseIndicatesUploadFailure(responseBody)) {
                return@withContext null
            }

            val root = JSONObject(responseBody)
            val data = if (root.has("data") && root.opt("data") is JSONObject) root.getJSONObject("data") else root
            val raw = data.optString("cover_image_url", "").trim().ifBlank { root.optString("cover_image_url", "") }
            MediaUrlResolver.resolve(raw)
        } catch (e: Exception) {
            Log.e(TAG, "uploadEventCoverImage failed", e)
            null
        }
    }

    suspend fun createEvent(data: EventFormData): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("${ApiClient.baseUrl.trimEnd('/')}/api/events/create")
            val connection = ApiClient.prepareRequest(url, "POST")
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")

            val payload = JSONObject()
                .put("title", data.title.trim())
                .put("description", data.description.trim())
                .put("start_at", data.startAt.trim())
                .put("end_at", data.endAt.trim())
                .put("timezone", data.timezone.trim())
                .put("location_name", data.locationName.trim())
                .put("location_address", data.locationAddress.trim())
                .put("location_lat", data.latitude.trim())
                .put("location_lng", data.longitude.trim())
                .put("whatsapp_group_url", data.whatsappGroupUrl.trim())
                .put("organizer_is_not_user", if (data.organizerIsNotUser) 1 else 0)
                .put("organizer_name", data.organizerName.trim())
                .put("organizer_whatsapp_country_code", data.organizerWhatsappCountryCode.trim())
                .put("organizer_whatsapp_number", data.organizerWhatsappNumber.trim())
                .put("cover_image_url", data.coverImageUrl.trim())

            val body = payload.toString()
            connection.outputStream.use { output ->
                output.write(body.toByteArray(Charsets.UTF_8))
                output.flush()
            }

            val status = connection.responseCode
            val responseBody = ApiClient.readBody(connection)
            if (status !in 200..299) {
                Log.e(TAG, "createEvent failed: HTTP $status body=$responseBody")
                return@withContext false
            }
            val json = JSONObject(responseBody)
            json.optBoolean("ok", true) || json.has("id") || json.optString("message", "").contains("created", ignoreCase = true)
        } catch (e: Exception) {
            Log.e(TAG, "createEvent failed", e)
            false
        }
    }

    suspend fun updateEvent(eventId: Int, data: EventFormData): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("${ApiClient.baseUrl.trimEnd('/')}/api/events/update")
            val connection = ApiClient.prepareRequest(url, "POST")
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")

            val payload = JSONObject()
                .put("id", eventId)
                .put("title", data.title.trim())
                .put("description", data.description.trim())
                .put("start_at", data.startAt.trim())
                .put("end_at", data.endAt.trim())
                .put("timezone", data.timezone.trim())
                .put("location_name", data.locationName.trim())
                .put("location_address", data.locationAddress.trim())
                .put("location_lat", data.latitude.trim())
                .put("location_lng", data.longitude.trim())
                .put("whatsapp_group_url", data.whatsappGroupUrl.trim())
                .put("organizer_is_not_user", if (data.organizerIsNotUser) 1 else 0)
                .put("organizer_name", data.organizerName.trim())
                .put("organizer_whatsapp_country_code", data.organizerWhatsappCountryCode.trim())
                .put("organizer_whatsapp_number", data.organizerWhatsappNumber.trim())
                .put("cover_image_url", data.coverImageUrl.trim())

            val body = payload.toString()
            connection.outputStream.use { output ->
                output.write(body.toByteArray(Charsets.UTF_8))
                output.flush()
            }

            val status = connection.responseCode
            val responseBody = ApiClient.readBody(connection)
            if (status !in 200..299) {
                Log.e(TAG, "updateEvent failed: HTTP $status body=$responseBody")
                return@withContext false
            }
            val json = JSONObject(responseBody)
            json.optBoolean("ok", true) || json.has("id") || json.optString("message", "").contains("updated", ignoreCase = true)
        } catch (e: Exception) {
            Log.e(TAG, "updateEvent failed", e)
            false
        }
    }

    suspend fun fetchUpcomingEvents(limit: Int = 10, scope: String = "upcoming"): List<EventSummary> = withContext(Dispatchers.IO) {
        try {
            val url = URL("${ApiClient.baseUrl.trimEnd('/')}/api/events?scope=${scope}&limit=$limit")
            val connection = ApiClient.prepareRequest(url, "GET")
            val status = connection.responseCode
            if (status !in 200..299) {
                Log.e(TAG, "Event fetch failed: HTTP $status")
                return@withContext emptyList()
            }

            val body = ApiClient.readBody(connection)
            val root = JSONObject(body)
            val data = root.optJSONObject("data") ?: JSONObject()
            val items = data.optJSONArray("items") ?: JSONArray()
            val result = mutableListOf<EventSummary>()
            for (i in 0 until items.length()) {
                val obj = items.getJSONObject(i)
                result.add(obj.toEventSummary())
            }
            result
        } catch (e: Exception) {
            Log.e(TAG, "fetchUpcomingEvents failed", e)
            emptyList()
        }
    }

    suspend fun fetchEventDetail(eventId: Int): EventSummary? = withContext(Dispatchers.IO) {
        try {
            val url = URL("${ApiClient.baseUrl.trimEnd('/')}/api/events/detail?id=$eventId")
            val connection = ApiClient.prepareRequest(url, "GET")
            val status = connection.responseCode
            if (status !in 200..299) {
                Log.e(TAG, "Event detail fetch failed: HTTP $status")
                return@withContext null
            }

            val body = ApiClient.readBody(connection)
            val root = JSONObject(body)
            val obj = when {
                root.has("data") && root.opt("data") is JSONObject -> root.getJSONObject("data")
                else -> root
            }
            obj.toEventSummary().takeIf { it.id > 0 }
        } catch (e: Exception) {
            Log.e(TAG, "fetchEventDetail failed", e)
            null
        }
    }

    suspend fun fetchEventUserState(eventId: Int): EventUserState = withContext(Dispatchers.IO) {
        try {
            val url = URL("${ApiClient.baseUrl.trimEnd('/')}/api/events/state?id=$eventId")
            val connection = ApiClient.prepareRequest(url, "GET")
            val status = connection.responseCode
            if (status !in 200..299) {
                Log.e(TAG, "Event user state fetch failed: HTTP $status")
                return@withContext EventUserState()
            }

            val body = ApiClient.readBody(connection)
            val root = JSONObject(body)
            val data = if (root.has("data") && root.opt("data") is JSONObject) root.getJSONObject("data") else root
            EventUserState(
                rsvpStatus = data.optNullableString("user_rsvp_status"),
                reaction = data.optNullableString("user_reaction")
            )
        } catch (e: Exception) {
            Log.e(TAG, "fetchEventUserState failed", e)
            EventUserState()
        }
    }

    suspend fun submitRsvp(eventId: Int, status: String?): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("${ApiClient.baseUrl.trimEnd('/')}/api/events/rsvp")
            val connection = ApiClient.prepareRequest(url, "POST")
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            val payload = JSONObject()
                .put("id", eventId)
                .put("status", status ?: JSONObject.NULL)
            val body = payload.toString()
            connection.outputStream.use { output ->
                output.write(body.toByteArray(Charsets.UTF_8))
                output.flush()
            }
            val httpStatus = connection.responseCode
            val responseBody = ApiClient.readBody(connection)
            if (httpStatus !in 200..299) {
                Log.e(TAG, "submitRsvp failed: HTTP $httpStatus body=$responseBody")
                return@withContext false
            }
            val json = JSONObject(responseBody)
            json.optBoolean("ok", false)
        } catch (e: Exception) {
            Log.e(TAG, "submitRsvp failed", e)
            false
        }
    }

    suspend fun submitComment(eventId: Int, content: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val clean = content.trim()
            if (clean.isEmpty()) return@withContext false
            val url = URL("${ApiClient.baseUrl.trimEnd('/')}/api/events/comment")
            val connection = ApiClient.prepareRequest(url, "POST")
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            val payload = JSONObject()
                .put("id", eventId)
                .put("content", clean)
            val body = payload.toString()
            connection.outputStream.use { output ->
                output.write(body.toByteArray(Charsets.UTF_8))
                output.flush()
            }
            val httpStatus = connection.responseCode
            val responseBody = ApiClient.readBody(connection)
            if (httpStatus !in 200..299) {
                Log.e(TAG, "submitComment failed: HTTP $httpStatus body=$responseBody")
                return@withContext false
            }
            JSONObject(responseBody).optBoolean("ok", false)
        } catch (e: Exception) {
            Log.e(TAG, "submitComment failed", e)
            false
        }
    }

    suspend fun submitUpdate(eventId: Int, content: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val clean = content.trim()
            if (clean.isEmpty()) return@withContext false
            val url = URL("${ApiClient.baseUrl.trimEnd('/')}/api/events/post-update")
            val connection = ApiClient.prepareRequest(url, "POST")
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            val payload = JSONObject()
                .put("id", eventId)
                .put("content", clean)
            val body = payload.toString()
            connection.outputStream.use { output ->
                output.write(body.toByteArray(Charsets.UTF_8))
                output.flush()
            }
            val httpStatus = connection.responseCode
            val responseBody = ApiClient.readBody(connection)
            if (httpStatus !in 200..299) {
                Log.e(TAG, "submitUpdate failed: HTTP $httpStatus body=$responseBody")
                return@withContext false
            }
            JSONObject(responseBody).optBoolean("ok", false)
        } catch (e: Exception) {
            Log.e(TAG, "submitUpdate failed", e)
            false
        }
    }

    suspend fun submitReaction(eventId: Int, reaction: String?): Boolean = withContext(Dispatchers.IO) {
        try {
            val normalized = reaction?.trim()?.lowercase() ?: ""
            if (normalized !in setOf("like", "dislike")) {
                return@withContext false
            }
            val url = URL("${ApiClient.baseUrl.trimEnd('/')}/api/events/reaction")
            val connection = ApiClient.prepareRequest(url, "POST")
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            val payload = JSONObject()
                .put("id", eventId)
                .put("reaction", normalized)
            val body = payload.toString()
            connection.outputStream.use { output ->
                output.write(body.toByteArray(Charsets.UTF_8))
                output.flush()
            }
            val httpStatus = connection.responseCode
            val responseBody = ApiClient.readBody(connection)
            if (httpStatus !in 200..299) {
                Log.e(TAG, "submitReaction failed: HTTP $httpStatus body=$responseBody")
                return@withContext false
            }
            val json = JSONObject(responseBody)
            json.optBoolean("ok", false)
        } catch (e: Exception) {
            Log.e(TAG, "submitReaction failed", e)
            false
        }
    }

    internal fun JSONObject.toEventSummary(): EventSummary {
        val organizerIsNotUser = optInt("organizer_is_not_user", 0) == 1
        val organizerName = optNullableString("organizer_name")
        val customOrganizerName = if (organizerIsNotUser) organizerName else null
        val resolvedOrganizer = EventOrganizerResolver.resolve(
            customName = organizerName,
            isCustomOrganizer = organizerIsNotUser,
            userName = optNullableString("user_name"),
            username = optNullableString("username")
        )

        val joinedAttendees = mutableListOf<String>()
        optJSONArray("attendees")?.let { array ->
            for (i in 0 until array.length()) {
                val attendee = array.getJSONObject(i)
                val name = attendee.optString("guest_name", "")
                    .ifBlank { attendee.optString("user_name", "") }
                    .ifBlank { attendee.optString("username", "") }
                    .ifBlank { attendee.optString("guest_email", "") }
                    .ifBlank { attendee.optString("email", "") }
                if (name.isNotBlank()) joinedAttendees.add(name)
            }
        }

        val interestedAttendees = mutableListOf<String>()
        optJSONArray("interested_attendees")?.let { array ->
            for (i in 0 until array.length()) {
                val attendee = array.getJSONObject(i)
                val name = attendee.optString("guest_name", "")
                    .ifBlank { attendee.optString("user_name", "") }
                    .ifBlank { attendee.optString("username", "") }
                    .ifBlank { attendee.optString("guest_email", "") }
                    .ifBlank { attendee.optString("email", "") }
                if (name.isNotBlank()) interestedAttendees.add(name)
            }
        }

        val updates = mutableListOf<EventUpdate>()
        optJSONArray("updates")?.let { array ->
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val authorName = obj.optString("name", "").ifBlank { obj.optString("username", "") }
                updates.add(
                    EventUpdate(
                        id = obj.optInt("id", 0),
                        userName = authorName.ifBlank { null },
                        username = obj.optNullableString("username"),
                        content = obj.optString("content", ""),
                        createdAt = obj.optNullableString("created_at")
                    )
                )
            }
        }

        val comments = mutableListOf<EventComment>()
        optJSONArray("comments")?.let { array ->
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val authorName = obj.optString("user_name", "").ifBlank { obj.optString("username", "") }
                comments.add(
                    EventComment(
                        id = obj.optInt("id", 0),
                        userName = authorName.ifBlank { null },
                        username = obj.optNullableString("username"),
                        content = obj.optString("content", ""),
                        createdAt = obj.optNullableString("created_at")
                    )
                )
            }
        }

        return EventSummary(
            id = optInt("id", 0),
            title = optString("title", "Untitled"),
            startAt = optNullableString("start_at"),
            endAt = optNullableString("end_at"),
            locationName = optNullableString("location_name"),
            locationAddress = optNullableString("location_address"),
            latitude = optDouble("location_lat", Double.NaN).takeUnless { it.isNaN() },
            longitude = optDouble("location_lng", Double.NaN).takeUnless { it.isNaN() },
            status = optNullableString("status"),
            description = optNullableString("description"),
            coverImageUrl = MediaUrlResolver.resolve(optNullableString("cover_image_url")),
            organizerName = resolvedOrganizer,
            organizerIsNotUser = organizerIsNotUser,
            organizerCustomName = customOrganizerName,
            organizerWhatsappCountryCode = optNullableString("organizer_whatsapp_country_code"),
            organizerWhatsappNumber = optNullableString("organizer_whatsapp_number"),
            whatsappGroupUrl = MediaUrlResolver.resolve(optNullableString("whatsapp_group_url")),
            userName = optNullableString("user_name"),
            username = optNullableString("username"),
            userRsvpStatus = optNullableString("user_rsvp_status"),
            userReaction = optNullableString("user_reaction"),
            likeCount = optInt("like_count", optInt("likes_count", 0)),
            dislikeCount = optInt("dislike_count", optInt("dislikes_count", 0)),
            interestedCount = optInt("interested_count", 0),
            goingCount = optInt("going_count", 0),
            joinedAttendees = joinedAttendees,
            interestedAttendees = interestedAttendees,
            updates = updates,
            comments = comments
        )
    }

    private fun JSONObject.optNullableString(name: String): String? {
        if (isNull(name)) return null
        val value = optString(name, "")
        return if (value.isBlank()) null else value
    }
}
