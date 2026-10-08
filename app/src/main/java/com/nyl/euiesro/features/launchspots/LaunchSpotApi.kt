package com.nyl.euiesro.features.launchspots

import android.content.Context
import android.net.Uri
import android.util.Log
import com.nyl.euiesro.ApiClient
import com.nyl.euiesro.MediaUrlResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder

object LaunchSpotApi {
    private const val TAG = "LaunchSpotApi"

    private fun safeLogError(message: String, throwable: Throwable) {
        try {
            Log.e(TAG, message, throwable)
        } catch (_: RuntimeException) {
            // Unit tests on the JVM do not mock android.util.Log.
        }
    }

    internal fun parseCreatedSpotId(responseBody: String): Int? {
        if (responseBody.isBlank()) return null

        return try {
            val idPattern = Regex("\\\"id\\\"\\s*:\\s*(\\d+)")
            val match = idPattern.find(responseBody) ?: return null
            match.groupValues[1].toIntOrNull()
        } catch (e: Exception) {
            safeLogError("parseCreatedSpotId failed", e)
            null
        }
    }

    internal fun parseLaunchSpotTypeGroups(responseBody: String): List<LaunchSpotTypeGroup> {
        if (responseBody.isBlank()) return emptyList()

        return try {
            val root = JSONObject(responseBody)
            val payload = if (root.has("data") && root.opt("data") is JSONObject) {
                root.getJSONObject("data")
            } else {
                root
            }

            val groups = mutableListOf<LaunchSpotTypeGroup>()
            val groupsArray = payload.optJSONArray("groups") ?: JSONArray()
            for (i in 0 until groupsArray.length()) {
                val groupObj = groupsArray.getJSONObject(i)
                val category = groupObj.optString("category", "General").trim()
                val typesArray = groupObj.optJSONArray("types") ?: JSONArray()
                val options = mutableListOf<LaunchSpotTypeOption>()
                for (j in 0 until typesArray.length()) {
                    val typeObj = typesArray.getJSONObject(j)
                    val id = typeObj.optInt("id", 0)
                    val label = typeObj.optString("name", typeObj.optString("label", "")).trim()
                    if (id > 0 && label.isNotBlank()) {
                        options.add(LaunchSpotTypeOption(id = id, label = label, category = category))
                    }
                }
                if (options.isNotEmpty()) {
                    groups.add(LaunchSpotTypeGroup(category = category, options = options))
                }
            }

            if (groups.isEmpty()) {
                val topLevelTypes = payload.optJSONArray("types") ?: JSONArray()
                val fallbackOptions = mutableListOf<LaunchSpotTypeOption>()
                for (j in 0 until topLevelTypes.length()) {
                    val typeObj = topLevelTypes.getJSONObject(j)
                    val id = typeObj.optInt("id", 0)
                    val label = typeObj.optString("name", typeObj.optString("label", "")).trim()
                    if (id > 0 && label.isNotBlank()) {
                        fallbackOptions.add(LaunchSpotTypeOption(id = id, label = label, category = "General"))
                    }
                }
                if (fallbackOptions.isNotEmpty()) {
                    groups.add(LaunchSpotTypeGroup(category = "General", options = fallbackOptions))
                }
            }

            groups
        } catch (e: Exception) {
            safeLogError("parseLaunchSpotTypeGroups failed", e)
            emptyList()
        }
    }

    private fun extractJsonArrayByKey(responseBody: String, key: String): String? {
        val token = "\"$key\""
        val startIndex = responseBody.indexOf(token)
        if (startIndex < 0) return null

        val arrayStart = responseBody.indexOf('[', startIndex)
        if (arrayStart < 0) return null

        var depth = 0
        var inString = false
        var escaped = false
        for (index in arrayStart until responseBody.length) {
            val ch = responseBody[index]
            if (inString) {
                if (escaped) {
                    escaped = false
                } else if (ch == '\\') {
                    escaped = true
                } else if (ch == '"') {
                    inString = false
                }
                continue
            }

            when (ch) {
                '"' -> inString = true
                '[' -> depth++
                ']' -> {
                    depth--
                    if (depth == 0) {
                        return responseBody.substring(arrayStart, index + 1)
                    }
                }
            }
        }
        return null
    }

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

