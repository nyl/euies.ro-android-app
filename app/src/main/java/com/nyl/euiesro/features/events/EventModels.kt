package com.nyl.euiesro.features.events

data class EventUserState(
    val rsvpStatus: String? = null,
    val reaction: String? = null
)

data class EventComment(
    val id: Int = 0,
    val userName: String? = null,
    val username: String? = null,
    val content: String = "",
    val createdAt: String? = null
)

data class EventUpdate(
    val id: Int = 0,
    val userName: String? = null,
    val username: String? = null,
    val content: String = "",
    val createdAt: String? = null
)

data class EventSummary(
    val id: Int,
    val title: String,
    val startAt: String?,
    val endAt: String? = null,
    val locationName: String?,
    val locationAddress: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val status: String?,
    val description: String? = null,
    val coverImageUrl: String? = null,
    val organizerName: String? = null,
    val organizerIsNotUser: Boolean = false,
    val organizerCustomName: String? = null,
    val organizerWhatsappCountryCode: String? = null,
    val organizerWhatsappNumber: String? = null,
    val whatsappGroupUrl: String? = null,
    val userName: String? = null,
    val username: String? = null,
    val userRsvpStatus: String? = null,
    val userReaction: String? = null,
    val likeCount: Int = 0,
    val dislikeCount: Int = 0,
    val interestedCount: Int = 0,
    val goingCount: Int = 0,
    val joinedAttendees: List<String> = emptyList(),
    val interestedAttendees: List<String> = emptyList(),
    val updates: List<EventUpdate> = emptyList(),
    val comments: List<EventComment> = emptyList()
)

data class EventFormData(
    val title: String = "",
    val description: String = "",
    val startAt: String = "",
    val endAt: String = "",
    val timezone: String = "",
    val locationName: String = "",
    val locationAddress: String = "",
    val latitude: String = "45.9432",
    val longitude: String = "24.9668",
    val whatsappGroupUrl: String = "",
    val organizerIsNotUser: Boolean = false,
    val organizerName: String = "",
    val organizerWhatsappCountryCode: String = "",
    val organizerWhatsappNumber: String = "",
    val coverImageUrl: String = ""
)
