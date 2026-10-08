package com.nyl.euiesro.features.auto.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nyl.euiesro.R
import com.nyl.euiesro.common.LocationUiItem
import com.nyl.euiesro.features.auto.AutoPlaceSummary

@Composable
fun AutoInfoCard(
    location: LocationUiItem,
    detail: AutoPlaceSummary?,
    onCall: () -> Unit,
    onWebsite: () -> Unit,
    onDirections: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.quick_info),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            detail?.description?.let {
                Text(
                    text = it,
                    modifier = Modifier.padding(top = 10.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    lineHeight = 22.sp
                )
            }

            if (detail?.typeNames?.isNotBlank() == true) {
                Text(
                    text = detail.typeNames,
                    modifier = Modifier.padding(top = 12.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (detail?.services?.isNotEmpty() == true) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 12.dp)
                ) {
                    items(detail.services) { service ->
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                        ) {
                            Text(
                                text = service,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!location.phone.isNullOrBlank()) {
                    Button(
                        onClick = onCall,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.call_action))
                    }
                }
                if (!location.website.isNullOrBlank()) {
                    Button(
                        onClick = onWebsite,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.website_action))
                    }
                }
                Button(
                    onClick = onDirections,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.directions_action))
                }
            }
        }
    }
}
