package com.nyl.euiesro.common.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MapSearchField(
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
            shape = RoundedCornerShape(22.dp),
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
                    tint = colors.onSurfaceVariant
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
                border = androidx.compose.foundation.BorderStroke(1.dp, glassBorder),
                shadowElevation = 2.dp
            ) {
                IconButton(
                    onClick = onFilterClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Filter",
                        tint = colors.onSurfaceVariant
                    )
                }
            }
        }
    }
}
