package com.nuvio.app.features.iptv

import com.nuvio.app.features.addons.httpGetText
import com.nuvio.app.features.trakt.TraktPlatformClock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class TvMazeEpisode(
    val id: Long = 0,
    val name: String = "",
    val season: Int = 0,
    val number: Int? = null,
    val airdate: String = "",
    val airtime: String = "",
    val runtime: Int? = null,
    val show: TvMazeShow? = null,
)

@Serializable
data class TvMazeShow(
    val id: Long = 0,
    val name: String = "",
    val type: String = "",
    val language: String = "",
    val genres: List<String> = emptyList(),
    val status: String? = null,
    val rating: TvMazeRating? = null,
    val summary: String? = null,
    val image: TvMazeImage? = null,
    val network: TvMazeNetwork? = null,
    val webChannel: TvMazeNetwork? = null,
)

@Serializable
data class TvMazeRating(val average: Double? = null)

@Serializable
data class TvMazeImage(val medium: String? = null, val original: String? = null)

@Serializable
data class TvMazeNetwork(val name: String = "", val country: TvMazeCountry? = null)

@Serializable
data class TvMazeCountry(val name: String = "", val code: String = "")

data class MatchedTvShow(
    val episode: TvMazeEpisode,
    val channel: IptvChannel,
)

object TvMazeClient {
    private const val BASE = "https://api.tvmaze.com"
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    suspend fun fetchSchedule(country: String = "US"): List<TvMazeEpisode> {
        val response = httpGetText("$BASE/schedule?country=$country")
        val all = json.decodeFromString<List<TvMazeEpisode>>(response)
        return filterCurrentAndUpcoming(all)
    }

    private fun filterCurrentAndUpcoming(episodes: List<TvMazeEpisode>): List<TvMazeEpisode> {
        val nowMinutes = (System.currentTimeMillis() / 60000).toInt()
        val nowHour = nowMinutes / 60 % 24
        val nowMin = nowMinutes % 60
        val nowTotal = nowHour * 60 + nowMin

        return episodes.filter { ep ->
            val parts = ep.airtime.split(":").map { it.toIntOrNull() ?: 0 }
            val epStart = parts.getOrElse(0) { 0 } * 60 + parts.getOrElse(1) { 0 }
            val epEnd = epStart + (ep.runtime ?: 30).coerceIn(15, 240)
            val withinWindow = epEnd >= nowTotal && epStart <= nowTotal + 360
            withinWindow
        }
    }

    fun matchToChannels(
        episodes: List<TvMazeEpisode>,
        channels: List<IptvChannel>,
    ): List<MatchedTvShow> {
        val matched = mutableListOf<MatchedTvShow>()
        for (ep in episodes) {
            val networkName = ep.show?.network?.name?.takeIf { it.isNotBlank() }
                ?: ep.show?.webChannel?.name?.takeIf { it.isNotBlank() } ?: continue
            val apiName = networkName.lowercase().trim()
            val apiWords = apiName.split(" ").filter { it.length > 2 }
            val found = channels.firstOrNull { ch ->
                val chName = ch.name.lowercase().trim()
                chName.contains(apiName) || apiName.contains(chName) ||
                apiWords.any { word -> chName.contains(word) } ||
                chName.split(" ").any { word -> word.length > 2 && apiName.contains(word) }
            }
            if (found != null) {
                matched.add(MatchedTvShow(episode = ep, channel = found))
            }
        }
        return matched
    }
}