    suspend fun uploadLaunchSpotImages(context: Context, spotId: Int, imageUris: List<Uri>): Boolean = withContext(Dispatchers.IO) {
        if (spotId <= 0 || imageUris.isEmpty()) return@withContext false
        if (!ApiClient.ensureValidSession(context)) return@withContext false

        try {
            val url = URL("${ApiClient.baseUrl.trimEnd('/')}/index.php?p=view-launch-spot&id=$spotId")
            val connection = ApiClient.prepareRequest(url, "POST")
            val boundary = "----EuIesroLaunchSpotUpload${System.currentTimeMillis()}"
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")

            connection.outputStream.use { outputStream ->
                val actionField = "launch_spots_upload_images"
                outputStream.write("--$boundary\r\n".toByteArray(Charsets.UTF_8))
                outputStream.write("Content-Disposition: form-data; name=\"action\"\r\n\r\n".toByteArray(Charsets.UTF_8))
                outputStream.write(actionField.toByteArray(Charsets.UTF_8))
                outputStream.write("\r\n".toByteArray(Charsets.UTF_8))

                imageUris.forEachIndexed { index, uri ->
                    val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                    val ext = when {
                        mimeType.contains("png") -> "png"
                        mimeType.contains("webp") -> "webp"
                        else -> "jpg"
                    }
                    val fileName = "launch_spot_${spotId}_${index}_${System.currentTimeMillis()}.$ext"
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
                Log.e(TAG, "uploadLaunchSpotImages failed: HTTP $status body=$body")
                return@withContext false
            }

            !responseIndicatesUploadFailure(body)
        } catch (e: Exception) {
            Log.e(TAG, "uploadLaunchSpotImages failed", e)
            false
        }
    }

    suspend fun fetchLaunchSpotDetail(id: Int): LaunchSpotSummary? = withContext(Dispatchers.IO) {
        try {
            val url = URL("${ApiClient.baseUrl.trimEnd('/')}/api/launch-spots/detail?id=$id")
            val connection = ApiClient.prepareRequest(url, "GET")
            val status = connection.responseCode
            if (status !in 200..299) {
                Log.e(TAG, "Launch spot detail fetch failed: HTTP $status")
                return@withContext null
            }

            val body = ApiClient.readBody(connection)
            val root = JSONObject(body)
            val obj = if (root.has("data") && root.opt("data") is JSONObject) root.getJSONObject("data") else root
            val imagesArray = obj.optJSONArray("images") ?: JSONArray()
            val images = mutableListOf<String>()
            for (j in 0 until imagesArray.length()) {
                val imageItem = imagesArray.getJSONObject(j)
                val imageUrl = imageItem.optString("image_url", "").ifBlank { imageItem.optString("url", "") }
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
            val reviewArray = obj.optJSONArray("reviews") ?: JSONArray()
            val reviews = mutableListOf<LaunchSpotReview>()
            for (j in 0 until reviewArray.length()) {
                val reviewObj = reviewArray.getJSONObject(j)
                reviews.add(
                    LaunchSpotReview(
                        id = reviewObj.optInt("id", 0),
                        userName = reviewObj.optString("user_name", "Anonymous"),
                        avatarUrl = reviewObj.optString("avatar_url", reviewObj.optString("user_avatar", "")).trim().ifBlank { null },
                        rating = reviewObj.optInt("rating", 0),
                        comment = reviewObj.optNullableString("comment"),
                        createdAt = reviewObj.optNullableString("created_at")
                    )
                )
            }
            LaunchSpotSummary(
                id = obj.optInt("id", id),
                title = obj.optString("title", "Untitled"),
                description = obj.optNullableString("description"),
                address = obj.optNullableString("address"),
                latitude = obj.optDouble("latitude", 0.0),
                longitude = obj.optDouble("longitude", 0.0),
                images = images,
                typeIds = typeIds,
                typeOptions = typeOptions,
                typeNames = obj.optString("type_names", ""),
                surfaceLabel = obj.optNullableString("surface_label"),
                depthLabel = obj.optNullableString("depth_label"),
                accessibilityLabel = obj.optNullableString("accessibility_label"),
                userId = obj.optInt("user_id", 0),
                addedBy = obj.optNullableString("added_by"),
                createdAt = obj.optNullableString("created_at"),
                updatedAt = obj.optNullableString("updated_at"),
                averageRating = obj.optDouble("average_rating", 0.0).toFloat(),
                reviews = reviews,
                markerIcons = markerIcons
            )
        } catch (e: Exception) {
            Log.e(TAG, "fetchLaunchSpotDetail failed", e)
            null
        }
    }

    suspend fun submitReview(id: Int, rating: Int, comment: String): Boolean = withContext(Dispatchers.IO) {
        if (!ApiClient.validateSession()) return@withContext false
        try {
            val url = URL("${ApiClient.baseUrl.trimEnd('/')}/api/launch-spots/review")
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
            return@withContext json.optBoolean("ok", false) || json.optBoolean("success", false)
        } catch (e: Exception) {
            Log.e(TAG, "submitReview failed", e)
            false
        }
    }

    suspend fun createLaunchSpot(
        title: String,
        description: String?,
        address: String?,
        latitude: Double,
        longitude: Double,
        launchSurface: String?,
        waterDepth: String?,
        accessibility: String?,
        typeIds: List<Int>
    ): Int? = withContext(Dispatchers.IO) {
        try {
            val url = URL("${ApiClient.baseUrl.trimEnd('/')}/api/launch-spots/create")
            val connection = ApiClient.prepareRequest(url, "POST")
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")

            val payload = JSONObject()
                .put("title", title.trim())
                .put("description", description?.trim().orEmpty())
                .put("address", address?.trim().orEmpty())
                .put("latitude", latitude)
                .put("longitude", longitude)
                .put("launch_surface", launchSurface.orEmpty())
                .put("water_depth", waterDepth.orEmpty())
                .put("accessibility", accessibility.orEmpty())
                .put("type_ids", JSONArray(typeIds))

            connection.outputStream.use { output ->
                output.write(payload.toString().toByteArray(Charsets.UTF_8))
                output.flush()
            }

            val status = connection.responseCode
            val responseBody = ApiClient.readBody(connection)
            if (status !in 200..299) {
                Log.e(TAG, "createLaunchSpot failed: HTTP $status body=$responseBody")
                return@withContext null
            }

            parseCreatedSpotId(responseBody)
        } catch (e: Exception) {
            Log.e(TAG, "createLaunchSpot failed", e)
            null
        }
    }

    suspend fun updateLaunchSpot(
        id: Int,
        title: String,
        description: String?,
        address: String?,
        latitude: Double,
        longitude: Double,
        launchSurface: String?,
        waterDepth: String?,
        accessibility: String?,
        typeIds: List<Int>
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("${ApiClient.baseUrl.trimEnd('/')}/api/launch-spots/update")
            val connection = ApiClient.prepareRequest(url, "POST")
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")

            val payload = JSONObject()
                .put("id", id)
                .put("title", title.trim())
                .put("description", description?.trim().orEmpty())
                .put("address", address?.trim().orEmpty())
                .put("latitude", latitude)
                .put("longitude", longitude)
                .put("launch_surface", launchSurface.orEmpty())
                .put("water_depth", waterDepth.orEmpty())
                .put("accessibility", accessibility.orEmpty())
                .put("type_ids", JSONArray(typeIds))

            connection.outputStream.use { output ->
                output.write(payload.toString().toByteArray(Charsets.UTF_8))
                output.flush()
            }

            val status = connection.responseCode
            val responseBody = ApiClient.readBody(connection)
            if (status !in 200..299) {
                Log.e(TAG, "updateLaunchSpot failed: HTTP $status body=$responseBody")
                return@withContext false
            }

            val root = JSONObject(responseBody)
            val data = root.optJSONObject("data") ?: root
            root.optBoolean("ok", false) || root.optBoolean("success", false) || data.has("id") || (responseBody.contains("\"message\"") && responseBody.contains("Launch spot updated"))
        } catch (e: Exception) {
            Log.e(TAG, "updateLaunchSpot failed", e)
            false
        }
    }

    suspend fun submitLaunchSpotEditSuggestion(
        spotId: Int,
        title: String,
        description: String?,
        address: String?,
        latitude: Double,
        longitude: Double,
        launchSurface: String?,
        waterDepth: String?,
        accessibility: String?,
        typeIds: List<Int>
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("${ApiClient.baseUrl.trimEnd('/')}/api/launch-spots/suggest-edit")
            val connection = ApiClient.prepareRequest(url, "POST")
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")

            val payload = JSONObject()
                .put("id", spotId)
                .put("title", title.trim())
                .put("description", description?.trim().orEmpty())
                .put("address", address?.trim().orEmpty())
                .put("latitude", latitude)
                .put("longitude", longitude)
                .put("launch_surface", launchSurface.orEmpty())
                .put("water_depth", waterDepth.orEmpty())
                .put("accessibility", accessibility.orEmpty())
                .put("type_ids", JSONArray(typeIds))

            connection.outputStream.use { output ->
                output.write(payload.toString().toByteArray(Charsets.UTF_8))
                output.flush()
            }

            val status = connection.responseCode
            val responseBody = ApiClient.readBody(connection)
            if (status !in 200..299) {
                Log.e(TAG, "submitLaunchSpotEditSuggestion failed: HTTP $status body=$responseBody")
                return@withContext false
            }

            val root = JSONObject(responseBody)
            val data = root.optJSONObject("data") ?: root
            root.optBoolean("ok", false) || root.optBoolean("success", false) || data.has("id") || responseBody.contains("suggestion")
        } catch (e: Exception) {
            Log.e(TAG, "submitLaunchSpotEditSuggestion failed", e)
            false
        }
    }

