package com.nyl.euiesro

import com.nyl.euiesro.common.CurrentUserProfile
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CurrentUserProfileTest {
    @Test
    fun canEditLaunchSpot_allowsAdminRoleEvenWhenCanModerateIsFalse() {
        val profile = CurrentUserProfile(id = 12, username = "admin", role = "admin", canModerate = false)

        assertTrue(profile.canEditLaunchSpot(99))
    }

    @Test
    fun canEditLaunchSpot_rejectsNonOwnerNonModerator() {
        val profile = CurrentUserProfile(id = 12, username = "user", role = "user", canModerate = false)

        assertFalse(profile.canEditLaunchSpot(99))
    }
}
