package com.nyl.euiesro

import com.nyl.euiesro.common.AppScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun app_session_state_round_trips_screen_value() {
        val roundTrip = AppSessionStateManager.deserializeScreen(
            AppSessionStateManager.serializeScreen(AppScreen.MAIN)
        )

        assertEquals(AppScreen.MAIN, roundTrip)
        assertNull(AppSessionStateManager.deserializeScreen("not_a_valid_screen"))
    }

    @Test
    fun session_snapshot_tracks_login_state() {
        val snapshot = AppSessionStateManager.SessionSnapshot(
            screen = AppScreen.MAIN,
            userName = "alice",
            isLoggedIn = true
        )

        assertEquals(AppScreen.MAIN, snapshot.screen)
        assertEquals("alice", snapshot.userName)
        assertTrue(snapshot.isLoggedIn)
        assertFalse(Object().equals(null))
    }

    @Test
    fun cold_start_always_returns_to_main_screen() {
        val session = AppSessionStateManager.SessionSnapshot(
            screen = AppScreen.EDIT_ROAD_ASSISTANCE,
            userName = "alice",
            isLoggedIn = true
        )

        assertEquals(AppScreen.MAIN, AppSessionStateManager.coldStartScreen(session))
        assertNull(AppSessionStateManager.coldStartSelectedSection(session))
    }
}