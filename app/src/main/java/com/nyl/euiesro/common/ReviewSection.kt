package com.nyl.euiesro.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.nyl.euiesro.MediaUrlResolver
import com.nyl.euiesro.R
import com.nyl.euiesro.features.auto.AutoReview

@Composable
fun ReviewSection(
    reviewCount: Int,
    reviewTabOptions: List<Pair<String, String>>,
    reviewFilter: String,
    userIsLoggedIn: Boolean,
    showReviewForm: Boolean,
    selectedRating: Int,
    reviewComment: String,
    reviewMessage: String?,
    isSubmittingReview: Boolean,
    currentUserReview: AutoReview?,
    filteredReviews: List<AutoReview>,
    isOwner: Boolean,
    ownerReplyDrafts: MutableMap<Int, String>,
    replyReviewId: Int?,
    onReviewFilterChange: (String) -> Unit,
    onWriteReview: () -> Unit,
    onRatingChanged: (Int) -> Unit,
    onCommentChanged: (String) -> Unit,
    onSubmitReview: () -> Unit,
    onCancelReview: () -> Unit,
    onReplyDraftChanged: (Int, String) -> Unit,
    onReplyToggle: (Int, String) -> Unit,
    onSubmitReply: (Int, String) -> Unit,
    onCancelReply: (Int) -> Unit,
    ownerReplyLabel: String,
    editReviewText: String,
    replyButtonText: String,
    editReplyText: String,
    submitReviewText: String,
    submittingText: String,
    selectRatingMessage: String,
    reviewSubmittedText: String,
    reviewSubmitFailedText: String,
    replySubmitFailedText: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text(
                    text = "Reviews",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "$reviewCount",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (reviewCount > 0) {
                Row(
                    modifier = Modifier.padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    reviewTabOptions.forEach { (label, value) ->
                        val selected = reviewFilter == value
                        Surface(
                            onClick = { onReviewFilterChange(value) },
                            shape = RoundedCornerShape(999.dp),
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            tonalElevation = if (selected) 0.dp else 1.dp,
                            shadowElevation = 0.dp,
                            modifier = Modifier.clickable { onReviewFilterChange(value) }
                        ) {
                            Text(
                                text = label,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            if (!userIsLoggedIn) {
                Text(
                    text = stringResource(R.string.log_in_to_add_review),
                    modifier = Modifier.padding(top = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (!showReviewForm) {
                Button(
                    onClick = onWriteReview,
                    modifier = Modifier.padding(top = 12.dp)
                ) {
                    Text(if (currentUserReview != null) editReviewText else stringResource(R.string.write_review))
                }
            } else {
                Text(
                    text = stringResource(R.string.your_rating),
                    modifier = Modifier.padding(top = 10.dp),
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    (1..5).forEach { star ->
                        val selected = star <= selectedRating
                        Text(
                            text = if (selected) "★" else "☆",
                            modifier = Modifier.clickable { onRatingChanged(star) },
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 28.sp
                        )
                    }
                }

                OutlinedTextField(
                    value = reviewComment,
                    onValueChange = { onCommentChanged(it) },
                    label = { Text(stringResource(R.string.comment_label)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onSubmitReview,
                        enabled = !isSubmittingReview,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isSubmittingReview) submittingText else submitReviewText)
                    }
                    OutlinedButton(
                        onClick = onCancelReview,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.cancel_label))
                    }
                }
            }

            if (!reviewMessage.isNullOrBlank()) {
                Text(
                    text = reviewMessage,
                    modifier = Modifier.padding(top = 12.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (filteredReviews.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                filteredReviews.forEach { review ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                val avatarUrl = remember(review.avatarUrl) {
                                    MediaUrlResolver.resolve(review.avatarUrl)
                                }

                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!avatarUrl.isNullOrBlank()) {
                                        AsyncImage(
                                            model = avatarUrl,
                                            contentDescription = "${review.userName} profile picture",
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = "Anonymous user profile",
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = review.userName,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "${"★".repeat(review.rating)}${"☆".repeat(5 - review.rating)}",
                                            color = MaterialTheme.colorScheme.tertiary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(999.dp),
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                            tonalElevation = 0.dp
                                        ) {
                                            Text(
                                                text = "${review.rating}/5",
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            val sanitizedComment = review.comment?.takeUnless { it.equals("null", ignoreCase = true) }?.trim()
                            sanitizedComment?.takeIf { it.isNotEmpty() }?.let {
                                Text(
                                    text = it,
                                    modifier = Modifier.padding(top = 8.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 20.sp
                                )
                            }
                            val sanitizedOwnerReply = review.ownerReply?.takeUnless { it.equals("null", ignoreCase = true) }?.trim()
                            if (!sanitizedOwnerReply.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                                    tonalElevation = 0.dp,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = ownerReplyLabel,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            text = sanitizedOwnerReply,
                                            modifier = Modifier.padding(top = 4.dp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            lineHeight = 18.sp
                                        )
                                    }
                                }
                            }

                            if (isOwner) {
                                val replyDraft = ownerReplyDrafts[review.id] ?: review.ownerReply.orEmpty()
                                if (replyReviewId == review.id) {
                                    OutlinedTextField(
                                        value = replyDraft,
                                        onValueChange = { onReplyDraftChanged(review.id, it) },
                                        label = { Text(ownerReplyLabel) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp)
                                    )
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { onSubmitReply(review.id, ownerReplyDrafts[review.id].orEmpty()) },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(if (review.ownerReply.isNullOrBlank()) replyButtonText else editReplyText)
                                        }
                                        OutlinedButton(
                                            onClick = { onCancelReply(review.id) },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(stringResource(R.string.cancel_label))
                                        }
                                    }
                                } else {
                                    Button(
                                        onClick = { onReplyToggle(review.id, review.ownerReply.orEmpty()) },
                                        modifier = Modifier.padding(top = 8.dp)
                                    ) {
                                        Text(if (review.ownerReply.isNullOrBlank()) replyButtonText else editReplyText)
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (reviewCount > 0) {
                Text(
                    text = "No reviews match this filter.",
                    modifier = Modifier.padding(top = 12.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
