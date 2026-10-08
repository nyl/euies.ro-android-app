package com.nyl.euiesro

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.nyl.euiesro.app.AppLocale as AppLocale
import com.nyl.euiesro.app.AppLocaleManager as AppLocaleManager
import com.nyl.euiesro.app.EuIesroApp as AppShell
import com.nyl.euiesro.app.ThemePreferencesManager as AppThemePreferencesManager
import com.nyl.euiesro.app.screens.EditProfileScreen
import com.nyl.euiesro.app.screens.UpdateScreen
import com.nyl.euiesro.common.AppScreen
import com.nyl.euiesro.common.CompactFilterRow
import com.nyl.euiesro.common.CompactLocationListItem
import com.nyl.euiesro.common.CompactMapPreviewCard
import com.nyl.euiesro.common.CompactSearchField
import com.nyl.euiesro.common.CurrentUserProfile
import com.nyl.euiesro.common.GroupedTypeCategory
import com.nyl.euiesro.common.GroupedTypeOption
import com.nyl.euiesro.common.GroupedTypeSelector
import com.nyl.euiesro.common.LocationUiItem
import com.nyl.euiesro.common.map.MapBottomSheetLayout
import com.nyl.euiesro.common.ModuleItem
import com.nyl.euiesro.common.ModuleSection
import com.nyl.euiesro.common.LoginResult
import com.nyl.euiesro.features.auto.AddAutoPlaceScreen
import com.nyl.euiesro.features.auto.AutoDetailScreen
import com.nyl.euiesro.features.auto.AutoSection
import com.nyl.euiesro.features.auto.EditAutoPlaceScreen
import com.nyl.euiesro.features.auto.SuggestEditAutoPlaceScreen
import com.nyl.euiesro.features.events.CreateEventScreen
import com.nyl.euiesro.features.events.EditEventScreen
import com.nyl.euiesro.features.events.EventDetailScreen
import com.nyl.euiesro.features.events.EventSummary
import com.nyl.euiesro.features.events.EventsSection
import com.nyl.euiesro.features.launchspots.AddLaunchSpotsScreen
import com.nyl.euiesro.features.launchspots.EditLaunchSpotsScreen
import com.nyl.euiesro.features.launchspots.LaunchSpotDetailScreen
import com.nyl.euiesro.features.launchspots.LaunchSpotSummary
import com.nyl.euiesro.features.launchspots.LaunchSpotsSection
import com.nyl.euiesro.features.launchspots.SuggestEditScreen
import com.nyl.euiesro.features.moto.AddMotoPlaceScreen
import com.nyl.euiesro.features.moto.EditMotoPlaceScreen
import com.nyl.euiesro.features.moto.MotoDetailScreen
import com.nyl.euiesro.features.moto.MotoPlaceSummary
import com.nyl.euiesro.features.moto.SuggestEditMotoPlaceScreen
import com.nyl.euiesro.features.roadassistance.AddRoadAssistanceScreen
import com.nyl.euiesro.features.roadassistance.EditRoadAssistanceScreen
import com.nyl.euiesro.features.roadassistance.RoadAssistanceDetailScreen
import com.nyl.euiesro.features.roadassistance.RoadAssistanceSummary
import com.nyl.euiesro.features.roadassistance.SuggestEditRoadAssistanceScreen
import com.nyl.euiesro.features.roadassistance.common.RoadAssistanceBoundaryMapUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.Image
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CarRepair
import androidx.compose.material.icons.filled.LocalCarWash
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Water
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import com.nyl.euiesro.features.moto.MotoSection
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import coil.compose.AsyncImage
import com.nyl.euiesro.ui.theme.EuIesroTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.ui.res.stringResource
import org.osmdroid.views.MapView

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val savedDarkMode = AppThemePreferencesManager.isDarkModeEnabled(this)
        setContent {
            var darkMode by rememberSaveable { mutableStateOf(savedDarkMode) }
            EuIesroTheme(darkTheme = darkMode) {
                AppShell(
                    darkMode = darkMode,
                    onDarkModeChanged = { enabled ->
                        darkMode = enabled
                    }
                )
            }
        }
    }
}


object ThemePreferencesManager {
    private const val PREFS_NAME = "euiesro_theme_prefs"
    private const val KEY_DARK_MODE = "dark_mode_enabled"

    fun isDarkModeEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_DARK_MODE, false)
    }

    fun setDarkModeEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_DARK_MODE, enabled)
            .apply()
    }
}

object AppSessionStateManager {
    private const val PREFS_NAME = "euiesro_session_prefs"
    private const val KEY_SCREEN = "current_screen"
    private const val KEY_USER_NAME = "current_user_name"
    private const val KEY_LOGGED_IN = "logged_in"
    private const val KEY_SELECTED_SECTION = "selected_section"
    private const val KEY_SELECTED_LOCATION = "selected_location"
    private const val KEY_SELECTED_EVENT = "selected_event"
    private const val KEY_PROFILE = "profile"

    data class SessionSnapshot(
        val screen: AppScreen,
        val userName: String,
        val isLoggedIn: Boolean,
        val currentUserProfile: CurrentUserProfile = CurrentUserProfile(),
        val selectedSection: ModuleSection? = null,
        val selectedLocation: LocationUiItem? = null,
        val selectedEvent: EventSummary? = null
    )

    internal fun serializeScreen(screen: AppScreen): String = screen.name

    internal fun deserializeScreen(rawValue: String?): AppScreen? {
        if (rawValue.isNullOrBlank()) return null
        return AppScreen.entries.firstOrNull { it.name == rawValue }
    }

    private fun serializeProfile(profile: CurrentUserProfile): String {
        return JSONObject().apply {
            put("id", profile.id)
            put("username", profile.username)
            put("name", profile.name ?: JSONObject.NULL)
            put("role", profile.role)
            put("can_moderate", profile.canModerate)
            put("avatar_url", profile.avatarUrl)
        }.toString()
    }

    private fun deserializeProfile(rawValue: String?): CurrentUserProfile {
        if (rawValue.isNullOrBlank()) return CurrentUserProfile()
        return try {
            val root = JSONObject(rawValue)
            CurrentUserProfile(
                id = root.optInt("id", 0),
                username = root.optString("username", ""),
                name = root.optString("name", "").ifBlank { null },
                role = root.optString("role", ""),
                canModerate = root.optBoolean("can_moderate", false),
                avatarUrl = root.optString("avatar_url", "")
            )
        } catch (_: Exception) {
            CurrentUserProfile()
        }
    }

    private fun serializeLocation(location: LocationUiItem): String {
        return JSONObject().apply {
            put("id", location.id)
            put("section", location.section.name)
            put("title", location.title)
            put("description", location.description)
            put("address", location.address)
            put("latitude", location.latitude)
            put("longitude", location.longitude)
            put("phone", location.phone)
            put("website", location.website)
            put("email", location.email)
            put("services", JSONArray(location.services))
            put("image_urls", JSONArray(location.imageUrls))
            put("type_ids", JSONArray(location.typeIds.map { it }))
            put("marker_icons", JSONArray(location.markerIcons))
            put("average_rating", location.averageRating)
            put("reviews_count", location.reviewsCount)
        }.toString()
    }

    private fun deserializeLocation(rawValue: String?): LocationUiItem? {
        if (rawValue.isNullOrBlank()) return null
        return try {
            val root = JSONObject(rawValue)
            val sectionName = root.optString("section", "")
            val section = ModuleSection.entries.firstOrNull { it.name == sectionName } ?: ModuleSection.MOTO
            val services = root.optJSONArray("services")?.let { array ->
                List(array.length()) { index -> array.getString(index) }
            } ?: emptyList()
            val imageUrls = root.optJSONArray("image_urls")?.let { array ->
                List(array.length()) { index -> array.getString(index) }
            } ?: emptyList()
            val typeIds = root.optJSONArray("type_ids")?.let { array ->
                List(array.length()) { index -> array.getInt(index) }
            } ?: emptyList()
            val markerIcons = root.optJSONArray("marker_icons")?.let { array ->
                List(array.length()) { index -> array.getString(index) }
            } ?: emptyList()

            LocationUiItem(
                id = root.optInt("id", 0),
                section = section,
                title = root.optString("title", ""),
                description = root.optString("description", "").takeIf { it.isNotBlank() },
                address = root.optString("address", "").takeIf { it.isNotBlank() },
                latitude = root.optDouble("latitude", 0.0),
                longitude = root.optDouble("longitude", 0.0),
                phone = root.optString("phone", "").takeIf { it.isNotBlank() },
                website = root.optString("website", "").takeIf { it.isNotBlank() },
                email = root.optString("email", "").takeIf { it.isNotBlank() },
                services = services,
                imageUrls = imageUrls,
                typeIds = typeIds,
                markerIcons = markerIcons,
                averageRating = root.optDouble("average_rating", 0.0).toFloat(),
                reviewsCount = root.optInt("reviews_count", 0)
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun serializeEvent(event: EventSummary): String {
        return JSONObject().apply {
            put("id", event.id)
            put("title", event.title)
            put("start_at", event.startAt)
            put("end_at", event.endAt)
            put("location_name", event.locationName)
            put("location_address", event.locationAddress)
            put("latitude", event.latitude)
            put("longitude", event.longitude)
            put("status", event.status)
            put("description", event.description)
            put("cover_image_url", event.coverImageUrl)
            put("organizer_name", event.organizerName)
            put("organizer_is_not_user", event.organizerIsNotUser)
            put("organizer_custom_name", event.organizerCustomName)
            put("organizer_whatsapp_country_code", event.organizerWhatsappCountryCode)
            put("organizer_whatsapp_number", event.organizerWhatsappNumber)
            put("whatsapp_group_url", event.whatsappGroupUrl)
            put("user_name", event.userName)
            put("username", event.username)
            put("user_rsvp_status", event.userRsvpStatus)
            put("user_reaction", event.userReaction)
            put("like_count", event.likeCount)
            put("dislike_count", event.dislikeCount)
            put("interested_count", event.interestedCount)
            put("going_count", event.goingCount)
            put("joined_attendees", JSONArray(event.joinedAttendees))
            put("interested_attendees", JSONArray(event.interestedAttendees))
            put("updates", JSONArray(event.updates.map { JSONObject().apply {
                put("id", it.id)
                put("user_name", it.userName)
                put("username", it.username)
                put("content", it.content)
                put("created_at", it.createdAt)
            }}))
            put("comments", JSONArray(event.comments.map { JSONObject().apply {
                put("id", it.id)
                put("user_name", it.userName)
                put("username", it.username)
                put("content", it.content)
                put("created_at", it.createdAt)
            }}))
        }.toString()
    }

    private fun deserializeEvent(rawValue: String?): EventSummary? {
        if (rawValue.isNullOrBlank()) return null
        return try {
            val root = JSONObject(rawValue)
            EventSummary(
                id = root.optInt("id", 0),
                title = root.optString("title", ""),
                startAt = root.optString("start_at", "").takeIf { it.isNotBlank() },
                endAt = root.optString("end_at", "").takeIf { it.isNotBlank() },
                locationName = root.optString("location_name", "").takeIf { it.isNotBlank() },
                locationAddress = root.optString("location_address", "").takeIf { it.isNotBlank() },
                latitude = root.optDouble("latitude", 0.0).takeIf { !it.isNaN() },
                longitude = root.optDouble("longitude", 0.0).takeIf { !it.isNaN() },
                status = root.optString("status", "").takeIf { it.isNotBlank() },
                description = root.optString("description", "").takeIf { it.isNotBlank() },
                coverImageUrl = root.optString("cover_image_url", "").takeIf { it.isNotBlank() },
                organizerName = root.optString("organizer_name", "").takeIf { it.isNotBlank() },
                organizerIsNotUser = root.optBoolean("organizer_is_not_user", false),
                organizerCustomName = root.optString("organizer_custom_name", "").takeIf { it.isNotBlank() },
                organizerWhatsappCountryCode = root.optString("organizer_whatsapp_country_code", "").takeIf { it.isNotBlank() },
                organizerWhatsappNumber = root.optString("organizer_whatsapp_number", "").takeIf { it.isNotBlank() },
                whatsappGroupUrl = root.optString("whatsapp_group_url", "").takeIf { it.isNotBlank() },
                userName = root.optString("user_name", "").takeIf { it.isNotBlank() },
                username = root.optString("username", "").takeIf { it.isNotBlank() },
                userRsvpStatus = root.optString("user_rsvp_status", "").takeIf { it.isNotBlank() },
                userReaction = root.optString("user_reaction", "").takeIf { it.isNotBlank() },
                likeCount = root.optInt("like_count", 0),
                dislikeCount = root.optInt("dislike_count", 0),
                interestedCount = root.optInt("interested_count", 0),
                goingCount = root.optInt("going_count", 0),
                joinedAttendees = emptyList(),
                interestedAttendees = emptyList(),
                updates = emptyList(),
                comments = emptyList()
            )
        } catch (_: Exception) {
            null
        }
    }

    fun saveSession(
        context: Context,
        screen: AppScreen,
        userName: String = "Guest",
        isLoggedIn: Boolean = false,
        currentUserProfile: CurrentUserProfile = CurrentUserProfile(),
        selectedSection: ModuleSection? = null,
        selectedLocation: LocationUiItem? = null,
        selectedEvent: EventSummary? = null
    ) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SCREEN, serializeScreen(screen))
            .putString(KEY_USER_NAME, userName.ifBlank { "Guest" })
            .putBoolean(KEY_LOGGED_IN, isLoggedIn)
            .putString(KEY_SELECTED_SECTION, selectedSection?.name)
            .putString(KEY_SELECTED_LOCATION, selectedLocation?.let { serializeLocation(it) })
            .putString(KEY_SELECTED_EVENT, selectedEvent?.let { serializeEvent(it) })
            .putString(KEY_PROFILE, serializeProfile(currentUserProfile))
            .apply()
    }

    fun readSession(context: Context): SessionSnapshot? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val screen = deserializeScreen(prefs.getString(KEY_SCREEN, null)) ?: return null
        val selectedSectionName = prefs.getString(KEY_SELECTED_SECTION, null)
        val selectedSection = ModuleSection.entries.firstOrNull { it.name == selectedSectionName }

        return SessionSnapshot(
            screen = screen,
            userName = prefs.getString(KEY_USER_NAME, "Guest") ?: "Guest",
            isLoggedIn = prefs.getBoolean(KEY_LOGGED_IN, false),
            currentUserProfile = deserializeProfile(prefs.getString(KEY_PROFILE, null)),
            selectedSection = selectedSection,
            selectedLocation = deserializeLocation(prefs.getString(KEY_SELECTED_LOCATION, null)),
            selectedEvent = deserializeEvent(prefs.getString(KEY_SELECTED_EVENT, null))
        )
    }

