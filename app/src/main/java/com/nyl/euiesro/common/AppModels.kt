package com.nyl.euiesro.common

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class AppScreen {
    SPLASH,
    LOGIN,
    REGISTER_WEBVIEW,
    MAIN,
    UPDATE,
    SECTION,
    EDIT_PROFILE,
    ADD_LAUNCH_SPOT,
    EDIT_LAUNCH_SPOT,
    SUGGEST_EDIT_LAUNCH_SPOT,
    ADD_MOTO_PLACE,
    EDIT_MOTO_PLACE,
    SUGGEST_EDIT_MOTO_PLACE,
    ADD_AUTO_PLACE,
    EDIT_AUTO_PLACE,
    SUGGEST_EDIT_AUTO_PLACE,
    ADD_ROAD_ASSISTANCE,
    EDIT_ROAD_ASSISTANCE,
    SUGGEST_EDIT_ROAD_ASSISTANCE,
    LOCATION_DETAIL,
    ADD_EVENT,
    EDIT_EVENT,
    EVENT_DETAIL
}

enum class ModuleSection {
    EVENTS,
    MOTO,
    ROAD_ASSISTANCE,
    LAUNCH_SPOTS,
    AUTO
}

data class ModuleItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val tint: Color
) {
    val description: String
        get() = subtitle
}

data class LoginResult(
    val success: Boolean,
    val message: String,
    val userName: String = ""
)

data class CurrentUserProfile(
    val id: Int = 0,
    val username: String = "",
    val name: String? = null,
    val role: String = "",
    val canModerate: Boolean = false,
    val avatarUrl: String = ""
) {
    fun canEditLaunchSpot(spotOwnerUserId: Int): Boolean {
        val isModeratorOrAdmin = canModerate || role.equals("admin", ignoreCase = true) || role.equals("moderator", ignoreCase = true)
        if (isModeratorOrAdmin) return true
        if (id <= 0 || spotOwnerUserId <= 0) return false
        return id == spotOwnerUserId
    }

    fun canEditMotoPlace(placeOwnerUserId: Int): Boolean {
        val isModeratorOrAdmin = canModerate || role.equals("admin", ignoreCase = true) || role.equals("moderator", ignoreCase = true)
        if (isModeratorOrAdmin) return true
        if (id <= 0 || placeOwnerUserId <= 0) return false
        return id == placeOwnerUserId
    }
}

data class LocationUiItem(
    val id: Int,
    val section: ModuleSection,
    val title: String,
    val description: String?,
    val address: String?,
    val latitude: Double,
    val longitude: Double,
    val phone: String?,
    val website: String?,
    val email: String? = null,
    val services: List<String>,
    val imageUrls: List<String>,
    val typeIds: List<Int>,
    val markerIcons: List<String> = emptyList(),
    val averageRating: Float = 0f,
    val reviewsCount: Int = 0
) {
    companion object {
        fun averageCenter(items: List<LocationUiItem>): Pair<Double, Double> {
            if (items.isEmpty()) return 44.43 to 25.91
            val avgLat = items.map { it.latitude }.average()
            val avgLon = items.map { it.longitude }.average()
            return avgLat to avgLon
        }
    }
}
