package com.nyl.euiesro

import com.nyl.euiesro.features.auto.AutoPlaceSummary
import com.nyl.euiesro.features.auto.AutoReview
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoReviewStateTest {
    @Test
    fun currentUserReviewAndOwnerChecksWorkCaseInsensitive() {
        val place = AutoPlaceSummary(
            id = 10,
            title = "Test Place",
            description = null,
            address = null,
            latitude = 0.0,
            longitude = 0.0,
            addedBy = "Owner Name",
            reviews = listOf(
                AutoReview(
                    id = 1,
                    userName = "laurentiu",
                    rating = 5,
                    comment = "Nice",
                    createdAt = null
                )
            )
        )

        assertEquals(1, place.currentUserReview("LAURENTIU")?.id)
        assertTrue(place.isCurrentUserOwner("owner name"))
        assertFalse(place.isCurrentUserOwner("guest"))
    }

    @Test
    fun currentUserReviewMatchesDisplayNameAndUsernameVariants() {
        val place = AutoPlaceSummary(
            id = 11,
            title = "Test Place 2",
            description = null,
            address = null,
            latitude = 0.0,
            longitude = 0.0,
            reviews = listOf(
                AutoReview(
                    id = 7,
                    userName = "Test User",
                    rating = 5,
                    comment = "Already reviewed",
                    createdAt = null
                )
            )
        )

        assertEquals(7, place.currentUserReview("testuser")?.id)
        assertEquals(7, place.currentUserReview("Test User")?.id)
    }
}
