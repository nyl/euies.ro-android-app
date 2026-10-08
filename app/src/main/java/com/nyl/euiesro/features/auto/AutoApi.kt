package com.nyl.euiesro.features.auto

import android.util.Log
import com.nyl.euiesro.ApiClient
import com.nyl.euiesro.MediaUrlResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder

object AutoApi {
    private const val TAG = "AutoApi"

    fun defaultWorkingHoursPayload(): JSONObject {
        return JSONObject()
            .put("monday", JSONObject().put("type", "hours").put("open", "09:00").put("close", "18:00"))
            .put("tuesday", JSONObject().put("type", "hours").put("open", "09:00").put("close", "18:00"))
            .put("wednesday", JSONObject().put("type", "hours").put("open", "09:00").put("close", "18:00"))
            .put("thursday", JSONObject().put("type", "hours").put("open", "09:00").put("close", "18:00"))
            .put("friday", JSONObject().put("type", "hours").put("open", "09:00").put("close", "18:00"))
            .put("saturday", JSONObject().put("type", "hours").put("open", "09:00").put("close", "18:00"))
            .put("sunday", JSONObject().put("type", "closed").put("open", "").put("close", ""))
    }

    internal fun parseAutoTypeGroups(responseBody: String): List<AutoTypeGroup> {
        if (responseBody.isBlank()) return emptyList()

        return try {
            val root = JSONObject(responseBody)
            val payload = if (root.has("data") && root.opt("data") is JSONObject) {
                root.getJSONObject("data")
            } else {
                root
            }

            val groups = mutableListOf<AutoTypeGroup>()
            val groupsArray = payload.optJSONArray("groups") ?: JSONArray()
            for (i in 0 until groupsArray.length()) {
                val groupObj = groupsArray.getJSONObject(i)
                val category = groupObj.optString("category", "General").trim()
                val typesArray = groupObj.optJSONArray("types") ?: JSONArray()
                val options = mutableListOf<AutoTypeOption>()
                for (j in 0 until typesArray.length()) {
                    val typeObj = typesArray.getJSONObject(j)
                    val id = typeObj.optInt("id", 0)
                    val label = typeObj.optString("name", typeObj.optString("label", "")).trim()
                    if (id > 0 && label.isNotBlank()) {
                        options.add(AutoTypeOption(id = id, label = label, category = category))
                    }
                }
                if (options.isNotEmpty()) {
                    groups.add(AutoTypeGroup(category = category, options = options))
                }
            }

            if (groups.isEmpty()) {
                val topLevelTypes = payload.optJSONArray("types") ?: JSONArray()
                val fallbackOptions = mutableListOf<AutoTypeOption>()
                for (j in 0 until topLevelTypes.length()) {
                    val typeObj = topLevelTypes.getJSONObject(j)
                    val id = typeObj.optInt("id", 0)
                    val label = typeObj.optString("name", typeObj.optString("label", "")).trim()
                    if (id > 0 && label.isNotBlank()) {
                        fallbackOptions.add(AutoTypeOption(id = id, label = label, category = "General"))
                    }
                }
                if (fallbackOptions.isNotEmpty()) {
                    groups.add(AutoTypeGroup(category = "General", options = fallbackOptions))
                }
            }

            groups
        } catch (e: Exception) {
            Log.e(TAG, "parseAutoTypeGroups failed", e)
            emptyList()
        }
    }

    suspend fun fetchAutoTypeGroups(): List<AutoTypeGroup> = withContext(Dispatchers.IO) {
        try {
            val url = URL("${ApiClient.baseUrl.trimEnd('/')}/api/auto/types")
            val connection = ApiClient.prepareRequest(url, "GET")
            val status = connection.responseCode
            if (status !in 200..299) {
                Log.e(TAG, "fetchAutoTypeGroups failed: HTTP $status")
                return@withContext emptyList()
            }
            val body = ApiClient.readBody(connection)
            parseAutoTypeGroups(body)
        } catch (e: Exception) {
            Log.e(TAG, "fetchAutoTypeGroups failed", e)
            emptyList()
        }
    }

