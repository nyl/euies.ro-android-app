package com.nyl.euiesro.app.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nyl.euiesro.common.LocationUiItem
import com.nyl.euiesro.common.ModuleSection
import com.nyl.euiesro.features.events.EventSummary

@Composable
fun ModuleSectionScreen(
    section: ModuleSection,
    onBack: () -> Unit,
    onOpenLocation: (LocationUiItem) -> Unit,
    onOpenEvent: (EventSummary) -> Unit = {}
) {
    Scaffold(
        topBar = {
            EuIesroTopAppBar(title = section.name, onBack = onBack)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text(
                text = "Section: ${section.name}",
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
}
