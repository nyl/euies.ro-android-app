package com.nyl.euiesro.features.moto

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime

class MotoModelsTest {
    @Test
    fun `after closing time it shows next opening on the next day not the current one`() {
        val hours = mapOf(
            "sambata" to "09:00-17:00",
            "duminica" to "10:00-14:00"
        )

        val status = MotoPlaceSummary(
            id = 1,
            title = "Test",
            description = null,
            address = null,
            latitude = 0.0,
            longitude = 0.0,
            phone = null,
            website = null,
            services = emptyList(),
            workingHours = hours
        ).openingStatusAt("sambata", LocalTime.parse("18:00"), hours)

        assertEquals("Închis", status.label)
        assertEquals("Deschide la 10:00 duminică", status.detail)
    }
}
