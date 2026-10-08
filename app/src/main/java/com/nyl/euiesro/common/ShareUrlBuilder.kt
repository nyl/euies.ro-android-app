package com.nyl.euiesro.common

import java.text.Normalizer
import java.util.Locale

object ShareUrlBuilder {
    private const val SITE_BASE = "https://euies.ro"

    fun buildFor(location: LocationUiItem): String {
        val path = when (location.section) {
            ModuleSection.EVENTS -> "evenimente"
            ModuleSection.MOTO -> "moto"
            ModuleSection.ROAD_ASSISTANCE -> "asistenta-rutiera"
            ModuleSection.LAUNCH_SPOTS -> "locuri-de-lansare"
            ModuleSection.AUTO -> "auto"
        }
        val slug = slugify(location.title)
        return "$SITE_BASE/$path/$slug-${location.id}"
    }

    fun buildFor(moduleSection: ModuleSection, title: String, id: Int): String {
        val path = when (moduleSection) {
            ModuleSection.EVENTS -> "evenimente"
            ModuleSection.MOTO -> "moto"
            ModuleSection.ROAD_ASSISTANCE -> "asistenta-rutiera"
            ModuleSection.LAUNCH_SPOTS -> "locuri-de-lansare"
            ModuleSection.AUTO -> "auto"
        }
        val slug = slugify(title)
        return "$SITE_BASE/$path/$slug-${id}"
    }

    private fun slugify(value: String): String {
        val source = value.trim()
        if (source.isEmpty()) return "locatie"

        var normalized = Normalizer.normalize(source, Normalizer.Form.NFD)
        normalized = normalized.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
        normalized = normalized.lowercase(Locale.ROOT)
        normalized = normalized
            .replace("ă", "a")
            .replace("â", "a")
            .replace("î", "i")
            .replace("ș", "s")
            .replace("ş", "s")
            .replace("ț", "t")
            .replace("ţ", "t")
            .replace("é", "e")
            .replace("ö", "o")
            .replace("ü", "u")

        val slug = normalized.replace(Regex("[^a-z0-9]+"), "-").trim('-')
        return slug.ifEmpty { "locatie" }
    }
}
