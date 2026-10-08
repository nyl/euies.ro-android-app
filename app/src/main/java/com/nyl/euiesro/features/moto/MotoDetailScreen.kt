package com.nyl.euiesro.features.moto

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.nyl.euiesro.R
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.nyl.euiesro.calculateDistanceMeters
import com.nyl.euiesro.formatDistanceLabel
import com.nyl.euiesro.resolveBestCurrentLocationForDisplay
import coil.compose.AsyncImage
import com.nyl.euiesro.ApiClient
import com.nyl.euiesro.EuIesroTopAppBar
import com.nyl.euiesro.InteractiveLocationMap
import com.nyl.euiesro.common.CurrentUserProfile
import com.nyl.euiesro.common.ShareUrlBuilder
import kotlinx.coroutines.launch
import com.nyl.euiesro.common.LocationHeaderCard
import com.nyl.euiesro.common.LocationUiItem
import com.nyl.euiesro.common.ModuleSection
import com.nyl.euiesro.common.ReviewSection
import com.nyl.euiesro.features.moto.components.MotoGalleryCard
import com.nyl.euiesro.features.moto.components.MotoInfoCard
import com.nyl.euiesro.features.moto.components.MotoMapCard

@Composable
fun MotoDetailScreen(
    location: LocationUiItem,
    onBack: () -> Unit,
    currentUserName: String = "Guest",
    currentUserProfile: CurrentUserProfile = CurrentUserProfile(),
    onEditMotoPlace: () -> Unit = {},
    onSuggestMotoEdit: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val fetchedImages = remember { mutableStateListOf<String>() }
    var detailLocation by remember(location.id) { mutableStateOf(location) }
    var motoDetail by remember(location.id) { mutableStateOf<MotoPlaceSummary?>(null) }
    var showFullscreenMap by remember { mutableStateOf(false) }
    var isUploadingImages by remember { mutableStateOf(false) }
    var uploadMessage by remember { mutableStateOf<String?>(null) }
    var userIsLoggedIn by remember(currentUserName) { mutableStateOf(currentUserName.isNotBlank() && currentUserName != "Guest") }
    var selectedRating by remember(location.id) { mutableStateOf(0) }
    var reviewComment by remember(location.id) { mutableStateOf("") }
    var isSubmittingReview by rememberSaveable { mutableStateOf(false) }
    var reviewMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var showReviewForm by rememberSaveable { mutableStateOf(false) }
    var reviewFilter by rememberSaveable { mutableStateOf("all") }
    var replyReviewId by rememberSaveable { mutableStateOf<Int?>(null) }
    val ownerReplyDrafts = remember { mutableStateMapOf<Int, String>() }
    val ownerUserId = motoDetail?.ownerUserId ?: motoDetail?.userId ?: 0
    val canEditPlace = currentUserProfile.canEditMotoPlace(ownerUserId)
    val isOwner = currentUserProfile.id > 0 && (currentUserProfile.id == ownerUserId || currentUserProfile.canEditMotoPlace(ownerUserId))
    val currentUserReview = motoDetail?.currentUserReview(currentUserName)
    val reviewCount = motoDetail?.reviews?.size ?: 0
    val reviewTabOptions = listOf(
        "All" to "all",
        "5★" to "5",
        "4★" to "4",
        "3★" to "3",
        "2★" to "2",
        "1★" to "1"
    )
    val filteredReviews = when (reviewFilter) {
        "5" -> motoDetail?.reviews?.filter { it.rating == 5 } ?: emptyList()
        "4" -> motoDetail?.reviews?.filter { it.rating == 4 } ?: emptyList()
        "3" -> motoDetail?.reviews?.filter { it.rating == 3 } ?: emptyList()
        "2" -> motoDetail?.reviews?.filter { it.rating == 2 } ?: emptyList()
        "1" -> motoDetail?.reviews?.filter { it.rating == 1 } ?: emptyList()
        else -> motoDetail?.reviews ?: emptyList()
    }
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        showFullscreenMap = true
    }

    fun refreshDetail() {
        scope.launch {
            val detail = MotoApi.fetchMotoPlaceDetail(location.id)
            motoDetail = detail
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
                    imageUrls = detail.images.ifEmpty { location.imageUrls }
                )
            } else {
                location
            }
            fetchedImages.clear()
            fetchedImages.addAll(detail?.images ?: location.imageUrls)
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { imageUris ->
        if (imageUris.isEmpty()) return@rememberLauncherForActivityResult
        scope.launch {
            isUploadingImages = true
            uploadMessage = null
            val success = MotoApi.uploadMotoPlaceImages(context, location.id, imageUris)
            if (success) {
                uploadMessage = context.getString(R.string.moto_upload_success)
                refreshDetail()
            } else {
                uploadMessage = context.getString(R.string.moto_upload_failed)
            }
            isUploadingImages = false
        }
    }

    LaunchedEffect(location.id) {
        fetchedImages.clear()
        val detail = MotoApi.fetchMotoPlaceDetail(location.id)
        motoDetail = detail
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
                imageUrls = detail.images.ifEmpty { location.imageUrls }
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
    val workingHours = motoDetail?.workingHours ?: emptyMap()
    val uploadSuccessText = stringResource(R.string.moto_upload_success)
    val uploadFailedText = stringResource(R.string.moto_upload_failed)
    val reviewRequiredText = stringResource(R.string.moto_error_select_rating)
    val reviewSubmittedText = stringResource(R.string.moto_review_submitted)
    val reviewSubmitFailedText = stringResource(R.string.moto_review_submit_failed)
    val replySubmitFailedText = stringResource(R.string.moto_reply_submit_failed)

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
                    MotoGalleryCard(
                        images = fetchedImages,
                        title = detailLocation.title
                    )
                }
            }

            item {
                LocationHeaderCard(
                    title = detailLocation.title,
                    address = detailLocation.address,
                    averageRating = motoDetail?.averageRating ?: 0f,
                    reviewCount = reviewCount,
                    distanceText = distanceText
                )
            }

            item {
                MotoMapCard(
                    location = detailLocation,
                    onFullscreenClick = {
                        val fineGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        val coarseGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        hasLocationPermission = fineGranted || coarseGranted
                        showFullscreenMap = true
                        if (!hasLocationPermission) {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    },
                    onUseMyLocation = {
                        val fineGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        val coarseGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        hasLocationPermission = fineGranted || coarseGranted
                        if (!hasLocationPermission) {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                            return@MotoMapCard
                        }
                        showFullscreenMap = true
                    }
                )
            }

            if (userIsLoggedIn) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (canEditPlace) {
                            Button(onClick = onEditMotoPlace) {
                                Text(stringResource(R.string.moto_action_edit))
                            }
                        } else {
                            Button(onClick = onSuggestMotoEdit) {
                                Text(stringResource(R.string.moto_action_suggest_edit))
                            }
                        }
                        Button(
                            onClick = { imagePickerLauncher.launch("image/*") },
                            enabled = !isUploadingImages
                        ) {
                            Text(if (isUploadingImages) stringResource(R.string.moto_status_uploading) else stringResource(R.string.moto_action_add_images))
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

            item {
                MotoInfoCard(
                    location = detailLocation,
                    title = detailLocation.title
                )
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
                            reviewMessage = reviewRequiredText
                            return@ReviewSection
                        }
                        isSubmittingReview = true
                        reviewMessage = null
                        scope.launch {
                            val success = MotoApi.submitReview(location.id, selectedRating, reviewComment)
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
                            val success = MotoApi.submitOwnerReply(location.id, reviewId, replyText)
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
                    selectRatingMessage = reviewRequiredText,
                    reviewSubmittedText = reviewSubmittedText,
                    reviewSubmitFailedText = reviewSubmitFailedText,
                    replySubmitFailedText = replySubmitFailedText
                )
            }

            if (workingHours.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = stringResource(R.string.working_hours),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            workingHours.entries.forEach { (day, value) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = day.replaceFirstChar { it.titlecase() }, modifier = Modifier.weight(1f))
                                    Surface(
                                        shape = RoundedCornerShape(999.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = value,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
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
                            text = stringResource(R.string.coordinates_detail, detailLocation.latitude, detailLocation.longitude),
                            modifier = Modifier.padding(top = 8.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }

    if (showFullscreenMap) {
        val activity = context as? Activity
        @Suppress("DEPRECATION")
        DisposableEffect(activity) {
            if (activity == null) {
                onDispose { }
            } else {
                val window = activity.window
                val previousStatusColor = window.statusBarColor
                val previousNavigationColor = window.navigationBarColor
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                val previousLightStatus = insetsController.isAppearanceLightStatusBars
                val previousLightNav = insetsController.isAppearanceLightNavigationBars

                window.statusBarColor = Color.Black.toArgb()
                window.navigationBarColor = Color.Black.toArgb()
                insetsController.isAppearanceLightStatusBars = false
                insetsController.isAppearanceLightNavigationBars = false

                onDispose {
                    window.statusBarColor = previousStatusColor
                    window.navigationBarColor = previousNavigationColor
                    insetsController.isAppearanceLightStatusBars = previousLightStatus
                    insetsController.isAppearanceLightNavigationBars = previousLightNav
                }
            }
        }

        Dialog(
            onDismissRequest = { showFullscreenMap = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
            ) {
                InteractiveLocationMap(
                    items = listOf(detailLocation),
                    modifier = Modifier.fillMaxSize(),
                    interactive = true,
                    showUserLocation = hasLocationPermission
                )

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    tonalElevation = 6.dp,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.map_overview),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            onClick = { showFullscreenMap = false },
                            shape = RoundedCornerShape(999.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = stringResource(R.string.exit_fullscreen),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}
