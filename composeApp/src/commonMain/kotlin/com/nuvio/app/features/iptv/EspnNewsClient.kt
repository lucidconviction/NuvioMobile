package com.nuvio.app.features.iptv

import com.nuvio.app.features.addons.httpGetText
import kotlinx.serialization.json.Json

object EspnNewsClient {
    private val json = Json { ignoreUnknownKeys = true }
    private val newsUrls = listOf(
        "https://site.api.espn.com/apis/site/v2/sports/football/nfl/news",
        "https://site.api.espn.com/apis/site/v2/sports/basketball/nba/news",
        "https://site.api.espn.com/apis/site/v2/sports/baseball/mlb/news",
        "https://site.api.espn.com/apis/site/v2/sports/hockey/nhl/news",
        "https://site.api.espn.com/apis/site/v2/sports/football/college-football/news",
        "https://site.api.espn.com/apis/site/v2/sports/soccer/eng.1/news",
    )

    suspend fun fetchNews(): List<EspnNewsArticle> {
        for (url in newsUrls) {
            val articles = tryFetch(url)
            if (articles.isNotEmpty()) return articles
        }
        return emptyList()
    }

    private suspend fun tryFetch(url: String): List<EspnNewsArticle> {
        return try {
            val response = httpGetText(url)
            val parsed = json.decodeFromString<EspnNewsResponse>(response)
            parsed.articles
        } catch (_: Exception) {
            emptyList()
        }
    }
}
