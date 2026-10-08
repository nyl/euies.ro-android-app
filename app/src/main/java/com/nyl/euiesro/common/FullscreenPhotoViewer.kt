package com.nyl.euiesro.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import kotlin.math.abs

@Composable
fun FullscreenPhotoViewer(
    images: List<String>,
    title: String,
    initialIndex: Int,
    onDismiss: () -> Unit
) {
    if (images.isEmpty()) return

    val safeIndex = initialIndex.coerceIn(0, images.lastIndex)
    var currentIndex by remember(safeIndex) { mutableStateOf(safeIndex) }
    var scale by remember { mutableFloatStateOf(1f) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = true)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(currentIndex) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        if (zoom != 1f) {
                            scale = (scale * zoom).coerceIn(1f, 4f)
                        }
                        if (scale > 1f) {
                            dragOffset += pan
                        }
                    }
                }
                .pointerInput(currentIndex, scale) {
                    detectDragGestures(
                        onDragEnd = {
                            if (scale <= 1.01f) {
                                if (dragOffset.x < -120f && currentIndex < images.lastIndex) {
                                    currentIndex += 1
                                } else if (dragOffset.x > 120f && currentIndex > 0) {
                                    currentIndex -= 1
                                }
                            }
                            scale = 1f
                            dragOffset = Offset.Zero
                        },
                        onDrag = { _, dragAmount ->
                            if (scale > 1f) {
                                dragOffset += dragAmount
                            } else {
                                dragOffset += dragAmount
                            }
                        }
                    )
                }
        ) {
            AsyncImage(
                model = images[currentIndex],
                contentDescription = title,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = dragOffset.x,
                        translationY = dragOffset.y,
                        transformOrigin = TransformOrigin.Center
                    )
            )

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f))
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close photo",
                    tint = Color.White
                )
            }

            if (images.size > 1) {
                IconButton(
                    onClick = { if (currentIndex > 0) currentIndex -= 1 },
                    enabled = currentIndex > 0,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 16.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = "Previous photo",
                        tint = Color.White
                    )
                }

                IconButton(
                    onClick = { if (currentIndex < images.lastIndex) currentIndex += 1 },
                    enabled = currentIndex < images.lastIndex,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 16.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Next photo",
                        tint = Color.White
                    )
                }
            }
        }
    }
}
