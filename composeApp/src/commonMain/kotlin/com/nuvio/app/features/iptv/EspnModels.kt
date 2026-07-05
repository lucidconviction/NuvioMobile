package com.nuvio.app.features.iptv

import kotlinx.serialization.Serializable

@Serializable
data class EspnResponse(val events: List<EspnEvent> = emptyList())

@Serializable
data class EspnEvent(
    val id: String = "",
    val name: String = "",
    val shortName: String = "",
    val date: String = "",
    val competitions: List<EspnCompetition> = emptyList(),
)

@Serializable
data class EspnCompetition(
    val id: String = "",
    val date: String = "",
    val venue: EspnVenue? = null,
    val competitors: List<EspnCompetitor> = emptyList(),
    val broadcasts: List<EspnBroadcast>? = null,
    val status: EspnStatus? = null,
    val series: EspnSeries? = null,
    val notes: List<EspnNote>? = null,
)

@Serializable
data class EspnVenue(
    val fullName: String = "",
    val address: EspnAddress? = null,
)

@Serializable
data class EspnAddress(val city: String = "", val state: String = "")

@Serializable
data class EspnCompetitor(
    val team: EspnTeam? = null,
    val score: String? = null,
    val homeAway: String = "",
    val winner: Boolean = false,
    val records: List<EspnRecord>? = null,
)

@Serializable
data class EspnTeam(
    val id: String = "",
    val name: String = "",
    val abbreviation: String = "",
    val displayName: String = "",
    val logo: String? = null,
    val color: String? = null,
    val location: String = "",
    val nickname: String = "",
)

@Serializable
data class EspnRecord(
    val name: String = "",
    val summary: String = "",
)

@Serializable
data class EspnBroadcast(
    val market: String? = null,
    val names: List<String>? = null,
)

@Serializable
data class EspnStatus(
    val type: EspnStatusType? = null,
)

@Serializable
data class EspnStatusType(
    val name: String = "",
    val detail: String = "",
    val description: String = "",
    val state: String = "",
    val completed: Boolean = false,
)

@Serializable
data class EspnSeries(
    val title: String = "",
    val summary: String = "",
)

@Serializable
data class EspnNote(
    val headline: String = "",
    val type: String = "",
)

data class EspnProcessedEvent(
    val id: String,
    val title: String,
    val homeTeam: String,
    val awayTeam: String,
    val homeScore: String?,
    val awayScore: String?,
    val homeLogo: String?,
    val awayLogo: String?,
    val channel: String,
    val status: String,
    val detail: String,
    val date: String,
    val sport: String,
    val league: String,
    val isLive: Boolean,
    val isPpv: Boolean,
)
