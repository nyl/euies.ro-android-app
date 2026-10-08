package com.nyl.euiesro.features.roadassistance

import android.util.Log
import com.nyl.euiesro.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder

object RoadAssistanceApi {
    private const val TAG = "RoadAssistanceApi"

    fun buildReviewList(reviewsArray: JSONArray): List<RoadAssistanceReview> {
        val reviews = mutableListOf<RoadAssistanceReview>()
        for (i in 0 until reviewsArray.length()) {
            val reviewObj = reviewsArray.getJSONObject(i)
            val comment = reviewObj.optNullableText("comment")
            val ownerReply = reviewObj.optNullableText("owner_reply")
            reviews.add(
                RoadAssistanceReview(
                    id = reviewObj.optInt("id", 0),
                    userId = reviewObj.optInt("user_id", 0),
                    userName = reviewObj.optString("user_name", reviewObj.optString("userName", "Anonymous")),
                    avatarUrl = reviewObj.optNullableText("avatar_url"),
                    rating = reviewObj.optInt("rating", 0),
                    comment = comment,
                    createdAt = reviewObj.optNullableText("created_at"),
                    ownerReply = ownerReply
                )
            )
        }
        return reviews
    }

    private fun JSONObject.optNullableText(key: String): String? {
        val raw = if (isNull(key)) null else optString(key, "")
        val normalized = raw?.trim() ?: return null
        return if (normalized.isEmpty() || normalized.equals("null", ignoreCase = true)) null else normalized
    }

    suspend fun createRoadAssistancePlace(
        title: String,
        description: String?,
        address: String?,
        latitude: Double,
        longitude: Double,
        phone: String?,
        email: String?,
        whatsapp: String?,
        website: String?,
        facebook: String?,
        instagram: String?,
        coverageBoundary: String = "",
        services: List<String>
    ): Int? = withContext(Dispatchers.IO) {
        try {
            if (!ApiClient.validateSession()) return@withContext null
            val url = URL("${ApiClient.baseUrl.trimEnd('/')}/api/road-assistance/create")
            val connection = ApiClient.prepareRequest(url, "POST")
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")

            val payload = JSONObject()
                .put("title", title.trim())
                .put("description", description?.trim().orEmpty())
                .put("address", address?.trim().orEmpty())
                .put("latitude", latitude)
                .put("longitude", longitude)
                .put("phone", phone?.trim().orEmpty())
                .put("email", email?.trim().orEmpty())
                .put("whatsapp", whatsapp?.trim().orEmpty())
                .put("website", website?.trim().orEmpty())
                .put("facebook", facebook?.trim().orEmpty())
                .put("instagram", instagram?.trim().orEmpty())
                .put("coverage_boundary", coverageBoundary.trim())
                .put("services", JSONArray(services))

            connection.outputStream.use { output ->
                output.write(payload.toString().toByteArray(Charsets.UTF_8))
                output.flush()
            }

            val status = connection.responseCode
            val responseBody = ApiClient.readBody(connection)
            if (status !in 200..299) {
                Log.e(TAG, "createRoadAssistancePlace failed: HTTP $status body=$responseBody")
                return@withContext null
            }

            val root = JSONObject(responseBody)
            val data = root.optJSONObject("data") ?: root
            val id = data.optInt("id", 0)
            if (id > 0) id else null
        } catch (e: Exception) {
            Log.e(TAG, "createRoadAssistancePlace failed", e)
            null
        }
    }

    suspend fun updateRoadAssistancePlace(
        id: Int,
        title: String,
        description: String?,
        address: String?,
        latitude: Double,
        longitude: Double,
        phone: String?,
        email: String?,
        whatsapp: String?,
        website: String?,
        facebook: String?,
        instagram: String?,
        coverageBoundary: String = "",
        services: List<String>
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!ApiClient.validateSession()) return@withContext false
            val url = URL("${ApiClient.baseUrl.trimEnd('/')}/api/road-assistance/update")
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
                .put("email", email?.trim().orEmpty())
                .put("whatsapp", whatsapp?.trim().orEmpty())
                .put("website", website?.trim().orEmpty())
                .put("facebook", facebook?.trim().orEmpty())
                .put("instagram", instagram?.trim().orEmpty())
                .put("coverage_boundary", coverageBoundary.trim())
                .put("services", JSONArray(services))

