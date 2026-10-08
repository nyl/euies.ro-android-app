package com.nyl.euiesro

import com.nyl.euiesro.features.launchspots.LaunchSpotApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LaunchSpotApiTest {
    @Test
    fun parseCreatedSpotId_acceptsApiEnvelope() {
        val wrappedResponse = """
            {
              "ok": true,
              "data": {
                "id": 42,
                "message": "Launch spot created"
              }
            }
        """.trimIndent()

        val parsed = LaunchSpotApi.parseCreatedSpotId(wrappedResponse)
        println("wrapped=$parsed")
        assertEquals(42, parsed)
    }

    @Test
    fun parseCreatedSpotId_acceptsBareRootId() {
        val bareResponse = """
            {
              "id": 7,
              "message": "Launch spot created"
            }
        """.trimIndent()

        val parsed = LaunchSpotApi.parseCreatedSpotId(bareResponse)
        println("bare=$parsed")
        assertEquals(7, parsed)
    }

    @Test
    fun parseLaunchSpotTypeGroups_acceptsWrappedApiEnvelope() {
        val response = """
            {
              "ok": true,
              "data": {
                "groups": [
                  {
                    "category": "Ambarcatiuni nemotorizate",
                    "types": [
                      { "id": 1, "name": "Caiac (Kayak)" },
                      { "id": 2, "name": "Canoe" }
                    ]
                  }
                ]
              }
            }
        """.trimIndent()

        val groups = LaunchSpotApi.parseLaunchSpotTypeGroups(response)
        println("groups=$groups")
        assertEquals(1, groups.size)
        assertEquals("Ambarcatiuni nemotorizate", groups[0].category)
        assertEquals(2, groups[0].options.size)
        assertEquals(1, groups[0].options[0].id)
        assertEquals("Caiac (Kayak)", groups[0].options[0].label)
    }

    @Test
    fun uploadResponseDetection_flagsLoginRedirectAsFailure() {
        val html = """
            <html>
              <body>
                <div class="alert error">Please log in to continue.</div>
              </body>
            </html>
        """.trimIndent()

        assertTrue(LaunchSpotApi.responseIndicatesUploadFailure(html))
    }

    @Test
    fun uploadResponseDetection_acceptsNormalDetailPage() {
        val html = """
            <html>
              <body>
                <h2>Pescăruș</h2>
                <div class="card">Images uploaded successfully.</div>
              </body>
            </html>
        """.trimIndent()

        assertFalse(LaunchSpotApi.responseIndicatesUploadFailure(html))
    }
}