    fun coldStartScreen(snapshot: SessionSnapshot?): AppScreen = AppScreen.MAIN

    fun coldStartSelectedSection(snapshot: SessionSnapshot?): ModuleSection? = null

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_SCREEN)
            .remove(KEY_USER_NAME)
            .remove(KEY_LOGGED_IN)
            .remove(KEY_SELECTED_SECTION)
            .remove(KEY_SELECTED_LOCATION)
            .remove(KEY_SELECTED_EVENT)
            .remove(KEY_PROFILE)
            .apply()
    }
}

object PreferencesManager {
    private const val PREFS_NAME = "euiesro_prefs"
    private const val KEY_USERNAME = "username"
    private const val KEY_PASSWORD = "password"
    private const val KEY_REMEMBER = "remember"

    fun saveCredentials(context: Context, user: String, pass: String, remember: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            if (remember) {
                putString(KEY_USERNAME, user)
                putString(KEY_PASSWORD, pass)
            } else {
                remove(KEY_USERNAME)
                remove(KEY_PASSWORD)
            }
            putBoolean(KEY_REMEMBER, remember)
            apply()
        }
    }

    fun getSavedUsername(context: Context): String? =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_USERNAME, null)

    fun getSavedPassword(context: Context): String? =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_PASSWORD, null)

    fun isRememberMeEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_REMEMBER, false)

    fun clearCredentials(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().apply {
            remove(KEY_USERNAME)
            remove(KEY_PASSWORD)
            putBoolean(KEY_REMEMBER, false)
            apply()
        }
    }
}

object MediaUrlResolver {
    fun resolve(rawUrl: String?): String? {
        val value = rawUrl?.trim() ?: return null
        if (value.isEmpty()) return null
        return when {
            value.startsWith("http://", true) || value.startsWith("https://", true) -> value
            value.startsWith("/", true) -> "${ApiClient.baseUrl.trimEnd('/')}$value"
            else -> value
        }
    }
}

object EventOrganizerResolver {
    fun resolve(json: JSONObject): String {
        return resolve(
            customName = json.optString("organizer_name", "").trim(),
            isCustomOrganizer = json.optInt("organizer_is_not_user", 0) == 1,
            userName = json.optString("user_name", "").trim(),
            username = json.optString("username", "").trim()
        )
    }

    fun resolve(
        customName: String?,
        isCustomOrganizer: Boolean,
        userName: String?,
        username: String?
    ): String {
        val normalizedCustom = customName?.trim().orEmpty()
        if (isCustomOrganizer) {
            return normalizedCustom.ifEmpty { "Anonymous" }
        }

        val normalizedUser = userName?.trim().orEmpty()
        if (normalizedUser.isNotEmpty()) return normalizedUser

        val normalizedUsername = username?.trim().orEmpty()
        return normalizedUsername.ifEmpty { "Anonymous" }
    }
}

data class AppRelease(
    val versionName: String,
    val versionCode: Long,
    val apkUrl: String,
    val changelog: String = "",
    val force: Boolean = false
)

object AppUpdater {
    private const val TAG = "AppUpdater"
    private const val RELEASE_URL = "https://app.euies.ro/releases/latest.json"

    private fun normalizeApkUrl(rawUrl: String): String {
        val trimmed = rawUrl.trim()
        if (trimmed.isEmpty()) return ""
        if (trimmed.startsWith("http://", true) || trimmed.startsWith("https://", true)) {
            return trimmed
        }
        return if (trimmed.startsWith("/")) {
            "https://app.euies.ro$trimmed"
        } else {
            "https://app.euies.ro/$trimmed"
        }
    }

    fun currentVersionCode(context: Context): Long {
        return context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode
    }

    fun apkFileFor(context: Context, release: AppRelease): File {
        return File(context.getExternalFilesDir(null), "update-${release.versionCode}.apk")
    }

    fun hasDownloadedUpdate(context: Context, release: AppRelease): Boolean {
        val file = apkFileFor(context, release)
        return file.exists() && file.length() > 0L
    }

    fun deleteDownloadedUpdate(context: Context, release: AppRelease): Boolean {
        val file = apkFileFor(context, release)
        return !file.exists() || file.delete()
    }

    suspend fun checkForUpdate(context: Context): AppRelease? = withContext(Dispatchers.IO) {
        try {
            val url = URL(RELEASE_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.instanceFollowRedirects = true
            if (connection.responseCode !in 200..299) {
                return@withContext null
            }

            val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val root = JSONObject(body)
            val versionName = root.optString("version_name", "").ifBlank { "Unknown" }
            val versionCode = root.optLong("version_code", 0L)
            val apkUrl = normalizeApkUrl(root.optString("apk_url", ""))
            if (apkUrl.isBlank()) {
                return@withContext null
            }
            val changelog = root.optString("changelog", "")
            val force = root.optBoolean("force", false)
            val current = currentVersionCode(context)

            if (versionCode <= current) {
                return@withContext null
            }

            AppRelease(versionName, versionCode, apkUrl, changelog, force)
        } catch (e: Exception) {
            Log.e(TAG, "checkForUpdate failed", e)
            null
        }
    }

    suspend fun downloadUpdate(
        context: Context,
        release: AppRelease,
        onProgress: (Float) -> Unit = {},
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val apkFile = apkFileFor(context, release)
            if (apkFile.exists()) {
                apkFile.delete()
            }

            val resolvedUrl = normalizeApkUrl(release.apkUrl)
            val connection = URL(resolvedUrl).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 15000
            connection.readTimeout = 20000
            connection.instanceFollowRedirects = true

            if (connection.responseCode !in 200..299) {
                Log.e(TAG, "APK download failed with HTTP ${connection.responseCode}")
                return@withContext false
            }

            val totalBytes = connection.contentLengthLong
            var downloadedBytes = 0L
            val buffer = ByteArray(8192)

            connection.inputStream.use { input ->
                FileOutputStream(apkFile).use { output ->
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        downloadedBytes += read.toLong()
                        val progress = if (totalBytes > 0) {
                            (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                        } else {
                            0f
                        }
                        withContext(Dispatchers.Main.immediate) {
                            onProgress(progress)
                        }
                    }
                    output.flush()
                }
            }

            withContext(Dispatchers.Main.immediate) {
                onProgress(1f)
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "downloadUpdate failed", e)
            false
        }
    }

    @Suppress("DEPRECATION")
    suspend fun installDownloadedUpdate(
        context: Context,
        release: AppRelease,
        onInstallStarted: (() -> Unit)? = null,
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val packageManager = context.packageManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !packageManager.canRequestPackageInstalls()) {
                val intent = Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                intent.data = Uri.parse("package:${context.packageName}")
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                withContext(Dispatchers.Main.immediate) {
                    context.startActivity(intent)
                }
                return@withContext false
            }

            val apkFile = apkFileFor(context, release)
            if (!apkFile.exists() || apkFile.length() <= 0L) {
                return@withContext false
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val intent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            withContext(Dispatchers.Main.immediate) {
                context.startActivity(intent)
                onInstallStarted?.invoke()
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "installDownloadedUpdate failed", e)
            false
        }
    }
}

object ApiClient {
    private const val TAG = "ApiClient"
    var baseUrl: String = "https://euies.ro"
    private val sessionCookies = LinkedHashMap<String, String>()

    suspend fun testApi(): String = withContext(Dispatchers.IO) {
        try {
            val url = URL("${baseUrl.trimEnd('/')}/api/health")
            val connection = prepareRequest(url, "GET")
            val body = readBody(connection)
            val status = connection.responseCode
            if (status in 200..299) {
                "OK ($status): $body"
            } else {
                "ERROR ($status): $body"
            }
        } catch (e: Exception) {
            Log.e(TAG, "API test failed", e)
            "Connection failed: ${e.localizedMessage ?: "unknown error"}"
        }
    }

