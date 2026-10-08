package com.nyl.euiesro.features.launchspots

import com.nyl.euiesro.features.auto.AutoReview

data class LaunchSpotTypeOption(
    val id: Int,
    val label: String,
    val category: String? = null
)

data class LaunchSpotTypeGroup(
    val category: String,
    val options: List<LaunchSpotTypeOption>
)

data class LaunchSpotReview(
    val id: Int,
    val userName: String,
    val avatarUrl: String? = null,
    val rating: Int,
    val comment: String?,
    val createdAt: String?
)

data class LaunchSpotSummary(
    val id: Int,
    val title: String,
    val description: String?,
    val address: String?,
    val latitude: Double,
    val longitude: Double,
    val images: List<String>,
    val typeIds: List<Int>,
    val typeOptions: List<LaunchSpotTypeOption> = emptyList(),
    val typeNames: String = "",
    val surface: String? = null,
    val surfaceLabel: String? = null,
    val depth: String? = null,
    val depthLabel: String? = null,
    val accessibility: String? = null,
    val accessibilityLabel: String? = null,
    val userId: Int = 0,
    val addedBy: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val averageRating: Float = 0f,
    val reviewsCount: Int = 0,
    val reviews: List<LaunchSpotReview> = emptyList(),
    val markerIcons: List<String> = emptyList()
) {
    fun mappedReviews(): List<AutoReview> = reviews.map {
        AutoReview(
            id = it.id,
            userId = 0,
            userName = it.userName,
            avatarUrl = it.avatarUrl,
            rating = it.rating,
            comment = it.comment,
            createdAt = it.createdAt,
            ownerReply = null
        )
    }

    fun currentUserReview(currentUserName: String): AutoReview? {
        val normalized = normalizeUserIdentifier(currentUserName)
        if (normalized.isBlank() || normalized == "guest") return null

        return mappedReviews().firstOrNull { review ->
            val reviewNormalized = normalizeUserIdentifier(review.userName)
            reviewNormalized == normalized ||
                reviewNormalized.contains(normalized) ||
                normalized.contains(reviewNormalized)
        }
    }

    private fun normalizeUserIdentifier(value: String?): String {
        val cleaned = value?.trim().orEmpty()
            .lowercase()
            .replace("[\\s_()-]+".toRegex(), " ")
            .replace("[^a-z0-9\\s]+".toRegex(), "")
        return cleaned.split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .joinToString("")
    }
}
