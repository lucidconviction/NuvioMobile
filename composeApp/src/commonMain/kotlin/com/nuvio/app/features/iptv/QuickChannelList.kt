package com.nuvio.app.features.iptv

object QuickChannelList {
    private val pinnedNames = mutableSetOf<String>()

    val all: List<QuickChannel> get() = {
        val pinnedQc = pinnedNames.mapNotNull { name -> default.find { it.displayName == name } }
        pinnedQc + default.filter { it.displayName !in pinnedNames }
    }()

    fun isPinned(displayName: String): Boolean = displayName in pinnedNames

    fun togglePin(displayName: String) {
        if (displayName in pinnedNames) pinnedNames.remove(displayName)
        else pinnedNames.add(displayName)
    }

    fun unpin(displayName: String) { pinnedNames.remove(displayName) }

    private val regionUs = listOf("us", "usa", "united states")
    private val regionCa = listOf("ca", "canada", "canadian", "canadien", "canadiens")
    private val regionUk = listOf("uk", "united kingdom", "britain", "british", "england")

    private val regionTokensByDisplayName = mapOf(
        "US Channels" to regionUs,
        "CA Channels" to regionCa,
        "UK Channels" to regionUk,
    )

    fun matches(quickChannel: QuickChannel, channel: IptvChannel): Boolean {
        val regionTokens = regionTokensByDisplayName[quickChannel.displayName]
        if (regionTokens != null) {
            val name = channel.name.lowercase()
            val group = (channel.group ?: "").lowercase()
            return regionTokens.any { token ->
                val pattern = if (token.length <= 3) Regex("\\b$token\\b", RegexOption.IGNORE_CASE)
                              else Regex(token, RegexOption.IGNORE_CASE)
                pattern.containsMatchIn(name) || pattern.containsMatchIn(group)
            }
        }
        return channel.name.contains(quickChannel.displayName, ignoreCase = true) ||
            quickChannel.aliases.any { alias -> channel.name.contains(alias, ignoreCase = true) }
    }