    suspend fun fetchCurrentUserProfile(): CurrentUserProfile = withContext(Dispatchers.IO) {
        try {
            val url = URL("${baseUrl.trimEnd('/')}/api/me")
            val connection = prepareRequest(url, "GET")
            val status = connection.responseCode
            if (status !in 200..299) {
                return@withContext CurrentUserProfile()
            }
            val body = readBody(connection)
            if (body.isBlank()) {
                return@withContext CurrentUserProfile()
            }
            val root = JSONObject(body)
            val data = if (root.has("data") && root.opt("data") is JSONObject) root.getJSONObject("data") else root
            val id = data.optInt("id", 0)
            val username = data.optString("username", "").trim()
            val role = data.optString("role", "").trim()
            val name = data.optString("name", "").trim().ifBlank { null }
            CurrentUserProfile(
                id = id,
                username = username,
                name = name,
                role = role,
                canModerate = role.equals("admin", ignoreCase = true) || role.equals("moderator", ignoreCase = true) || data.optBoolean("can_moderate", false),
                avatarUrl = data.optString("avatar_url", "").trim()
            )
        } catch (e: Exception) {
            Log.e(TAG, "fetchCurrentUserProfile failed", e)
            CurrentUserProfile()
        }
    }

    suspend fun uploadCurrentUserAvatar(context: Context, imageUri: Uri): CurrentUserProfile = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(imageUri) ?: "image/jpeg"
            val inputStream = contentResolver.openInputStream(imageUri) ?: return@withContext CurrentUserProfile()
            val bytes = inputStream.use { it.readBytes() }
            val boundary = "----EuIesroProfileAvatar${System.currentTimeMillis()}"
            val fileName = imageUri.lastPathSegment?.takeIf { it.isNotBlank() } ?: "profile-avatar.jpg"
            val url = URL("${baseUrl.trimEnd('/')}/api/profile/avatar")
            val connection = prepareRequest(url, "POST")
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")

            connection.outputStream.use { output ->
                output.write("--$boundary\r\n".toByteArray(Charsets.UTF_8))
                output.write("Content-Disposition: form-data; name=\"avatar_file\"; filename=\"${fileName.replace("\\".toRegex(), "_")}\"\r\n".toByteArray(Charsets.UTF_8))
                output.write("Content-Type: $mimeType\r\n\r\n".toByteArray(Charsets.UTF_8))
                output.write(bytes)
                output.write("\r\n--$boundary--\r\n".toByteArray(Charsets.UTF_8))
            }

            val status = connection.responseCode
            if (status !in 200..299) {
                return@withContext CurrentUserProfile()
            }