    fun fetchAutoTypeGroupsSync(): List<AutoTypeGroup> = kotlinx.coroutines.runBlocking {
        fetchAutoTypeGroups()
    }

    internal fun parseAutoPlaceSummary(obj: JSONObject, fallbackId: Int = obj.optInt("id", 0)): AutoPlaceSummary {
        val imagesArray = obj.optJSONArray("images") ?: JSONArray()
        val images = mutableListOf<String>()
        for (j in 0 until imagesArray.length()) {
            val imageItem = imagesArray.getJSONObject(j)
            val imageUrl = imageItem.optString("image_url", imageItem.optString("url", "")).ifBlank { imageItem.optString("url", "") }
            val resolved = MediaUrlResolver.resolve(imageUrl)
            if (!resolved.isNullOrBlank()) images.add(resolved)
        }

        val imageUrlList = obj.optJSONArray("image_urls")
        if (imageUrlList != null && imageUrlList.length() > 0 && images.isEmpty()) {
            for (j in 0 until imageUrlList.length()) {
                val value = imageUrlList.getString(j)
                val resolved = MediaUrlResolver.resolve(value)
                if (!resolved.isNullOrBlank()) images.add(resolved)
            }
        }

        val servicesArray = obj.optJSONArray("services") ?: JSONArray()
        val services = mutableListOf<String>()
        for (j in 0 until servicesArray.length()) {
            val service = servicesArray.getString(j)
            if (service.isNotBlank()) services.add(service)
        }

        val typeIds = mutableListOf<Int>()
        val rawTypeIds = obj.optJSONArray("type_ids") ?: JSONArray()
        for (j in 0 until rawTypeIds.length()) {
            typeIds.add(rawTypeIds.getInt(j))
        }
        val typeOptions = buildTypeOptions(rawTypeIds, obj.optJSONArray("types"))

        val markerIconsArray = obj.optJSONArray("marker_icons") ?: JSONArray()
        val markerIcons = mutableListOf<String>()
        for (j in 0 until markerIconsArray.length()) {
            val markerIcon = markerIconsArray.getString(j)
            if (markerIcon.isNotBlank()) markerIcons.add(markerIcon)
        }

        val reviewsArray = obj.optJSONArray("reviews") ?: JSONArray()
        val parsedReviews = buildReviewList(reviewsArray)
        val workingHours = parseWorkingHours(obj.opt("working_hours"))
        val averageRatingFromReviews = if (parsedReviews.isNotEmpty()) parsedReviews.map { it.rating.toDouble() }.average().toFloat() else 0f
        val averageRating = obj.optDouble("average_rating", averageRatingFromReviews.toDouble()).toFloat()
        val reviewsCount = obj.optInt("reviews_count", parsedReviews.size).coerceAtLeast(parsedReviews.size)

        return AutoPlaceSummary(
            id = obj.optInt("id", fallbackId),
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
            images = images,
            typeIds = typeIds,
            typeOptions = typeOptions,
            typeNames = obj.optString("type_names", ""),
            addedBy = obj.optNullableString("added_by") ?: obj.optNullableString("user_name") ?: obj.optNullableString("owner_name"),
            ownerUserName = obj.optNullableString("owner_user_name") ?: obj.optNullableString("added_by") ?: obj.optNullableString("user_name"),
            userId = obj.optInt("user_id", 0).takeIf { it > 0 } ?: obj.optInt("owner_user_id", 0).takeIf { it > 0 },
            createdAt = obj.optNullableString("created_at"),
            updatedAt = obj.optNullableString("updated_at"),
            averageRating = averageRating,
            reviewsCount = reviewsCount,
            reviews = parsedReviews,
            markerIcons = markerIcons,
            workingHours = workingHours
        )
    }

