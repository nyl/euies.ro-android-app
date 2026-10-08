package com.nyl.euiesro.features.moto

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

internal data class AddressSuggestion(
    val displayName: String,
    val latitude: Double,
    val longitude: Double
)

internal suspend fun geocodeAddress(query: String): Pair<Double, Double>? = withContext(Dispatchers.IO) {
    try {
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        val url = URL("https://nominatim.openstreetmap.org/search?format=jsonv2&limit=1&q=$encoded")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        connection.setRequestProperty("User-Agent", "EuIesro/1.0")
        connection.setRequestProperty("Accept", "application/json")

        if (connection.responseCode !in 200..299) {
            Log.e("MotoAddressSearch", "Geocode failed: ${connection.responseCode}")
            return@withContext null
        }

        val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        val array = JSONArray(body)
        if (array.length() == 0) return@withContext null
        val item = array.getJSONObject(0)
        val lat = item.optDouble("lat", Double.NaN)
        val lon = item.optDouble("lon", Double.NaN)
        if (lat.isNaN() || lon.isNaN()) return@withContext null
        lat to lon
    } catch (e: Exception) {
        Log.e("MotoAddressSearch", "geocodeAddress failed", e)
        null
    }
}

internal suspend fun searchAddressSuggestions(query: String): List<AddressSuggestion> = withContext(Dispatchers.IO) {
    if (query.trim().length < 2) return@withContext emptyList()

    try {
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        val url = URL("https://nominatim.openstreetmap.org/search?format=jsonv2&limit=5&q=$encoded")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        connection.setRequestProperty("User-Agent", "EuIesro/1.0")
        connection.setRequestProperty("Accept", "application/json")

        if (connection.responseCode !in 200..299) {
            return@withContext emptyList()
        }

        val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        val array = JSONArray(body)
        val results = mutableListOf<AddressSuggestion>()
        for (i in 0 until array.length()) {
            val item = array.getJSONObject(i)
            val displayName = item.optString("display_name", "").trim()
            val lat = item.optDouble("lat", Double.NaN)
            val lon = item.optDouble("lon", Double.NaN)
            if (displayName.isNotBlank() && !lat.isNaN() && !lon.isNaN()) {
                results.add(AddressSuggestion(displayName, lat, lon))
            }
        }
        results
    } catch (e: Exception) {
        Log.e("MotoAddressSearch", "searchAddressSuggestions failed", e)
        emptyList()
    }
}
