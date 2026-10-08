package com.nyl.euiesro.features.launchspots

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import coil.compose.AsyncImage
import com.nyl.euiesro.R
import kotlinx.coroutines.launch
import com.nyl.euiesro.ApiClient
import com.nyl.euiesro.calculateDistanceMeters
import com.nyl.euiesro.formatDistanceLabel
import com.nyl.euiesro.resolveBestCurrentLocationForDisplay
import com.nyl.euiesro.EuIesroTopAppBar
import com.nyl.euiesro.InteractiveLocationMap
import com.nyl.euiesro.common.CurrentUserProfile
import com.nyl.euiesro.common.LocationHeaderCard
import com.nyl.euiesro.common.ShareUrlBuilder
import com.nyl.euiesro.common.LocationUiItem
import com.nyl.euiesro.common.ReviewSection
import com.nyl.euiesro.features.launchspots.components.LaunchSpotGalleryCard
import com.nyl.euiesro.features.launchspots.components.LaunchSpotMapCard

@Composable
fun LaunchSpotDetailScreen(
    location: LocationUiItem,
    onBack: () -> Unit,
    currentUserName: String = "Guest",
    currentUserProfile: CurrentUserProfile = CurrentUserProfile(),
    onEditLaunchSpot: () -> Unit = {},
    onSuggestLaunchSpotEdit: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val fetchedImages = remember { mutableStateListOf<String>() }
    var detailLocation by remember(location.id) { mutableStateOf(location) }
    var launchSpotDetail by remember(location.id) { mutableStateOf<LaunchSpotSummary?>(null) }
    var selectedRating by remember(location.id) { mutableStateOf(0) }
    var reviewComment by remember(location.id) { mutableStateOf("") }
    var isSubmittingReview by rememberSaveable { mutableStateOf(false) }
    var isUploadingImages by remember { mutableStateOf(false) }
    var reviewMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var uploadMessage by remember { mutableStateOf<String?>(null) }
    var showReviewForm by rememberSaveable { mutableStateOf(false) }
    var reviewFilter by rememberSaveable { mutableStateOf("all") }
    var replyReviewId by rememberSaveable { mutableStateOf<Int?>(null) }
    val ownerReplyDrafts = remember { mutableStateMapOf<Int, String>() }
    var userIsLoggedIn by remember(currentUserName) { mutableStateOf(currentUserName.isNotBlank() && currentUserName != "Guest") }
    val canEditSpot = currentUserProfile.canEditLaunchSpot(launchSpotDetail?.userId ?: 0)
    val selectRatingMessage = stringResource(R.string.select_rating_message)
    val currentUserReview = launchSpotDetail?.currentUserReview(currentUserName)
    val reviewTabOptions = listOf(
        "All" to "all",
        "5★" to "5",
        "4★" to "4",
        "3★" to "3",
        "2★" to "2",
        "1★" to "1"
    )
    val filteredReviews = when (reviewFilter) {
        "5" -> launchSpotDetail?.mappedReviews()?.filter { it.rating == 5 } ?: emptyList()
        "4" -> launchSpotDetail?.mappedReviews()?.filter { it.rating == 4 } ?: emptyList()
        "3" -> launchSpotDetail?.mappedReviews()?.filter { it.rating == 3 } ?: emptyList()
        "2" -> launchSpotDetail?.mappedReviews()?.filter { it.rating == 2 } ?: emptyList()
        "1" -> launchSpotDetail?.mappedReviews()?.filter { it.rating == 1 } ?: emptyList()
        else -> launchSpotDetail?.mappedReviews() ?: emptyList()
    }

    fun refreshDetail() {
        coroutineScope.launch {
            val detail = LaunchSpotApi.fetchLaunchSpotDetail(location.id)
            launchSpotDetail = detail
            if (detail != null) {
                detailLocation = location.copy(
                    title = detail.title,
                    description = detail.description ?: location.description,
                    address = detail.address ?: location.address,
                    latitude = detail.latitude,
                    longitude = detail.longitude,
                    phone = null,
                    website = null,
                    email = null,
                    services = emptyList(),
                    imageUrls = detail.images.ifEmpty { location.imageUrls },
                    typeIds = detail.typeIds,
                    markerIcons = detail.markerIcons
                )
                fetchedImages.clear()
                fetchedImages.addAll(detail.images)
            }
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { imageUris ->
        if (imageUris.isEmpty()) return@rememberLauncherForActivityResult
        coroutineScope.launch {
            isUploadingImages = true
            uploadMessage = null
            val success = LaunchSpotApi.uploadLaunchSpotImages(context, location.id, imageUris)
            if (success) {
                uploadMessage = "Images uploaded successfully."
                refreshDetail()
            } else {
                uploadMessage = "Your session expired. Please log in again."
            }
            isUploadingImages = false
        }
    }
    val reviewSubmittedText = stringResource(R.string.review_submitted)
    val reviewSubmitFailedText = stringResource(R.string.review_submit_failed)
    val submittingText = stringResource(R.string.submitting)
    val submitReviewText = stringResource(R.string.submit_review)

    LaunchedEffect(location.id) {
        fetchedImages.clear()
        val detail = LaunchSpotApi.fetchLaunchSpotDetail(location.id)
        launchSpotDetail = detail
        detailLocation = if (detail != null) {
            location.copy(
                title = detail.title,
                description = detail.description ?: location.description,
                address = detail.address ?: location.address,
                latitude = detail.latitude,
                longitude = detail.longitude,
                phone = null,
                website = null,
                email = null,
                services = emptyList(),
                imageUrls = detail.images.ifEmpty { location.imageUrls },
                typeIds = detail.typeIds,
                markerIcons = detail.markerIcons
            )
        } else {
            location
        }
        fetchedImages.addAll(detail?.images ?: location.imageUrls)
    }

    val directionsUri = Uri.parse("geo:${detailLocation.latitude},${detailLocation.longitude}?q=${detailLocation.latitude},${detailLocation.longitude}(${Uri.encode(detailLocation.title)})")
    val shareUrl = ShareUrlBuilder.buildFor(detailLocation)
    val userLocation = remember(context) { resolveBestCurrentLocationForDisplay(context) }
    val distanceText = userLocation?.let {
        calculateDistanceMeters(it.latitude, it.longitude, detailLocation.latitude, detailLocation.longitude)
            ?.let { meters -> formatDistanceLabel(meters) }
    }
    val averageRating = launchSpotDetail?.averageRating ?: 0f
    val reviewCount = launchSpotDetail?.reviews?.size ?: 0

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
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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
                        if (canEditSpot) {
                            Button(onClick = onEditLaunchSpot) {
                                Text("Edit")
                            }
                        } else {
                            Button(onClick = onSuggestLaunchSpotEdit) {
                                Text("Suggest edit")
                            }
                        }
                        Button(
                            onClick = { imagePickerLauncher.launch("image/*") },
                            enabled = !isUploadingImages
                        ) {
                            Text(if (isUploadingImages) "Uploading..." else "Add images")
                        }
                    }
                }
            }

            uploadMessage?.let {
                item {
                    Text(
                        text = it,
                        color = if (it.contains("Unable")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            if (fetchedImages.isNotEmpty()) {
                item {
                    LaunchSpotGalleryCard(
                        images = fetchedImages,
                        title = detailLocation.title
                    )
                }
            }

            item {
                LaunchSpotMapCard(
                    location = detailLocation,
                    onFullscreenClick = { /* no-op: same card pattern is used across modules */ },
                    onUseMyLocation = null,
                    onOpenDirections = {
                        val intent = Intent(Intent.ACTION_VIEW, directionsUri)
                        context.startActivity(intent)
                    }
                )
            }

            item {
                if (launchSpotDetail?.typeNames?.isNotBlank() == true || detailLocation.description != null || launchSpotDetail?.typeOptions?.isNotEmpty() == true) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            if (launchSpotDetail?.typeNames?.isNotBlank() == true) {
                                Text(
                                    text = launchSpotDetail?.typeNames ?: "",
                                    modifier = Modifier.padding(bottom = 8.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            detailLocation.description?.let {
                                Text(
                                    text = it,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                    lineHeight = 22.sp
                                )
                            }

                            if (launchSpotDetail?.typeOptions?.isNotEmpty() == true) {
                                Text(
                                    text = stringResource(R.string.types_label),
                                    modifier = Modifier.padding(top = 14.dp),
                                    fontWeight = FontWeight.SemiBold
                                )
                                Row(
                                    modifier = Modifier.padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    launchSpotDetail?.typeOptions?.forEach { option ->
                                        Surface(
                                            shape = RoundedCornerShape(999.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            tonalElevation = 0.dp
                                        ) {
                                            Text(
                                                text = option.label,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                style = MaterialTheme.typography.labelMedium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, directionsUri)
                            context.startActivity(intent)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.directions_action))
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.quick_details),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        launchSpotDetail?.surfaceLabel?.let {
                            Text(text = stringResource(R.string.surface_detail, it), modifier = Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        launchSpotDetail?.depthLabel?.let {
                            Text(text = stringResource(R.string.depth_detail, it), modifier = Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        launchSpotDetail?.accessibilityLabel?.let {
                            Text(text = stringResource(R.string.accessibility_detail, it), modifier = Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        launchSpotDetail?.addedBy?.let {
                            Text(text = stringResource(R.string.added_by_detail, it), modifier = Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Text(
                            text = stringResource(R.string.coordinates_detail, detailLocation.latitude, detailLocation.longitude),
                            modifier = Modifier.padding(top = 8.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        launchSpotDetail?.createdAt?.let {
                            Text(text = stringResource(R.string.created_detail, it), modifier = Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        launchSpotDetail?.updatedAt?.let {
                            Text(text = stringResource(R.string.updated_detail, it), modifier = Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
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
                    isOwner = false,
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
                        coroutineScope.launch {
                            isSubmittingReview = true
                            reviewMessage = null
                            val success = LaunchSpotApi.submitReview(location.id, selectedRating, reviewComment)
                            if (success) {
                                reviewComment = ""
                                selectedRating = 0
                                showReviewForm = false
                                reviewMessage = reviewSubmittedText
                                refreshDetail()
                            } else {
                                reviewMessage = "Your session expired. Please log in again."
                            }
                            isSubmittingReview = false
                        }
                    },
                    onCancelReview = { showReviewForm = false },
                    onReplyDraftChanged = { reviewId, value -> ownerReplyDrafts[reviewId] = value },
                    onReplyToggle = { reviewId, value ->
                        ownerReplyDrafts[reviewId] = value
                        replyReviewId = reviewId
                    },
                    onSubmitReply = { _, _ -> },
                    onCancelReply = { reviewId ->
                        ownerReplyDrafts.remove(reviewId)
                        replyReviewId = null
                    },
                    ownerReplyLabel = "Owner reply",
                    editReviewText = "Edit review",
                    replyButtonText = "Reply",
                    editReplyText = "Edit reply",
                    submitReviewText = submitReviewText,
                    submittingText = submittingText,
                    selectRatingMessage = selectRatingMessage,
                    reviewSubmittedText = reviewSubmittedText,
                    reviewSubmitFailedText = reviewSubmitFailedText,
                    replySubmitFailedText = "Unable to submit reply."
                )
            }
        }
    }
}
