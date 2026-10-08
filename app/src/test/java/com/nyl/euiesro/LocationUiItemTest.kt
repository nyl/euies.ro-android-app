package com.nyl.euiesro

import com.nyl.euiesro.common.LocationUiItem
import com.nyl.euiesro.common.ModuleSection
import org.junit.Assert.assertEquals
import org.junit.Test

class LocationUiItemTest {
    @Test
    fun `averageCenter returns midpoint of all coordinates`() {
        val items = listOf(
            LocationUiItem(
                id = 1,
                section = ModuleSection.MOTO,
                title = "Moto One",
                description = null,
                address = "Street 1",
                latitude = 44.0,
                longitude = 25.0,
                phone = null,
                website = null,
                services = emptyList(),
                imageUrls = emptyList(),
                typeIds = emptyList(),
                markerIcons = emptyList()
            ),
            LocationUiItem(
                id = 2,
                section = ModuleSection.MOTO,
                title = "Moto Two",
                description = null,
                address = "Street 2",
                latitude = 46.0,
                longitude = 27.0,
                phone = null,
                website = null,
                services = emptyList(),
                imageUrls = emptyList(),
                typeIds = emptyList(),
                markerIcons = emptyList()
            )
        )

        val center = LocationUiItem.averageCenter(items)

        assertEquals(45.0, center.first, 0.0001)
        assertEquals(26.0, center.second, 0.0001)
    }

    @Test
    fun `moto marker icons match the site service icons`() {
        val icons = SiteMarkerIconResolver.resolveMotoMarkerIcons(listOf("magazine", "vulcanizare"))

        assertEquals(listOf("🏍️", "🛞"), icons)
    }

    @Test
    fun `relative media paths are normalized to the site base url`() {
        val absoluteUrl = MediaUrlResolver.resolve("/data/uploads/events/event.jpg")
        val remoteUrl = MediaUrlResolver.resolve("https://cdn.example.com/event.jpg")

        assertEquals("https://euies.ro/data/uploads/events/event.jpg", absoluteUrl)
        assertEquals("https://cdn.example.com/event.jpg", remoteUrl)
    }

    @Test
    fun `custom organizer names override the default user name when non-user organizer is selected`() {
        assertEquals(
            "Moto Club X",
            EventOrganizerResolver.resolve(
                customName = "Moto Club X",
                isCustomOrganizer = true,
                userName = "Laurentiu",
                username = "nyl"
            )
        )
        assertEquals(
            "Laurentiu",
            EventOrganizerResolver.resolve(
                customName = "",
                isCustomOrganizer = false,
                userName = "Laurentiu",
                username = "nyl"
            )
        )
    }
}
