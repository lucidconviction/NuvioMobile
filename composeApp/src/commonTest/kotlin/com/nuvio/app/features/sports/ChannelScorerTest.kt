package com.nuvio.app.features.sports

import com.nuvio.app.features.iptv.EspnProcessedEvent
import com.nuvio.app.features.iptv.IptvChannel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ChannelTextTest {

    @Test
    fun foldsAccents() {
        assertEquals("sao paulo", ChannelText.normalize("São Paulo"))
        assertEquals("barcelona", ChannelText.normalize("Barcelona"))
        assertEquals("barca", ChannelText.normalize("Barça"))
        assertEquals("real madrid", ChannelText.normalize("Real Madrid"))
        assertEquals("dortmund", ChannelText.normalize("Dortmund"))
    }

    @Test
    fun lowercasesAndCollapsesWhitespace() {
        assertEquals("espn 2 hd", ChannelText.normalize("  ESPN   2   HD  "))
    }

    @Test
    fun stripsQualitySuffix() {
        assertEquals("espn", ChannelText.channelNameKey("ESPN  HD"))
        assertEquals("sky sports", ChannelText.channelNameKey("Sky Sports FHD"))
        assertEquals("futbol tv", ChannelText.channelNameKey("Fútbol TV 4K"))
        assertEquals("bein sport 1", ChannelText.channelNameKey("beIN Sport 1 50fps"))
        assertEquals("nba tv", ChannelText.channelNameKey("NBA TV"))
    }
}

class TeamNameKeysTest {

    @Test
    fun expandsFullNameFirst() {
        assertEquals("arsenal", TeamNameKeys.expand("Arsenal", "eng.1").first())
    }

    @Test
    fun stripsTrailingClubSuffix() {
        val keys = TeamNameKeys.expand("Real Betis FC", "esp.1")
        assertTrue(keys.contains("real betis"))
    }

    @Test
    fun stripsLeadingClubPrefix() {
        val keys = TeamNameKeys.expand("FC Barcelona", "esp.1")
        assertTrue(keys.contains("barcelona"))
    }

    @Test
    fun firstWordFallbackForManchesterWeirdness() {
        val keys = TeamNameKeys.expand("Manchester United", "eng.1")
        assertTrue(keys.contains("manchester united"))
        assertTrue(keys.contains("manchester"))
    }

    @Test
    fun ambiguousFirstWordIsGuarded() {
        val keys = TeamNameKeys.expand("Real Madrid", "esp.1")
        assertTrue(keys.contains("real madrid"))
        assertFalse(keys.contains("real"))
    }

    @Test
    fun usLeaguesGetLastToken() {
        val keys = TeamNameKeys.expand("New York Yankees", "mlb")
        assertTrue(keys.contains("yankees"))
        val nba = TeamNameKeys.expand("Los Angeles Lakers", "nba")
        assertTrue(nba.contains("lakers"))
    }

    @Test
    fun soccerDoesNotGetLastToken() {
        val keys = TeamNameKeys.expand("Newcastle United", "eng.1")
        assertFalse(keys.contains("united"))
    }
}

class ChannelScorerTest {

    private fun event(home: String, away: String, league: String = "nba") = EspnProcessedEvent(
        id = "1", title = "$home vs $away", homeTeam = home, awayTeam = away,
        homeScore = null, awayScore = null, homeLogo = null, awayLogo = null,
        channel = "", status = "", detail = "", date = "", sport = "Basketball",
        league = league, isLive = true, isPpv = false,
    )

    private fun channel(name: String, group: String? = null, url: String = "http://x/$name") = IptvChannel(
        id = name, name = name, group = group, url = url,
        sourceType = com.nuvio.app.features.iptv.SourceType.M3U, sourceId = "src",
    )

    @Test
    fun bothTeamsOutranksLoneTeam() {
        val ev = event("Los Angeles Lakers", "Golden State Warriors")
        val realChannel = channel("Lakers Warriors", "Sports")
        val generic = channel("ESPN", "Sports")
        val results = ChannelScorer.scoreChannels(ev, listOf(generic, realChannel))
        assertEquals(realChannel.name, results.first().channel.name)
        assertTrue(results.first().score > results.last().score)
    }

