package com.nyl.euiesro.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.nyl.euiesro.EuIesroApp as LegacyEuIesroApp

@Composable
fun EuIesroApp(
    darkMode: Boolean = ThemePreferencesManager.isDarkModeEnabled(LocalContext.current),
    onDarkModeChanged: (Boolean) -> Unit = {}
) {
    LegacyEuIesroApp(
        darkMode = darkMode,
        onDarkModeChanged = onDarkModeChanged
    )
}
