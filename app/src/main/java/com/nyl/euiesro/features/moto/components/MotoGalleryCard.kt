package com.nyl.euiesro.features.moto.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.nyl.euiesro.common.FullscreenPhotoViewer

@Composable
fun MotoGalleryCard(images: List<String>, title: String) {
    var selectedIndex by remember(images) { mutableStateOf(0) }
    var viewerOpen by remember(images) { mutableStateOf(false) }

    if (viewerOpen && images.isNotEmpty()) {
        FullscreenPhotoViewer(
            images = images,
            title = title,
            initialIndex = selectedIndex,
            onDismiss = { viewerOpen = false }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(images.withIndex().toList()) { item ->
                    val imageIndex = item.index
                    val imageUrl = item.value
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = title,
                        modifier = Modifier
                            .width(110.dp)
                            .height(80.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                selectedIndex = imageIndex
                                viewerOpen = true
                            },
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}