            val body = readBody(connection)
            val root = JSONObject(body)
            val data = if (root.has("data") && root.opt("data") is JSONObject) root.getJSONObject("data") else root
            val id = data.optInt("id", 0)
            val username = data.optString("username", "").trim()
            val role = data.optString("role", "").trim()
            val name = data.optString("name", "").trim().ifBlank { null }
            CurrentUserProfile(
                id = id,
                username = username,
                name = name,
                role = role,
                canModerate = role.equals("admin", true) || role.equals("moderator", true) || data.optBoolean("can_moderate", false),
                avatarUrl = data.optString("avatar_url", "").trim()
            )
        } catch (e: Exception) {
            Log.e(TAG, "uploadCurrentUserAvatar failed", e)
            CurrentUserProfile()
        }
    }

    suspend fun deleteCurrentUserAvatar(): CurrentUserProfile = withContext(Dispatchers.IO) {
        try {
            val url = URL("${baseUrl.trimEnd('/')}/api/profile/avatar")
            val connection = prepareRequest(url, "DELETE")
            val status = connection.responseCode
            if (status !in 200..299) {
                return@withContext CurrentUserProfile()
            }
            val body = readBody(connection)
            val root = JSONObject(body)
            val data = if (root.has("data") && root.opt("data") is JSONObject) root.getJSONObject("data") else root
            val id = data.optInt("id", 0)
            val username = data.optString("username", "").trim()
            val role = data.optString("role", "").trim()
            val name = data.optString("name", "").trim().ifBlank { null }
            CurrentUserProfile(
                id = id,
                username = username,
                name = name,
                role = role,
                canModerate = role.equals("admin", true) || role.equals("moderator", true) || data.optBoolean("can_moderate", false),
                avatarUrl = data.optString("avatar_url", "").trim()
            )
        } catch (e: Exception) {
            Log.e(TAG, "deleteCurrentUserAvatar failed", e)
            CurrentUserProfile()
        }
    }

    suspend fun logout() {
        sessionCookies.clear()
    }

    suspend fun validateSession(): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("${baseUrl.trimEnd('/')}/api/me")
            val connection = prepareRequest(url, "GET")
            val status = connection.responseCode
            if (status == 401 || status == 403) {
                sessionCookies.clear()
                return@withContext false
            }
            if (status !in 200..299) {
                return@withContext false
            }
            val body = readBody(connection)
            if (body.isBlank()) {
                return@withContext false
            }
            val root = JSONObject(body)
            val payload = if (root.has("data") && root.opt("data") is JSONObject) root.getJSONObject("data") else root
            val hasUser = payload.optInt("id", 0) > 0 || payload.has("username") || payload.has("name") || body.contains("\"user\"", ignoreCase = true)
            hasUser
        } catch (e: Exception) {
            Log.e(TAG, "validateSession failed", e)
            false
        }
    }

    suspend fun ensureValidSession(context: Context): Boolean = withContext(Dispatchers.IO) {
        if (validateSession()) {
            return@withContext true
        }

        val rememberMe = PreferencesManager.isRememberMeEnabled(context)
        val savedUser = PreferencesManager.getSavedUsername(context)
        val savedPass = PreferencesManager.getSavedPassword(context)

        if (!rememberMe || savedUser.isNullOrBlank() || savedPass.isNullOrBlank()) {
            logout()
            return@withContext false
        }

        val result = login(savedUser, savedPass)
        if (result.success) {
            return@withContext true
        }

        logout()
        PreferencesManager.clearCredentials(context)
        false
    }

    suspend fun login(username: String, password: String): LoginResult = withContext(Dispatchers.IO) {
        if (username.isBlank()) {
            return@withContext LoginResult(false, "Username is required.")
        }
        if (password.isBlank()) {
            return@withContext LoginResult(false, "Password is required.")
        }

        val safeUser = username.trim()
        try {
            val url = URL("${baseUrl.trimEnd('/')}/login.php")
            val connection = prepareRequest(url, "POST")
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")

            val body = buildString {
                append("username=")
                append(URLEncoder.encode(safeUser, "UTF-8"))
                append("&password=")
                append(URLEncoder.encode(password, "UTF-8"))
                append("&remember_me=1")
            }

            connection.outputStream.use { output ->
                output.write(body.toByteArray(Charsets.UTF_8))
                output.flush()
            }

            val status = connection.responseCode
            val responseBody = readBody(connection)
            val redirected = connection.getHeaderField("Location")
            storeSessionCookies(connection)
            val hasSession = sessionCookies.isNotEmpty()

            if ((status in 200..299 || status in 300..399) && (hasSession || redirected != null)) {
                val normalized = responseBody.lowercase()
                if (normalized.contains("invalid username or password") || normalized.contains("login_error") || normalized.contains("alert error")) {
                    return@withContext LoginResult(false, "Invalid username or password.")
                }
                return@withContext LoginResult(true, "Login successful", safeUser)
            }

            if (status == 401 || status == 403) {
                return@withContext LoginResult(false, "Unauthorized: invalid credentials.")
            }

            val extracted = extractHtmlError(responseBody)
            LoginResult(false, extracted ?: "Login failed with HTTP $status.")
        } catch (e: Exception) {
            Log.e(TAG, "Login failed", e)
            LoginResult(false, "Connection failed: ${e.localizedMessage ?: "unknown error"}")
        }
    }

    private fun buildLaunchSpotTypeOptions(rawTypeIds: JSONArray, rawTypes: JSONArray?): List<com.nyl.euiesro.features.launchspots.LaunchSpotTypeOption> {
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
            .map { (id, label) -> com.nyl.euiesro.features.launchspots.LaunchSpotTypeOption(id = id, label = label) }
    }

    private fun JSONObject.optNullableString(name: String): String? {
        if (isNull(name)) return null
        val value = optString(name, "")
        return if (value.isBlank()) null else value
    }

    suspend fun fetchUpcomingEvents(limit: Int = 10, scope: String = "upcoming"): List<EventSummary> = withContext(Dispatchers.IO) {
        try {
            val url = URL("${baseUrl.trimEnd('/')}/api/events?scope=${scope}&limit=$limit")
            val connection = prepareRequest(url, "GET")
            val status = connection.responseCode
            if (status !in 200..299) {
                Log.e(TAG, "Event fetch failed: HTTP $status")
                return@withContext emptyList()
            }

            val body = readBody(connection)
            val root = JSONObject(body)
            val data = root.optJSONObject("data") ?: JSONObject()
            val items = data.optJSONArray("items") ?: JSONArray()
            val result = mutableListOf<EventSummary>()
            for (i in 0 until items.length()) {
                val obj = items.getJSONObject(i)
                result.add(
                    EventSummary(
                        id = obj.optInt("id", 0),
                        title = obj.optString("title", "Untitled"),
                        startAt = obj.optNullableString("start_at"),
                        locationName = obj.optNullableString("location_name"),
                        locationAddress = obj.optNullableString("location_address"),
                        latitude = obj.optDouble("location_lat", Double.NaN).takeUnless { it.isNaN() },
                        longitude = obj.optDouble("location_lng", Double.NaN).takeUnless { it.isNaN() },
                        status = obj.optNullableString("status"),
                        description = obj.optNullableString("description"),
                        coverImageUrl = MediaUrlResolver.resolve(obj.optNullableString("cover_image_url")),
                        organizerName = EventOrganizerResolver.resolve(obj),
                        interestedCount = obj.optInt("interested_count", 0),
                        goingCount = obj.optInt("going_count", 0)
                    )
                )
            }
            result
        } catch (e: Exception) {
            Log.e(TAG, "fetchUpcomingEvents failed", e)
            emptyList()
        }
    }

    suspend fun fetchMotoPlaceDetail(id: Int): MotoPlaceSummary? = withContext(Dispatchers.IO) {
        try {
            val url = URL("${baseUrl.trimEnd('/')}/api/moto/detail?id=$id")
            val connection = prepareRequest(url, "GET")
            val status = connection.responseCode
            if (status !in 200..299) {
                Log.e(TAG, "Moto detail fetch failed: HTTP $status")
                return@withContext null
            }

            val body = readBody(connection)
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
                val imageUrl = imageEntry.optString("image_url", "").ifBlank {
                    imageEntry.optString("url", "")
                }
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
                        val entry = workingHoursObject.optJSONObject(key)
                        if (entry == null) continue
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
                    // ignore malformed working-hours payloads; they should still render the rest of the detail page
                }
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
                primaryImage = imageUrls.firstOrNull(),
                images = imageUrls,
                workingHours = workingHours
            )
        } catch (e: Exception) {
            Log.e(TAG, "fetchMotoPlaceDetail failed", e)
            null
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
            val url = URL("${baseUrl.trimEnd('/')}/api/moto?$parameters")
            val connection = prepareRequest(url, "GET")
            val status = connection.responseCode
            if (status !in 200..299) {
                Log.e(TAG, "Moto fetch failed: HTTP $status")
                return@withContext emptyList()
            }

            val body = readBody(connection)
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
                        primaryImage = imageCandidates.firstOrNull(),
                        workingHours = workingHours,
                        averageRating = obj.optDouble("average_rating", 0.0).toFloat(),
                        reviewsCount = obj.optInt("reviews_count", 0)
                    )
                )
            }
            result
        } catch (e: Exception) {
            Log.e(TAG, "fetchMotoPlaces failed", e)
            emptyList()
        }
    }

    suspend fun fetchRoadAssistancePlaces(
        limit: Int = 500,
        query: String? = null,
        service: String? = null,
        availableInAreaOnly: Boolean = false,
        userLatitude: Double? = null,
        userLongitude: Double? = null
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
                if (availableInAreaOnly) {
                    append("&available_in_area=1")
                    if (userLatitude != null && userLongitude != null) {
                        append("&lat=")
                        append(String.format(Locale.US, "%.7f", userLatitude))
                        append("&lng=")
                        append(String.format(Locale.US, "%.7f", userLongitude))
                    }
                }
            }
            val url = URL("${baseUrl.trimEnd('/')}/api/road-assistance?$parameters")
            val connection = prepareRequest(url, "GET")
            val status = connection.responseCode
            if (status !in 200..299) {
                Log.e(TAG, "Road assistance fetch failed: HTTP $status")
                return@withContext emptyList()
            }

            val body = readBody(connection)
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
                val reviews = com.nyl.euiesro.features.roadassistance.RoadAssistanceApi.buildReviewList(reviewsArray)
                val averageRating = obj.optDouble("average_rating", if (reviews.isNotEmpty()) reviews.map { it.rating.toDouble() }.average().toFloat().toDouble() else 0.0).toFloat()
                val reviewsCount = obj.optInt("reviews_count", reviews.size).coerceAtLeast(reviews.size)
                result.add(
                    RoadAssistanceSummary(
                        id = obj.optInt("id", 0),
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
            val url = URL("${baseUrl.trimEnd('/')}/api/road-assistance/detail?id=$id")
            val connection = prepareRequest(url, "GET")
            val status = connection.responseCode
            if (status !in 200..299) {
                Log.e(TAG, "Road assistance detail fetch failed: HTTP $status")
                return@withContext null
            }

            val body = readBody(connection)
            val root = JSONObject(body)
            val obj = if (root.has("data") && root.opt("data") is JSONObject) root.getJSONObject("data") else root
            val servicesArray = obj.optJSONArray("services") ?: JSONArray()
            val services = mutableListOf<String>()
            for (j in 0 until servicesArray.length()) {
                val value = servicesArray.getString(j)
                if (value.isNotBlank()) services.add(value)
            }
            val reviewsArray = obj.optJSONArray("reviews") ?: JSONArray()
            val reviews = com.nyl.euiesro.features.roadassistance.RoadAssistanceApi.buildReviewList(reviewsArray)
            val averageRating = obj.optDouble("average_rating", if (reviews.isNotEmpty()) reviews.map { it.rating.toDouble() }.average().toFloat().toDouble() else 0.0).toFloat()
            val reviewsCount = obj.optInt("reviews_count", reviews.size).coerceAtLeast(reviews.size)
            RoadAssistanceSummary(
                id = obj.optInt("id", id),
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

    suspend fun fetchLaunchSpotDetail(id: Int): LaunchSpotSummary? = withContext(Dispatchers.IO) {
        try {
            val url = URL("${baseUrl.trimEnd('/')}/api/launch-spots/detail?id=$id")
            val connection = prepareRequest(url, "GET")
            val status = connection.responseCode
            if (status !in 200..299) {
                Log.e(TAG, "Launch spot detail fetch failed: HTTP $status")
                return@withContext null
            }

            val body = readBody(connection)
            val root = JSONObject(body)
            val obj = if (root.has("data") && root.opt("data") is JSONObject) root.getJSONObject("data") else root
            val imagesArray = obj.optJSONArray("images") ?: JSONArray()
            val images = mutableListOf<String>()
            for (j in 0 until imagesArray.length()) {
                val imageItem = imagesArray.getJSONObject(j)
                val imageUrl = imageItem.optString("image_url", "").ifBlank {
                    imageItem.optString("url", "")
                }
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
            val typeOptions = buildLaunchSpotTypeOptions(rawTypeIds, obj.optJSONArray("types"))
            val markerIconsArray = obj.optJSONArray("marker_icons") ?: JSONArray()
            val markerIcons = mutableListOf<String>()
            for (j in 0 until markerIconsArray.length()) {
                val markerIcon = markerIconsArray.getString(j)
                if (markerIcon.isNotBlank()) markerIcons.add(markerIcon)
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
                markerIcons = markerIcons
            )
        } catch (e: Exception) {
            Log.e(TAG, "fetchLaunchSpotDetail failed", e)
            null
        }
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
            val url = URL("${baseUrl.trimEnd('/')}/api/launch-spots?$parameters")
            val connection = prepareRequest(url, "GET")
            val status = connection.responseCode
            if (status !in 200..299) {
                Log.e(TAG, "Launch spots fetch failed: HTTP $status")
                return@withContext emptyList()
            }

            val body = readBody(connection)
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
                val typeOptions = buildLaunchSpotTypeOptions(rawTypeIds, obj.optJSONArray("types"))
                val markerIconsArray = obj.optJSONArray("marker_icons") ?: JSONArray()
                val markerIcons = mutableListOf<String>()
                for (j in 0 until markerIconsArray.length()) {
                    val markerIcon = markerIconsArray.getString(j)
                    if (markerIcon.isNotBlank()) markerIcons.add(markerIcon)
                }
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

    fun mapPreviewUrl(latitude: Double, longitude: Double, width: Int = 700, height: Int = 320): String {
        return "https://staticmap.openstreetmap.de/staticmap.php?center=${latitude},${longitude}&zoom=12&size=${width}x${height}&markers=${latitude},${longitude},red-pushpin"
    }

    fun prepareRequest(url: URL, method: String): HttpURLConnection {
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = method
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        connection.instanceFollowRedirects = false
        connection.setRequestProperty("Accept", "application/json, text/html, */*")
        if (sessionCookies.isNotEmpty()) {
            connection.setRequestProperty("Cookie", sessionCookies.entries.joinToString("; ") { (key, value) -> "$key=$value" })
        }

        if (method == "POST") {
            connection.doOutput = true
            connection.doInput = true
        }
        return connection
    }

    private fun storeSessionCookies(connection: HttpURLConnection) {
        val headers = connection.headerFields["Set-Cookie"] ?: emptyList()
        for (header in headers) {
            val cookie = header.substringBefore(';').trim()
            if (cookie.isBlank() || !cookie.contains('=')) continue
            val key = cookie.substringBefore('=').trim()
            val value = cookie.substringAfter('=', "").trim()
            if (key.isNotBlank()) {
                sessionCookies[key] = value
            }
        }
    }

    fun readBody(connection: HttpURLConnection): String {
        val stream = if (connection.responseCode in 200..299 || connection.responseCode in 300..399) connection.inputStream else connection.errorStream
        return try {
            stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
        } finally {
            stream?.close()
        }
    }

    private fun extractHtmlError(html: String): String? {
        val alertMatch = Regex("(?is)<div[^>]*class=\"[^\"]*alert[^\"]*error[^\"]*\"[^>]*>(.*?)</div>")
            .find(html)
        if (alertMatch != null) {
            val text = alertMatch.groupValues[1]
                .replace("<[^>]*>".toRegex(), " ")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("\\s+".toRegex(), " ")
                .trim()
            if (text.isNotEmpty()) {
                return text
            }
        }

        val text = Regex("(?is)Invalid username or password|login_error|alert error|Unauthorized|Forbidden").find(html)
        return text?.value
    }
}

@Composable
fun EuIesroApp(
    darkMode: Boolean = ThemePreferencesManager.isDarkModeEnabled(LocalContext.current),
    onDarkModeChanged: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val activity = context as? ComponentActivity
    var appLocale by rememberSaveable {
        mutableStateOf(AppLocaleManager.getCurrentLocale(context))
    }
    var isDarkMode by rememberSaveable {
        mutableStateOf(darkMode)
    }

    LaunchedEffect(darkMode) {
        isDarkMode = darkMode
    }

    LaunchedEffect(appLocale) {
        AppLocaleManager.setLocale(context, appLocale)
    }

    LaunchedEffect(isDarkMode) {
        ThemePreferencesManager.setDarkModeEnabled(context, isDarkMode)
        onDarkModeChanged(isDarkMode)
    }

    val restoredSession = remember(context) { AppSessionStateManager.readSession(context) }
    var screen by rememberSaveable { mutableStateOf(AppSessionStateManager.coldStartScreen(restoredSession)) }
    var currentUserName by rememberSaveable { mutableStateOf(restoredSession?.userName ?: "Guest") }
    var isLoggedIn by rememberSaveable { mutableStateOf(restoredSession?.isLoggedIn ?: false) }
    var currentUserProfile by remember { mutableStateOf(restoredSession?.currentUserProfile ?: CurrentUserProfile()) }
    var selectedSection by rememberSaveable { mutableStateOf(AppSessionStateManager.coldStartSelectedSection(restoredSession)) }
    var selectedLocation by remember { mutableStateOf(null as LocationUiItem?) }
    var selectedEvent by remember { mutableStateOf(null as EventSummary?) }

    if (restoredSession != null && !restoredSession.isLoggedIn && restoredSession.screen != AppScreen.SPLASH && restoredSession.screen != AppScreen.LOGIN) {
        LaunchedEffect(Unit) {
            screen = AppScreen.LOGIN
            currentUserName = "Guest"
            currentUserProfile = CurrentUserProfile()
            selectedSection = null
            selectedLocation = null
            selectedEvent = null
        }
    }

    LaunchedEffect(restoredSession?.screen, restoredSession?.isLoggedIn) {
        if (restoredSession == null || !restoredSession.isLoggedIn) {
            return@LaunchedEffect
        }

        val rememberMe = PreferencesManager.isRememberMeEnabled(context)
        val savedUser = PreferencesManager.getSavedUsername(context)
        val savedPass = PreferencesManager.getSavedPassword(context)

        if (!rememberMe || savedUser.isNullOrBlank() || savedPass.isNullOrBlank()) {
            isLoggedIn = false
            currentUserName = "Guest"
            currentUserProfile = CurrentUserProfile()
            AppSessionStateManager.clear(context)
            screen = AppScreen.LOGIN
            return@LaunchedEffect
        }

        val result = ApiClient.login(savedUser, savedPass)
        if (result.success) {
            currentUserName = result.userName
            isLoggedIn = true
            currentUserProfile = ApiClient.fetchCurrentUserProfile()
            if (screen == AppScreen.LOGIN || screen == AppScreen.SPLASH) {
                screen = if (selectedSection != null) AppScreen.SECTION else AppScreen.MAIN
            }
        } else {
            ApiClient.logout()
            isLoggedIn = false
            currentUserName = "Guest"
            currentUserProfile = CurrentUserProfile()
            AppSessionStateManager.clear(context)
            PreferencesManager.clearCredentials(context)
            screen = AppScreen.LOGIN
        }
    }

    var showExitDialog by rememberSaveable { mutableStateOf(false) }
    var pendingUpdate by remember { mutableStateOf<AppRelease?>(null) }
    var updateStatusText by remember { mutableStateOf<String?>(null) }
    var isDownloadingUpdate by remember { mutableStateOf(false) }
    var updateDownloadProgress by remember { mutableStateOf(0f) }
    var updateInstallStarted by remember { mutableStateOf(false) }
    val updateAlreadyDownloaded = pendingUpdate != null && AppUpdater.hasDownloadedUpdate(context, pendingUpdate!!)

    LaunchedEffect(screen, currentUserName, isLoggedIn, currentUserProfile, selectedSection, selectedLocation, selectedEvent) {
        AppSessionStateManager.saveSession(
            context = context,
            screen = screen,
            userName = currentUserName,
            isLoggedIn = isLoggedIn,
            currentUserProfile = currentUserProfile,
            selectedSection = selectedSection,
            selectedLocation = selectedLocation,
            selectedEvent = selectedEvent
        )
    }

    fun handleBackNavigation() {
        when (screen) {
            AppScreen.MAIN -> showExitDialog = true
            AppScreen.UPDATE -> {
                screen = AppScreen.MAIN
            }
            AppScreen.SECTION -> {
                screen = AppScreen.MAIN
                selectedSection = null
            }
            AppScreen.EDIT_PROFILE -> {
                screen = AppScreen.MAIN
            }
            AppScreen.ADD_LAUNCH_SPOT -> {
                screen = AppScreen.SECTION
            }
            AppScreen.ADD_EVENT -> {
                screen = AppScreen.SECTION
            }
            AppScreen.ADD_ROAD_ASSISTANCE -> {
                screen = AppScreen.SECTION
            }
            AppScreen.EDIT_LAUNCH_SPOT -> {
                screen = AppScreen.LOCATION_DETAIL
            }
            AppScreen.EDIT_ROAD_ASSISTANCE -> {
                screen = AppScreen.LOCATION_DETAIL
            }
            AppScreen.EDIT_EVENT -> {
                screen = AppScreen.EVENT_DETAIL
            }
            AppScreen.SUGGEST_EDIT_LAUNCH_SPOT -> {
                screen = AppScreen.LOCATION_DETAIL
            }
            AppScreen.SUGGEST_EDIT_ROAD_ASSISTANCE -> {
                screen = AppScreen.LOCATION_DETAIL
            }
            AppScreen.LOCATION_DETAIL -> {
                screen = AppScreen.SECTION
                selectedLocation = null
            }
            AppScreen.EVENT_DETAIL -> {
                screen = AppScreen.SECTION
                selectedEvent = null
            }
            else -> Unit
        }
    }

    BackHandler {
        handleBackNavigation()
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text(stringResource(R.string.close_app)) },
            text = { Text(stringResource(R.string.confirm_exit)) },
            confirmButton = {
                Button(
                    onClick = {
                        showExitDialog = false
                        activity?.finish()
                    }
                ) {
                    Text(stringResource(R.string.yes))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showExitDialog = false }) {
                    Text(stringResource(R.string.no))
                }
            }
        )
    }

    val checkUpdateNow: () -> Unit = {
        scope.launch {
            val release = AppUpdater.checkForUpdate(context)
            if (release != null) {
                pendingUpdate = release
                updateStatusText = null
                updateDownloadProgress = 0f
                updateInstallStarted = false
                screen = AppScreen.UPDATE
            } else {
                pendingUpdate = null
                updateStatusText = context.getString(R.string.no_update_available)
            }
        }
    }

    LaunchedEffect(Unit) {
        if (screen != AppScreen.SPLASH && restoredSession != null) {
            return@LaunchedEffect
        }

        if (screen == AppScreen.SPLASH) {
            pendingUpdate = AppUpdater.checkForUpdate(context)
            if (pendingUpdate != null) {
                screen = AppScreen.UPDATE
                return@LaunchedEffect
            }
            delay(1400)
        }

        val rememberMe = PreferencesManager.isRememberMeEnabled(context)
        val savedUser = PreferencesManager.getSavedUsername(context)
        val savedPass = PreferencesManager.getSavedPassword(context)

        if (rememberMe && savedUser != null && savedPass != null) {
            val result = ApiClient.login(savedUser, savedPass)
            if (result.success) {
                currentUserName = result.userName
                isLoggedIn = true
                currentUserProfile = ApiClient.fetchCurrentUserProfile()
                screen = if (pendingUpdate != null) AppScreen.UPDATE else AppScreen.MAIN
                return@LaunchedEffect
            }
            isLoggedIn = false
            currentUserProfile = CurrentUserProfile()
            screen = AppScreen.MAIN
            return@LaunchedEffect
        }

        isLoggedIn = false
        screen = AppScreen.MAIN
    }
    if (updateStatusText != null) {
        AlertDialog(
            onDismissRequest = { updateStatusText = null },
            title = { Text(stringResource(R.string.app_update_status)) },
            text = { Text(updateStatusText ?: "") },
            confirmButton = {
                Button(onClick = { updateStatusText = null }) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }

    when (screen) {
        AppScreen.SPLASH -> SplashScreen()
        AppScreen.UPDATE -> UpdateScreen(
            release = pendingUpdate,
            isDownloading = isDownloadingUpdate,
            downloadProgress = updateDownloadProgress,
            downloadedFileExists = updateAlreadyDownloaded,
            installStarted = updateInstallStarted,
            onBack = { screen = AppScreen.MAIN },
            onDownload = {
                val release = pendingUpdate ?: return@UpdateScreen
                isDownloadingUpdate = true
                updateInstallStarted = false
                updateDownloadProgress = 0f
                updateStatusText = null
                scope.launch {
                    val success = AppUpdater.downloadUpdate(
                        context,
                        release,
                        onProgress = { progress ->
                            updateDownloadProgress = progress
                        }
                    )
                    isDownloadingUpdate = false
                    if (success) {
                        updateStatusText = "Update downloaded successfully."
                    } else {
                        updateStatusText = "Download failed. Please try again."
                    }
                }
            },
            onInstall = {
                val release = pendingUpdate ?: return@UpdateScreen
                val fileExists = AppUpdater.hasDownloadedUpdate(context, release)
                if (!fileExists) {
                    updateStatusText = "The update file is missing. Please download it first."
                    return@UpdateScreen
                }
                scope.launch {
                    val started = AppUpdater.installDownloadedUpdate(
                        context,
                        release,
                        onInstallStarted = {
                            updateInstallStarted = true
                            updateStatusText = "Opening the installer..."
                        }
                    )
                    if (!started) {
                        updateStatusText = "Could not start installation. Try downloading again."
                    }
                }
            },
            onRedownload = {
                val release = pendingUpdate ?: return@UpdateScreen
                AppUpdater.deleteDownloadedUpdate(context, release)
                isDownloadingUpdate = true
                updateInstallStarted = false
                updateDownloadProgress = 0f
                updateStatusText = null
                scope.launch {
                    val success = AppUpdater.downloadUpdate(
                        context,
                        release,
                        onProgress = { progress ->
                            updateDownloadProgress = progress
                        }
                    )
                    isDownloadingUpdate = false
                    if (success) {
                        updateStatusText = "Update downloaded successfully."
                    } else {
                        updateStatusText = "Redownload failed. Please try again."
                    }
                }
            }
        )
        AppScreen.LOGIN -> com.nyl.euiesro.app.screens.LoginScreen(
            onLoginSuccess = { name ->
                currentUserName = name
                isLoggedIn = true
                scope.launch {
                    currentUserProfile = ApiClient.fetchCurrentUserProfile()
                    screen = AppScreen.MAIN
                }
            },
            onRegisterClick = {
                screen = AppScreen.REGISTER_WEBVIEW
            }
        )
        AppScreen.REGISTER_WEBVIEW -> RegisterWebViewScreen(
            onBack = { screen = AppScreen.LOGIN },
            onRegistrationComplete = {
                screen = AppScreen.LOGIN
            }
        )
        AppScreen.MAIN -> MainScreen(
            userName = currentUserName,
            currentUserProfile = currentUserProfile,
            appLocale = appLocale,
            isDarkMode = isDarkMode,
            onDarkModeChanged = { isDarkMode = it },
            onLocaleChanged = { appLocale = it },
            onCheckForUpdates = checkUpdateNow,
            onLogout = {
                scope.launch {
                    ApiClient.logout()
                    currentUserProfile = CurrentUserProfile()
                    currentUserName = "Guest"
                    isLoggedIn = false
                    AppSessionStateManager.clear(context)
                    PreferencesManager.clearCredentials(context)
                    screen = AppScreen.LOGIN
                }
            },
            onProfileUpdated = { updated -> currentUserProfile = updated },
            onOpenEditProfile = { screen = AppScreen.EDIT_PROFILE },
            onSectionSelected = {
                selectedSection = it
                selectedLocation = null
                screen = AppScreen.SECTION
            }
        )
        AppScreen.SECTION -> selectedSection?.let {
            ModuleSectionScreen(
                section = it,
                onBack = { screen = AppScreen.MAIN },
                onOpenLocation = { location ->
                    selectedLocation = location
                    screen = AppScreen.LOCATION_DETAIL
                },
                onOpenEvent = { event ->
                    selectedEvent = event
                    screen = AppScreen.EVENT_DETAIL
                },
                onAddLaunchSpot = {
                    screen = AppScreen.ADD_LAUNCH_SPOT
                },
                onAddMotoPlace = {
                    screen = AppScreen.ADD_MOTO_PLACE
                },
                onAddAutoPlace = {
                    screen = AppScreen.ADD_AUTO_PLACE
                },
                onAddRoadAssistance = {
                    screen = AppScreen.ADD_ROAD_ASSISTANCE
                },
                onAddEvent = {
                    screen = AppScreen.ADD_EVENT
                }
            )
        } ?: run {
            MainScreen(
                userName = currentUserName,
                currentUserProfile = currentUserProfile,
                appLocale = appLocale,
                isDarkMode = isDarkMode,
                onDarkModeChanged = { isDarkMode = it },
                onLocaleChanged = { appLocale = it },
                onCheckForUpdates = checkUpdateNow,
                onLogout = { screen = AppScreen.LOGIN },
                onProfileUpdated = { updated -> currentUserProfile = updated },
                onOpenEditProfile = { screen = AppScreen.EDIT_PROFILE },
                onSectionSelected = {}
            )
        }
        AppScreen.EDIT_PROFILE -> EditProfileScreen(
            userName = currentUserName,
            currentUserProfile = currentUserProfile,
            onBack = { screen = AppScreen.MAIN },
            onProfileSaved = { updated ->
                currentUserProfile = updated
                currentUserName = updated.username.ifBlank { currentUserName }
            }
        )
        AppScreen.ADD_LAUNCH_SPOT -> AddLaunchSpotsScreen(
            onBack = { screen = AppScreen.SECTION },
            onLocationAdded = { selectedSection = ModuleSection.LAUNCH_SPOTS }
        )
        AppScreen.ADD_EVENT -> CreateEventScreen(
            onBack = { screen = AppScreen.SECTION },
            onEventCreated = {
                screen = AppScreen.SECTION
                selectedSection = ModuleSection.EVENTS
            }
        )
        AppScreen.EDIT_EVENT -> selectedEvent?.let {
            EditEventScreen(
                event = it,
                onBack = { screen = AppScreen.EVENT_DETAIL },
                onEventUpdated = { updated ->
                    selectedEvent = updated
                    screen = AppScreen.EVENT_DETAIL
                }
            )
        } ?: run {
            MainScreen(
                userName = currentUserName,
                currentUserProfile = currentUserProfile,
                appLocale = appLocale,
                isDarkMode = isDarkMode,
                onDarkModeChanged = { isDarkMode = it },
                onLocaleChanged = { appLocale = it },
                onCheckForUpdates = checkUpdateNow,
                onLogout = { screen = AppScreen.LOGIN },
                onProfileUpdated = { updated -> currentUserProfile = updated },
                onSectionSelected = {
                    selectedSection = it
                    screen = AppScreen.SECTION
                }
            )
        }
        AppScreen.ADD_MOTO_PLACE -> AddMotoPlaceScreen(
            onBack = { screen = AppScreen.SECTION },
            onLocationAdded = { selectedSection = ModuleSection.MOTO }
        )
        AppScreen.ADD_AUTO_PLACE -> AddAutoPlaceScreen(
            onBack = { screen = AppScreen.SECTION },
            onLocationAdded = { selectedSection = ModuleSection.AUTO }
        )
        AppScreen.ADD_ROAD_ASSISTANCE -> AddRoadAssistanceScreen(
            onBack = { screen = AppScreen.SECTION },
            onLocationAdded = { selectedSection = ModuleSection.ROAD_ASSISTANCE }
        )
        AppScreen.EDIT_LAUNCH_SPOT -> selectedLocation?.let {
            EditLaunchSpotsScreen(
                location = it,
                onBack = { screen = AppScreen.LOCATION_DETAIL },
                onLocationUpdated = { updated ->
                    selectedLocation = updated
                    screen = AppScreen.LOCATION_DETAIL
                }
            )
        } ?: run {
            MainScreen(
                userName = currentUserName,
                currentUserProfile = currentUserProfile,
                appLocale = appLocale,
                isDarkMode = isDarkMode,
                onDarkModeChanged = { isDarkMode = it },
                onLocaleChanged = { appLocale = it },
                onCheckForUpdates = checkUpdateNow,
                onLogout = { screen = AppScreen.LOGIN },
                onProfileUpdated = { updated -> currentUserProfile = updated },
                onSectionSelected = {
                    selectedSection = it
                    screen = AppScreen.SECTION
                }
            )
        }
        AppScreen.SUGGEST_EDIT_LAUNCH_SPOT -> selectedLocation?.let {
            SuggestEditScreen(
                location = it,
                onBack = { screen = AppScreen.LOCATION_DETAIL },
                onSuggestionSubmitted = {
                    screen = AppScreen.LOCATION_DETAIL
                }
            )
        } ?: run {
            MainScreen(
                userName = currentUserName,
                currentUserProfile = currentUserProfile,
                appLocale = appLocale,
                isDarkMode = isDarkMode,
                onDarkModeChanged = { isDarkMode = it },
                onLocaleChanged = { appLocale = it },
                onCheckForUpdates = checkUpdateNow,
                onLogout = { screen = AppScreen.LOGIN },
                onProfileUpdated = { updated -> currentUserProfile = updated },
                onSectionSelected = {
                    selectedSection = it
                    screen = AppScreen.SECTION
                }
            )
        }
        AppScreen.EDIT_MOTO_PLACE -> selectedLocation?.let {
            EditMotoPlaceScreen(
                location = it,
                onBack = { screen = AppScreen.LOCATION_DETAIL },
                onLocationUpdated = { updated ->
                    selectedLocation = updated
                    screen = AppScreen.LOCATION_DETAIL
                }
            )
        } ?: run {
            MainScreen(
                userName = currentUserName,
                currentUserProfile = currentUserProfile,
                appLocale = appLocale,
                isDarkMode = isDarkMode,
                onDarkModeChanged = { isDarkMode = it },
                onLocaleChanged = { appLocale = it },
                onCheckForUpdates = checkUpdateNow,
                onLogout = { screen = AppScreen.LOGIN },
                onProfileUpdated = { updated -> currentUserProfile = updated },
                onSectionSelected = {
                    selectedSection = it
                    screen = AppScreen.SECTION
                }
            )
        }
        AppScreen.SUGGEST_EDIT_MOTO_PLACE -> selectedLocation?.let {
            SuggestEditMotoPlaceScreen(
                location = it,
                onBack = { screen = AppScreen.LOCATION_DETAIL },
                onSuggestionSubmitted = {
                    screen = AppScreen.LOCATION_DETAIL
                }
            )
        } ?: run {
            MainScreen(
                userName = currentUserName,
                currentUserProfile = currentUserProfile,
                appLocale = appLocale,
                isDarkMode = isDarkMode,
                onDarkModeChanged = { isDarkMode = it },
                onLocaleChanged = { appLocale = it },
                onCheckForUpdates = checkUpdateNow,
                onLogout = { screen = AppScreen.LOGIN },
                onProfileUpdated = { updated -> currentUserProfile = updated },
                onSectionSelected = {
                    selectedSection = it
                    screen = AppScreen.SECTION
                }
            )
        }
        AppScreen.EDIT_AUTO_PLACE -> selectedLocation?.let {
            EditAutoPlaceScreen(
                location = it,
                onBack = { screen = AppScreen.LOCATION_DETAIL },
                onLocationUpdated = { updated ->
                    selectedLocation = updated
                    screen = AppScreen.LOCATION_DETAIL
                }
            )
        } ?: run {
            MainScreen(
                userName = currentUserName,
                currentUserProfile = currentUserProfile,
                appLocale = appLocale,
                isDarkMode = isDarkMode,
                onDarkModeChanged = { isDarkMode = it },
                onLocaleChanged = { appLocale = it },
                onCheckForUpdates = checkUpdateNow,
                onLogout = { screen = AppScreen.LOGIN },
                onProfileUpdated = { updated -> currentUserProfile = updated },
                onSectionSelected = {
                    selectedSection = it
                    screen = AppScreen.SECTION
                }
            )
        }
        AppScreen.SUGGEST_EDIT_AUTO_PLACE -> selectedLocation?.let {
            SuggestEditAutoPlaceScreen(
                location = it,
                onBack = { screen = AppScreen.LOCATION_DETAIL },
                onSuggestionSubmitted = {
                    screen = AppScreen.LOCATION_DETAIL
                }
            )
        } ?: run {
            MainScreen(
                userName = currentUserName,
                currentUserProfile = currentUserProfile,
                appLocale = appLocale,
                isDarkMode = isDarkMode,
                onDarkModeChanged = { isDarkMode = it },
                onLocaleChanged = { appLocale = it },
                onCheckForUpdates = checkUpdateNow,
                onLogout = { screen = AppScreen.LOGIN },
                onProfileUpdated = { updated -> currentUserProfile = updated },
                onSectionSelected = {
                    selectedSection = it
                    screen = AppScreen.SECTION
                }
            )
        }
        AppScreen.EDIT_ROAD_ASSISTANCE -> selectedLocation?.let {
            EditRoadAssistanceScreen(
                location = it,
                onBack = { screen = AppScreen.LOCATION_DETAIL },
                onLocationUpdated = { updated ->
                    selectedLocation = updated
                    screen = AppScreen.LOCATION_DETAIL
                }
            )
        } ?: run {
            MainScreen(
                userName = currentUserName,
                currentUserProfile = currentUserProfile,
                appLocale = appLocale,
                isDarkMode = isDarkMode,
                onDarkModeChanged = { isDarkMode = it },
                onLocaleChanged = { appLocale = it },
                onCheckForUpdates = checkUpdateNow,
                onLogout = { screen = AppScreen.LOGIN },
                onProfileUpdated = { updated -> currentUserProfile = updated },
                onSectionSelected = {
                    selectedSection = it
                    screen = AppScreen.SECTION
                }
            )
        }
        AppScreen.SUGGEST_EDIT_ROAD_ASSISTANCE -> selectedLocation?.let {
            SuggestEditRoadAssistanceScreen(
                location = it,
                onBack = { screen = AppScreen.LOCATION_DETAIL },
                onSuggestionSubmitted = {
                    screen = AppScreen.LOCATION_DETAIL
                }
            )
        } ?: run {
            MainScreen(
                userName = currentUserName,
                currentUserProfile = currentUserProfile,
                appLocale = appLocale,
                isDarkMode = isDarkMode,
                onDarkModeChanged = { isDarkMode = it },
                onLocaleChanged = { appLocale = it },
                onCheckForUpdates = checkUpdateNow,
                onLogout = { screen = AppScreen.LOGIN },
                onProfileUpdated = { updated -> currentUserProfile = updated },
                onSectionSelected = {
                    selectedSection = it
                    screen = AppScreen.SECTION
                }
            )
        }
        AppScreen.LOCATION_DETAIL -> selectedLocation?.let {
            when (it.section) {
                ModuleSection.MOTO -> MotoDetailScreen(
                    location = it,
                    onBack = { screen = AppScreen.SECTION },
                    currentUserName = currentUserName,
                    currentUserProfile = currentUserProfile,
                    onEditMotoPlace = {
                        screen = AppScreen.EDIT_MOTO_PLACE
                    },
                    onSuggestMotoEdit = {
                        screen = AppScreen.SUGGEST_EDIT_MOTO_PLACE
                    }
                )
                ModuleSection.ROAD_ASSISTANCE -> RoadAssistanceDetailScreen(
                    location = it,
                    onBack = { screen = AppScreen.SECTION },
                    currentUserName = currentUserName,
                    currentUserProfile = currentUserProfile,
                    onEditRoadAssistance = {
                        screen = AppScreen.EDIT_ROAD_ASSISTANCE
                    },
                    onSuggestRoadAssistanceEdit = {
                        screen = AppScreen.SUGGEST_EDIT_ROAD_ASSISTANCE
                    }
                )
                ModuleSection.LAUNCH_SPOTS -> LaunchSpotDetailScreen(
                    location = it,
                    onBack = { screen = AppScreen.SECTION },
                    currentUserName = currentUserName,
                    currentUserProfile = currentUserProfile,
                    onEditLaunchSpot = {
                        screen = AppScreen.EDIT_LAUNCH_SPOT
                    },
                    onSuggestLaunchSpotEdit = {
                        screen = AppScreen.SUGGEST_EDIT_LAUNCH_SPOT
                    }
                )
                ModuleSection.AUTO -> AutoDetailScreen(
                    location = it,
                    onBack = { screen = AppScreen.SECTION },
                    currentUserName = currentUserName,
                    onEditAutoPlace = {
                        screen = AppScreen.EDIT_AUTO_PLACE
                    },
                    onSuggestAutoEdit = {
                        screen = AppScreen.SUGGEST_EDIT_AUTO_PLACE
                    }
                )
                else -> LocationDetailScreen(
                    location = it,
                    onBack = { screen = AppScreen.SECTION }
                )
            }
        } ?: run {
            MainScreen(
                userName = currentUserName,
                currentUserProfile = currentUserProfile,
                appLocale = appLocale,
                isDarkMode = isDarkMode,
                onDarkModeChanged = { isDarkMode = it },
                onLocaleChanged = { appLocale = it },
                onCheckForUpdates = checkUpdateNow,
                onLogout = { screen = AppScreen.LOGIN },
                onProfileUpdated = { updated -> currentUserProfile = updated },
                onSectionSelected = {
                    selectedSection = it
                    screen = AppScreen.SECTION
                }
            )
        }
        AppScreen.EVENT_DETAIL -> selectedEvent?.let {
            EventDetailScreen(
                event = it,
                currentUserName = currentUserName,
                onBack = {
                    screen = AppScreen.SECTION
                },
                onEditEvent = {
                    screen = AppScreen.EDIT_EVENT
                },
                onEventUpdated = { updated ->
                    selectedEvent = updated
                }
            )
        } ?: run {
            MainScreen(
                userName = currentUserName,
                currentUserProfile = currentUserProfile,
                appLocale = appLocale,
                isDarkMode = isDarkMode,
                onDarkModeChanged = { isDarkMode = it },
                onLocaleChanged = { appLocale = it },
                onCheckForUpdates = checkUpdateNow,
                onLogout = { screen = AppScreen.LOGIN },
                onProfileUpdated = { updated -> currentUserProfile = updated },
                onSectionSelected = {
                    selectedSection = it
                    screen = AppScreen.SECTION
                }
            )
        }
    }
}

@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.euiesro_logo),
                contentDescription = "EuIesro logo",
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(175.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))
            CircularProgressIndicator()
        }
    }
}