    private val default: List<QuickChannel> = listOf(
        // ── 0. REGION SUB-CHANNEL BUNDLES (always listed first) ──────────────
        QuickChannel("US Channels", listOf("us", "usa", "united states", "american"), listOf("US"), listOf("region")),
        QuickChannel("CA Channels", listOf("ca", "canada", "canadian", "canadien", "canadiens"), listOf("CA"), listOf("region")),
        QuickChannel("UK Channels", listOf("uk", "united kingdom", "britain", "british", "england"), listOf("UK"), listOf("region")),

        // ── 1. NEWS ──────────────────────────────────────────────────────────

        QuickChannel("CNN", listOf("CNN", "CNN US", "CNN USA", "CNN International", "CNNI"), listOf("US"), listOf("news")),
        QuickChannel("Fox News", listOf("Fox News", "Fox News Channel", "FNC"), listOf("US"), listOf("news")),
        QuickChannel("MSNBC", listOf("MSNBC", "MSNBC US"), listOf("US"), listOf("news")),
        QuickChannel("BBC News", listOf("BBC News", "BBC World News", "BBC News UK"), listOf("UK", "US"), listOf("news")),
        QuickChannel("Sky News & World", listOf("Sky News", "Sky News UK", "Al Jazeera", "Al Jazeera English", "CNBC", "CNBC World", "Bloomberg", "Bloomberg TV", "Bloomberg Television", "France 24", "DW", "Deutsche Welle", "Euronews", "RT News", "RT International", "Russia Today", "TRT World", "Sky Sports News"), listOf("UK", "US", "CA", "EU"), listOf("news")),
        QuickChannel("Canadian News", listOf("CBC News", "CBC News Network", "CBCNN", "CTV News", "CTV News Channel", "Global News", "CP24"), listOf("CA"), listOf("news")),

        // ── 2. US SPORTS & REGIONAL ──────────────────────────────────────────

        QuickChannel("ESPN", listOf("ESPN", "ESPN US", "ESPN 2", "ESPN2", "ESPN News", "ESPNNews", "ESPN U", "ESPNU", "SEC Network", "SECN", "ACC Network", "ACCN"), listOf("US"), listOf("sports")),
        QuickChannel("Fox Sports", listOf("FS1", "Fox Sports 1", "FS2", "Fox Sports 2", "Big Ten Network", "BTN", "NBC Sports"), listOf("US"), listOf("sports")),
        QuickChannel("CBS Sports", listOf("CBS Sports Network", "CBSSN"), listOf("US"), listOf("sports")),
        QuickChannel("US League Networks", listOf("NFL Network", "NFLN", "NFL RedZone", "RedZone", "NBA TV", "NBATV", "MLB Network", "MLBN", "Golf Channel", "Tennis Channel", "Olympic Channel", "NBA", "NFL", "NHL", "MLB", "Tennis", "Golf", "Bally Sports", "FanDuel Sports", "Prime Video Sport", "Cricket"), listOf("US"), listOf("sports")),
        QuickChannel("US Regional Sports", listOf("YES Network", "NESN", "MASN", "MSG Network", "MSG", "Marquee Sports Network", "NBC Sports Bay Area", "NBCS Bay Area", "NBCSBA", "NBC Sports California", "NBCS California", "NBCSCA"), listOf("US", "bay-area"), listOf("sports", "regional")),

        // ── 3. INTERNATIONAL SPORTS ──────────────────────────────────────────

        QuickChannel("Sky Sports", listOf("Sky Sports", "Sky Sports Main Event", "Sky Sports Premier League", "Sky Sports PL", "Sky Sports Football", "Sky Sports Cricket", "Sky Sports Golf", "Sky Sports F1", "Sky Sports Action"), listOf("UK"), listOf("sports")),
        QuickChannel("TNT Sports", listOf("TNT Sports", "TNT Sports 1", "TNT Sports 2", "TNT Sports 3", "TNT Sports 4", "BT Sport", "BT Sport 1", "BT Sport 2", "BT Sport 3", "BT Sport ESPN"), listOf("UK"), listOf("sports")),
        QuickChannel("Canadian Sports", listOf("TSN", "TSN 1", "TSN1", "TSN 2", "TSN2", "TSN 3", "TSN3", "TSN 4", "TSN4", "TSN 5", "TSN5", "Sportsnet", "Sportsnet 360", "SN360", "Sportsnet ONE", "SN1", "Sportsnet Ontario", "Sportsnet East", "Sportsnet West", "Sportsnet Pacific", "RDS", "RDS 2", "RDS2", "CBC Sports"), listOf("CA"), listOf("sports")),
        QuickChannel("Global Sports Networks", listOf("DAZN", "DAZN 1", "DAZN 2", "DAZN 1 UK", "DAZN 2 UK", "Eurosport", "Eurosport 1", "Eurosport 2", "beIN Sports", "beIN Sports 1", "beIN Sports 2", "beIN", "Sport TV", "Sport TV 1", "Sport TV 2", "Sport TV 3"), listOf("US", "UK", "CA", "EU"), listOf("sports")),
        QuickChannel("Combat Sports", listOf("UFC", "UFC Fight Night", "UFC PPV", "WWE", "WWE Raw", "WWE SmackDown", "AEW", "AEW Dynamite", "Boxing", "Bellator", "PFL", "Bellator/PFL", "PPV Events", "PPV", "ONE Championship", "ONE FC", "AXS Wrestling"), listOf("US", "UK", "CA", "EU"), listOf("sports")),
        QuickChannel("Motorsport", listOf("F1", "Formula 1", "Formula One", "MotoGP", "NASCAR", "IndyCar", "Indy 500", "WRC", "World Rally", "Superbike", "WSBK"), listOf("US", "UK", "CA", "EU"), listOf("sports")),
        QuickChannel("Soccer / Football", listOf("Champions League", "UEFA Champions League", "Premier League", "EPL", "La Liga", "Serie A", "Bundesliga", "Ligue 1", "MLS", "World Cup", "Eredivisie", "Primeira Liga", "Süper Lig", "Super Lig", "African Football", "Copa Libertadores", "SuperSport", "TNT Sports Football"), listOf("US", "UK", "CA", "EU"), listOf("sports")),

        // ── 4. PREMIUM MOVIES & ENTERTAINMENT ────────────────────────────────

        QuickChannel("HBO & Cinemax", listOf("HBO", "HBO US", "HBO East", "HBO West", "HBO 2", "HBO Signature", "HBO Family", "HBO Canada", "Cinemax", "MoreMax", "ActionMax", "ThrillerMax"), listOf("US", "CA"), listOf("premium")),
        QuickChannel("Premium Movies", listOf("Showtime", "Showtime East", "Starz", "Starz East", "Starz Encore", "Paramount", "Paramount Network", "Paramount+", "Lifetime", "Lifetime Movies", "Hallmark", "Hallmark Channel", "Hallmark Movies & Mysteries", "TCM", "Turner Classic Movies", "OSN Movies", "OSN Movies 1", "OSN Movies 2", "Netflix", "Apple TV+"), listOf("US", "UK", "CA", "EU"), listOf("premium")),
        QuickChannel("MGM+ & TMC", listOf("MGM+", "MGM Plus", "Epix", "Epix 2", "Epix Hits", "TMC", "The Movie Channel", "TMC Extra"), listOf("US"), listOf("premium")),
        QuickChannel("Sky Cinema", listOf("Sky Cinema", "Sky Cinema Premiere", "Sky Cinema Greats", "Sky Cinema Family", "Sky Cinema Action", "Sky Cinema Select"), listOf("UK"), listOf("premium")),
        QuickChannel("Canadian Premium", listOf("Crave", "Crave 1", "Crave 2", "Crave 3", "Crave Movies", "Super Channel", "Super Channel Fuse", "Super Channel Heart & Home"), listOf("CA"), listOf("premium")),

        // ── 5. US BROADCAST & CABLE ──────────────────────────────────────────

        QuickChannel("US Major Broadcast", listOf("ABC", "ABC US", "CBS", "CBS US", "NBC", "NBC US", "FOX", "FOX US"), listOf("US"), listOf("broadcast")),
        QuickChannel("US Cable Networks", listOf("TNT", "TNT US", "TNT USA", "TBS", "TBS US", "USA Network", "USA", "FX", "FXX", "AMC", "Comedy Central", "Syfy", "Bravo", "Paramount Network", "TLC", "HGTV", "Food Network", "Discovery Channel", "History Channel", "National Geographic"), listOf("US"), listOf("entertainment")),
        QuickChannel("Documentary", listOf("Discovery", "Discovery Channel", "Discovery Science", "History", "History Channel", "H2", "Nat Geo", "National Geographic", "National Geographic Wild", "Animal Planet", "TLC", "Food Network", "HGTV", "Investigation Discovery", "ID", "Cooking Channel", "Vice", "Vice TV", "Smithsonian Channel", "American Heroes Channel"), listOf("US", "UK", "CA", "EU"), listOf("entertainment")),
        QuickChannel("Music Channels", listOf("MTV", "MTV Hits", "MTV Live", "MTV 00s", "VH1", "VH1 Classic", "BET", "BET+", "BET Her", "MBC Masr", "MBC Music"), listOf("US", "UK", "CA", "EU"), listOf("entertainment")),
        QuickChannel("Kids & Family", listOf("Disney Channel", "Disney XD", "Disney Junior", "Cartoon Network", "CN", "Adult Swim", "Nickelodeon", "Nick", "Nick Jr", "NickToons", "Boomerang", "PBS Kids", "Baby TV", "Baby First", "Spacetoon"), listOf("US"), listOf("kids")),

        // ── 6. UK BROADCAST & ENTERTAINMENT ──────────────────────────────────

        QuickChannel("BBC Networks", listOf("BBC", "BBC One", "BBC1", "BBC Two", "BBC2", "BBC Three", "BBC Four"), listOf("UK"), listOf("broadcast")),
        QuickChannel("UK Commercial Networks", listOf("ITV", "ITV1", "ITV2", "ITV3", "ITV4", "Channel 4", "C4", "E4", "More4", "Channel 5", "5USA", "5STAR", "Sky Atlantic", "Sky Max", "Sky Showcase"), listOf("UK"), listOf("broadcast", "entertainment")),

        // ── 7. CANADA & REGIONAL LOCALS ──────────────────────────────────────

        QuickChannel("Canadian Broadcast", listOf("CBC", "CBC Television", "CTV", "CTV 2", "CTV2", "Global TV", "Global", "Showcase", "W Network"), listOf("CA"), listOf("broadcast")),
        QuickChannel("KTVU Fox 2", listOf("KTVU", "KTVU Fox 2", "FOX 2 KTVU", "KTVU San Francisco"), listOf("US", "bay-area"), listOf("regional", "news")),
        QuickChannel("KPIX CBS 5", listOf("KPIX", "KPIX CBS 5", "CBS 5 KPIX", "CBS Bay Area"), listOf("US", "bay-area"), listOf("regional", "news")),
        QuickChannel("KGO ABC 7", listOf("KGO", "KGO ABC 7", "ABC 7 KGO", "ABC7 Bay Area"), listOf("US", "bay-area"), listOf("regional", "news")),
        QuickChannel("KRON 4", listOf("KRON", "KRON 4", "KRON4", "KRON News", "KRON On"), listOf("US", "bay-area"), listOf("regional", "news")),
        QuickChannel("KNTV NBC Bay Area", listOf("KNTV", "KNTV NBC 11", "NBC Bay Area", "NBC 11", "KNTV Bay Area"), listOf("US", "bay-area"), listOf("regional", "news")),
        QuickChannel("KQED 9", listOf("KQED", "KQED 9", "KQED PBS", "PBS KQED", "KQED Plus"), listOf("US", "bay-area"), listOf("regional")),
        QuickChannel("KBCW CW 44", listOf("KBCW", "KBCW CW 44", "CW 44", "CW Bay Area"), listOf("US", "bay-area"), listOf("regional")),
        QuickChannel("KICU MyNetwork 36", listOf("KICU", "KICU 36", "MyNetwork 36", "KICU Bay Area"), listOf("US", "bay-area"), listOf("regional")),
        QuickChannel("KDTV Univision 14", listOf("KDTV", "KDTV Univision 14", "Univision 14", "Univision Bay Area"), listOf("US", "bay-area"), listOf("regional")),
        QuickChannel("KTSF 26", listOf("KTSF", "KTSF 26", "KTSF Bay Area", "KTSF 26 San Francisco"), listOf("US", "bay-area"), listOf("regional")),
        QuickChannel("KTVU Fox 2 Plus", listOf("KTVU Plus", "KTVU Fox 2 Plus", "KTVU+"), listOf("US", "bay-area"), listOf("regional")),
        QuickChannel("NBC Bay Area News", listOf("NBC Bay Area News", "NBCBA News", "NBC Bay Area Nonstop"), listOf("US", "bay-area"), listOf("regional", "news")),
        QuickChannel("KPJK 60", listOf("KPJK", "KPJK 60", "KPJK San Francisco"), listOf("US", "bay-area"), listOf("regional")),
        QuickChannel("KRCB 22", listOf("KRCB", "KRCB 22", "KRCB North Bay", "KRCB PBS"), listOf("US", "bay-area"), listOf("regional")),
        QuickChannel("KCSM 43", listOf("KCSM", "KCSM 43", "KCSM San Mateo"), listOf("US", "bay-area"), listOf("regional")),
        QuickChannel("KEMO 50", listOf("KEMO", "KEMO 50", "KEMO San Francisco"), listOf("US", "bay-area"), listOf("regional")),
    )
}