            connection.outputStream.use { output ->
                output.write(payload.toString().toByteArray(Charsets.UTF_8))
                output.flush()
            }

            val status = connection.responseCode
            val responseBody = ApiClient.readBody(connection)
            if (status !in 200..299) {
                Log.e(TAG, "updateRoadAssistancePlace failed: HTTP $status body=$responseBody")
                return@withContext false
            }

            val root = JSONObject(responseBody)
            root.optBoolean("ok", false) || root.optBoolean("success", false) || root.optJSONObject("data")?.has("id") == true || responseBody.contains("updated", ignoreCase = true)
        } catch (e: Exception) {
            Log.e(TAG, "updateRoadAssistancePlace failed", e)
            false
        }
    }

    suspend fun submitRoadAssistanceEditSuggestion(
        id: Int,
        title: String,
        description: String?,
        address: String?,
        latitude: Double,
        longitude: Double,
        phone: String?,
        email: String?,
        whatsapp: String?,
        website: String?,
        facebook: String?,
        instagram: String?,
        coverageBoundary: String = "",
        services: List<String>
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!ApiClient.validateSession()) return@withContext false
            val url = URL("${ApiClient.baseUrl.trimEnd('/')}/api/road-assistance/suggest-edit")
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
                .put("email", email?.trim().orEmpty())
                .put("whatsapp", whatsapp?.trim().orEmpty())
                .put("website", website?.trim().orEmpty())
                .put("facebook", facebook?.trim().orEmpty())
                .put("instagram", instagram?.trim().orEmpty())
                .put("coverage_boundary", coverageBoundary.trim())
                .put("services", JSONArray(services))

            connection.outputStream.use { output ->
                output.write(payload.toString().toByteArray(Charsets.UTF_8))
                output.flush()
            }

            val status = connection.responseCode
            val responseBody = ApiClient.readBody(connection)
            if (status !in 200..299) {
                Log.e(TAG, "submitRoadAssistanceEditSuggestion failed: HTTP $status body=$responseBody")
                return@withContext false
            }

            val root = JSONObject(responseBody)
            root.optBoolean("ok", false) || root.optBoolean("success", false) || root.optJSONObject("data")?.has("id") == true || responseBody.contains("suggestion", ignoreCase = true)
        } catch (e: Exception) {
            Log.e(TAG, "submitRoadAssistanceEditSuggestion failed", e)
            false
        }
    }

    suspend fun fetchRoadAssistancePlaces(
        limit: Int = 500,
        query: String? = null,
        service: String? = null
    ): List<RoadAssistanceSummary> = withContext(Dispatchers.IO) {
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
            }
            val url = URL("${ApiClient.baseUrl.trimEnd('/')}/api/road-assistance?$parameters")
            val connection = ApiClient.prepareRequest(url, "GET")
            val status = connection.responseCode
            if (status !in 200..299) {
                Log.e(TAG, "Road assistance fetch failed: HTTP $status")
                return@withContext emptyList()
            }

            val body = ApiClient.readBody(connection)
            val root = JSONObject(body)
            val dataObj = root.optJSONObject("data") ?: root
            val items = dataObj.optJSONArray("items") ?: root.optJSONArray("items") ?: JSONArray()
            val result = mutableListOf<RoadAssistanceSummary>()
            for (i in 0 until items.length()) {
                val obj = items.getJSONObject(i)
                val servicesArray = obj.optJSONArray("services") ?: JSONArray()
                val services = mutableListOf<String>()
                for (j in 0 until servicesArray.length()) {
                    val value = servicesArray.getString(j)
                    if (value.isNotBlank()) services.add(value)
                }
                val reviewsArray = obj.optJSONArray("reviews") ?: JSONArray()
                val reviews = buildReviewList(reviewsArray)
                val averageRating = obj.optDouble("average_rating", if (reviews.isNotEmpty()) reviews.map { it.rating.toDouble() }.average().toFloat().toDouble() else 0.0).toFloat()
                val reviewsCount = obj.optInt("reviews_count", reviews.size).coerceAtLeast(reviews.size)
                result.add(
                    RoadAssistanceSummary(
                        id = obj.optInt("id", 0),
                        userId = obj.optInt("user_id", 0),
                        title = obj.optString("title", "Untitled"),
                        description = obj.optNullableString("description"),
                        address = obj.optNullableString("address"),
                        latitude = obj.optDouble("latitude", 0.0),
                        longitude = obj.optDouble("longitude", 0.0),
                        phone = obj.optNullableString("phone"),
                        email = obj.optNullableString("email"),
                        whatsapp = obj.optNullableString("whatsapp"),
                        website = obj.optNullableString("website"),
                        facebook = obj.optNullableString("facebook"),
                        instagram = obj.optNullableString("instagram"),
                        coverageBoundary = obj.optNullableString("coverage_boundary") ?: "",
                        services = services,
                        averageRating = averageRating,
                        reviewsCount = reviewsCount,
                        reviews = reviews
                    )
                )
            }
            result
        } catch (e: Exception) {
            Log.e(TAG, "fetchRoadAssistancePlaces failed", e)
            emptyList()
        }
    }

    suspend fun fetchRoadAssistancePlaceDetail(id: Int): RoadAssistanceSummary? = withContext(Dispatchers.IO) {
        try {
            val url = URL("${ApiClient.baseUrl.trimEnd('/')}/api/road-assistance/detail?id=$id")
            val connection = ApiClient.prepareRequest(url, "GET")
            val status = connection.responseCode
            if (status !in 200..299) {
                Log.e(TAG, "Road assistance detail fetch failed: HTTP $status")
                return@withContext null
            }

            val body = ApiClient.readBody(connection)
            val root = JSONObject(body)
            val obj = if (root.has("data") && root.opt("data") is JSONObject) root.getJSONObject("data") else root
            val servicesArray = obj.optJSONArray("services") ?: JSONArray()
            val services = mutableListOf<String>()
            for (j in 0 until servicesArray.length()) {
                val value = servicesArray.getString(j)
                if (value.isNotBlank()) services.add(value)
            }
            val reviewsArray = obj.optJSONArray("reviews") ?: JSONArray()
            val reviews = buildReviewList(reviewsArray)
            val averageRating = obj.optDouble("average_rating", if (reviews.isNotEmpty()) reviews.map { it.rating.toDouble() }.average().toFloat().toDouble() else 0.0).toFloat()
            val reviewsCount = obj.optInt("reviews_count", reviews.size).coerceAtLeast(reviews.size)
            RoadAssistanceSummary(
                id = obj.optInt("id", id),
                userId = obj.optInt("user_id", 0),
                title = obj.optString("title", "Untitled"),
                description = obj.optNullableString("description"),
                address = obj.optNullableString("address"),
                latitude = obj.optDouble("latitude", 0.0),
                longitude = obj.optDouble("longitude", 0.0),
                phone = obj.optNullableString("phone"),
                email = obj.optNullableString("email"),
                whatsapp = obj.optNullableString("whatsapp"),
                website = obj.optNullableString("website"),
                facebook = obj.optNullableString("facebook"),
                instagram = obj.optNullableString("instagram"),
                coverageBoundary = obj.optNullableString("coverage_boundary") ?: "",
                services = services,
                averageRating = averageRating,
                reviewsCount = reviewsCount,
                reviews = reviews
            )
        } catch (e: Exception) {
            Log.e(TAG, "fetchRoadAssistancePlaceDetail failed", e)
            null
        }
    }

    suspend fun submitReview(id: Int, rating: Int, comment: String): Boolean = withContext(Dispatchers.IO) {
        if (!ApiClient.validateSession()) return@withContext false
        try {
            val url = URL("${ApiClient.baseUrl.trimEnd('/')}/api/road-assistance/review")
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
            val url = URL("${ApiClient.baseUrl.trimEnd('/')}/api/road-assistance/reply")
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

    private fun JSONObject.optNullableString(name: String): String? {
        if (isNull(name)) return null
        val value = optString(name, "")
        return if (value.isBlank()) null else value
    }
}