@Composable
fun RegisterWebViewScreen(
    onBack: () -> Unit,
    onRegistrationComplete: () -> Unit
) {
    val context = LocalContext.current
    val webView = remember(context) {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.setSupportZoom(true)
            settings.builtInZoomControls = true
            settings.displayZoomControls = false
            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    if (!url.isNullOrBlank() && (url.contains("index.php?p=profile", ignoreCase = true) || url.contains("/profile", ignoreCase = true))) {
                        onRegistrationComplete()
                    }
                }
            }
        }
    }

    BackHandler {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            onBack()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Create account",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        AndroidView(
            factory = { webView },
            modifier = Modifier.fillMaxSize()
        )
    }

    LaunchedEffect(Unit) {
        webView.loadUrl("https://euies.ro/index.php?p=register&nohf=1")
    }
}

@Composable
fun EuIesroTopAppBar(
    title: String,
    onBack: (() -> Unit)? = null,
    onLogout: (() -> Unit)? = null,
    onUserProfile: (() -> Unit)? = null,
    onAppSettings: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null,
    profileImageUrl: String? = null,
    showAppIconInsteadOfTitle: Boolean = false
) {
    com.nyl.euiesro.app.screens.EuIesroTopAppBar(
        title = title,
        onBack = onBack,
        onLogout = onLogout,
        onUserProfile = onUserProfile,
        onAppSettings = onAppSettings,
        onShare = onShare,
        profileImageUrl = profileImageUrl,
        showAppIconInsteadOfTitle = showAppIconInsteadOfTitle
    )
}