    @Test
    fun realBroadcasterWithEpgBeatsRandomEspn() {
        val ev = event("Boston Celtics", "Miami Heat")
        val epgLookup = ChannelScorer.buildEpgLookup(mapOf("espn2" to "Boston Celtics vs Miami Heat"))
        val espn2 = channel("ESPN2 HD", "Sports")
        val unrelatedEspn = channel("ESPN", "Sports")
        val results = ChannelScorer.scoreChannels(ev, listOf(unrelatedEspn, espn2), epgLookup)
        assertEquals(espn2.name, results.first().channel.name)
    }

    @Test
    fun blankChannelStillMatches() {
        val ev = event("UFC 310", "Jon Jones vs Alex Pereira", "ufc").let {
            it.copy(
                homeTeam = "Jon Jones",
                awayTeam = "Alex Pereira",
                title = "Jones vs Pereira",
            )
        }
        val ppv = channel("DAZN PPV", "Fight")
        val results = ChannelScorer.scoreChannels(ev, listOf(ppv))
        assertTrue(results.isNotEmpty())
    }

    @Test
    fun nonSportsChannelSkipped() {
        val ev = event("Los Angeles Lakers", "Golden State Warriors")
        val news = channel("MSNBC", "News")
        val results = ChannelScorer.scoreChannels(ev, listOf(news))
        assertTrue(results.isEmpty())
    }

    @Test
    fun ambiguousCityWordDoesNotMatch() {
        val ev = event("Manchester City", "Chelsea", "eng.1")
        val city = channel("City", "UK")
        // "city" alone is not the team: full "manchester city" must appear in the name
        val results = ChannelScorer.scoreChannels(ev, listOf(city))
        assertTrue(results.isEmpty())
    }

    @Test
    fun accentedTeamMatchesAccentlessChannel() {
        val ev = event("São Paulo", "Flamengo", "bra.1")
        val ch = channel("Sao Paulo FC HD", "Brasil")
        val results = ChannelScorer.scoreChannels(ev, listOf(ch))
        assertTrue(results.isNotEmpty())
        assertEquals(MatchType.TEAM, results.first().matchType)
    }

    @Test
    fun epgSignalFlowsThroughMatchChannelsAndBoostsRanking() {
        val ev = event("Boston Celtics", "Miami Heat")
        val espn2 = channel("ESPN2 HD", "Sports")
        val unrelated = channel("ESPN", "Sports")
        val epgLookup = ChannelScorer.buildEpgLookup(
            mapOf("espn2" to "Boston Celtics vs Miami Heat"),
        )
        val results = GameToChannelMatcher.matchChannels(
            event = ev,
            channels = listOf(espn2, unrelated),
            currentEpgTitleFor = epgLookup,
        )
        assertEquals(espn2.name, results.first().channel.name)
        assertTrue(results.first().score > results.last().score)
    }

    @Test
    fun matchChannelsCarriesScoreAndReasons() {
        val ev = event("Los Angeles Lakers", "Golden State Warriors")
        val realChannel = channel("Lakers Warriors", "Sports")
        val matches = GameToChannelMatcher.matchChannels(ev, listOf(realChannel))
        assertEquals(1, matches.size)
        assertTrue(matches.first().score > 0)
        assertTrue(matches.first().reasons.isNotEmpty())
    }

    @Test
    fun sameStreamUrlIsDeduplicatedAcrossSources() {
        val ev = event("Los Angeles Lakers", "Golden State Warriors")
        val a = channel("Lakers Warriors HD", "Sports", url = "http://cdn/feed")
        val b = channel("Lakers Warriors", "Sports", url = "http://cdn/feed")
        val results = ChannelScorer.scoreChannels(ev, listOf(a, b))
        assertEquals(1, results.size)
    }

