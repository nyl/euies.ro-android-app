package com.nyl.euiesro.app.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nyl.euiesro.R
import com.nyl.euiesro.app.AppLocale

@Composable
fun SettingsDialog(
    isDarkMode: Boolean,
    appLocale: AppLocale,
    onDarkModeChanged: (Boolean) -> Unit,
    onLocaleChanged: (AppLocale) -> Unit,
    onCheckForUpdates: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.app_settings)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.dark_mode))
                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = onDarkModeChanged
                    )
                }

                TextButton(onClick = onCheckForUpdates) {
                    Text(stringResource(R.string.check_for_updates))
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppLocale.entries.forEach { locale ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onLocaleChanged(locale) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = appLocale == locale,
                                onClick = { onLocaleChanged(locale) }
                            )
                            Text(locale.label)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.ok))
            }
        }
    )
}