@Composable
fun MainScreen(
    userName: String,
    currentUserProfile: CurrentUserProfile = CurrentUserProfile(),
    appLocale: com.nyl.euiesro.app.AppLocale,
    isDarkMode: Boolean,
    onDarkModeChanged: (Boolean) -> Unit,
    onLocaleChanged: (com.nyl.euiesro.app.AppLocale) -> Unit,
    onCheckForUpdates: () -> Unit,
    onLogout: () -> Unit,
    onProfileUpdated: (CurrentUserProfile) -> Unit = {},
    onOpenEditProfile: () -> Unit = {},
    onSectionSelected: (ModuleSection) -> Unit
) {
    com.nyl.euiesro.app.screens.MainScreen(
        userName = userName,
        currentUserProfile = currentUserProfile,
        appLocale = appLocale,
        isDarkMode = isDarkMode,
        onDarkModeChanged = onDarkModeChanged,
        onLocaleChanged = onLocaleChanged,
        onCheckForUpdates = onCheckForUpdates,
        onLogout = onLogout,
        onProfileUpdated = onProfileUpdated,
        onOpenEditProfile = onOpenEditProfile,
        onSectionSelected = onSectionSelected
    )
}

@Composable
fun ModuleSectionScreen(
    section: ModuleSection,
    onBack: () -> Unit,
    onOpenLocation: (LocationUiItem) -> Unit,
    onOpenEvent: (EventSummary) -> Unit = {},
    onAddLaunchSpot: () -> Unit = {},
    onAddMotoPlace: () -> Unit = {},
    onAddAutoPlace: () -> Unit = {},
    onAddRoadAssistance: () -> Unit = {},
    onAddEvent: () -> Unit = {}
) {
    val title = when (section) {
        ModuleSection.EVENTS -> stringResource(R.string.module_events_title)
        ModuleSection.MOTO -> stringResource(R.string.module_moto_title)
        ModuleSection.ROAD_ASSISTANCE -> stringResource(R.string.module_road_assistance_title)
        ModuleSection.LAUNCH_SPOTS -> stringResource(R.string.module_launch_spots_title)
        ModuleSection.AUTO -> stringResource(R.string.module_auto_title)
    }

    Scaffold(
        topBar = {
            EuIesroTopAppBar(title = title, onBack = onBack)
        }
    ) { innerPadding ->
        when (section) {
            ModuleSection.EVENTS -> EventsSection(innerPadding, onOpenEvent, onAddEvent)
            ModuleSection.MOTO -> com.nyl.euiesro.features.moto.MotoSection(innerPadding, onOpenLocation, onAddMotoPlace)
            ModuleSection.ROAD_ASSISTANCE -> RoadAssistanceSection(innerPadding, onOpenLocation, onAddRoadAssistance)
            ModuleSection.LAUNCH_SPOTS -> LaunchSpotsSection(innerPadding, onOpenLocation, onAddLaunchSpot)
            ModuleSection.AUTO -> AutoSection(innerPadding, onOpenLocation, onAddAutoPlace)
        }
    }
}

