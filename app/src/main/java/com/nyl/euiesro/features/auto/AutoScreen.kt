package com.nyl.euiesro.features.auto

import androidx.compose.runtime.Composable
import com.nyl.euiesro.common.LocationUiItem

@Composable
fun AutoSection(
    innerPadding: androidx.compose.foundation.layout.PaddingValues,
    onOpenLocation: (LocationUiItem) -> Unit,
    onAddAutoPlace: () -> Unit = {}
) {
    AutoListScreen(innerPadding, onOpenLocation, onAddAutoPlace)
}

@Composable
fun AutoDetailScreen(
    location: LocationUiItem,
    onBack: () -> Unit,
    currentUserName: String = "Guest",
    onEditAutoPlace: () -> Unit = {},
    onSuggestAutoEdit: () -> Unit = {}
) {
    AutoDetailContent(location, onBack, currentUserName, onEditAutoPlace, onSuggestAutoEdit)
}
