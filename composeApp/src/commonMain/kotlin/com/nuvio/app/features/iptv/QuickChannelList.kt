package com.nuvio.app.features.iptv

object QuickChannelList {
    val all: List<QuickChannel> = listOf(

        // ── 1. NEWS ──────────────────────────────────────────────────────────

        QuickChannel("CNN", listOf("CNN", "CNN US", "CNN USA", "CNN International", "CNNI"), listOf("US"), listOf("news")),
        QuickChannel("Fox News", listOf("Fox News", "Fox News Channel", "FNC"), listOf("US"), listOf("news")),
        QuickChannel("MSNBC", listOf("MSNBC", "MSNBC US"), listOf("US"), listOf("news")),
        QuickChannel("BBC News", listOf("BBC News", "BBC World News", "BBC News UK"), listOf("UK", "US"), listOf("news")),
        QuickChannel("Canadian News", listOf("CBC News", "CBC News Network", "CBCNN", "CTV News", "CTV News Channel", "Global News", "CP24"), listOf("CA"), listOf("news")),

        // ── 2. US SPORTS & REGIONAL ──────────────────────────────────────────

        QuickChannel("ESPN", listOf("ESPN", "ESPN US", "ESPN 2", "ESPN2", "ESPN News", "ESPNNews", "ESPN U", "ESPNU", "SEC Network", "SECN", "ACC Network", "ACCN"), listOf("US"), listOf("sports")),
        QuickChannel("Fox Sports", listOf("FS1", "Fox Sports 1", "FS2", "Fox Sports 2", "Big Ten Network", "BTN"), listOf("US"), listOf("sports")),
        QuickChannel("CBS Sports", listOf("CBS Sports Network", "CBSSN"), listOf("US"), listOf("sports")),
        QuickChannel("US League Networks", listOf("NFL Network", "NFLN", "NFL RedZone", "RedZone", "NBA TV", "NBATV", "MLB Network", "MLBN", "Golf Channel", "Tennis Channel", "Olympic Channel"), listOf("US"), listOf("sports")),
        QuickChannel("US Regional Sports", listOf("YES Network", "NESN", "MASN", "MSG Network", "MSG", "Marquee Sports Network", "NBC Sports Bay Area", "NBCS Bay Area", "NBCSBA", "NBC Sports California", "NBCS California", "NBCSCA"), listOf("US", "bay-area"), listOf("sports", "regional")),

        // ── 3. INTERNATIONAL SPORTS ──────────────────────────────────────────

        QuickChannel("Sky Sports", listOf("Sky Sports", "Sky Sports Main Event", "Sky Sports Premier League", "Sky Sports PL", "Sky Sports Football", "Sky Sports Cricket", "Sky Sports Golf", "Sky Sports F1", "Sky Sports Action"), listOf("UK"), listOf("sports")),
        QuickChannel("TNT Sports", listOf("TNT Sports", "TNT Sports 1", "TNT Sports 2", "TNT Sports 3", "TNT Sports 4", "BT Sport", "BT Sport 1", "BT Sport 2", "BT Sport 3", "BT Sport ESPN"), listOf("UK"), listOf("sports")),
        QuickChannel("Canadian Sports", listOf("TSN", "TSN 1", "TSN1", "TSN 2", "TSN2", "TSN 3", "TSN3", "TSN 4", "TSN4", "TSN 5", "TSN5", "Sportsnet", "Sportsnet 360", "SN360", "Sportsnet ONE", "SN1", "Sportsnet Ontario", "Sportsnet East", "Sportsnet West", "Sportsnet Pacific", "RDS", "RDS 2", "RDS2", "CBC Sports"), listOf("CA"), listOf("sports")),
        QuickChannel("Global Sports Networks", listOf("DAZN", "DAZN 1", "DAZN 2", "DAZN 1 UK", "DAZN 2 UK", "Eurosport", "Eurosport 1", "Eurosport 2", "beIN Sports", "beIN Sports 1", "beIN Sports 2", "beIN", "Sport TV", "Sport TV 1", "Sport TV 2", "Sport TV 3"), listOf("US", "UK", "CA", "EU"), listOf("sports")),

        // ── 4. PREMIUM MOVIES & ENTERTAINMENT ────────────────────────────────

        QuickChannel("HBO & Cinemax", listOf("HBO", "HBO US", "HBO East", "HBO West", "HBO 2", "HBO Signature", "HBO Family", "HBO Canada", "Cinemax", "MoreMax", "ActionMax", "ThrillerMax"), listOf("US", "CA"), listOf("premium")),
        QuickChannel("MGM+ & TMC", listOf("MGM+", "MGM Plus", "Epix", "Epix 2", "Epix Hits", "TMC", "The Movie Channel", "TMC Extra"), listOf("US"), listOf("premium")),
        QuickChannel("Sky Cinema", listOf("Sky Cinema", "Sky Cinema Premiere", "Sky Cinema Greats", "Sky Cinema Family", "Sky Cinema Action", "Sky Cinema Select"), listOf("UK"), listOf("premium")),
        QuickChannel("Canadian Premium", listOf("Crave", "Crave 1", "Crave 2", "Crave 3", "Crave Movies", "Super Channel", "Super Channel Fuse", "Super Channel Heart & Home"), listOf("CA"), listOf("premium")),

        // ── 5. US BROADCAST & CABLE ──────────────────────────────────────────

        QuickChannel("US Major Broadcast", listOf("ABC", "ABC US", "CBS", "CBS US", "NBC", "NBC US", "FOX", "FOX US"), listOf("US"), listOf("broadcast")),
        QuickChannel("US Cable Networks", listOf("TNT", "TNT US", "TBS", "TBS US", "USA Network", "USA", "FX", "FXX", "AMC", "Comedy Central", "Syfy", "Bravo", "Paramount Network", "TLC", "HGTV", "Food Network", "Discovery Channel", "History Channel", "National Geographic"), listOf("US"), listOf("entertainment")),
        QuickChannel("Kids & Family", listOf("Disney Channel", "Disney XD", "Disney Junior", "Cartoon Network", "CN", "Adult Swim", "Nickelodeon", "Nick", "Nick Jr"), listOf("US"), listOf("kids")),

        // ── 6. UK BROADCAST & ENTERTAINMENT ──────────────────────────────────

        QuickChannel("BBC Networks", listOf("BBC", "BBC One", "BBC1", "BBC Two", "BBC2", "BBC Three", "BBC Four"), listOf("UK"), listOf("broadcast")),
        QuickChannel("UK Commercial Networks", listOf("ITV", "ITV1", "ITV2", "ITV3", "ITV4", "Channel 4", "C4", "E4", "More4", "Channel 5", "5USA", "5STAR", "Sky Atlantic", "Sky Max", "Sky Showcase"), listOf("UK"), listOf("broadcast", "entertainment")),

        // ── 7. CANADA & REGIONAL LOCALS ──────────────────────────────────────

        QuickChannel("Canadian Broadcast", listOf("CBC", "CBC Television", "CTV", "CTV 2", "CTV2", "Global TV", "Global", "Showcase", "W Network"), listOf("CA"), listOf("broadcast")),
        QuickChannel("Bay Area Locals", listOf("KTVU", "KTVU Fox 2", "KPIX", "KPIX CBS 5", "CBS Bay Area", "KGO", "KGO ABC 7", "ABC7 Bay Area", "KRON", "KRON 4", "KRON4"), listOf("US", "bay-area"), listOf("regional", "news", "broadcast")),
    )
}