@Composable
fun RoadAssistanceSection(
    innerPadding: PaddingValues,
    onOpenLocation: (LocationUiItem) -> Unit,
    onAddRoadAssistance: () -> Unit = {}
) {
    val context = LocalContext.current
    var places by remember { mutableStateOf<List<RoadAssistanceSummary>>(emptyList()) }
    var loading by rememberSaveable { mutableStateOf(true) }
    var error by rememberSaveable { mutableStateOf("") }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedServices by rememberSaveable { mutableStateOf<Set<String>>(emptySet()) }
    var onlyAvailableInMyArea by rememberSaveable { mutableStateOf(false) }
    var filterUserLatitude by rememberSaveable { mutableStateOf<Double?>(null) }
    var filterUserLongitude by rememberSaveable { mutableStateOf<Double?>(null) }
    var viewerLatitude by rememberSaveable { mutableStateOf<Double?>(null) }
    var viewerLongitude by rememberSaveable { mutableStateOf<Double?>(null) }
    var showFilters by rememberSaveable { mutableStateOf(false) }
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var hasLocationPermission by rememberSaveable {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    LaunchedEffect(onlyAvailableInMyArea, hasLocationPermission) {
        if (!onlyAvailableInMyArea) {
            filterUserLatitude = null
            filterUserLongitude = null
            return@LaunchedEffect
        }
        if (!hasLocationPermission) {
            return@LaunchedEffect
        }

        val currentLocation = resolveBestCurrentLocationSuspend(context, maxAccuracyMeters = 1000f)
            ?: resolveBestCurrentLocationForDisplay(context, maxAccuracyMeters = 1000f)

        filterUserLatitude = currentLocation?.latitude
        filterUserLongitude = currentLocation?.longitude
    }

    LaunchedEffect(hasLocationPermission) {
        if (!hasLocationPermission) {
            viewerLatitude = null
            viewerLongitude = null
            return@LaunchedEffect
        }

        val currentLocation = resolveBestCurrentLocationSuspend(context, maxAccuracyMeters = 1000f)
            ?: resolveBestCurrentLocationForDisplay(context, maxAccuracyMeters = 1000f)

        viewerLatitude = currentLocation?.latitude
        viewerLongitude = currentLocation?.longitude
    }

    val selectedServiceFilterQuery = selectedServices.joinToString(",")
    val serviceFilterGroups = listOf(
        GroupedTypeCategory(
            category = stringResource(R.string.road_assistance_service_mobile),
            options = listOf(
                GroupedTypeOption(value = "service_service_mobile_moto", label = stringResource(R.string.road_assistance_vehicle_moto)),
                GroupedTypeOption(value = "service_service_mobile_auto", label = stringResource(R.string.road_assistance_vehicle_auto)),
                GroupedTypeOption(value = "service_service_mobile_atv", label = stringResource(R.string.road_assistance_vehicle_atv))
            )
        ),
        GroupedTypeCategory(
            category = stringResource(R.string.road_assistance_vulcanizare_mobile),
            options = listOf(
                GroupedTypeOption(value = "service_vulcanizare_mobile_moto", label = stringResource(R.string.road_assistance_vehicle_moto)),
                GroupedTypeOption(value = "service_vulcanizare_mobile_auto", label = stringResource(R.string.road_assistance_vehicle_auto)),
                GroupedTypeOption(value = "service_vulcanizare_mobile_atv", label = stringResource(R.string.road_assistance_vehicle_atv))
            )
        ),
        GroupedTypeCategory(
            category = stringResource(R.string.road_assistance_transport_vehicule),
            options = listOf(
                GroupedTypeOption(value = "service_transport_vehicule_moto", label = stringResource(R.string.road_assistance_vehicle_moto)),
                GroupedTypeOption(value = "service_transport_vehicule_auto", label = stringResource(R.string.road_assistance_vehicle_auto)),
                GroupedTypeOption(value = "service_transport_vehicule_atv", label = stringResource(R.string.road_assistance_vehicle_atv))
            )
        )
    )

    LaunchedEffect(searchQuery, selectedServices, onlyAvailableInMyArea, hasLocationPermission, filterUserLatitude, filterUserLongitude) {
        loading = true
        error = ""

        if (onlyAvailableInMyArea && !hasLocationPermission) {
            loading = false
            places = emptyList()
            error = "Activează permisiunea de locație pentru filtrul din zona mea."
            return@LaunchedEffect
        }

        if (onlyAvailableInMyArea && (filterUserLatitude == null || filterUserLongitude == null)) {
            loading = false
            places = emptyList()
            error = "Nu am putut determina locația ta curentă."
            return@LaunchedEffect
        }

        places = ApiClient.fetchRoadAssistancePlaces(
            limit = 500,
            query = searchQuery,
            service = selectedServiceFilterQuery.ifEmpty { null },
            availableInAreaOnly = onlyAvailableInMyArea,
            userLatitude = filterUserLatitude,
            userLongitude = filterUserLongitude
        )
        loading = false
        if (places.isEmpty()) {
            error = if (onlyAvailableInMyArea) {
                "Nu există prestatori disponibili în aria ta de acoperire."
            } else {
                "No road assistance providers available right now."
            }
        }
    }

    val areaFilteredPlaces = if (onlyAvailableInMyArea && filterUserLatitude != null && filterUserLongitude != null) {
        places.filter { place ->
            val points = RoadAssistanceBoundaryMapUtils.parseBoundary(place.coverageBoundary)
            points.size >= 3 && isPointInsidePolygon(
                lat = filterUserLatitude ?: return@filter false,
                lng = filterUserLongitude ?: return@filter false,
                polygon = points
            )
        }
    } else {
        places
    }

    val userLocationForSort = remember(context, viewerLatitude, viewerLongitude) {
        val resolved = resolveBestCurrentLocationForDisplay(context, maxAccuracyMeters = 1000f)
        if (resolved != null) {
            resolved
        } else if (viewerLatitude != null && viewerLongitude != null) {
            android.location.Location("viewer").apply {
                latitude = viewerLatitude!!
                longitude = viewerLongitude!!
            }
        } else {
            null
        }
    }

    val items = sortLocationsByDistance(
        areaFilteredPlaces.map { place ->
            LocationUiItem(
                id = place.id,
                section = ModuleSection.ROAD_ASSISTANCE,
                title = place.title,
                description = place.description,
                address = place.address,
                latitude = place.latitude,
                longitude = place.longitude,
                phone = place.phone,
                website = place.website,
                email = place.email,
                services = place.services,
                imageUrls = emptyList(),
                typeIds = emptyList(),
                markerIcons = SiteMarkerIconResolver.resolveRoadAssistanceMarkerIcons(place.services),
                averageRating = place.averageRating,
                reviewsCount = place.reviewsCount
            )
        },
        userLocationForSort
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
    ) {
        if (loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
        } else if (error.isNotBlank()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(error, color = MaterialTheme.colorScheme.error)
            }
        } else {
            MapBottomSheetLayout(
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                searchPlaceholder = stringResource(R.string.search_provider),
                onFilterClick = if (serviceFilterGroups.isNotEmpty()) { { showFilters = !showFilters } } else null,
                onCloseFilter = { showFilters = false },
                useFilterModal = true,
                filterContent = if (showFilters) {
                    {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val nextValue = !onlyAvailableInMyArea
                                    onlyAvailableInMyArea = nextValue
                                    if (nextValue && !hasLocationPermission) {
                                        locationPermissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.ACCESS_FINE_LOCATION,
                                                Manifest.permission.ACCESS_COARSE_LOCATION
                                            )
                                        )
                                    }
                                }
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Arată doar disponibile în zona mea",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Switch(
                                checked = onlyAvailableInMyArea,
                                onCheckedChange = { checked ->
                                    onlyAvailableInMyArea = checked
                                    if (checked && !hasLocationPermission) {
                                        locationPermissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.ACCESS_FINE_LOCATION,
                                                Manifest.permission.ACCESS_COARSE_LOCATION
                                            )
                                        )
                                    }
                                }
                            )
                        }

                        GroupedTypeSelector(
                            title = "",
                            groups = serviceFilterGroups,
                            selectedValues = selectedServices,
                            onToggle = { value ->
                                selectedServices = if (selectedServices.contains(value)) {
                                    selectedServices - value
                                } else {
                                    selectedServices + value
                                }
                            }
                        )
                    }
                } else null,
                items = items,
                onMapReady = { mapView = it },
                onCenterLocation = { sheetRatio ->
                    if (!hasLocationPermission) {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                        return@MapBottomSheetLayout
                    }
                    centerMapOnUserLocation(mapView, sheetRatio)
                },
                onZoomIn = { mapView?.controller?.zoomIn() },
                onZoomOut = { mapView?.controller?.zoomOut() },
                showUserLocation = hasLocationPermission,
                sheetHeader = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${items.size} de locații găsite",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                sheetContent = {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(items) { place ->
                            val distanceMeters = calculateDistanceMeters(
                                fromLat = viewerLatitude,
                                fromLng = viewerLongitude,
                                toLat = place.latitude,
                                toLng = place.longitude
                            )
                            RoadAssistanceLocationCard(
                                place = place,
                                distanceMeters = distanceMeters,
                                onClick = { onOpenLocation(place) }
                            )
                        }
                    }
                },
                onAddAction = onAddRoadAssistance,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

