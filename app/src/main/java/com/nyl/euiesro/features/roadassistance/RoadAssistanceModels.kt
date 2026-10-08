package com.nyl.euiesro.features.roadassistance

import java.util.Locale

object RoadAssistanceServiceKeys {
    fun normalize(rawServices: List<String>): Set<String> {
        val selected = linkedSetOf<String>()
        rawServices.forEach { rawService ->
            val matches = mapServiceLabelToKeys(rawService)
            if (matches.isNotEmpty()) {
                selected.addAll(matches)
            }
        }
        return selected
    }

    private fun mapServiceLabelToKeys(rawService: String): Set<String> {
        val value = rawService.trim()
        if (value.isEmpty()) return emptySet()

        val lower = value.lowercase(Locale.getDefault())
        val category = when {
            lower.startsWith("service mobil") -> "service_service_mobile"
            lower.startsWith("vulcanizare mobil") -> "service_vulcanizare_mobile"
            lower.startsWith("transport vehicule") -> "service_transport_vehicule"
            else -> null
        }

        if (category == null) {
            val directMatch = when (value.lowercase(Locale.getDefault())) {
                "service mobil" -> setOf("service_service_mobile")
                "vulcanizare mobilă", "vulcanizare mobila" -> setOf("service_vulcanizare_mobile")
                "transport vehicule" -> setOf("service_transport_vehicule")
                else -> emptySet()
            }
            if (directMatch.isNotEmpty()) return directMatch
            return emptySet()
        }

        val matches = linkedSetOf<String>()
        if (lower.contains("moto")) matches += "${category}_moto"
        if (lower.contains("auto")) matches += "${category}_auto"
        if (lower.contains("atv")) matches += "${category}_atv"
        if (lower.contains("service mobil") || lower.contains("vulcanizare mobil") || lower.contains("transport vehicule")) {
            matches += category
        }
        return matches
    }
}

data class RoadAssistanceReview(
    val id: Int,
    val userId: Int = 0,
    val userName: String,
    val avatarUrl: String? = null,
    val rating: Int,
    val comment: String?,
    val createdAt: String?,
    val ownerReply: String? = null
)

data class RoadAssistanceSummary(
    val id: Int,
    val userId: Int = 0,
    val title: String,
    val description: String?,
    val address: String?,
    val latitude: Double,
    val longitude: Double,
    val phone: String?,
    val email: String? = null,
    val whatsapp: String? = null,
    val website: String?,
    val facebook: String? = null,
    val instagram: String? = null,
    val coverageBoundary: String = "",
    val services: List<String>,
    val averageRating: Float = 0f,
    val reviewsCount: Int = 0,
    val reviews: List<RoadAssistanceReview> = emptyList()
)
