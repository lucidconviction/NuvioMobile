package com.nuvio.app.features.sports

data class Sync2CalEvent(
    val id: Long,
    val title: String,
    val startTime: String,
    val endTime: String,
    val allDay: Boolean = false,
    val location: String? = null,
    val description: String? = null,
    val ticketUrl: String? = null,
    val leagueId: String = "",
    val leagueName: String = "",
)

data class Sync2CalCategory(
    val id: Int,
    val name: String,
    val uuid: String,
    val slug: String,
    val breadcrumb: String? = null,
    val parentName: String? = null,
    val addable: Boolean = false,
)

data class Sync2CalTvChannel(
    val name: String,
)

data class Sync2CalLeagueEvents(
    val leagueId: String,
    val leagueName: String,
    val events: List<Sync2CalEvent>,
)
