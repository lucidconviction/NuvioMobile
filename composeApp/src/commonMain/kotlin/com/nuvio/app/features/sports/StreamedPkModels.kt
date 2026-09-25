package com.nuvio.app.features.sports

import com.nuvio.app.features.addons.httpGetTextWithHeaders
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray

@Serializable
data class StreamedPkMatch(
    val id: String,
    val title: String,
    val category: String,
    val date: Long,
    val poster: String? = null,
    val popular: Boolean = false,
    val teams: StreamedPkTeams? = null,
    val sources: List<StreamedPkSource> = emptyList(),
) {
    val startEpochMs: Long = date
    val isLive: Boolean
        get() {
            val now = System.currentTimeMillis()
            return now >= startEpochMs - 3600000 && now < startEpochMs + 7200000
        }
}

@Serializable
data class StreamedPkTeams(
    val home: StreamedPkTeam? = null,
    val away: StreamedPkTeam? = null,
)

@Serializable
data class StreamedPkTeam(
    val name: String,
    val badge: String? = null,
)

@Serializable
data class StreamedPkSource(
    val source: String,
    val id: String,
)

@Serializable
data class StreamedPkStream(
    val id: String,
    val streamNo: Int,
    val language: String,
    val hd: Boolean,
    val embedUrl: String,
    val source: String,
)