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
        QuickChannel("Newsmax", listOf("Newsmax", "Newsmax TV", "Newsmax HD"), listOf("US"), listOf("news")),
        QuickChannel("NewsNation", listOf("NewsNation", "News Nation", "NewsNation Now"), listOf("US"), listOf("news")),
        QuickChannel("Fox Business", listOf("Fox Business", "Fox Business Network", "FBN"), listOf("US"), listOf("news")),
        QuickChannel("The Weather Channel", listOf("The Weather Channel", "Weather Channel", "TWC", "WeatherNation", "Weather Nation"), listOf("US", "CA"), listOf("news")),
        QuickChannel("Court TV", listOf("Court TV", "CourtTV", "Court TV LIVE", "Court TV Live"), listOf("US"), listOf("news", "entertainment")),
        QuickChannel("OAN", listOf("OAN", "One America News", "One America News Network", "OAN Plus"), listOf("US"), listOf("news")),
        QuickChannel("Real America's Voice", listOf("Real America's Voice", "Real America's Voice News", "RAV"), listOf("US"), listOf("news")),
        QuickChannel("Newsy", listOf("Newsy", "Scripps News", "Scripps News International"), listOf("US"), listOf("news")),
        QuickChannel("Cheddar", listOf("Cheddar", "Cheddar News"), listOf("US"), listOf("news")),
        QuickChannel("ABC News Live", listOf("ABC News Live", "ABC News 24", "ABC News Direct"), listOf("US"), listOf("news")),
        QuickChannel("CBS News 24/7", listOf("CBS News 24/7", "CBS News Now", "CBS News Streaming"), listOf("US"), listOf("news")),
        QuickChannel("NBC News Now", listOf("NBC News Now", "NBC News NOW", "NBC News 24/7"), listOf("US"), listOf("news")),
        QuickChannel("Fox Weather", listOf("Fox Weather", "FOX Weather", "Fox Weather Network"), listOf("US"), listOf("news")),
        QuickChannel("CBN News", listOf("CBN News", "CBN News Channel", "CBN"), listOf("US"), listOf("news")),
        QuickChannel("i24 News", listOf("i24 News", "i24NEWS", "i24 English"), listOf("US", "EU"), listOf("news")),
        QuickChannel("CGTN", listOf("CGTN", "CGTN America", "CGTN English", "China Global Television Network"), listOf("US", "EU", "CA"), listOf("news")),
        QuickChannel("NHK World Japan", listOf("NHK World", "NHK World Japan", "NHK WORLD-JAPAN"), listOf("US", "CA", "EU"), listOf("news")),
        QuickChannel("Dubai One", listOf("Dubai One", "Dubai TV", "Dubai One English"), listOf("EU"), listOf("news")),
        QuickChannel("GB News", listOf("GB News", "GB News UK"), listOf("UK"), listOf("news")),
        QuickChannel("TalkTV", listOf("TalkTV", "Talk TV", "Talk TV UK"), listOf("UK"), listOf("news")),
        QuickChannel("LBC", listOf("LBC", "LBC News", "LBC UK"), listOf("UK"), listOf("news")),
        QuickChannel("STV News", listOf("STV News", "STV", "STV Scotland", "STV News Scotland"), listOf("UK"), listOf("news")),
        QuickChannel("Sky News Australia", listOf("Sky News Australia", "Sky News AU", "Sky News Australia HD"), listOf("AU"), listOf("news")),
        QuickChannel("Fox News Australia", listOf("Fox News Australia", "FOX News Australia", "FNA"), listOf("AU"), listOf("news")),
        QuickChannel("ABC News Australia", listOf("ABC News Australia", "ABC News 24", "ABC Australia", "ABC News AU"), listOf("AU"), listOf("news")),

        // ── 2. US SPORTS & REGIONAL ──────────────────────────────────────────

        QuickChannel("ESPN", listOf("ESPN", "ESPN US", "ESPN 2", "ESPN2", "ESPN News", "ESPNNews", "ESPN U", "ESPNU", "SEC Network", "SECN", "ACC Network", "ACCN"), listOf("US"), listOf("sports")),
        QuickChannel("Fox Sports", listOf("FS1", "Fox Sports 1", "FS2", "Fox Sports 2", "Big Ten Network", "BTN", "NBC Sports"), listOf("US"), listOf("sports")),
        QuickChannel("CBS Sports", listOf("CBS Sports Network", "CBSSN"), listOf("US"), listOf("sports")),
        QuickChannel("US League Networks", listOf("NFL Network", "NFLN", "NFL RedZone", "RedZone", "NBA TV", "NBATV", "MLB Network", "MLBN", "Golf Channel", "Tennis Channel", "Olympic Channel", "NBA", "NFL", "NHL", "MLB", "Tennis", "Golf", "Bally Sports", "FanDuel Sports", "Prime Video Sport", "Cricket"), listOf("US"), listOf("sports")),
        QuickChannel("NHL Network", listOf("NHL Network", "NHLN", "NHL Network US", "NHL Network Canada"), listOf("US", "CA"), listOf("sports")),
        QuickChannel("Fox Soccer Plus", listOf("Fox Soccer Plus", "FSP", "Fox Soccer"), listOf("US"), listOf("sports")),
        QuickChannel("GolTV", listOf("GolTV", "GolTV US", "Gol TV"), listOf("US"), listOf("sports")),
        QuickChannel("Willow Cricket", listOf("Willow", "Willow Cricket", "Willow HD"), listOf("US", "CA"), listOf("sports")),
        QuickChannel("TVG", listOf("TVG", "TVG Network", "TwinSpires TVG"), listOf("US"), listOf("sports")),
        QuickChannel("Stadium", listOf("Stadium", "Stadium Network", "Stadium Sports"), listOf("US"), listOf("sports")),
        QuickChannel("Pac-12 Networks", listOf("Pac-12", "Pac-12 Network", "Pac-12 Networks", "Pac12"), listOf("US"), listOf("sports")),
        QuickChannel("Fight Network", listOf("Fight Network", "The Fight Network", "Fight TV"), listOf("CA", "US"), listOf("sports")),
        QuickChannel("ESPN Deportes", listOf("ESPN Deportes", "ESPN Deportes US", "ESPN2 Deportes"), listOf("US", "LA"), listOf("sports")),
        QuickChannel("Fox Deportes", listOf("Fox Deportes", "FOX Deportes", "Fox Sports Deportes"), listOf("US", "LA"), listOf("sports")),
        QuickChannel("TUDN", listOf("TUDN", "TUDN USA", "Univision Deportes", "Univision Deportes Network"), listOf("US", "LA"), listOf("sports")),
        QuickChannel("US Regional Sports", listOf("YES Network", "NESN", "MASN", "MSG Network", "MSG", "Marquee Sports Network", "NBC Sports Bay Area", "NBCS Bay Area", "NBCSBA", "NBC Sports California", "NBCS California", "NBCSCA"), listOf("US", "bay-area"), listOf("sports", "regional")),

        // ── 3. INTERNATIONAL SPORTS ──────────────────────────────────────────

        QuickChannel("Sky Sports", listOf("Sky Sports", "Sky Sports Main Event", "Sky Sports Premier League", "Sky Sports PL", "Sky Sports Football", "Sky Sports Cricket", "Sky Sports Golf", "Sky Sports F1", "Sky Sports Action"), listOf("UK"), listOf("sports")),
        QuickChannel("TNT Sports", listOf("TNT Sports", "TNT Sports 1", "TNT Sports 2", "TNT Sports 3", "TNT Sports 4", "BT Sport", "BT Sport 1", "BT Sport 2", "BT Sport 3", "BT Sport ESPN"), listOf("UK"), listOf("sports")),
        QuickChannel("Canadian Sports", listOf("TSN", "TSN 1", "TSN1", "TSN 2", "TSN2", "TSN 3", "TSN3", "TSN 4", "TSN4", "TSN 5", "TSN5", "Sportsnet", "Sportsnet 360", "SN360", "Sportsnet ONE", "SN1", "Sportsnet Ontario", "Sportsnet East", "Sportsnet West", "Sportsnet Pacific", "RDS", "RDS 2", "RDS2", "CBC Sports"), listOf("CA"), listOf("sports")),
        QuickChannel("Global Sports Networks", listOf("DAZN", "DAZN 1", "DAZN 2", "DAZN 1 UK", "DAZN 2 UK", "Eurosport", "Eurosport 1", "Eurosport 2", "beIN Sports", "beIN Sports 1", "beIN Sports 2", "beIN", "Sport TV", "Sport TV 1", "Sport TV 2", "Sport TV 3"), listOf("US", "UK", "CA", "EU"), listOf("sports")),
        QuickChannel("Combat Sports", listOf("UFC", "UFC Fight Night", "UFC PPV", "WWE", "WWE Raw", "WWE SmackDown", "AEW", "AEW Dynamite", "Boxing", "Bellator", "PFL", "Bellator/PFL", "PPV Events", "PPV", "ONE Championship", "ONE FC", "AXS Wrestling"), listOf("US", "UK", "CA", "EU"), listOf("sports")),
        QuickChannel("Motorsport", listOf("F1", "Formula 1", "Formula One", "MotoGP", "NASCAR", "IndyCar", "Indy 500", "WRC", "World Rally", "Superbike", "WSBK"), listOf("US", "UK", "CA", "EU"), listOf("sports")),
        QuickChannel("Soccer / Football", listOf("Champions League", "UEFA Champions League", "Premier League", "EPL", "La Liga", "Serie A", "Bundesliga", "Ligue 1", "MLS", "World Cup", "Eredivisie", "Primeira Liga", "Süper Lig", "Super Lig", "African Football", "Copa Libertadores", "SuperSport", "TNT Sports Football"), listOf("US", "UK", "CA", "EU"), listOf("sports")),
        QuickChannel("Viaplay Sports", listOf("Viaplay", "Viaplay Sports", "Viaplay Sports 1", "Viaplay Sports 2"), listOf("EU", "UK", "CA"), listOf("sports")),
        QuickChannel("Eleven Sports", listOf("Eleven Sports", "Eleven Sports 1", "Eleven Sports 2", "Eleven"), listOf("EU", "UK"), listOf("sports")),
        QuickChannel("Setanta Sports", listOf("Setanta", "Setanta Sports", "Setanta Sports 1", "Setanta Sports 2"), listOf("EU", "CA", "US"), listOf("sports")),
        QuickChannel("Sport Klub", listOf("Sport Klub", "Sport Klub 1", "Sport Klub 2", "Sport Klub HD"), listOf("EU"), listOf("sports")),
        QuickChannel("Sky Sport Italia", listOf("Sky Sport", "Sky Sport Italia", "Sky Sport 1", "Sky Sport 24", "Sky Sport Uno"), listOf("EU"), listOf("sports")),
        QuickChannel("Sky Sport DE", listOf("Sky Sport DE", "Sky Sport Germany", "Sky Sport Bundesliga", "Sky Sport 1 Germany"), listOf("EU"), listOf("sports")),
        QuickChannel("RAI Sport", listOf("Rai Sport", "RAI Sport", "Rai Sport 1", "Rai Sport HD"), listOf("EU"), listOf("sports")),
        QuickChannel("RMC Sport", listOf("RMC Sport", "RMC Sport 1", "RMC Sport 2", "RMC Sport News"), listOf("EU"), listOf("sports")),
        QuickChannel("Canal+ Sport", listOf("Canal+ Sport", "Canal Plus Sport", "Canal+ Sport 1", "Canal+ Sport HD"), listOf("EU"), listOf("sports")),
        QuickChannel("Match TV", listOf("Match TV", "Match TV Russia", "Матч ТВ", "Match TV HD"), listOf("EU"), listOf("sports")),
        QuickChannel("Nova Sports", listOf("Nova Sports", "Nova Sports 1", "Nova Sports 2", "Nova Sports 3", "NovaSports"), listOf("EU"), listOf("sports")),
        QuickChannel("Star Sports India", listOf("Star Sports", "Star Sports 1", "Star Sports 2", "Star Sports HD", "Star Sports 1 Hindi"), listOf("IN"), listOf("sports")),
        QuickChannel("Sony Sports India", listOf("Sony Sports", "Sony Sports 1", "Sony Sports 2", "Sony Ten", "Sony Ten 1", "Sony Ten 2", "Sony Ten 3"), listOf("IN"), listOf("sports")),
        QuickChannel("ESPN Caribbean", listOf("ESPN Caribbean", "ESPN Caribbean HD", "ESPN Play Caribbean"), listOf("CA"), listOf("sports")),
        QuickChannel("Sportsnet World", listOf("Sportsnet World", "Sportsnet World HD", "Sportsnet World 2"), listOf("CA"), listOf("sports")),

        // ── 4. PREMIUM MOVIES & ENTERTAINMENT ────────────────────────────────

        QuickChannel("HBO & Cinemax", listOf("HBO", "HBO US", "HBO East", "HBO West", "HBO 2", "HBO Signature", "HBO Family", "HBO Canada", "Cinemax", "MoreMax", "ActionMax", "ThrillerMax"), listOf("US", "CA"), listOf("premium")),
        QuickChannel("Premium Movies", listOf("Showtime", "Showtime East", "Starz", "Starz East", "Starz Encore", "Paramount", "Paramount Network", "Paramount+", "Lifetime", "Lifetime Movies", "Hallmark", "Hallmark Channel", "Hallmark Movies & Mysteries", "TCM", "Turner Classic Movies", "OSN Movies", "OSN Movies 1", "OSN Movies 2", "Netflix", "Apple TV+"), listOf("US", "UK", "CA", "EU"), listOf("premium")),
        QuickChannel("MGM+ & TMC", listOf("MGM+", "MGM Plus", "Epix", "Epix 2", "Epix Hits", "TMC", "The Movie Channel", "TMC Extra"), listOf("US"), listOf("premium")),
        QuickChannel("Sky Cinema", listOf("Sky Cinema", "Sky Cinema Premiere", "Sky Cinema Greats", "Sky Cinema Family", "Sky Cinema Action", "Sky Cinema Select"), listOf("UK"), listOf("premium")),
        QuickChannel("Canadian Premium", listOf("Crave", "Crave 1", "Crave 2", "Crave 3", "Crave Movies", "Super Channel", "Super Channel Fuse", "Super Channel Heart & Home"), listOf("CA"), listOf("premium")),
        QuickChannel("FXM", listOf("FXM", "FX Movie Channel", "Fox Movies", "FX Movies"), listOf("US"), listOf("premium")),
        QuickChannel("Sony Movies", listOf("Sony Movies", "Sony Movies HD", "Sony Movie Channel", "Sony Movies Action", "Sony Movies Classic"), listOf("US", "UK", "CA", "EU"), listOf("premium")),
        QuickChannel("Movies!", listOf("Movies!", "Movies 1", "Movies! TV", "Movies Hd Movies"), listOf("US"), listOf("premium")),
        QuickChannel("AXN", listOf("AXN", "AXN HD", "AXN Movies", "AXN Action"), listOf("US", "EU", "UK", "CA"), listOf("premium")),
        QuickChannel("Warner TV", listOf("Warner TV", "Warner Channel", "Warner TV HD", "Warner Bros TV"), listOf("US", "EU", "CA", "LA"), listOf("premium")),
        QuickChannel("Criterion Channel", listOf("Criterion", "The Criterion Channel", "Criterion Channel"), listOf("US", "CA", "EU"), listOf("premium")),
        QuickChannel("MUBI", listOf("MUBI", "MUBI TV", "MUBI HD"), listOf("US", "UK", "EU", "CA"), listOf("premium")),
        QuickChannel("Film4", listOf("Film4", "Film 4", "Film4 HD"), listOf("UK"), listOf("premium")),
        QuickChannel("Great! Movies", listOf("Great! Movies", "Great! Movies Classic", "Great Movies UK", "Good TV Movies"), listOf("UK"), listOf("premium")),

        // ── 5. US BROADCAST & CABLE ──────────────────────────────────────────

        QuickChannel("US Major Broadcast", listOf("ABC", "ABC US", "CBS", "CBS US", "NBC", "NBC US", "FOX", "FOX US"), listOf("US"), listOf("broadcast")),
        QuickChannel("ION Television", listOf("ION", "Ion Television", "ION TV", "ION Plus", "Ion Mystery", "Ion Life"), listOf("US"), listOf("broadcast")),
        QuickChannel("MeTV", listOf("MeTV", "Me TV", "Memorable Entertainment Television", "MeTV Network"), listOf("US"), listOf("broadcast")),
        QuickChannel("Cozi TV", listOf("Cozi", "Cozi TV", "COZI TV"), listOf("US"), listOf("broadcast")),
        QuickChannel("Buzzr", listOf("Buzzr", "BUZZR", "Buzzr TV"), listOf("US"), listOf("broadcast")),
        QuickChannel("This TV", listOf("This TV", "ThisTV", "This TV Network"), listOf("US"), listOf("broadcast")),
        QuickChannel("Decades", listOf("Decades", "Decades TV", "Decades Network"), listOf("US"), listOf("broadcast")),
        QuickChannel("Rewind TV", listOf("Rewind TV", "Rewind TV Network"), listOf("US"), listOf("broadcast")),
        QuickChannel("Antenna TV", listOf("Antenna TV", "Antenna TV Network"), listOf("US"), listOf("broadcast")),
        QuickChannel("Grit", listOf("Grit", "Grit TV", "GRIT Network"), listOf("US"), listOf("broadcast")),
        QuickChannel("Bounce TV", listOf("Bounce", "Bounce TV", "BounceTV"), listOf("US"), listOf("broadcast")),
        QuickChannel("UPtv", listOf("UPtv", "UP TV", "UPtv Network"), listOf("US"), listOf("broadcast")),
        QuickChannel("Pop TV", listOf("Pop", "Pop TV", "POP TV", "Pop TV Network"), listOf("US"), listOf("entertainment")),
        QuickChannel("TV Land", listOf("TV Land", "TVLand", "TV Land HD"), listOf("US"), listOf("entertainment")),
        QuickChannel("Game Show Network", listOf("Game Show Network", "GSN", "Game Show Networks"), listOf("US"), listOf("entertainment")),
        QuickChannel("Telemundo", listOf("Telemundo", "Telemundo US", "Telemundo 48", "Telemundo Internacional"), listOf("US", "LA"), listOf("broadcast")),
        QuickChannel("UniMás", listOf("UniMas", "UniMás", "Unimas", "Univision UniMas"), listOf("US", "LA"), listOf("broadcast")),
        QuickChannel("US Cable Networks", listOf("TNT", "TNT US", "TNT USA", "TBS", "TBS US", "USA Network", "USA", "FX", "FXX", "AMC", "Comedy Central", "Syfy", "Bravo", "Paramount Network", "TLC", "HGTV", "Food Network", "Discovery Channel", "History Channel", "National Geographic"), listOf("US"), listOf("entertainment")),
        QuickChannel("TruTV & Reality", listOf("TruTV", "trutv", "Tru TV", "A&E", "A&E Network", "Oxygen", "Oxygen Network", "Sundance TV", "WE tv", "We TV", "TLC", "Bravo", "E!", "E! Entertainment", "Lifetime", "OWN", "Oprah Winfrey Network", "Investigation Discovery"), listOf("US", "CA"), listOf("entertainment")),
        QuickChannel("A&E", listOf("A&E", "A&E Network", "A&E HD"), listOf("US"), listOf("entertainment")),
        QuickChannel("OWN", listOf("OWN", "OWN US", "Oprah Winfrey Network"), listOf("US"), listOf("entertainment")),
        QuickChannel("Freeform", listOf("Freeform", "Freeform HD", "ABC Family"), listOf("US"), listOf("entertainment")),
        QuickChannel("E! Entertainment", listOf("E!", "E! Entertainment", "E! Entertainment Television", "Style Network"), listOf("US", "CA", "UK"), listOf("entertainment")),
        QuickChannel("BBC America", listOf("BBC America", "BBC America US", "BBCA"), listOf("US"), listOf("entertainment")),
        QuickChannel("Documentary", listOf("Discovery", "Discovery Channel", "Discovery Science", "History", "History Channel", "H2", "Nat Geo", "National Geographic", "National Geographic Wild", "Animal Planet", "TLC", "Food Network", "HGTV", "Investigation Discovery", "ID", "Cooking Channel", "Vice", "Vice TV", "Smithsonian Channel", "American Heroes Channel"), listOf("US", "UK", "CA", "EU"), listOf("entertainment")),
        QuickChannel("VICE TV", listOf("Vice", "VICELAND", "Viceland", "VICE TV", "VICE HD", "Vice UK", "Vice Canada"), listOf("US", "UK", "CA"), listOf("entertainment")),
        QuickChannel("Music Channels", listOf("MTV", "MTV Hits", "MTV Live", "MTV 00s", "VH1", "VH1 Classic", "BET", "BET+", "BET Her", "MBC Masr", "MBC Music"), listOf("US", "UK", "CA", "EU"), listOf("entertainment")),
        QuickChannel("CMT", listOf("CMT", "CMT Music", "Country Music Television", "CMT HD"), listOf("US", "CA"), listOf("entertainment")),
        QuickChannel("4Music", listOf("4Music", "4 Music", "4Music UK"), listOf("UK"), listOf("entertainment")),
        QuickChannel("Now 80s", listOf("Now 80s", "Now 80s UK", "NOW 80s"), listOf("UK"), listOf("entertainment")),
        QuickChannel("Kerrang!", listOf("Kerrang", "Kerrang!", "Kerrang TV", "Kerrang Radio TV"), listOf("UK"), listOf("entertainment")),
        QuickChannel("Magic Radio TV", listOf("Magic", "Magic Radio", "Magic TV", "Magic Radio TV"), listOf("UK"), listOf("entertainment")),
        QuickChannel("The Box", listOf("The Box", "The Box Plus", "The Box UK", "The Box Plus Network"), listOf("UK"), listOf("entertainment")),
        QuickChannel("Clubland TV", listOf("Clubland", "Clubland TV", "Clubland TV UK"), listOf("UK"), listOf("entertainment")),
        QuickChannel("Much", listOf("Much", "MuchMusic", "Much Canada", "Much Music"), listOf("CA"), listOf("entertainment")),
        QuickChannel("Stingray Music", listOf("Stingray", "Stingray Music", "Stingray Hits", "Stingray Retro", "Stingray Rock", "Stingray Top Hits"), listOf("CA", "US"), listOf("entertainment")),
        QuickChannel("Kids & Family", listOf("Disney Channel", "Disney XD", "Disney Junior", "Cartoon Network", "CN", "Adult Swim", "Nickelodeon", "Nick", "Nick Jr", "NickToons", "Boomerang", "PBS Kids", "Baby TV", "Baby First", "Spacetoon"), listOf("US"), listOf("kids")),
        QuickChannel("Universal Kids", listOf("Universal Kids", "Sprout", "Universal Kids US"), listOf("US"), listOf("kids")),
        QuickChannel("YTV", listOf("YTV", "YTV Canada", "YTV HD"), listOf("CA"), listOf("kids")),
        QuickChannel("Treehouse", listOf("Treehouse", "Treehouse TV", "Treehouse Canada"), listOf("CA"), listOf("kids")),
        QuickChannel("Family Channel", listOf("Family Channel", "Family Channel Canada", "Family Jr", "Family CHRGD"), listOf("CA"), listOf("kids")),
        QuickChannel("CBeebies", listOf("CBeebies", "CBBC", "Cbeebies UK", "BBC Cbeebies"), listOf("UK"), listOf("kids")),
        QuickChannel("Milkshake!", listOf("Milkshake", "Milkshake!", "Channel 5 Milkshake"), listOf("UK"), listOf("kids")),
        QuickChannel("POP UK", listOf("Pop", "POP UK", "Pop Max", "Pop Kids", "Tiny Pop"), listOf("UK"), listOf("kids")),
        QuickChannel("TVOKids", listOf("TVO", "TVOKids", "TVO Kids", "TVO Ontario"), listOf("CA"), listOf("kids")),

        // ── 6. UK BROADCAST & ENTERTAINMENT ──────────────────────────────────

        QuickChannel("BBC Networks", listOf("BBC", "BBC One", "BBC1", "BBC Two", "BBC2", "BBC Three", "BBC Four"), listOf("UK"), listOf("broadcast")),
        QuickChannel("UK Commercial Networks", listOf("ITV", "ITV1", "ITV2", "ITV3", "ITV4", "Channel 4", "C4", "E4", "More4", "Channel 5", "5USA", "5STAR", "Sky Atlantic", "Sky Max", "Sky Showcase"), listOf("UK"), listOf("broadcast", "entertainment")),
        QuickChannel("Sky Arts", listOf("Sky Arts", "Sky Arts UK", "Arts TV"), listOf("UK"), listOf("entertainment", "premium")),
        QuickChannel("Sky Crime", listOf("Sky Crime", "Sky Crime UK"), listOf("UK"), listOf("entertainment")),
        QuickChannel("Sky History", listOf("Sky History", "Sky History UK", "Sky History 2"), listOf("UK"), listOf("entertainment")),
        QuickChannel("Sky Witness", listOf("Sky Witness", "Sky Witness UK"), listOf("UK"), listOf("entertainment")),
        QuickChannel("W Channel", listOf("W Channel", "W UK", "The W Channel", "W HD"), listOf("UK"), listOf("entertainment")),
        QuickChannel("Dave", listOf("Dave", "Dave UK", "Dave HD", "Dave Jokes"), listOf("UK"), listOf("entertainment")),
        QuickChannel("Gold", listOf("Gold", "Gold UK", "GOLD", "Gold HD"), listOf("UK"), listOf("entertainment")),
        QuickChannel("Drama", listOf("Drama", "Drama UK", "Drama Channel"), listOf("UK"), listOf("entertainment")),
        QuickChannel("Yesterday", listOf("Yesterday", "Yesterday UK", "Yesterday Channel"), listOf("UK"), listOf("entertainment")),
        QuickChannel("Challenge", listOf("Challenge", "Challenge TV", "Challenge UK"), listOf("UK"), listOf("entertainment")),
        QuickChannel("Talking Pictures TV", listOf("Talking Pictures TV", "TPTV", "Talking Pictures"), listOf("UK"), listOf("entertainment")),
        QuickChannel("Blaze", listOf("Blaze", "Blaze UK", "Blaze +1"), listOf("UK"), listOf("entertainment")),
        QuickChannel("ITVBe", listOf("ITVBe", "ITV Be", "ITVBe UK"), listOf("UK"), listOf("entertainment")),
        QuickChannel("Movies4Men", listOf("Movies4Men", "Movies 4 Men", "Movies4Men UK"), listOf("UK"), listOf("premium")),
        QuickChannel("London Live", listOf("London Live", "London Live UK"), listOf("UK"), listOf("entertainment")),

        // ── 7. CANADA & REGIONAL LOCALS ──────────────────────────────────────

        QuickChannel("Canadian Broadcast", listOf("CBC", "CBC Television", "CTV", "CTV 2", "CTV2", "Global TV", "Global", "Showcase", "W Network"), listOf("CA"), listOf("broadcast")),
        QuickChannel("CPAC", listOf("CPAC", "CPAC Canada", "Canadian Parliamentary Channel"), listOf("CA"), listOf("news")),
        QuickChannel("TVA", listOf("TVA", "TVA Quebec", "TVA Network", "TVA HD"), listOf("CA"), listOf("broadcast")),
        QuickChannel("Noovo", listOf("Noovo", "Noovo Quebec", "V TV"), listOf("CA"), listOf("broadcast")),
        QuickChannel("Vrak", listOf("Vrak", "Vrak TV", "Vrak QC"), listOf("CA"), listOf("kids", "entertainment")),
        QuickChannel("AMI", listOf("AMI", "AMI-télé", "AMI Accessible Media"), listOf("CA"), listOf("entertainment")),
        QuickChannel("ABC Spark", listOf("ABC Spark", "ABC Spark Canada"), listOf("CA"), listOf("entertainment")),
        QuickChannel("CTV Comedy Channel", listOf("CTV Comedy", "CTV Comedy Channel", "The Comedy Channel"), listOf("CA"), listOf("entertainment")),
        QuickChannel("CTV Drama Channel", listOf("CTV Drama", "CTV Drama Channel", "BRAVO Canada", "CTV Sci-Fi Channel"), listOf("CA"), listOf("entertainment")),
        QuickChannel("Discovery Science Canada", listOf("Discovery Science", "Discovery Science Canada", "Science Channel Canada", "The Zone Canada"), listOf("CA"), listOf("entertainment")),
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
        QuickChannel("KMAX CW 31", listOf("KMAX", "KMAX 31", "KMAX CW 31", "CW 31 KMAX", "KMAX Sacramento", "CW Sacramento"), listOf("US", "bay-area"), listOf("regional")),

        // ── 8. INTERNATIONAL & ETHNIC ────────────────────────────────────────

        QuickChannel("Zee TV", listOf("Zee TV", "Zee TV India", "Zee TV HD", "Zee Anmol"), listOf("IN"), listOf("entertainment")),
        QuickChannel("Colors TV", listOf("Colors", "Colors TV", "Colors India", "Colors TV HD"), listOf("IN"), listOf("entertainment")),
        QuickChannel("Star Plus", listOf("Star Plus", "StarPlus", "Star Plus HD"), listOf("IN"), listOf("entertainment")),
        QuickChannel("Sony Entertainment TV", listOf("Sony Entertainment", "Sony TV", "SET", "Sony Entertainment Television", "Sony Sab"), listOf("IN"), listOf("entertainment")),
        QuickChannel("Hum TV", listOf("Hum TV", "Hum TV Pakistan", "Hum TV HD"), listOf("PK"), listOf("entertainment")),
        QuickChannel("Geo TV", listOf("Geo TV", "Geo Entertainment", "Geo TV Pakistan", "Geo News"), listOf("PK"), listOf("entertainment", "news")),
        QuickChannel("ARY Digital", listOf("ARY Digital", "ARY", "ARY Digital Pakistan"), listOf("PK"), listOf("entertainment")),
        QuickChannel("Aaj Tak", listOf("Aaj Tak", "AajTak", "Aaj Tak HD"), listOf("IN"), listOf("news")),
        QuickChannel("Times Now", listOf("Times Now", "Times Now India", "Times Now HD"), listOf("IN"), listOf("news")),
        QuickChannel("Republic TV", listOf("Republic TV", "Republic Bharat", "Republic World"), listOf("IN"), listOf("news")),
        QuickChannel("NDTV", listOf("NDTV", "NDTV India", "NDTV 24x7"), listOf("IN"), listOf("news")),
        QuickChannel("Asianet", listOf("Asianet", "Asianet Malayalam", "Asianet Plus", "Asianet Movies"), listOf("IN"), listOf("entertainment")),
        QuickChannel("Surya TV", listOf("Surya TV", "Surya TV Malayalam"), listOf("IN"), listOf("entertainment")),
        QuickChannel("Sun TV", listOf("Sun TV", "Sun TV Tamil", "Sun TV India"), listOf("IN"), listOf("entertainment")),
        QuickChannel("Vijay TV", listOf("Vijay TV", "Star Vijay", "Vijay TV Tamil"), listOf("IN"), listOf("entertainment")),
        QuickChannel("ETV", listOf("ETV", "ETV Telugu", "ETV Plus", "ETV Cinema"), listOf("IN"), listOf("entertainment")),
        QuickChannel("Gemini TV", listOf("Gemini TV", "Gemini Telugu", "Gemini Movies"), listOf("IN"), listOf("entertainment")),
        QuickChannel("Dunya News", listOf("Dunya News", "Dunya News Pakistan"), listOf("PK"), listOf("news")),
        QuickChannel("SAMAA TV", listOf("SAMAA", "SAMAA TV", "Samaa TV Pakistan"), listOf("PK"), listOf("news")),
        QuickChannel("Bollywood Movies", listOf("Bollywood Movies", "Bollywood", "B4U Movies", "Zee Cinema", "Star Gold"), listOf("IN"), listOf("premium")),

        // ── 9. MORE BAY AREA & SACRAMENTO LOCALS ────────────────────────────

        QuickChannel("KOFY TV 20", listOf("KOFY", "KOFY TV", "KOFY 20", "KOFY San Francisco"), listOf("US", "bay-area"), listOf("regional")),
        QuickChannel("KTNC TV", listOf("KTNC", "KTNC TV", "KTNC 42"), listOf("US", "bay-area"), listOf("regional")),
        QuickChannel("KXTV ABC 10", listOf("KXTV", "KXTV ABC", "ABC 10 KXTV", "News 10 ABC"), listOf("US", "bay-area"), listOf("regional", "news")),
        QuickChannel("KCRA NBC 3", listOf("KCRA", "KCRA 3", "KCRA NBC", "NBC 3 KCRA"), listOf("US", "bay-area"), listOf("regional", "news")),
        QuickChannel("KOVR CBS 13", listOf("KOVR", "KOVR 13", "KOVR CBS", "CBS 13 KOVR"), listOf("US", "bay-area"), listOf("regional", "news")),
        QuickChannel("KTXL FOX 40", listOf("KTXL", "KTXL FOX", "FOX 40 KTXL", "KTXL Sacramento"), listOf("US", "bay-area"), listOf("regional", "news")),
        QuickChannel("KVIE PBS 6", listOf("KVIE", "KVIE PBS", "PBS KVIE", "KVIE 6"), listOf("US", "bay-area"), listOf("regional")),
        QuickChannel("KFVT UniMás", listOf("KFVT", "KFVT UniMas", "UniMas Sacramento"), listOf("US", "bay-area"), listOf("regional")),
        QuickChannel("KFSF UniMás 66", listOf("KFSF", "KFSF UniMas", "UniMas 66", "UniMas Bay Area"), listOf("US", "bay-area"), listOf("regional")),
    )
}
