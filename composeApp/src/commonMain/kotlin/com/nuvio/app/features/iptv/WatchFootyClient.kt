package com.nuvio.app.features.iptv

import com.nuvio.app.features.addons.httpGetText
import kotlinx.serialization.json.Json

object WatchFootyClient {
    private val json = Json { ignoreUnknownKeys = true }
    private const val BASE = "https://api.watchfooty.st/api/v1"

    suspend fun fetchNews(): List<WatchFootyNewsArticle> {
        return try {
            val response = httpGetText("$BASE/news")
            val parsed = json.decodeFromString<WatchFootyNewsResponse>(response)
            parsed.articles
        } catch (_: Exception) {
            emptyList()
        }
    }
}
