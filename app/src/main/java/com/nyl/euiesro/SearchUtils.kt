package com.nyl.euiesro

import java.text.Normalizer

object SearchUtils {
    fun normalize(value: String?): String {
        if (value == null) return ""
        return Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace("\\p{Mn}".toRegex(), "")
            .lowercase()
            .trim()
    }

    fun matchesSearch(query: String?, vararg values: String?): Boolean {
        val normalizedQuery = normalize(query)
        if (normalizedQuery.isEmpty()) return true

        return values.any { value ->
            val normalizedValue = normalize(value)
            normalizedValue.contains(normalizedQuery)
        }
    }
}
