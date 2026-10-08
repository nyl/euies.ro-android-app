package com.nyl.euiesro.features.moto.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nyl.euiesro.InteractiveLocationMap
import com.nyl.euiesro.common.LocationUiItem

@Composable
fun MotoMapPreviewCard(
    count: Int,
    items: List<LocationUiItem>,
    modifier: Modifier = Modifier,
    onOpenFullscreen: (() -> Unit)? = null,
    onCenterLocation: (() -> Unit)? = null,
    onZoomIn: (() -> Unit)? = null,
    onZoomOut: (() -> Unit)? = null
) {
    val colors = MaterialTheme.colorScheme

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.surface)
            ) {
                InteractiveLocationMap(
                    items = items,
                    modifier = Modifier.fillMaxSize(),
                    interactive = false,
                    showUserLocation = false
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 12.dp, end = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (onCenterLocation != null) {
                        MapMiniControlButton(
                            icon = Icons.Default.MyLocation,
                            contentDescription = "Center location",
                            onClick = onCenterLocation
                        )
                    }
                    if (onZoomIn != null) {
                        MapMiniControlButton(
                            icon = Icons.Default.Add,
                            contentDescription = "Zoom in",
                            onClick = onZoomIn
                        )
                    }
                    if (onZoomOut != null) {
                        MapMiniControlButton(
                            icon = Icons.Default.Remove,
                            contentDescription = "Zoom out",
                            onClick = onZoomOut
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, start = 4.dp, end = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$count de locații găsite",
                    color = colors.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )

                if (onOpenFullscreen != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = colors.primaryContainer,
                        tonalElevation = 0.dp,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(
                            text = "Ecran complet",
                            color = colors.onPrimaryContainer,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MapMiniControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        tonalElevation = 2.dp,
        shadowElevation = 2.dp,
        modifier = Modifier.size(36.dp)
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
