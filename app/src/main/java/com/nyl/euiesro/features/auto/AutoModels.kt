package com.nyl.euiesro.features.auto

data class AutoTypeOption(
    val id: Int,
    val label: String,
    val category: String = "General"
)

data class AutoTypeGroup(
    val category: String,
    val options: List<AutoTypeOption>
)

data class AutoReview(
    val id: Int,
    val userId: Int = 0,
    val userName: String,
    val avatarUrl: String? = null,
    val rating: Int,
    val comment: String?,
    val createdAt: String?,
    val ownerReply: String? = null
)

data class AutoPlaceSummary(
    val id: Int,
    val title: String,
    val description: String?,
    val address: String?,
    val latitude: Double,
    val longitude: Double,
    val phone: String? = null,
    val website: String? = null,
    val email: String? = null,
    val facebook: String? = null,
    val instagram: String? = null,
    val tiktok: String? = null,
    val youtube: String? = null,
    val services: List<String> = emptyList(),
    val images: List<String> = emptyList(),
    val typeIds: List<Int> = emptyList(),
    val typeOptions: List<AutoTypeOption> = emptyList(),
    val typeNames: String = "",
    val addedBy: String? = null,
    val ownerUserName: String? = null,
    val userId: Int? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val averageRating: Float = 0f,
    val reviewsCount: Int = 0,
    val reviews: List<AutoReview> = emptyList(),
    val markerIcons: List<String> = emptyList(),
    val workingHours: Map<String, String> = emptyMap()
) {
    data class OpeningStatus(
        val state: State,
        val label: String,
        val detail: String
    ) {
        enum class State {
            OPEN,
            CLOSING_SOON,
            CLOSED
        }
    }

    fun openingStatus(): OpeningStatus {
        if (workingHours.isEmpty()) {
            return OpeningStatus(OpeningStatus.State.CLOSED, "Închis", "Program indisponibil")
        }

        val normalizedHours = workingHours.mapKeys { (day, _) -> normalizeWorkingHourDayKey(day) }
        val today = currentWeekdayKey()
        val now = java.time.LocalTime.now()
        val todayRange = parseHoursRange(normalizedHours[today])

        if (todayRange != null) {
            val (openAt, closeAt) = todayRange
            if (now.isBefore(openAt)) {
                return OpeningStatus(OpeningStatus.State.CLOSED, "Închis", "Deschide la ${formatTime(openAt)}")
            }
            if (now.isBefore(closeAt)) {
                val minutesLeft = java.time.Duration.between(now, closeAt).toMinutes()
                if (minutesLeft <= 30) {
                    return OpeningStatus(OpeningStatus.State.CLOSING_SOON, "Deschis", "Închide în curând, până la ${formatTime(closeAt)}")
                }
                return OpeningStatus(OpeningStatus.State.OPEN, "Deschis", "până la ${formatTime(closeAt)}")
            }
        }

        val nextOpening = findNextOpening(normalizedHours, today)
        if (nextOpening != null) {
            return OpeningStatus(
                OpeningStatus.State.CLOSED,
                "Închis",
                "Deschide la ${formatTime(nextOpening.first)} ${nextOpening.second}"
            )
        }

        return OpeningStatus(OpeningStatus.State.CLOSED, "Închis", "Program indisponibil")
    }

    fun isOpenNow(): Boolean {
        val status = openingStatus()
        return status.state == OpeningStatus.State.OPEN || status.state == OpeningStatus.State.CLOSING_SOON
    }

    private fun currentWeekdayKey(): String {
        val orderedDays = listOf("luni", "marti", "miercuri", "joi", "vineri", "sambata", "duminica")
        val todayIndex = java.time.LocalDate.now().dayOfWeek.value - 1
        return orderedDays[todayIndex.coerceIn(0, orderedDays.lastIndex)]
    }

    private fun findNextOpening(
        normalizedHours: Map<String, String>,
        currentDay: String
    ): Pair<java.time.LocalTime, String>? {
        val orderedDays = listOf("luni", "marti", "miercuri", "joi", "vineri", "sambata", "duminica")
        val currentIndex = orderedDays.indexOf(currentDay).takeIf { it >= 0 } ?: 0

        for (offset in 1..7) {
            val candidateDay = orderedDays[(currentIndex + offset) % orderedDays.size]
            val range = parseHoursRange(normalizedHours[candidateDay]) ?: continue
            val label = when (candidateDay) {
                "luni" -> "luni"
                "marti" -> "marți"
                "miercuri" -> "miercuri"
                "joi" -> "joi"
                "vineri" -> "vineri"
                "sambata" -> "sâmbătă"
                "duminica" -> "duminică"
                else -> candidateDay
            }
            return range.first to label
        }
        return null
    }

    private fun parseHoursRange(value: String?): Pair<java.time.LocalTime, java.time.LocalTime>? {
        val normalized = value?.trim() ?: return null
        val lower = normalized.lowercase()
        if (lower.contains("închis") || lower.contains("closed")) return null
        if (lower.contains("non-stop") || lower.contains("nonstop")) {
            return java.time.LocalTime.MIN to java.time.LocalTime.MAX
        }

        val match = Regex("(\\d{1,2}:\\d{2})\\s*-\\s*(\\d{1,2}:\\d{2})").find(normalized) ?: return null
        return java.time.LocalTime.parse(match.groupValues[1]) to java.time.LocalTime.parse(match.groupValues[2])
    }

    private fun formatTime(time: java.time.LocalTime): String {
        return time.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
    }

    private fun normalizeWorkingHourDayKey(day: String): String {
        val value = day.trim().lowercase()
        val map = mapOf(
            "monday" to "luni",
            "mon" to "luni",
            "lun" to "luni",
            "luni" to "luni",
            "tuesday" to "marti",
            "tue" to "marti",
            "mar" to "marti",
            "marti" to "marti",
            "wednesday" to "miercuri",
            "wed" to "miercuri",
            "mie" to "miercuri",
            "miercuri" to "miercuri",
            "thursday" to "joi",
            "thu" to "joi",
            "joi" to "joi",
            "friday" to "vineri",
            "fri" to "vineri",
            "vin" to "vineri",
            "vineri" to "vineri",
            "saturday" to "sambata",
            "sat" to "sambata",
            "sam" to "sambata",
            "sambata" to "sambata",
            "sunday" to "duminica",
            "sun" to "duminica",
            "dum" to "duminica",
            "duminica" to "duminica"
        )
        return map[value] ?: value
    }

    fun currentUserReview(currentUserName: String): AutoReview? {
        val normalized = normalizeUserIdentifier(currentUserName)
        if (normalized.isBlank() || normalized == "guest") {
            return null
        }

        return reviews.firstOrNull { review ->
            val reviewNormalized = normalizeUserIdentifier(review.userName)
            reviewNormalized == normalized ||
                reviewNormalized.contains(normalized) ||
                normalized.contains(reviewNormalized)
        }
    }

    fun isCurrentUserOwner(currentUserName: String): Boolean {
        val normalized = normalizeUserIdentifier(currentUserName)
        if (normalized.isBlank() || normalized == "guest") {
            return false
        }

        val candidates = listOfNotNull(addedBy, ownerUserName)
        return candidates.any { candidate ->
            normalizeUserIdentifier(candidate).let { name ->
                name == normalized ||
                    name.startsWith(normalized) ||
                    normalized.startsWith(name) ||
                    name.contains(normalized) ||
                    normalized.contains(name)
            }
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
