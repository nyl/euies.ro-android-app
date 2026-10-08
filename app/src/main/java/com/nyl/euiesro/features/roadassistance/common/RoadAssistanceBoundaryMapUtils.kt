package com.nyl.euiesro.features.roadassistance.common

import java.util.Locale
import kotlin.math.abs

object RoadAssistanceBoundaryMapUtils {
    fun parseBoundary(rawValue: String?): List<Pair<Double, Double>> {
        val cleanValue = rawValue?.trim().orEmpty()
        if (cleanValue.isEmpty()) return emptyList()

        return cleanValue
            .split('|')
            .mapNotNull { piece ->
                val normalized = piece.trim()
                if (normalized.isEmpty()) return@mapNotNull null

                val candidate = normalized
                    .replace(Regex("^\\s*\\[\\s*\\d+\\s*\\]\\s*"), "")
                    .trim()

                if (candidate.isEmpty()) return@mapNotNull null

                val parts = candidate
                    .split(',')
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }

                if (parts.size < 2) return@mapNotNull null

                val lat = parts[0].toDoubleOrNull() ?: return@mapNotNull null
                val lon = parts[1].toDoubleOrNull() ?: return@mapNotNull null
                val normalizedPoint = normalizePoint(lat, lon)
                if (normalizedPoint == null) return@mapNotNull null
                normalizedPoint
            }
            .distinctBy { (lat, lon) -> "${lat.format()}|${lon.format()}" }
    }

    fun sanitizeBoundaryString(rawValue: String?): String {
        val parsed = parseBoundary(rawValue)
        return serializeBoundary(parsed)
    }

    fun serializeBoundary(points: List<Pair<Double, Double>>): String {
        val normalizedPoints = points.mapNotNull { (lat, lon) -> normalizePoint(lat, lon) }
            .distinctBy { (lat, lon) -> "${lat.format()}|${lon.format()}" }

        return normalizedPoints.mapIndexed { index, pair ->
            val normalized = normalizePoint(pair.first, pair.second) ?: return@mapIndexed ""
            "[${index + 1}]${normalized.first.format()},${normalized.second.format()}"
        }.joinToString("|")
    }

    fun addBoundaryPoint(points: List<Pair<Double, Double>>, lat: Double, lon: Double): List<Pair<Double, Double>> {
        val normalizedPoint = normalizePoint(lat, lon) ?: return points
        if (points.any { it.first.format() == normalizedPoint.first.format() && it.second.format() == normalizedPoint.second.format() }) {
            return points
        }
        return points + normalizedPoint
    }

    fun removeLastBoundaryPoint(points: List<Pair<Double, Double>>): List<Pair<Double, Double>> = points.dropLast(1)

    fun removeBoundaryPoint(points: List<Pair<Double, Double>>, index: Int): List<Pair<Double, Double>> {
        if (index !in points.indices) return points
        return points.filterIndexed { currentIndex, _ -> currentIndex != index }
    }

    fun isBoundaryValid(points: List<Pair<Double, Double>>): Boolean {
        return points.size >= 3 && points.all { (lat, lon) ->
            isFiniteCoordinate(lat) && isFiniteCoordinate(lon) && lat in -90.0..90.0 && lon in -180.0..180.0
        }
    }

    fun boundaryToPolygonCoordinates(points: List<Pair<Double, Double>>): List<Pair<Double, Double>> {
        return points.mapNotNull { (lat, lon) -> normalizePoint(lat, lon) }
    }

    private fun normalizePoint(lat: Double, lon: Double): Pair<Double, Double>? {
        if (!isFiniteCoordinate(lat) || !isFiniteCoordinate(lon)) return null
        val normalizedLat = lat.coerceIn(-90.0, 90.0)
        val normalizedLon = lon.coerceIn(-180.0, 180.0)
        return normalizedLat to normalizedLon
    }

    private fun Double.format(): String = String.format(Locale.US, "%.6f", this)
    private fun isFiniteCoordinate(value: Double): Boolean = !value.isNaN() && !value.isInfinite() && abs(value) <= 1e12
}