    @Test
    fun distinctUrlsAreKept() {
        val ev = event("Los Angeles Lakers", "Golden State Warriors")
        val a = channel("Lakers", "Sports", url = "http://cdn/one")
        val b = channel("Lakers 2", "Sports", url = "http://cdn/two")
        val results = ChannelScorer.scoreChannels(ev, listOf(a, b))
        assertEquals(2, results.size)
    }

    @Test
    fun strongSingleMatchAutoplays() {
        val ev = event("Los Angeles Lakers", "Golden State Warriors")
        val lakers = channel("Lakers Warriors", "Sports")
        val scored = ChannelScorer.scoreChannels(ev, listOf(lakers))
        assertEquals(lakers.name, ChannelScorer.autoplayCandidate(scored)?.channel?.name)
    }

    @Test
    fun weakLeagueOnlyMatchDoesNotAutoplay() {
        val ev = event("Seattle Seahawks", "Dallas Cowboys", "nfl")
        val espn = channel("ESPN", "Sports")
        val scored = ChannelScorer.scoreChannels(ev, listOf(espn))
        assertTrue(scored.isNotEmpty(), "league match still surfaces below autoplay floor")
        assertNull(ChannelScorer.autoplayCandidate(scored))
    }

    @Test
    fun tightTopTwoDoesNotAutoplay() {
        val ev = event("Los Angeles Lakers", "Golden State Warriors")
        val lakers = channel("Lakers Warriors", "Sports")
        val lakers2 = channel("Lakers vs Warriors HD FHD", "Sports")
        val scored = ChannelScorer.scoreChannels(ev, listOf(lakers2, lakers))
        assertTrue(scored.size >= 2)
        assertNull(ChannelScorer.autoplayCandidate(scored))
    }

    // ── Real-event validation (documented in sport_channel_matching_report.md gap list) ──

    @Test
    fun manCityChannelMatchesManchesterCityGame() {
        // Report gap: "Manchester vs Man Utd/Man City all miss or misfire".
        val ev = event("Manchester City", "Chelsea", "eng.1")
        val manCity = channel("Man City HD", "Premier League")
        val results = ChannelScorer.scoreChannels(ev, listOf(manCity))
        assertTrue(results.isNotEmpty(), "Man City must match a Manchester City game")
    }

    @Test
    fun wolvesChannelMatchesWolverhamptonGame() {
        val ev = event("Wolverhampton", "Brighton", "eng.1")
        val wolves = channel("Wolves TV", "UK")
        val results = ChannelScorer.scoreChannels(ev, listOf(wolves))
        assertTrue(results.isNotEmpty())
    }

    @Test
    fun spursAliasOnlyForSoccerTottenham() {
        val ev = event("Tottenham", "Everton", "eng.1")
        val spurs = channel("SPURS", "UK")
        assertTrue(ChannelScorer.scoreChannels(ev, listOf(spurs)).isNotEmpty())
    }

    @Test
    fun interMilanShortNameMatchesInterGame() {
        val ev = event("Inter", "Lazio", "seriea")
        val inter = channel("Inter Channels", "Italy")
        val results = ChannelScorer.scoreChannels(ev, listOf(inter))
        assertTrue(results.isNotEmpty())
    }

    @Test
    fun realBroadcasterOutranksGenericEspnForSoccer() {
        // True game channel (via league keywords + team) must beat a random "ESPN".
        val ev = event("Chelsea", "Arsenal", "eng.1")
        val sky = channel("Sky Sports Premier League UHD", "UK")
        val espn = channel("ESPN", "Sports")
        val results = ChannelScorer.scoreChannels(ev, listOf(espn, sky))
        assertEquals(sky.name, results.first().channel.name)
    }

    @Test
    fun redSoxChannelMatchesBostonRedSoxGame() {
        val ev = event("Boston Red Sox", "New York Yankees", "mlb")
        val nesn = channel("NESN - Red Sox", "MLB")
        val results = ChannelScorer.scoreChannels(ev, listOf(nesn))
        assertTrue(results.isNotEmpty())
        assertEquals(MatchType.TEAM, results.first().matchType)
    }
}