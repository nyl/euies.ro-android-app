package com.nyl.euiesro.app.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.CarRepair
import androidx.compose.material.icons.filled.LocalCarWash
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Water
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nyl.euiesro.R
import com.nyl.euiesro.app.AppLocale
import com.nyl.euiesro.app.components.ModuleGridCard
import com.nyl.euiesro.app.components.ProfileDialog
import com.nyl.euiesro.app.components.SettingsDialog
import com.nyl.euiesro.app.components.UserWelcomeCard
import com.nyl.euiesro.common.CurrentUserProfile
import com.nyl.euiesro.common.ModuleItem
import com.nyl.euiesro.common.ModuleSection

@Composable
fun MainScreen(
    userName: String,
    currentUserProfile: CurrentUserProfile = CurrentUserProfile(),
    appLocale: AppLocale,
    isDarkMode: Boolean,
    onDarkModeChanged: (Boolean) -> Unit,
    onLocaleChanged: (AppLocale) -> Unit,
    onCheckForUpdates: () -> Unit,
    onLogout: () -> Unit,
    onProfileUpdated: (CurrentUserProfile) -> Unit = {},
    onOpenEditProfile: () -> Unit = {},
    onSectionSelected: (ModuleSection) -> Unit
) {
    var showProfileDialog by rememberSaveable { mutableStateOf(false) }
    var showSettingsDialog by rememberSaveable { mutableStateOf(false) }

    if (showProfileDialog) {
        ProfileDialog(
            userName = userName,
            currentUserProfile = currentUserProfile,
            onAvatarUpdated = {
                onProfileUpdated(it)
                showProfileDialog = false
            },
            onEditProfile = {
                showProfileDialog = false
                onOpenEditProfile()
            },
            onDismiss = { showProfileDialog = false }
        )
    }

    if (showSettingsDialog) {
        SettingsDialog(
            isDarkMode = isDarkMode,
            appLocale = appLocale,
            onDarkModeChanged = onDarkModeChanged,
            onLocaleChanged = onLocaleChanged,
            onCheckForUpdates = {
                onCheckForUpdates()
                showSettingsDialog = false
            },
            onDismiss = { showSettingsDialog = false }
        )
    }

    val moduleEventsTitle = stringResource(R.string.module_events_title)
    val moduleEventsSubtitle = stringResource(R.string.module_events_subtitle)
    val moduleMotoTitle = stringResource(R.string.module_moto_title)
    val moduleMotoSubtitle = stringResource(R.string.module_moto_subtitle)
    val moduleRoadAssistanceTitle = stringResource(R.string.module_road_assistance_title)
    val moduleRoadAssistanceSubtitle = stringResource(R.string.module_road_assistance_subtitle)
    val moduleLaunchSpotsTitle = stringResource(R.string.module_launch_spots_title)
    val moduleLaunchSpotsSubtitle = stringResource(R.string.module_launch_spots_subtitle)
    val moduleAutoTitle = stringResource(R.string.module_auto_title)
    val moduleAutoSubtitle = stringResource(R.string.module_auto_subtitle)

    val modules = listOf(
        ModuleItem(moduleEventsTitle, moduleEventsSubtitle, Icons.Default.Event, Color(0xFF5B8DEF)),
        ModuleItem(moduleMotoTitle, moduleMotoSubtitle, Icons.Default.DirectionsBike, Color(0xFF13B98D)),
        ModuleItem(moduleRoadAssistanceTitle, moduleRoadAssistanceSubtitle, Icons.Default.CarRepair, Color(0xFF22A6B3)),
        ModuleItem(moduleLaunchSpotsTitle, moduleLaunchSpotsSubtitle, Icons.Default.Water, Color(0xFFB16CF8)),
        ModuleItem(moduleAutoTitle, moduleAutoSubtitle, Icons.Default.LocalCarWash, Color(0xFFFCB97D))
    )

    Scaffold(
        topBar = {
            EuIesroTopAppBar(
                title = "Euiesro",
                onUserProfile = { showProfileDialog = true },
                onAppSettings = { showSettingsDialog = true },
                onLogout = onLogout,
                profileImageUrl = currentUserProfile.avatarUrl,
                showAppIconInsteadOfTitle = true
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            UserWelcomeCard(userName = userName)

            Spacer(modifier = Modifier.height(14.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(modules) { module ->
                    ModuleGridCard(
                        module = module,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            when (module.title) {
                                moduleEventsTitle -> onSectionSelected(ModuleSection.EVENTS)
                                moduleMotoTitle -> onSectionSelected(ModuleSection.MOTO)
                                moduleRoadAssistanceTitle -> onSectionSelected(ModuleSection.ROAD_ASSISTANCE)
                                moduleLaunchSpotsTitle -> onSectionSelected(ModuleSection.LAUNCH_SPOTS)
                                moduleAutoTitle -> onSectionSelected(ModuleSection.AUTO)
                            }
                        }
                    )
                }
            }
        }
    }
}