    suspend fun fetchLaunchSpotTypeGroups(): List<LaunchSpotTypeGroup> = withContext(Dispatchers.IO) {
        try {
            val url = URL("${ApiClient.baseUrl.trimEnd('/')}/api/launch-spots/types")
            val connection = ApiClient.prepareRequest(url, "GET")
            val status = connection.responseCode
            if (status !in 200..299) {
                Log.e(TAG, "fetchLaunchSpotTypeGroups failed: HTTP $status")
                return@withContext emptyList()
            }

            val body = ApiClient.readBody(connection)
            parseLaunchSpotTypeGroups(body)
        } catch (e: Exception) {
            Log.e(TAG, "fetchLaunchSpotTypeGroups failed", e)
            emptyList()
        }
    }

    suspend fun fetchLaunchSpotTypes(): List<LaunchSpotTypeOption> = withContext(Dispatchers.IO) {
        try {
            val items = fetchLaunchSpots(limit = 500)
            val typeMap = linkedMapOf<Int, String>()
            items.forEach { summary ->
                summary.typeOptions.forEach { option ->
                    if (option.id > 0 && option.label.isNotBlank()) {
                        typeMap[option.id] = option.label
                    }
                }
            }
            if (typeMap.isNotEmpty()) {
                return@withContext typeMap.entries
                    .sortedBy { it.key }
                    .map { (id, label) -> LaunchSpotTypeOption(id, label) }
            }

            fetchLaunchSpotTypeGroups().flatMap { it.options }
        } catch (e: Exception) {
            Log.e(TAG, "fetchLaunchSpotTypes failed", e)
            emptyList()
        }
    }