private fun isPointInsidePolygon(lat: Double, lng: Double, polygon: List<Pair<Double, Double>>): Boolean {
    if (polygon.size < 3) return false

    var inside = false
    var j = polygon.lastIndex
    for (i in polygon.indices) {
        val xi = polygon[i].second
        val yi = polygon[i].first
        val xj = polygon[j].second
        val yj = polygon[j].first

        val intersects = ((yi > lat) != (yj > lat)) &&
            (lng < ((xj - xi) * (lat - yi) / ((yj - yi) + 1e-12) + xi))

        if (intersects) {
            inside = !inside
        }
        j = i
    }

    return inside
}

@Composable
fun FilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
        ),
        shape = RoundedCornerShape(999.dp)
    ) {
        Text(label)
    }
}

@Composable
private fun RoadAssistanceLocationCard(
    place: LocationUiItem,
    distanceMeters: Float?,
    onClick: () -> Unit
) {
    val distanceLabel = formatDistanceLabel(distanceMeters)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = place.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                        fontSize = 14.sp
                    )
                    if (place.averageRating > 0f) {
                        Text(
                            text = "★ ${"%.1f".format(place.averageRating)}",
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                }

                if (!place.address.isNullOrBlank()) {
                    Text(
                        text = place.address,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                if (place.reviewsCount > 0) {
                    Text(
                        text = "${place.reviewsCount} recenzii",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                distanceLabel?.let { label ->
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        tonalElevation = 0.dp,
                        modifier = Modifier.padding(top = 6.dp)
                    ) {
                        Text(
                            text = label,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                if (place.services.isNotEmpty()) {
                    Row(
                        modifier = Modifier.padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        place.services.take(3).forEach { chip ->
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                tonalElevation = 0.dp,
                                modifier = Modifier.defaultMinSize(minHeight = 20.dp)
                            ) {
                                Text(
                                    text = chip,
                                    modifier = Modifier
                                        .widthIn(max = 100.dp)
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                tonalElevation = 0.dp,
                modifier = Modifier.size(40.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun LocationDetailScreen(location: LocationUiItem, onBack: () -> Unit) {
    val context = LocalContext.current
    val fetchedImages = remember { mutableStateListOf<String>() }
    var detailLocation by remember(location.id, location.section) { mutableStateOf(location) }
    var motoDetail by remember(location.id) { mutableStateOf<MotoPlaceSummary?>(null) }
    var roadDetail by remember(location.id) { mutableStateOf<RoadAssistanceSummary?>(null) }
    var launchSpotDetail by remember(location.id) { mutableStateOf<LaunchSpotSummary?>(null) }

    LaunchedEffect(location.id, location.section) {
        fetchedImages.clear()

        when (location.section) {
            ModuleSection.MOTO -> {
                val detail = ApiClient.fetchMotoPlaceDetail(location.id)
                motoDetail = detail
                detailLocation = if (detail != null) {
                    location.copy(
                        title = detail.title,
                        description = detail.description ?: location.description,
                        address = detail.address ?: location.address,
                        latitude = detail.latitude,
                        longitude = detail.longitude,
                        phone = detail.phone ?: location.phone,
                        website = detail.website ?: location.website,
                        email = detail.email ?: location.email,
                        services = detail.services.ifEmpty { location.services },
                        imageUrls = detail.images.ifEmpty { location.imageUrls }
                    )
                } else {
                    location
                }
                fetchedImages.addAll(detail?.images ?: location.imageUrls)
            }

            ModuleSection.ROAD_ASSISTANCE -> {
                val detail = ApiClient.fetchRoadAssistancePlaceDetail(location.id)
                roadDetail = detail
                detailLocation = if (detail != null) {
                    location.copy(
                        title = detail.title,
                        description = detail.description ?: location.description,
                        address = detail.address ?: location.address,
                        latitude = detail.latitude,
                        longitude = detail.longitude,
                        phone = detail.phone ?: location.phone,
                        website = detail.website ?: location.website,
                        email = detail.email ?: location.email,
                        services = detail.services.ifEmpty { location.services },
                        imageUrls = location.imageUrls
                    )
                } else {
                    location
                }
                fetchedImages.addAll(location.imageUrls)
            }

            ModuleSection.LAUNCH_SPOTS -> {
                val detail = ApiClient.fetchLaunchSpotDetail(location.id)
                launchSpotDetail = detail
                detailLocation = if (detail != null) {
                    location.copy(
                        title = detail.title,
                        description = detail.description ?: location.description,
                        address = detail.address ?: location.address,
                        latitude = detail.latitude,
                        longitude = detail.longitude,
                        phone = null,
                        website = null,
                        email = null,
                        services = emptyList(),
                        imageUrls = detail.images.ifEmpty { location.imageUrls },
                        typeIds = detail.typeIds,
                        markerIcons = detail.markerIcons
                    )
                } else {
                    location
                }
                fetchedImages.addAll(detail?.images ?: location.imageUrls)
            }

            else -> {
                detailLocation = location
                fetchedImages.addAll(location.imageUrls)
            }
        }
    }

    val directionsUri = Uri.parse("geo:${detailLocation.latitude},${detailLocation.longitude}?q=${detailLocation.latitude},${detailLocation.longitude}(${Uri.encode(detailLocation.title)})")
    val workingHours = if (location.section == ModuleSection.MOTO) motoDetail?.workingHours ?: emptyMap() else emptyMap()

    Scaffold(
        topBar = {
            EuIesroTopAppBar(title = detailLocation.title, onBack = onBack)
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (fetchedImages.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            AsyncImage(
                                model = fetchedImages.first(),
                                contentDescription = detailLocation.title,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(240.dp)
                                    .clip(RoundedCornerShape(18.dp)),
                                contentScale = ContentScale.Crop
                            )

                            if (fetchedImages.size > 1) {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 10.dp)
                                ) {
                                    items(fetchedImages.drop(1)) { imageUrl ->
                                        AsyncImage(
                                            model = imageUrl,
                                            contentDescription = detailLocation.title,
                                            modifier = Modifier
                                                .width(110.dp)
                                                .height(80.dp)
                                                .clip(RoundedCornerShape(12.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .padding(8.dp)
                    ) {
                        InteractiveLocationMap(
                            items = listOf(detailLocation),
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                        Text(
                            text = detailLocation.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )

                        detailLocation.address?.let {
                            Text(
                                text = it,
                                modifier = Modifier.padding(top = 6.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (location.section == ModuleSection.MOTO) {
                            Text(
                                text = "Descriere & Activitate:",
                                modifier = Modifier.padding(top = 12.dp),
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        detailLocation.description?.let {
                            Text(
                                text = it,
                                modifier = Modifier.padding(top = 4.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                lineHeight = 22.sp
                            )
                        }

                        if (!detailLocation.services.isNullOrEmpty()) {
                            Text(
                                text = "Services:",
                                modifier = Modifier.padding(top = 10.dp),
                                fontWeight = FontWeight.SemiBold
                            )
                            Row(
                                modifier = Modifier.padding(top = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                detailLocation.services.forEach { service ->
                                    Surface(
                                        shape = RoundedCornerShape(999.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        tonalElevation = 0.dp
                                    ) {
                                        Text(
                                            text = service,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                }
                            }
                        }

                        if (detailLocation.typeIds.isNotEmpty()) {
                            Text(
                                text = "Types: ${detailLocation.typeIds.joinToString(", ")}",
                                modifier = Modifier.padding(top = 8.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            if (workingHours.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Program de lucru",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            workingHours.entries.forEach { (day, value) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = day.replaceFirstChar { it.titlecase() }, modifier = Modifier.weight(1f))
                                    Surface(
                                        shape = RoundedCornerShape(999.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = value,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!detailLocation.phone.isNullOrBlank()) {
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${detailLocation.phone}"))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Call")
                        }
                    }
                    if (!detailLocation.website.isNullOrBlank()) {
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(detailLocation.website))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Website")
                        }
                    }
                    if (location.section != ModuleSection.ROAD_ASSISTANCE) {
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, directionsUri)
                                context.startActivity(intent)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Directions")
                        }
                    } else {
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:${detailLocation.latitude},${detailLocation.longitude}?q=${detailLocation.latitude},${detailLocation.longitude}(${Uri.encode(detailLocation.title)})"))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.open_map))
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if (location.section == ModuleSection.ROAD_ASSISTANCE) stringResource(R.string.assistance_center) else stringResource(R.string.quick_info),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        detailLocation.phone?.let {
                            Text(text = "Phone: $it", modifier = Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        detailLocation.email?.let {
                            Text(text = "Email: $it", modifier = Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        detailLocation.website?.let {
                            Text(text = "Website: $it", modifier = Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        if (location.section == ModuleSection.ROAD_ASSISTANCE) {
                            Text(
                                text = "Service area is centered around this location and managed as a local coverage point.",
                                modifier = Modifier.padding(top = 8.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Text(
                            text = "Coordinates: ${detailLocation.latitude}, ${detailLocation.longitude}",
                            modifier = Modifier.padding(top = 8.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}
