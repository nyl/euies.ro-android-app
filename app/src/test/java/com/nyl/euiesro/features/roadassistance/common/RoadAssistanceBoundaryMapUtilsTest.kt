package com.nyl.euiesro.features.roadassistance.common

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoadAssistanceBoundaryMapUtilsTest {
    @Test
    fun parseBoundaryString_returnsNormalizedPoints() {
        val points = RoadAssistanceBoundaryMapUtils.parseBoundary("[1]45.123456,24.123456|[2]45.234567,24.234567|[3]45.345678,24.345678")

        assertEquals(listOf(
            45.123456 to 24.123456,
            45.234567 to 24.234567,
            45.345678 to 24.345678
        ), points)
    }

    @Test
    fun serializeBoundaryString_usesPhpCompatibleFormat() {
        val value = RoadAssistanceBoundaryMapUtils.serializeBoundary(listOf(
            45.123456 to 24.123456,
            45.234567 to 24.234567,
            45.345678 to 24.345678
        ))

        assertEquals("[1]45.123456,24.123456|[2]45.234567,24.234567|[3]45.345678,24.345678", value)
    }

    @Test
    fun isBoundaryValid_acceptsFiniteCoordinates() {
        val valid = RoadAssistanceBoundaryMapUtils.isBoundaryValid(listOf(
            44.0 to 23.0,
            45.0 to 24.0,
            46.0 to 25.0
        ))

        assertTrue(valid)
    }

    @Test
    fun serializeBoundary_clampsOutOfRangeCoordinates() {
        val value = RoadAssistanceBoundaryMapUtils.serializeBoundary(listOf(
            44.0 to 37630.0,
            45.0 to -99999.0
        ))

        assertEquals("[1]44.000000,180.000000|[2]45.000000,-180.000000", value)
    }

    @Test
    fun sanitizeBoundaryString_discardsMalformedPointsAndKeepsPhPCompatibleFormat() {
        val value = RoadAssistanceBoundaryMapUtils.sanitizeBoundaryString(
            "[1]45.123456,24.123456|bad|[3]45.345678,24.345678|[4]45.345678,24.345678|[5]400.0,300.0"
        )

        assertEquals("[1]45.123456,24.123456|[2]45.345678,24.345678|[3]90.000000,180.000000", value)
    }

    @Test
    fun parseBoundary_acceptsPhpIndexedFormat() {
        val points = RoadAssistanceBoundaryMapUtils.parseBoundary(
            "[1]44.376830,26.291853|[2]44.313976,26.176496|[3]44.294320,26.028181"
        )

        assertEquals(
            listOf(
                44.37683 to 26.291853,
                44.313976 to 26.176496,
                44.29432 to 26.028181
            ),
            points
        )
    }

    @Test
    fun serializeBoundary_usesDotDecimalSeparatorIndependentOfLocale() {
        Locale.setDefault(Locale.FRANCE)

        val value = RoadAssistanceBoundaryMapUtils.serializeBoundary(listOf(
            44.37683 to 26.291853,
            44.313976 to 26.176496
        ))

        assertEquals("[1]44.376830,26.291853|[2]44.313976,26.176496", value)
    }
}