    fun fetchLaunchSpotTypeGroupsSync(): List<LaunchSpotTypeGroup> = runBlocking {
        fetchLaunchSpotTypeGroups()
    }

    fun fetchLaunchSpotTypesSync(): List<LaunchSpotTypeOption> = runBlocking {
        fetchLaunchSpotTypes()
    }

    suspend fun fetchLaunchSpots(
        limit: Int = 500,
        query: String? = null,
        subcategoryId: Int? = null
    ): List<LaunchSpotSummary> = withContext(Dispatchers.IO) {
        try {
            val parameters = buildString {
                append("limit=")
                append(limit)
                if (!query.isNullOrBlank()) {
                    append("&q=")
                    append(URLEncoder.encode(query.trim(), "UTF-8"))
                }
                if (subcategoryId != null && subcategoryId > 0) {
                    append("&subcategory_id=")
                    append(subcategoryId)
                }
                append("&all=true")
            }
            val url = URL("${ApiClient.baseUrl.trimEnd('/')}/api/launch-spots?$parameters")
            val connection = ApiClient.prepareRequest(url, "GET")
            val status = connection.responseCode
            if (status !in 200..299) {
                Log.e(TAG, "Launch spots fetch failed: HTTP $status")
                return@withContext emptyList()
            }

            val body = ApiClient.readBody(connection)
            val root = JSONObject(body)
            val data = root.optJSONObject("data") ?: JSONObject()
            val items = data.optJSONArray("items") ?: JSONArray()
            val result = mutableListOf<LaunchSpotSummary>()
            for (i in 0 until items.length()) {
                val obj = items.getJSONObject(i)
                val imagesArray = obj.optJSONArray("images") ?: JSONArray()
                val images = mutableListOf<String>()
                for (j in 0 until imagesArray.length()) {
                    val imageObj = imagesArray.getJSONObject(j)
                    val urlValue = imageObj.optString("image_url", "")
                    val resolved = MediaUrlResolver.resolve(urlValue)
                    if (!resolved.isNullOrBlank()) {
                        images.add(resolved)
                    }
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
                val reviews = mutableListOf<LaunchSpotReview>()
                for (j in 0 until reviewsArray.length()) {
                    val reviewObj = reviewsArray.getJSONObject(j)
                    reviews.add(
                        LaunchSpotReview(
                            id = reviewObj.optInt("id", 0),
                            userName = reviewObj.optString("user_name", reviewObj.optString("username", "Anonymous")).ifBlank { "Anonymous" },
                            avatarUrl = reviewObj.optString("avatar_url", reviewObj.optString("user_avatar", "")).trim().ifBlank { null },
                            rating = reviewObj.optInt("rating", 0),
                            comment = reviewObj.optNullableString("comment"),
                            createdAt = reviewObj.optNullableString("created_at")
                        )
                    )
                }

                val averageRating = obj.optDouble("average_rating", 0.0).toFloat()
                val reviewsCount = obj.optInt("reviews_count", reviews.size).coerceAtLeast(reviews.size)

                result.add(
                    LaunchSpotSummary(
                        id = obj.optInt("id", 0),
                        title = obj.optString("title", "Untitled"),
                        description = obj.optNullableString("description"),
                        address = obj.optNullableString("address"),
                        latitude = obj.optDouble("latitude", 0.0),
                        longitude = obj.optDouble("longitude", 0.0),
                        images = images,
                        typeIds = typeIds,
                        typeOptions = typeOptions,
                        surface = obj.optNullableString("launch_surface_value") ?: obj.optNullableString("launch_surface"),
                        surfaceLabel = obj.optNullableString("surface_label") ?: obj.optNullableString("launch_surface"),
                        depth = obj.optNullableString("water_depth_value") ?: obj.optNullableString("water_depth"),
                        depthLabel = obj.optNullableString("depth_label") ?: obj.optNullableString("water_depth"),
                        accessibility = obj.optNullableString("accessibility_value") ?: obj.optNullableString("accessibility"),
                        accessibilityLabel = obj.optNullableString("accessibility_label") ?: obj.optNullableString("accessibility"),
                        averageRating = averageRating,
                        reviewsCount = reviewsCount,
                        reviews = reviews,
                        markerIcons = markerIcons
                    )
                )
            }
            result
        } catch (e: Exception) {
            Log.e(TAG, "fetchLaunchSpots failed", e)
            emptyList()
        }
    }

    private fun buildTypeOptions(rawTypeIds: JSONArray, rawTypes: JSONArray?): List<LaunchSpotTypeOption> {
        val mapped = linkedMapOf<Int, String>()
        if (rawTypes != null) {
            for (index in 0 until rawTypes.length()) {
                val item = rawTypes.getJSONObject(index)
                val id = item.optInt("id", -1)
                val label = item.optString("name", "").trim()
                if (id > 0 && label.isNotBlank()) {
                    mapped[id] = label
                }
            }
        }
        for (index in 0 until rawTypeIds.length()) {
            val id = rawTypeIds.getInt(index)
            if (id <= 0 || mapped.containsKey(id)) {
                continue
            }
            mapped[id] = "Type $id"
        }
        return mapped.entries
            .sortedBy { it.key }
            .map { (id, label) -> LaunchSpotTypeOption(id = id, label = label) }
    }

    private fun JSONObject.optNullableString(name: String): String? {
        if (isNull(name)) return null
        val value = optString(name, "")
        return if (value.isBlank()) null else value
    }
}
