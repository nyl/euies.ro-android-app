package com.nyl.euiesro.app

import com.nyl.euiesro.ApiClient

object MediaUrlResolver {
    fun resolve(rawUrl: String?): String? {
        val value = rawUrl?.trim() ?: return null
        if (value.isEmpty()) return null
        return when {
            value.startsWith("http://", true) || value.startsWith("https://", true) -> value
            value.startsWith("/", true) -> "${ApiClient.baseUrl.trimEnd('/')}$value"
            else -> value
        }
    }
}
