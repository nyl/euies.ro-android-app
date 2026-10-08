package com.nyl.euiesro.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nyl.euiesro.InteractiveLocationMap

@Composable
fun CompactSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Caută locație",
    onFilterClick: (() -> Unit)? = null
) {
    val colors = MaterialTheme.colorScheme
    val glassSurface = colors.surface.copy(alpha = 0.68f)
    val glassBorder = colors.outline.copy(alpha = 0.25f)

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            placeholder = {
                Text(
                    text = placeholder,
                    color = colors.onSurfaceVariant,
                    fontSize = 14.sp
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = colors.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = glassSurface,
                focusedContainerColor = glassSurface,
                unfocusedBorderColor = glassBorder,
                focusedBorderColor = colors.primary.copy(alpha = 0.8f),
                cursorColor = colors.primary,
                unfocusedTextColor = colors.onSurface,
                focusedTextColor = colors.onSurface
            )
        )

        if (onFilterClick != null) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = glassSurface,
                tonalElevation = 0.dp,
                border = BorderStroke(1.dp, glassBorder),
                shadowElevation = 2.dp
            ) {
                IconButton(
                    onClick = onFilterClick,
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Filter",
                        tint = colors.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CompactFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val selectedBg = colors.primary.copy(alpha = 0.82f)
    val selectedText = colors.onPrimary
    val defaultBg = colors.surface.copy(alpha = 0.62f)
    val defaultText = colors.onSurfaceVariant
    val glassBorder = colors.outline.copy(alpha = 0.22f)

    Surface(
        modifier = Modifier
            .height(32.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) selectedBg else defaultBg,
        tonalElevation = 0.dp,
        border = BorderStroke(1.dp, glassBorder),
        shadowElevation = 1.dp
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = if (selected) selectedText else defaultText,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                fontSize = 12.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
fun CompactFilterRow(
    items: List<Pair<String, String>>,
    selectedKey: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            CompactFilterChip(
                label = "Toate",
                selected = selectedKey == null,
                onClick = { onSelect(null) }
            )
        }
        items(items) { (key, label) ->
            CompactFilterChip(
                label = label,
                selected = selectedKey == key,
                onClick = { onSelect(if (selectedKey == key) null else key) }
            )
        }
    }
}

@Composable
fun CompactMapPreviewCard(
    count: Int,
    items: List<LocationUiItem>,
    onOpenFullscreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenFullscreen),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Previzualizare hartă",
                color = colors.onSurface,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
            Text(
                text = "Locații din apropiere",
                color = colors.onSurfaceVariant,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surface)
            ) {
                InteractiveLocationMap(
                    items = items,
                    modifier = Modifier.fillMaxSize(),
                    interactive = false,
                    showUserLocation = false
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "$count locații",
                    color = colors.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colors.primaryContainer,
                    tonalElevation = 0.dp,
                    modifier = Modifier.clickable(onClick = onOpenFullscreen)
                ) {
                    Text(
                        text = "Ecran complet",
                        color = colors.onPrimaryContainer,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
fun CompactMockupLocationRow(
    title: String,
    subtitle: String,
    statusText: String? = null,
    rating: Float = 0f,
    reviewCount: Int = 0,
    chips: List<String> = emptyList(),
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val colors = MaterialTheme.colorScheme

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        color = colors.surfaceVariant,
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = colors.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        color = colors.onSurfaceVariant,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                if (rating > 0f || reviewCount > 0 || chips.isNotEmpty()) {
                    Row(
                        modifier = Modifier.padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (rating > 0f) {
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = colors.secondaryContainer,
                                tonalElevation = 0.dp
                            ) {
                                Text(
                                    text = "★ ${"%.1f".format(rating)}",
                                    color = colors.onSecondaryContainer,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                )
                            }
                        }

                        if (reviewCount > 0) {
                            Text(
                                text = "$reviewCount recenzii",
                                color = colors.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }

                        chips.take(3).forEach { chip ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = colors.surface,
                                tonalElevation = 0.dp
                            ) {
                                Text(
                                    text = chip,
                                    color = colors.onSurface,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (!statusText.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = colors.primaryContainer,
                    tonalElevation = 0.dp
                ) {
                    Text(
                        text = statusText,
                        color = colors.onPrimaryContainer,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CompactLocationListItem(
    title: String,
    subtitle: String,
    badge: String,
    chips: List<String> = emptyList(),
    rating: Float = 0f,
    reviewCount: Int = 0,
    statusText: String? = null,
    distanceText: String? = null,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val colors = MaterialTheme.colorScheme

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    color = colors.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )

                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        color = colors.onSurfaceVariant,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                if (rating > 0f || reviewCount > 0 || chips.isNotEmpty() || !distanceText.isNullOrBlank()) {
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        modifier = Modifier.padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (rating > 0f) {
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = colors.secondaryContainer,
                                tonalElevation = 0.dp,
                                modifier = Modifier.defaultMinSize(minHeight = 18.dp)
                            ) {
                                Text(
                                    text = "★ ${"%.1f".format(rating)}",
                                    color = colors.onSecondaryContainer,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        if (reviewCount > 0) {
                            Text(
                                text = "$reviewCount recenzii",
                                color = colors.onSurfaceVariant,
                                fontSize = 10.sp
                            )
                        }

                        distanceText?.takeIf { it.isNotBlank() }?.let {
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = colors.primary.copy(alpha = 0.12f),
                                tonalElevation = 0.dp,
                                modifier = Modifier.defaultMinSize(minHeight = 18.dp)
                            ) {
                                Text(
                                    text = it,
                                    color = colors.primary,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .widthIn(max = 90.dp)
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }

                        chips.take(3).forEach { chip ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = colors.surface,
                                tonalElevation = 0.dp,
                                modifier = Modifier.defaultMinSize(minHeight = 18.dp)
                            ) {
                                Text(
                                    text = chip,
                                    color = colors.onSurface,
                                    fontSize = 8.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .widthIn(max = 90.dp)
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (!statusText.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = colors.primaryContainer,
                    tonalElevation = 0.dp
                ) {
                    Text(
                        text = statusText,
                        color = colors.onPrimaryContainer,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            } else if (badge.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = colors.primaryContainer,
                    tonalElevation = 0.dp
                ) {
                    Text(
                        text = badge,
                        color = colors.onPrimaryContainer,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