    suspend fun fetchAutoPlaces(
        limit: Int = 500,
        query: String? = null,
        typeId: Int? = null
    ): List<AutoPlaceSummary> = withContext(Dispatchers.IO) {
        try {
            val parameters = buildString {
                append("limit=")
                append(limit)
                if (!query.isNullOrBlank()) {
                    append("&q=")
                    append(URLEncoder.encode(query.trim(), "UTF-8"))
                }
                if (typeId != null && typeId > 0) {
                    append("&type_id=")
                    append(typeId)
                }
                append("&all=true")
            }
            val url = URL("${ApiClient.baseUrl.trimEnd('/')}/api/auto?$parameters")
            val connection = ApiClient.prepareRequest(url, "GET")
            val status = connection.responseCode
            if (status !in 200..299) {
                Log.e(TAG, "Auto fetch failed: HTTP $status")
                return@withContext emptyList()
            }

            val body = ApiClient.readBody(connection)
            val root = JSONObject(body)
            val data = root.optJSONObject("data") ?: JSONObject()
            val items = data.optJSONArray("items") ?: JSONArray()
            val result = mutableListOf<AutoPlaceSummary>()
            for (i in 0 until items.length()) {
                result.add(parseAutoPlaceSummary(items.getJSONObject(i)))
            }
            result
        } catch (e: Exception) {
            Log.e(TAG, "fetchAutoPlaces failed", e)
            emptyList()
        }
    }

    suspend fun fetchAutoPlaceDetail(id: Int): AutoPlaceSummary? = withContext(Dispatchers.IO) {
        try {
            val url = URL("${ApiClient.baseUrl.trimEnd('/')}/api/auto/detail?id=$id")
            val connection = ApiClient.prepareRequest(url, "GET")
            val status = connection.responseCode
            if (status !in 200..299) {
                Log.e(TAG, "Auto detail fetch failed: HTTP $status")
                return@withContext null
            }

            val body = ApiClient.readBody(connection)
            val root = JSONObject(body)
            val obj = if (root.has("data") && root.opt("data") is JSONObject) root.getJSONObject("data") else root
            parseAutoPlaceSummary(obj, fallbackId = id)
        } catch (e: Exception) {
            Log.e(TAG, "fetchAutoPlaceDetail failed", e)
            null
        }
    }

    suspend fun createAutoPlace(
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
        typeIds: List<Int> = emptyList(),
        workingHours: Map<String, com.nyl.euiesro.common.WorkingHoursEntry> = emptyMap()
    ): Int? = withContext(Dispatchers.IO) {
        try {
            val url = URL("${ApiClient.baseUrl.trimEnd('/')}/api/auto/create")
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
                .put("working_hours", com.nyl.euiesro.common.workingHoursMapToJson(if (workingHours.isEmpty()) com.nyl.euiesro.common.defaultWorkingHoursMap() else workingHours))
                .put("type_ids", JSONArray(typeIds))

            connection.outputStream.use { output ->
                output.write(payload.toString().toByteArray(Charsets.UTF_8))
                output.flush()
            }

            val status = connection.responseCode
            val responseBody = ApiClient.readBody(connection)
            if (status !in 200..299) {
                Log.e(TAG, "createAutoPlace failed: HTTP $status body=$responseBody")
                return@withContext null
            }

            val root = JSONObject(responseBody)
            val data = root.optJSONObject("data") ?: root
            val id = data.optInt("id", 0)
            if (id > 0) id else null
        } catch (e: Exception) {
            Log.e(TAG, "createAutoPlace failed", e)
            null
        }
    }

