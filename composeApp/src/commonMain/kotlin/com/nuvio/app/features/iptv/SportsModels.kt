package com.nuvio.app.features.iptv

import kotlinx.serialization.Serializable

@Serializable
data class SportEvent(
    val idEvent: String,
    val strEvent: String = "",
    val strSport: String = "",
    val strLeague: String = "",
    val strHomeTeam: String = "",
    val strAwayTeam: String = "",
    val strDate: String = "",
    val strTime: String = "",
    val strThumb: String? = null,
    val strChannel: String = "",
    val intHomeScore: String? = null,
    val intAwayScore: String? = null,
    val strStatus: String? = null,
    val strFilename: String? = null,
    val strVideo: String? = null,
)

data class MatchedSportEvent(
    val event: SportEvent,
    val channel: IptvChannel,
)
