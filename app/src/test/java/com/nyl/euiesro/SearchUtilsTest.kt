package com.nyl.euiesro

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchUtilsTest {
    @Test
    fun normalize_removes_accents_and_uppercase() {
        assertTrue(SearchUtils.normalize("București").contains("bucuresti"))
        assertTrue(SearchUtils.normalize("ÎNCHISOARE").contains("inchisoare"))
    }

    @Test
    fun matchesSearch_ignores_case_and_accents() {
        assertTrue(SearchUtils.matchesSearch("bucuresti", "București", "Str. Principală"))
        assertTrue(SearchUtils.matchesSearch("vulc", "Vulcanizare auto", "Calea Moșilor"))
        assertTrue(SearchUtils.matchesSearch("langa", "Lângă gară", "Centrul vechi"))
    }

    @Test
    fun matchesSearch_returns_false_for_non_matching_values() {
        assertFalse(SearchUtils.matchesSearch("parc", "București", "Strada Libertății"))
    }
}
