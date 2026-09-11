package com.nuvio.app.features.sports

object Sync2CalMappings {
    data class Sync2CalLeague(
        val leagueId: String,
        val sync2calSlug: String,
        val teamSlugPrefix: String = "",
    )

    val leagueMappings: List<Sync2CalLeague> = listOf(
        Sync2CalLeague("nfl", "sports/football/nfl", "sports/football/nfl/"),
        Sync2CalLeague("nba", "sports/basketball/nba", "sports/basketball/nba/"),
        Sync2CalLeague("mlb", "sports/baseball/mlb", "sports/baseball/mlb/"),
        Sync2CalLeague("nhl", "sports/hockey/nhl", "sports/hockey/nhl/"),
        Sync2CalLeague("ufc", "sports/fighting/ufc"),
        Sync2CalLeague("boxing", "sports/fighting/boxing"),
        Sync2CalLeague("pfl", "sports/fighting/pfl"),
        Sync2CalLeague("bkfc", "sports/fighting/bkfc"),
        Sync2CalLeague("mls", "sports/soccer/usa/mls", "sports/soccer/usa/mls/"),
        Sync2CalLeague("ucl", "sports/soccer/europe/champions-league"),
        Sync2CalLeague("f1", "sports/racing/f1"),
        Sync2CalLeague("tennis", "sports/tennis/atp"),
        Sync2CalLeague("golf", "sports/golf/pga-tour"),
        Sync2CalLeague("cfb", "sports/football/ncaa-football", "sports/football/ncaa-football/"),
        Sync2CalLeague("cbb", "sports/basketball/ncaa-basketball", "sports/basketball/ncaa-basketball/"),
        Sync2CalLeague("wnba", "sports/basketball/wnba", "sports/basketball/wnba/"),
    )

    private val slugToLeagueId = leagueMappings.associate { it.sync2calSlug to it.leagueId }

    fun leagueIdFromSlug(slug: String): String? {
        return slugToLeagueId.entries.firstOrNull { slug.startsWith(it.key) }?.value
    }

    fun leagueNameFromId(leagueId: String): String {
        return when (leagueId) {
            "nfl" -> "NFL"; "nba" -> "NBA"; "mlb" -> "MLB"; "nhl" -> "NHL"
            "ufc" -> "UFC"; "boxing" -> "Boxing"; "pfl" -> "PFL"
            "mls" -> "MLS"
            "ucl" -> "Champions League"; "f1" -> "Formula 1"
            "tennis" -> "Tennis"; "golf" -> "Golf"
            "cfb" -> "College Football"; "cbb" -> "College Basketball"
            "wnba" -> "WNBA"
            else -> leagueId.uppercase()
        }
    }
}
