package com.nyl.euiesro.features.auto

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nyl.euiesro.R
import com.nyl.euiesro.EuIesroTopAppBar
import com.nyl.euiesro.calculateDistanceMeters
import com.nyl.euiesro.common.LocationUiItem
import com.nyl.euiesro.formatDistanceLabel
import com.nyl.euiesro.resolveBestCurrentLocationForDisplay
import com.nyl.euiesro.common.LocationHeaderCard
import com.nyl.euiesro.common.ShareUrlBuilder
import com.nyl.euiesro.features.auto.components.AutoGalleryCard
import com.nyl.euiesro.features.auto.components.AutoInfoCard
import com.nyl.euiesro.features.auto.components.AutoMapCard
import com.nyl.euiesro.common.ReviewSection
import com.nyl.euiesro.features.auto.components.AutoWorkingHoursCard
import kotlinx.coroutines.launch

@Composable
fun AutoDetailContent(
    location: LocationUiItem,
    onBack: () -> Unit,
    currentUserName: String = "Guest",
    onEditAutoPlace: () -> Unit = {},
    onSuggestAutoEdit: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val fetchedImages = remember { androidx.compose.runtime.mutableStateListOf<String>() }
    var detailLocation by remember(location.id) { mutableStateOf(location) }
    var detail by remember(location.id) { mutableStateOf<AutoPlaceSummary?>(null) }
    var selectedRating by remember(location.id) { mutableStateOf(0) }
    var reviewComment by remember(location.id) { mutableStateOf("") }
    var isSubmittingReview by rememberSaveable { mutableStateOf(false) }
    var reviewMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var showReviewForm by rememberSaveable { mutableStateOf(false) }
    var reviewFilter by rememberSaveable { mutableStateOf("all") }
    var replyReviewId by rememberSaveable { mutableStateOf<Int?>(null) }
    val ownerReplyDrafts = remember { mutableStateMapOf<Int, String>() }
    var userIsLoggedIn by remember(currentUserName) { mutableStateOf(currentUserName.isNotBlank() && currentUserName != "Guest") }
    val selectRatingMessage = stringResource(R.string.select_rating_message)
    val reviewSubmittedText = stringResource(R.string.review_submitted)
    val reviewSubmitFailedText = stringResource(R.string.review_submit_failed)
    val submittingText = stringResource(R.string.submitting)
    val submitReviewText = stringResource(R.string.submit_review)
    val editReviewText = stringResource(R.string.edit_review)
    val ownerReplyLabel = stringResource(R.string.owner_reply)
    val replyButtonText = stringResource(R.string.reply_action)
    val editReplyText = stringResource(R.string.edit_reply)
    val replySubmitFailedText = stringResource(R.string.reply_submit_failed)
    val isOwner = detail?.isCurrentUserOwner(currentUserName) == true
    val currentUserReview = detail?.currentUserReview(currentUserName)

    LaunchedEffect(location.id) {
        fetchedImages.clear()
        val fetched = AutoApi.fetchAutoPlaceDetail(location.id)
        detail = fetched
        detailLocation = if (fetched != null) {
            location.copy(
                title = fetched.title,
                description = fetched.description ?: location.description,
                address = fetched.address ?: location.address,
                latitude = fetched.latitude,
                longitude = fetched.longitude,
                phone = fetched.phone ?: location.phone,
                website = fetched.website ?: location.website,
                email = fetched.email ?: location.email,
                services = fetched.services.ifEmpty { location.services },
                imageUrls = fetched.images.ifEmpty { location.imageUrls },
                typeIds = fetched.typeIds,
                markerIcons = fetched.markerIcons
            )
        } else {
            location
        }
        fetchedImages.addAll(fetched?.images ?: location.imageUrls)
    }

    val directionsUri = Uri.parse("geo:${detailLocation.latitude},${detailLocation.longitude}?q=${detailLocation.latitude},${detailLocation.longitude}(${Uri.encode(detailLocation.title)})")
    val shareUrl = ShareUrlBuilder.buildFor(detailLocation)
    val userLocation = remember(context) { resolveBestCurrentLocationForDisplay(context) }
    val distanceText = userLocation?.let {
        calculateDistanceMeters(it.latitude, it.longitude, detailLocation.latitude, detailLocation.longitude)
            ?.let { meters -> formatDistanceLabel(meters) }
    }
    val averageRating = detail?.averageRating ?: 0f
    val reviewCount = detail?.reviews?.size ?: 0
    val reviewTabOptions = listOf(
        "All" to "all",
        "5★" to "5",
        "4★" to "4",
        "3★" to "3",
        "2★" to "2",
        "1★" to "1"
    )
    val filteredReviews = when (reviewFilter) {
        "5" -> detail?.reviews?.filter { it.rating == 5 } ?: emptyList()
        "4" -> detail?.reviews?.filter { it.rating == 4 } ?: emptyList()
        "3" -> detail?.reviews?.filter { it.rating == 3 } ?: emptyList()
        "2" -> detail?.reviews?.filter { it.rating == 2 } ?: emptyList()
        "1" -> detail?.reviews?.filter { it.rating == 1 } ?: emptyList()
        else -> detail?.reviews ?: emptyList()
    }

    fun refreshDetail() {
        coroutineScope.launch {
            val fetched = AutoApi.fetchAutoPlaceDetail(location.id)
            detail = fetched
            if (fetched != null) {
                detailLocation = location.copy(
                    title = fetched.title,
                    description = fetched.description ?: location.description,
                    address = fetched.address ?: location.address,
                    latitude = fetched.latitude,
                    longitude = fetched.longitude,
                    phone = fetched.phone ?: location.phone,
                    website = fetched.website ?: location.website,
                    email = fetched.email ?: location.email,
                    services = fetched.services.ifEmpty { location.services },
                    imageUrls = fetched.images.ifEmpty { location.imageUrls },
                    typeIds = fetched.typeIds,
                    markerIcons = fetched.markerIcons
                )
                fetchedImages.clear()
                fetchedImages.addAll(fetched.images)
            }
        }
    }

    Scaffold(
        topBar = {
            EuIesroTopAppBar(
                title = detailLocation.title,
                onBack = onBack,
                onShare = {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareUrl)
                        putExtra(Intent.EXTRA_SUBJECT, detailLocation.title)
                    }
                    context.startActivity(Intent.createChooser(intent, "Share"))
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (fetchedImages.isNotEmpty()) {
                item {
                    AutoGalleryCard(
                        images = fetchedImages,
                        title = detailLocation.title
                    )
                }
            }

            item {
                LocationHeaderCard(
                    title = detailLocation.title,
                    address = detailLocation.address,
                    averageRating = averageRating,
                    reviewCount = reviewCount,
                    distanceText = distanceText
                )
            }

            if (userIsLoggedIn) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (detail?.addedBy != null && (detail?.ownerUserName == currentUserName || detail?.addedBy == currentUserName || isOwner)) {
                            Button(onClick = onEditAutoPlace) {
                                Text("Edit")
                            }
                        } else {
                            Button(onClick = onSuggestAutoEdit) {
                                Text("Suggest edit")
                            }
                        }
                    }
                }
            }

            item {
                AutoInfoCard(
                    location = detailLocation,
                    detail = detail,
                    onCall = {
                        if (!detailLocation.phone.isNullOrBlank()) {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${detailLocation.phone}"))
                            context.startActivity(intent)
                        }
                    },
                    onWebsite = {
                        if (!detailLocation.website.isNullOrBlank()) {
                            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(detailLocation.website))
                            context.startActivity(webIntent)
                        }
                    },
                    onDirections = {
                        val intent = Intent(Intent.ACTION_VIEW, directionsUri)
                        context.startActivity(intent)
                    }
                )
            }

            item {
                AutoWorkingHoursCard(detail = detail)
            }

            item {
                ReviewSection(
                    reviewCount = reviewCount,
                    reviewTabOptions = reviewTabOptions,
                    reviewFilter = reviewFilter,
                    userIsLoggedIn = userIsLoggedIn,
                    showReviewForm = showReviewForm,
                    selectedRating = selectedRating,
                    reviewComment = reviewComment,
                    reviewMessage = reviewMessage,
                    isSubmittingReview = isSubmittingReview,
                    currentUserReview = currentUserReview,
                    filteredReviews = filteredReviews,
                    isOwner = isOwner,
                    ownerReplyDrafts = ownerReplyDrafts,
                    replyReviewId = replyReviewId,
                    onReviewFilterChange = { reviewFilter = it },
                    onWriteReview = {
                        if (currentUserReview != null) {
                            selectedRating = currentUserReview.rating
                            reviewComment = currentUserReview.comment ?: ""
                        } else {
                            selectedRating = 0
                            reviewComment = ""
                        }
                        showReviewForm = true
                    },
                    onRatingChanged = { selectedRating = it },
                    onCommentChanged = { reviewComment = it },
                    onSubmitReview = {
                        if (selectedRating == 0) {
                            reviewMessage = selectRatingMessage
                            return@ReviewSection
                        }
                        isSubmittingReview = true
                        reviewMessage = null
                        coroutineScope.launch {
                            val success = AutoApi.submitReview(location.id, selectedRating, reviewComment)
                            isSubmittingReview = false
                            if (success) {
                                reviewMessage = reviewSubmittedText
                                selectedRating = 0
                                reviewComment = ""
                                showReviewForm = false
                                refreshDetail()
                            } else {
                                reviewMessage = "Your session expired. Please log in again."
                            }
                        }
                    },
                    onCancelReview = { showReviewForm = false },
                    onReplyDraftChanged = { reviewId, value -> ownerReplyDrafts[reviewId] = value },
                    onReplyToggle = { reviewId, value ->
                        ownerReplyDrafts[reviewId] = value
                        replyReviewId = reviewId
                    },
                    onSubmitReply = { reviewId, value ->
                        val replyText = value.trim()
                        if (replyText.isBlank()) return@ReviewSection
                        coroutineScope.launch {
                            val success = AutoApi.submitOwnerReply(location.id, reviewId, replyText)
                            if (success) {
                                ownerReplyDrafts.remove(reviewId)
                                replyReviewId = null
                                refreshDetail()
                            } else {
                                reviewMessage = "Your session expired. Please log in again."
                            }
                        }
                    },
                    onCancelReply = { reviewId ->
                        ownerReplyDrafts.remove(reviewId)
                        replyReviewId = null
                    },
                    ownerReplyLabel = ownerReplyLabel,
                    editReviewText = editReviewText,
                    replyButtonText = replyButtonText,
                    editReplyText = editReplyText,
                    submitReviewText = submitReviewText,
                    submittingText = submittingText,
                    selectRatingMessage = selectRatingMessage,
                    reviewSubmittedText = reviewSubmittedText,
                    reviewSubmitFailedText = reviewSubmitFailedText,
                    replySubmitFailedText = replySubmitFailedText
                )
            }

            item {
                AutoMapCard(
                    location = detailLocation,
                    directionsUri = directionsUri,
                    onOpenDirections = {
                        val intent = Intent(Intent.ACTION_VIEW, directionsUri)
                        context.startActivity(intent)
                    }
                )
            }
        }
    }
}
