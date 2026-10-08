package com.nyl.euiesro.features.roadassistance

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextButton
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
import androidx.core.content.ContextCompat
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nyl.euiesro.R
import coil.compose.AsyncImage
import com.nyl.euiesro.ApiClient
import com.nyl.euiesro.EuIesroTopAppBar
import kotlinx.coroutines.launch
import com.nyl.euiesro.common.LocationUiItem
import com.nyl.euiesro.common.LocationHeaderCard
import com.nyl.euiesro.common.ReviewSection
import com.nyl.euiesro.common.ShareUrlBuilder
import com.nyl.euiesro.features.auto.AutoReview
import com.nyl.euiesro.features.roadassistance.common.RoadAssistanceBoundaryMapCard
import com.nyl.euiesro.features.roadassistance.common.RoadAssistanceBoundaryMapUtils
import com.nyl.euiesro.resolveBestCurrentLocationSuspend

@Composable
fun RoadAssistanceDetailScreen(
    location: LocationUiItem,
    onBack: () -> Unit,
    currentUserName: String = "Guest",
    currentUserProfile: com.nyl.euiesro.common.CurrentUserProfile = com.nyl.euiesro.common.CurrentUserProfile(),
    onEditRoadAssistance: () -> Unit = {},
    onSuggestRoadAssistanceEdit: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val fetchedImages = remember { mutableStateListOf<String>() }
    var detailLocation by remember(location.id) { mutableStateOf(location) }
    var roadDetail by remember(location.id) { mutableStateOf<RoadAssistanceSummary?>(null) }
    var userDistanceMeters by remember(location.id) { mutableStateOf<Float?>(null) }
    var userIsLoggedIn by remember(currentUserName) { mutableStateOf(currentUserName.isNotBlank() && currentUserName != "Guest") }
    var selectedRating by remember(location.id) { mutableStateOf(0) }
    var reviewComment by remember(location.id) { mutableStateOf("") }
    var isSubmittingReview by rememberSaveable { mutableStateOf(false) }
    var reviewMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var showReviewForm by rememberSaveable { mutableStateOf(false) }
    var reviewFilter by rememberSaveable { mutableStateOf("all") }
    var replyReviewId by rememberSaveable { mutableStateOf<Int?>(null) }
    val ownerReplyDrafts = remember { mutableStateMapOf<Int, String>() }
    val currentUserReview = roadDetail?.reviews?.firstOrNull { review ->
        val sameUserName = review.userName.equals(currentUserName, ignoreCase = true)
        val sameProfileId = currentUserProfile.id > 0 && review.userId > 0 && review.userId == currentUserProfile.id
        sameUserName || sameProfileId
    }?.let { review ->
        AutoReview(
            id = review.id,
            userId = review.userId,
            userName = review.userName,
            avatarUrl = review.avatarUrl,
            rating = review.rating,
            comment = review.comment,
            createdAt = review.createdAt,
            ownerReply = review.ownerReply
        )
    }
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionRequestTriggered by remember { mutableStateOf(false) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    LaunchedEffect(location.id, hasLocationPermission) {
        fetchedImages.clear()
        val detail = RoadAssistanceApi.fetchRoadAssistancePlaceDetail(location.id)
        roadDetail = detail
        detailLocation = if (detail != null) {
            location.copy(
                title = detail.title,
                description = detail.description ?: location.description,
                address = detail.address ?: location.address,
                latitude = detail.latitude,
                longitude = detail.longitude,
                phone = detail.phone ?: location.phone,
                website = detail.website ?: location.website,
                email = detail.email ?: location.email,
                services = detail.services.ifEmpty { location.services },
                imageUrls = location.imageUrls
            )
        } else {
            location
        }
        fetchedImages.addAll(location.imageUrls)

        if (!hasLocationPermission && !permissionRequestTriggered) {
            permissionRequestTriggered = true
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
            return@LaunchedEffect
        }

        if (hasLocationPermission) {
            val currentLocation = resolveBestCurrentLocationSuspend(context)
            if (currentLocation != null) {
                val results = FloatArray(1)
                Location.distanceBetween(
                    currentLocation.latitude,
                    currentLocation.longitude,
                    detailLocation.latitude,
                    detailLocation.longitude,
                    results
                )
                userDistanceMeters = results[0]
            } else {
                userDistanceMeters = null
            }
        } else {
            userDistanceMeters = null
        }
    }

    LaunchedEffect(hasLocationPermission) {
        if (!hasLocationPermission && !permissionRequestTriggered) {
            permissionRequestTriggered = true
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    val directionsUri = Uri.parse("geo:${detailLocation.latitude},${detailLocation.longitude}?q=${detailLocation.latitude},${detailLocation.longitude}(${Uri.encode(detailLocation.title)})")
    val shareUrl = ShareUrlBuilder.buildFor(detailLocation)
    val distanceText = userDistanceMeters?.let {
        if (it >= 1000f) {
            val distanceKm = String.format(java.util.Locale.US, "%.1f", it / 1000f)
            stringResource(R.string.road_assistance_distance_km, distanceKm)
        } else {
            stringResource(R.string.road_assistance_distance_m, it.toInt())
        }
    }
    val selectRatingMessage = stringResource(R.string.moto_error_select_rating)
    val reviewSubmittedText = stringResource(R.string.moto_review_submitted)
    val reviewSubmitFailedText = stringResource(R.string.moto_review_submit_failed)
    val replySubmitFailedText = stringResource(R.string.moto_reply_submit_failed)
    val reviewCount = roadDetail?.reviewsCount ?: roadDetail?.reviews?.size ?: 0
    val reviewTabOptions = listOf(
        "All" to "all",
        "5★" to "5",
        "4★" to "4",
        "3★" to "3",
        "2★" to "2",
        "1★" to "1"
    )
    val mappedReviews = roadDetail?.reviews?.map {
        AutoReview(
            id = it.id,
            userId = it.userId,
            userName = it.userName,
            avatarUrl = it.avatarUrl,
            rating = it.rating,
            comment = it.comment,
            createdAt = it.createdAt,
            ownerReply = it.ownerReply
        )
    } ?: emptyList()
    val filteredReviews = when (reviewFilter) {
        "5" -> mappedReviews.filter { it.rating == 5 }
        "4" -> mappedReviews.filter { it.rating == 4 }
        "3" -> mappedReviews.filter { it.rating == 3 }
        "2" -> mappedReviews.filter { it.rating == 2 }
        "1" -> mappedReviews.filter { it.rating == 1 }
        else -> mappedReviews
    }
    val isOwner = currentUserProfile.id > 0 && roadDetail?.userId == currentUserProfile.id
    fun refreshDetail() {
        scope.launch {
            val detail = RoadAssistanceApi.fetchRoadAssistancePlaceDetail(location.id)
            roadDetail = detail
            detailLocation = if (detail != null) {
                location.copy(
                    title = detail.title,
                    description = detail.description ?: location.description,
                    address = detail.address ?: location.address,
                    latitude = detail.latitude,
                    longitude = detail.longitude,
                    phone = detail.phone ?: location.phone,
                    website = detail.website ?: location.website,
                    email = detail.email ?: location.email,
                    services = detail.services.ifEmpty { location.services },
                    imageUrls = location.imageUrls
                )
            } else {
                location
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
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (fetchedImages.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            AsyncImage(
                                model = fetchedImages.first(),
                                contentDescription = detailLocation.title,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp)
                                    .clip(RoundedCornerShape(18.dp)),
                                contentScale = ContentScale.Crop
                            )

                            if (fetchedImages.size > 1) {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 10.dp)
                                ) {
                                    items(fetchedImages.drop(1)) { imageUrl ->
                                        AsyncImage(
                                            model = imageUrl,
                                            contentDescription = detailLocation.title,
                                            modifier = Modifier
                                                .width(110.dp)
                                                .height(80.dp)
                                                .clip(RoundedCornerShape(12.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .padding(8.dp)
                    ) {
                        RoadAssistanceBoundaryMapCard(
                            latitude = detailLocation.latitude.toString(),
                            longitude = detailLocation.longitude.toString(),
                            boundaryPoints = RoadAssistanceBoundaryMapUtils.parseBoundary(roadDetail?.coverageBoundary ?: ""),
                            onBoundaryChanged = {},
                            onLatitudeLongitudeChanged = { _, _ -> },
                            onFullscreenClick = null,
                            modifier = Modifier.fillMaxSize(),
                            mapHeight = 180.dp,
                            title = stringResource(R.string.road_assistance_map_title),
                            showControls = false,
                            showUserLocation = hasLocationPermission
                        )
                    }
                }
            }

            item {
                LocationHeaderCard(
                    title = detailLocation.title,
                    address = detailLocation.address,
                    averageRating = roadDetail?.averageRating ?: 0f,
                    reviewCount = reviewCount,
                    distanceText = distanceText
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                        Text(
                            text = detailLocation.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )

                        detailLocation.address?.let {
                            Text(
                                text = it,
                                modifier = Modifier.padding(top = 6.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        detailLocation.description?.let {
                            Text(
                                text = it,
                                modifier = Modifier.padding(top = 6.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                lineHeight = 22.sp
                            )
                        }

                        if (detailLocation.services.isNotEmpty()) {
                            Text(
                                text = stringResource(R.string.services_label),
                                modifier = Modifier.padding(top = 10.dp),
                                fontWeight = FontWeight.SemiBold
                            )
                            Row(
                                modifier = Modifier.padding(top = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                detailLocation.services.forEach { service ->
                                    Surface(
                                        shape = RoundedCornerShape(999.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        tonalElevation = 0.dp
                                    ) {
                                        Text(
                                            text = service.replace('_', ' '),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
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

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!detailLocation.phone.isNullOrBlank()) {
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${detailLocation.phone}"))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.call_action))
                        }
                    }
                    if (!detailLocation.website.isNullOrBlank()) {
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(detailLocation.website))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.website_action))
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
                        scope.launch {
                            val success = RoadAssistanceApi.submitReview(location.id, selectedRating, reviewComment)
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
                        scope.launch {
                            val success = RoadAssistanceApi.submitOwnerReply(location.id, reviewId, replyText)
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
                    ownerReplyLabel = stringResource(R.string.owner_reply),
                    editReviewText = stringResource(R.string.edit_review),
                    replyButtonText = stringResource(R.string.reply_action),
                    editReplyText = stringResource(R.string.edit_reply),
                    submitReviewText = stringResource(R.string.submit_review),
                    submittingText = stringResource(R.string.moto_status_submitting),
                    selectRatingMessage = selectRatingMessage,
                    reviewSubmittedText = reviewSubmittedText,
                    reviewSubmitFailedText = reviewSubmitFailedText,
                    replySubmitFailedText = replySubmitFailedText
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onEditRoadAssistance,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.moto_action_edit))
                    }
                    Button(
                        onClick = onSuggestRoadAssistanceEdit,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.moto_action_suggest_edit))
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
                            text = stringResource(R.string.road_assistance_support_center),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        detailLocation.phone?.let {
                            Text(text = stringResource(R.string.phone_detail, it), modifier = Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        detailLocation.email?.let {
                            Text(text = stringResource(R.string.email_detail, it), modifier = Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        detailLocation.website?.let {
                            Text(text = stringResource(R.string.website_detail, it), modifier = Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Text(
                            text = stringResource(R.string.coordinates_detail, detailLocation.latitude.toFloat(), detailLocation.longitude.toFloat()),
                            modifier = Modifier.padding(top = 8.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        userDistanceMeters?.let {
                            val distanceText = if (it >= 1000f) {
                                val distanceKm = String.format(java.util.Locale.US, "%.1f", it / 1000f)
                                stringResource(R.string.road_assistance_distance_km, distanceKm)
                            } else {
                                stringResource(R.string.road_assistance_distance_m, it.toInt())
                            }
                            Text(
                                text = distanceText,
                                modifier = Modifier.padding(top = 8.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}