    suspend fun updateAutoPlace(
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
        typeIds: List<Int> = emptyList(),
        workingHours: Map<String, com.nyl.euiesro.common.WorkingHoursEntry> = emptyMap()
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("${ApiClient.baseUrl.trimEnd('/')}/api/auto/update")
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
                .put("working_hours", com.nyl.euiesro.common.workingHoursMapToJson(if (workingHours.isEmpty()) com.nyl.euiesro.common.defaultWorkingHoursMap() else workingHours))
                .put("type_ids", JSONArray(typeIds))

            connection.outputStream.use { output ->
                output.write(payload.toString().toByteArray(Charsets.UTF_8))
                output.flush()
            }

            val status = connection.responseCode
            val responseBody = ApiClient.readBody(connection)
            if (status !in 200..299) {
                Log.e(TAG, "updateAutoPlace failed: HTTP $status body=$responseBody")
                return@withContext false
            }

            val root = JSONObject(responseBody)
            root.optBoolean("ok", false) || root.optBoolean("success", false) || (root.optJSONObject("data") ?: root).has("id") || responseBody.contains("updated", ignoreCase = true)
        } catch (e: Exception) {
            Log.e(TAG, "updateAutoPlace failed", e)
            false
        }
    }

    suspend fun submitAutoEditSuggestion(
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
        typeIds: List<Int> = emptyList(),
        workingHours: Map<String, com.nyl.euiesro.common.WorkingHoursEntry> = emptyMap()
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("${ApiClient.baseUrl.trimEnd('/')}/api/auto/suggest-edit")
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
                .put("working_hours", com.nyl.euiesro.common.workingHoursMapToJson(if (workingHours.isEmpty()) com.nyl.euiesro.common.defaultWorkingHoursMap() else workingHours))
                .put("type_ids", JSONArray(typeIds))

            connection.outputStream.use { output ->
                output.write(payload.toString().toByteArray(Charsets.UTF_8))
                output.flush()
            }

            val status = connection.responseCode
            val responseBody = ApiClient.readBody(connection)
            if (status !in 200..299) {
                Log.e(TAG, "submitAutoEditSuggestion failed: HTTP $status body=$responseBody")
                return@withContext false
            }

            val root = JSONObject(responseBody)
            root.optBoolean("ok", false) || root.optBoolean("success", false) || (root.optJSONObject("data") ?: root).has("status") || responseBody.contains("suggestion", ignoreCase = true)
        } catch (e: Exception) {
            Log.e(TAG, "submitAutoEditSuggestion failed", e)
            false
        }
    }

    suspend fun submitReview(id: Int, rating: Int, comment: String): Boolean = withContext(Dispatchers.IO) {
        if (!ApiClient.validateSession()) return@withContext false
        try {
            val url = URL("${ApiClient.baseUrl.trimEnd('/')}/api/auto/review")
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
            val url = URL("${ApiClient.baseUrl.trimEnd('/')}/api/auto/reply")
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

    private fun buildTypeOptions(rawTypeIds: JSONArray, rawTypes: JSONArray?): List<AutoTypeOption> {
        val mapped = linkedMapOf<Int, String>()
        if (rawTypes != null) {
            for (index in 0 until rawTypes.length()) {
                val item = rawTypes.getJSONObject(index)
                val typeId = item.optInt("id", -1)
                val label = item.optString("name", "").trim()
                if (typeId > 0 && label.isNotBlank()) {
                    mapped[typeId] = label
                }
            }
        }
        for (index in 0 until rawTypeIds.length()) {
            val id = rawTypeIds.getInt(index)
            if (id > 0 && !mapped.containsKey(id)) {
                mapped[id] = "Type $id"
            }
        }
        return mapped.entries
            .sortedBy { it.key }
            .map { (id, label) -> AutoTypeOption(id = id, label = label) }
    }

    private fun parseWorkingHours(rawValue: Any?): Map<String, String> {
        val workingHours = linkedMapOf<String, String>()
        if (rawValue == null || rawValue == JSONObject.NULL) return workingHours

        val rawString = when (rawValue) {
            is String -> rawValue.trim()
            is JSONObject -> rawValue.toString()
            else -> return workingHours
        }
        if (rawString.isBlank()) return workingHours

        return try {
            val json = JSONObject(rawString)
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val entry = json.optJSONObject(key) ?: continue
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
            workingHours
        } catch (_: Exception) {
            workingHours
        }
    }

    private fun buildReviewList(reviewsArray: JSONArray?): List<AutoReview> {
        if (reviewsArray == null || reviewsArray.length() == 0) return emptyList()
        val reviews = mutableListOf<AutoReview>()
        for (i in 0 until reviewsArray.length()) {
            val reviewObj = reviewsArray.getJSONObject(i)
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
        return reviews
    }

    private fun JSONObject.optNullableString(name: String): String? {
        if (isNull(name)) return null
        val value = optString(name, "")
        return if (value.isBlank()) null else value
    }
}
