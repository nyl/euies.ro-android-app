package com.nyl.euiesro.features.moto

import android.content.Context
import android.net.Uri
import android.util.Log
import com.nyl.euiesro.ApiClient
import com.nyl.euiesro.MediaUrlResolver
import com.nyl.euiesro.common.WorkingHoursEntry
import com.nyl.euiesro.common.defaultWorkingHoursMap
import com.nyl.euiesro.common.workingHoursMapToJson
import com.nyl.euiesro.features.auto.AutoReview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder

object MotoApi {
    private const val TAG = "MotoApi"

    internal fun responseIndicatesUploadFailure(responseBody: String): Boolean {
        val cleaned = responseBody.lowercase()
        val markers = listOf(
            "please log in",
            "log in to continue",
            "login",
            "unauthorized",
            "forbidden",
            "alert error",
            "invalid username or password",
            "access denied"
        )
        return markers.any { cleaned.contains(it) }
    }

    suspend fun uploadMotoPlaceImages(context: Context, placeId: Int, imageUris: List<Uri>): Boolean = withContext(Dispatchers.IO) {
        if (placeId <= 0 || imageUris.isEmpty()) return@withContext false
        if (!ApiClient.ensureValidSession(context)) return@withContext false

        try {
            val url = URL("${ApiClient.baseUrl.trimEnd('/')}/index.php?p=view-moto-place&id=$placeId")
            val connection = ApiClient.prepareRequest(url, "POST")
            val boundary = "----EuIesroMotoUpload${System.currentTimeMillis()}"
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")

            connection.outputStream.use { outputStream ->
                outputStream.write("--$boundary\r\n".toByteArray(Charsets.UTF_8))
                outputStream.write("Content-Disposition: form-data; name=\"action\"\r\n\r\n".toByteArray(Charsets.UTF_8))
                outputStream.write("moto_upload_images".toByteArray(Charsets.UTF_8))
                outputStream.write("\r\n".toByteArray(Charsets.UTF_8))

                imageUris.forEachIndexed { index, uri ->
                    val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                    val ext = when {
                        mimeType.contains("png") -> "png"
                        mimeType.contains("webp") -> "webp"
                        else -> "jpg"
                    }
                    val fileName = "moto_place_${placeId}_${index}_${System.currentTimeMillis()}.$ext"
                    outputStream.write("--$boundary\r\n".toByteArray(Charsets.UTF_8))
                    outputStream.write("Content-Disposition: form-data; name=\"images[]\"; filename=\"$fileName\"\r\n".toByteArray(Charsets.UTF_8))
                    outputStream.write("Content-Type: $mimeType\r\n\r\n".toByteArray(Charsets.UTF_8))
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        input.copyTo(outputStream)
                    }
                    outputStream.write("\r\n".toByteArray(Charsets.UTF_8))
                }

                outputStream.write("--$boundary--\r\n".toByteArray(Charsets.UTF_8))
                outputStream.flush()
            }

            val status = connection.responseCode
            val body = ApiClient.readBody(connection)
            if (status !in 200..299) {
                Log.e(TAG, "uploadMotoPlaceImages failed: HTTP $status body=$body")
                return@withContext false
            }

            !responseIndicatesUploadFailure(body)
        } catch (e: Exception) {
            Log.e(TAG, "uploadMotoPlaceImages failed", e)
            false
        }
    }

    suspend fun fetchMotoPlaceDetail(id: Int): MotoPlaceSummary? = withContext(Dispatchers.IO) {
        try {
            val url = URL("${ApiClient.baseUrl.trimEnd('/')}/api/moto/detail?id=$id")
            val connection = ApiClient.prepareRequest(url, "GET")
            val status = connection.responseCode
            if (status !in 200..299) {
                Log.e(TAG, "Moto detail fetch failed: HTTP $status")
                return@withContext null
            }

            val body = ApiClient.readBody(connection)
            val root = JSONObject(body)
            val obj = if (root.has("data") && root.opt("data") is JSONObject) root.getJSONObject("data") else root
            val servicesArray = obj.optJSONArray("services") ?: JSONArray()
            val services = mutableListOf<String>()
            for (j in 0 until servicesArray.length()) {
                services.add(servicesArray.getString(j))
            }

            val imageUrls = mutableListOf<String>()
            val imagesArray = obj.optJSONArray("images") ?: JSONArray()
            for (j in 0 until imagesArray.length()) {
                val imageEntry = imagesArray.getJSONObject(j)
                val imageUrl = imageEntry.optString("image_url", "").ifBlank { imageEntry.optString("url", "") }
                val resolved = MediaUrlResolver.resolve(imageUrl)
                if (!resolved.isNullOrBlank()) imageUrls.add(resolved)
            }
            val imageUrlList = obj.optJSONArray("image_urls")
            if (imageUrlList != null && imageUrlList.length() > 0 && imageUrls.isEmpty()) {
                for (j in 0 until imageUrlList.length()) {
                    val value = imageUrlList.getString(j)
                    val resolved = MediaUrlResolver.resolve(value)
                    if (!resolved.isNullOrBlank()) imageUrls.add(resolved)
                }
            }

            val workingHours = linkedMapOf<String, String>()
            val rawWorkingHours = obj.optString("working_hours", "")
            if (rawWorkingHours.isNotBlank()) {
                try {
                    val workingHoursObject = JSONObject(rawWorkingHours)
                    val keys = workingHoursObject.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        val entry = workingHoursObject.optJSONObject(key) ?: continue
                        val type = entry.optString("type", "hours")
                        val open = entry.optString("open", "")
                        val close = entry.optString("close", "")
                        val display = when (type) {
                            "closed" -> "Închis"
                            "nonstop" -> "Non-stop"
                            else -> if (open.isNotBlank() && close.isNotBlank()) "$open - $close" else "Program nedisponibil"
                        }
                        workingHours[key] = display
                    }
                } catch (_: Exception) {
                }
            }

            val reviewsArray = obj.optJSONArray("reviews") ?: JSONArray()
            val reviews = mutableListOf<AutoReview>()
            for (j in 0 until reviewsArray.length()) {
                val reviewObj = reviewsArray.getJSONObject(j)
                reviews.add(
                    AutoReview(
                        id = reviewObj.optInt("id", 0),
                        userId = reviewObj.optInt("user_id", 0),
                        userName = reviewObj.optString("user_name", reviewObj.optString("username", "Anonymous")).ifBlank { "Anonymous" },
                        avatarUrl = reviewObj.optString("avatar_url", reviewObj.optString("user_avatar", "")).trim().ifBlank { null },
                        rating = reviewObj.optInt("rating", 0),
                        comment = reviewObj.optNullableString("comment"),
                        createdAt = reviewObj.optNullableString("created_at"),
                        ownerReply = reviewObj.optNullableString("owner_reply")
                    )
                )
            }

            MotoPlaceSummary(
                id = obj.optInt("id", id),
                title = obj.optString("title", "Untitled"),
                description = obj.optNullableString("description"),
                address = obj.optNullableString("address"),
                latitude = obj.optDouble("latitude", 0.0),
                longitude = obj.optDouble("longitude", 0.0),
                phone = obj.optNullableString("phone"),
                website = obj.optNullableString("website"),
                email = obj.optNullableString("email"),
                facebook = obj.optNullableString("facebook"),
                instagram = obj.optNullableString("instagram"),
                tiktok = obj.optNullableString("tiktok"),
                youtube = obj.optNullableString("youtube"),
                services = services,
                userId = obj.optInt("user_id", 0),
                ownerUserId = obj.optInt("owner_user_id", 0).takeIf { it > 0 } ?: obj.optInt("user_id", 0).takeIf { it > 0 },
                primaryImage = imageUrls.firstOrNull(),
                images = imageUrls,
                workingHours = workingHours,
                averageRating = obj.optDouble("average_rating", 0.0).toFloat(),
                reviews = reviews
            )
        } catch (e: Exception) {
            Log.e(TAG, "fetchMotoPlaceDetail failed", e)
            null
        }
    }

    fun parseWorkingHoursForEditor(rawHours: Map<String, String>): LinkedHashMap<String, WorkingHoursEntry> {
        val result = linkedMapOf<String, WorkingHoursEntry>()
        val days = listOf("monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday")
        days.forEach { day ->
            val value = rawHours[day].orEmpty()
            val entry = when {
                value.contains("-", ignoreCase = true) -> {
                    val match = Regex("""(\d{1,2}:\d{2})\s*[-–]\s*(\d{1,2}:\d{2})""").find(value)
                    if (match != null) WorkingHoursEntry(type = "hours", open = match.groupValues[1], close = match.groupValues[2])
                    else WorkingHoursEntry(type = "hours", open = "09:00", close = "18:00")
                }
                value.contains("non-stop", ignoreCase = true) || value.contains("nonstop", ignoreCase = true) -> WorkingHoursEntry(type = "nonstop", open = "", close = "")
                value.contains("închis", ignoreCase = true) || value.contains("closed", ignoreCase = true) -> WorkingHoursEntry(type = "closed", open = "", close = "")
                else -> defaultWorkingHoursMap()[day] ?: WorkingHoursEntry(type = "hours", open = "09:00", close = "18:00")
            }
            result[day] = entry
        }
        return result
    }

    suspend fun createMotoPlace(
        title: String,
        description: String?,
        address: String?,
        latitude: Double,
        longitude: Double,
        phone: String?,
        website: String?,
        email: String?,
        facebook: String?,
        instagram: String?,
        tiktok: String?,
        youtube: String?,
        services: List<String>,
        isOwner: Boolean = false,
        workingHours: Map<String, WorkingHoursEntry> = emptyMap()
    ): Int? = withContext(Dispatchers.IO) {
        try {
            val url = URL("${ApiClient.baseUrl.trimEnd('/')}/api/moto/create")
            val connection = ApiClient.prepareRequest(url, "POST")
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")

            val payload = JSONObject()
                .put("title", title.trim())
                .put("description", description?.trim().orEmpty())
                .put("address", address?.trim().orEmpty())
                .put("latitude", latitude)
                .put("longitude", longitude)
                .put("phone", phone?.trim().orEmpty())
                .put("website", website?.trim().orEmpty())
                .put("email", email?.trim().orEmpty())
                .put("facebook", facebook?.trim().orEmpty())
                .put("instagram", instagram?.trim().orEmpty())
                .put("tiktok", tiktok?.trim().orEmpty())
                .put("youtube", youtube?.trim().orEmpty())
                .put("working_hours", workingHoursMapToJson(if (workingHours.isEmpty()) defaultWorkingHoursMap() else workingHours))
                .put("services", JSONArray(services))
                .put("is_owner", isOwner)

            connection.outputStream.use { output ->
                output.write(payload.toString().toByteArray(Charsets.UTF_8))
                output.flush()
            }

            val status = connection.responseCode
            val responseBody = ApiClient.readBody(connection)
            if (status !in 200..299) {
                Log.e(TAG, "createMotoPlace failed: HTTP $status body=$responseBody")
                return@withContext null
            }

            val root = JSONObject(responseBody)
            val data = root.optJSONObject("data") ?: root
            val id = data.optInt("id", 0)
            if (id > 0) id else null
        } catch (e: Exception) {
            Log.e(TAG, "createMotoPlace failed", e)
            null
        }
    }

    suspend fun submitReview(id: Int, rating: Int, comment: String): Boolean = withContext(Dispatchers.IO) {
        if (!ApiClient.validateSession()) return@withContext false
        try {
            val url = URL("${ApiClient.baseUrl.trimEnd('/')}/api/moto/review")
            val connection = ApiClient.prepareRequest(url, "POST")
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            val payload = JSONObject()
                .put("id", id)
                .put("rating", rating)
                .put("comment", comment.trim())
            val body = payload.toString()
            connection.outputStream.use { output ->
                output.write(body.toByteArray(Charsets.UTF_8))
                output.flush()
            }
            val status = connection.responseCode
            val responseBody = ApiClient.readBody(connection)
            if (status !in 200..299) {
                Log.e(TAG, "submitReview failed: HTTP $status body=$responseBody")
                return@withContext false
            }
            val json = JSONObject(responseBody)
            json.optBoolean("ok", false) || json.optBoolean("success", false)
        } catch (e: Exception) {
            Log.e(TAG, "submitReview failed", e)
            false
        }
    }

    suspend fun submitOwnerReply(placeId: Int, reviewId: Int, reply: String): Boolean = withContext(Dispatchers.IO) {
        if (!ApiClient.validateSession()) return@withContext false
        try {
            val url = URL("${ApiClient.baseUrl.trimEnd('/')}/api/moto/reply")
            val connection = ApiClient.prepareRequest(url, "POST")
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            val payload = JSONObject()
                .put("id", placeId)
                .put("review_id", reviewId)
                .put("reply", reply.trim())
            val body = payload.toString()
            connection.outputStream.use { output ->
                output.write(body.toByteArray(Charsets.UTF_8))
                output.flush()
            }
            val status = connection.responseCode
            val responseBody = ApiClient.readBody(connection)
            if (status !in 200..299) {
                Log.e(TAG, "submitOwnerReply failed: HTTP $status body=$responseBody")
                return@withContext false
            }
            val json = JSONObject(responseBody)
            json.optBoolean("ok", false) || json.optBoolean("success", false)
        } catch (e: Exception) {
            Log.e(TAG, "submitOwnerReply failed", e)
            false
        }
    }

    suspend fun updateMotoPlace(
        id: Int,
        title: String,
        description: String?,
        address: String?,
        latitude: Double,
        longitude: Double,
        phone: String?,
        website: String?,
        email: String?,
        facebook: String?,
        instagram: String?,
        tiktok: String?,
        youtube: String?,
        services: List<String>,
        isOwner: Boolean = false,
        workingHours: Map<String, WorkingHoursEntry> = emptyMap()
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("${ApiClient.baseUrl.trimEnd('/')}/api/moto/update")
            val connection = ApiClient.prepareRequest(url, "POST")
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")

            val payload = JSONObject()
                .put("id", id)
                .put("title", title.trim())
                .put("description", description?.trim().orEmpty())
                .put("address", address?.trim().orEmpty())
                .put("latitude", latitude)
                .put("longitude", longitude)
                .put("phone", phone?.trim().orEmpty())
                .put("website", website?.trim().orEmpty())
                .put("email", email?.trim().orEmpty())
                .put("facebook", facebook?.trim().orEmpty())
                .put("instagram", instagram?.trim().orEmpty())
                .put("tiktok", tiktok?.trim().orEmpty())
                .put("youtube", youtube?.trim().orEmpty())
                .put("working_hours", workingHoursMapToJson(if (workingHours.isEmpty()) defaultWorkingHoursMap() else workingHours))
                .put("services", JSONArray(services))
                .put("is_owner", isOwner)

            connection.outputStream.use { output ->
                output.write(payload.toString().toByteArray(Charsets.UTF_8))
                output.flush()
            }

            val status = connection.responseCode
            val responseBody = ApiClient.readBody(connection)
            if (status !in 200..299) {
                Log.e(TAG, "updateMotoPlace failed: HTTP $status body=$responseBody")
                return@withContext false
            }

            val root = JSONObject(responseBody)
            val data = root.optJSONObject("data") ?: root
            root.optBoolean("ok", false) || root.optBoolean("success", false) || data.has("id") || responseBody.contains("Moto place updated")
        } catch (e: Exception) {
            Log.e(TAG, "updateMotoPlace failed", e)
            false
        }
    }

    suspend fun submitMotoEditSuggestion(
        id: Int,
        title: String,
        description: String?,
        address: String?,
        latitude: Double,
        longitude: Double,
        phone: String?,
        website: String?,
        email: String?,
        facebook: String?,
        instagram: String?,
        tiktok: String?,
        youtube: String?,
        services: List<String>,
        workingHours: Map<String, WorkingHoursEntry> = emptyMap()
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("${ApiClient.baseUrl.trimEnd('/')}/api/moto/suggest-edit")
            val connection = ApiClient.prepareRequest(url, "POST")
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")

            val payload = JSONObject()
                .put("id", id)
                .put("title", title.trim())
                .put("description", description?.trim().orEmpty())
                .put("address", address?.trim().orEmpty())
                .put("latitude", latitude)
                .put("longitude", longitude)
                .put("phone", phone?.trim().orEmpty())
                .put("website", website?.trim().orEmpty())
                .put("email", email?.trim().orEmpty())
                .put("facebook", facebook?.trim().orEmpty())
                .put("instagram", instagram?.trim().orEmpty())
                .put("tiktok", tiktok?.trim().orEmpty())
                .put("youtube", youtube?.trim().orEmpty())
                .put("working_hours", workingHoursMapToJson(if (workingHours.isEmpty()) defaultWorkingHoursMap() else workingHours))
                .put("services", JSONArray(services))

            connection.outputStream.use { output ->
                output.write(payload.toString().toByteArray(Charsets.UTF_8))
                output.flush()
            }

            val status = connection.responseCode
            val responseBody = ApiClient.readBody(connection)
            if (status !in 200..299) {
                Log.e(TAG, "submitMotoEditSuggestion failed: HTTP $status body=$responseBody")
                return@withContext false
            }

            val root = JSONObject(responseBody)
            val data = root.optJSONObject("data") ?: root
            root.optBoolean("ok", false) || root.optBoolean("success", false) || data.has("id") || responseBody.contains("suggestion")
        } catch (e: Exception) {
            Log.e(TAG, "submitMotoEditSuggestion failed", e)
            false
        }
    }

    suspend fun fetchMotoPlaces(
        limit: Int = 500,
        query: String? = null,
        service: String? = null
    ): List<MotoPlaceSummary> = withContext(Dispatchers.IO) {
        try {
            val parameters = buildString {
                append("limit=")
                append(limit)
                if (!query.isNullOrBlank()) {
                    append("&q=")
                    append(URLEncoder.encode(query.trim(), "UTF-8"))
                }
                if (!service.isNullOrBlank()) {
                    append("&service=")
                    append(URLEncoder.encode(service.trim(), "UTF-8"))
                }
                append("&all=true")
            }
            val url = URL("${ApiClient.baseUrl.trimEnd('/')}/api/moto?$parameters")
            val connection = ApiClient.prepareRequest(url, "GET")
            val status = connection.responseCode
            if (status !in 200..299) {
                Log.e(TAG, "Moto fetch failed: HTTP $status")
                return@withContext emptyList()
            }

            val body = ApiClient.readBody(connection)
            val root = JSONObject(body)
            val data = root.optJSONObject("data") ?: JSONObject()
            val items = data.optJSONArray("items") ?: JSONArray()
            val result = mutableListOf<MotoPlaceSummary>()
            for (i in 0 until items.length()) {
                val obj = items.getJSONObject(i)
                val servicesArray = obj.optJSONArray("services") ?: JSONArray()
                val services = mutableListOf<String>()
                for (j in 0 until servicesArray.length()) {
                    services.add(servicesArray.getString(j))
                }
                val imageCandidates = mutableListOf<String>()
                val objImageUrls = obj.optJSONArray("image_urls")
                if (objImageUrls != null) {
                    for (j in 0 until objImageUrls.length()) {
                        val resolved = MediaUrlResolver.resolve(objImageUrls.getString(j))
                        if (!resolved.isNullOrBlank()) imageCandidates.add(resolved)
                    }
                }
                if (imageCandidates.isEmpty()) {
                    val directImage = obj.optNullableString("image_url")
                    val resolvedDirect = MediaUrlResolver.resolve(directImage)
                    if (!resolvedDirect.isNullOrBlank()) imageCandidates.add(resolvedDirect)
                }

                val workingHours = linkedMapOf<String, String>()
                val rawWorkingHours = obj.optString("working_hours", "")
                if (rawWorkingHours.isNotBlank()) {
                    try {
                        val workingHoursObject = JSONObject(rawWorkingHours)
                        val keys = workingHoursObject.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            val entry = workingHoursObject.optJSONObject(key) ?: continue
                            val type = entry.optString("type", "hours")
                            val open = entry.optString("open", "")
                            val close = entry.optString("close", "")
                            val display = when (type) {
                                "closed" -> "Închis"
                                "nonstop" -> "Non-stop"
                                else -> if (open.isNotBlank() && close.isNotBlank()) "$open - $close" else "Program nedisponibil"
                            }
                            workingHours[key] = display
                        }
                    } catch (_: Exception) {
                    }
                }

                result.add(
                    MotoPlaceSummary(
                        id = obj.optInt("id", 0),
                        title = obj.optString("title", "Untitled"),
                        description = obj.optNullableString("description"),
                        address = obj.optNullableString("address"),
                        latitude = obj.optDouble("latitude", 0.0),
                        longitude = obj.optDouble("longitude", 0.0),
                        phone = obj.optNullableString("phone"),
                        website = obj.optNullableString("website"),
                        email = obj.optNullableString("email"),
                        facebook = obj.optNullableString("facebook"),
                        instagram = obj.optNullableString("instagram"),
                        tiktok = obj.optNullableString("tiktok"),
                        youtube = obj.optNullableString("youtube"),
                        services = services,
                        userId = obj.optInt("user_id", 0),
                        ownerUserId = obj.optInt("owner_user_id", 0).takeIf { it > 0 } ?: obj.optInt("user_id", 0).takeIf { it > 0 },
                        primaryImage = imageCandidates.firstOrNull(),
                        images = imageCandidates,
                        workingHours = workingHours,
                        averageRating = obj.optDouble("average_rating", 0.0).toFloat(),
                        reviewsCount = obj.optInt("reviews_count", 0),
                        reviews = emptyList()
                    )
                )
            }
            result
        } catch (e: Exception) {
            Log.e(TAG, "fetchMotoPlaces failed", e)
            emptyList()
        }
    }

    private fun JSONObject.optNullableString(name: String): String? {
        if (isNull(name)) return null
        val value = optString(name, "")
        return if (value.isBlank()) null else value
    }
}
