package com.nuvio.app.features.sports

/**
 * Text normalization for reliable sport channel matching.
 *
 * Pure Kotlin (no JVM-only deps): manual unicode/`java.text`-free accent folding so it
 * works on Android, iOS, desktop and JS targets.
 */
object ChannelText {

    /**
     * Accent folding for Latin script. Kept exhaustive enough for real team/channel names.
     * Apply before lowercasing so the map keys are lower-case.
     */
    private val DEBURR: Map<Char, Char> by lazy {
        val map = HashMap<Char, Char>()
        fun add(chars: String, plain: Char) = chars.forEach { map[it] = plain }
        add("àáâãäåāăąằắặẳ", 'a'); add("çćĉċč", 'c'); add("ďđ", 'd')
        add("èéêëēĕėęě", 'e'); add("f", 'f'); add("ĝğġģ", 'g'); add("ĥħ", 'h')
        add("ìíîïĩīĭįı", 'i'); add("ĵ", 'j'); add("ķĸ", 'k'); add("ĺļľł", 'l')
        add("ñńņňŉ", 'n'); add("òóôõöøōŏő", 'o'); add("þ", 'p'); add("ŕŗř", 'r')
        add("śŝşš", 's'); add("ťŧ", 't'); add("ùúûüũūŭůűų", 'u'); add("ŵ", 'w')
        add("ýÿŷ", 'y'); add("źżž", 'z')
        // High code-point latin extensions
        add("ą", 'a'); add("ę", 'e'); add("ł", 'l'); add("ś", 's'); add("ź", 'z'); add("ż", 'z')
        add("ć", 'c'); add("ń", 'n'); add("ó", 'o')
        map
    }

    fun deburr(text: String): String {
        if (text.isEmpty()) return text
        val sb = StringBuilder(text.length)
        for (ch in text) {
            val c = ch.lowercaseChar()
            sb.append(DEBURR[c] ?: ch)
        }
        return sb.toString()
    }

    /** Lowercase + accent-fold + collapse whitespace + trim. */
    fun normalize(text: String): String {
        return deburr(text)
            .lowercase()
            .trim()
            .replace(Regex("\\s+"), " ")
    }

    /** Removes trailing quality tags like " HD", " 4K", "50fps" so they don't break name matching. */
    private val QUALITY_TAG = Regex("""(?:[\s\-_/]*[\(\[]?(?:sd|hd|fhd|uhd|qhd|hq|4k|8k|hevc|h265|50fps|60fps)[\)\]]?\s*)+$""", RegexOption.IGNORE_CASE)

    fun stripQualitySuffix(name: String): String {
        return name.replace(QUALITY_TAG, "").trim()
    }

    /** Combined: deburr + lowercase + strip quality + collapse whitespace. */
    fun channelNameKey(name: String): String {
        return normalize(stripQualitySuffix(name))
    }
}

/**
 * Team-name expansion for matching channel names against a game's two teams.
 *
 * Mirrors RunTV's `_f95009`: try the full name, then club prefix/suffix stripping,
 * then a first-word fallback (guarded by an ambiguity blacklist), and for US pro
 * leagues a last-token fallback (handles "Red Sox", "Giants", "Lakers" style names).
 */
object TeamNameKeys {

    private val TRAILING_CLUB = Regex(
        "\\s+(?:fc|ac|cf|sc|afc|sfc|united|city)$",
        RegexOption.IGNORE_CASE
    )
    private val LEADING_CLUB = Regex(
        "^(?:fc|sc|ac|as|afc|vfl|vfb|ssc|ss|us|rc|rcd|cd|ca|sv|ogc|sport[-\\s]?club)\\s+",
        RegexOption.IGNORE_CASE
    )

    /**
     * Well-known shorthand/alias mappings the report called out as real misses
     * ("Manchester → Man Utd/Man City", "wolverhampton", "tottenham").
     * Full normalized team name → additional channel-name keys to try.
     */
    private val MANUAL_ALIASES = mapOf(
        "manchester united" to listOf("man utd", "manchester utd"),
        "manchester city" to listOf("man city"),
        "wolverhampton" to listOf("wolves", "wolverhampton wanderers"),
        "tottenham" to listOf("spurs", "tottenham hotspur"),
        "chelsea" to listOf("chelsea fc"),
        "newcastle united" to listOf("newcastle"),
        "west ham united" to listOf("west ham"),
        "barcelona" to listOf("fc barcelona"),
        "real madrid" to listOf("real madrid cf"),
        "inter milan" to listOf("inter"),
        "ac milan" to listOf("ac milan"),
        "juventus" to listOf("juventus fc"),
        "bayern munich" to listOf("bayern"),
        "borussia dortmund" to listOf("dortmund"),
        "psg" to listOf("paris saint-germain"),
        "new york yankees" to listOf("yankees"),
        "boston red sox" to listOf("red sox"),
        "los angeles lakers" to listOf("lakers"),
        "golden state warriors" to listOf("warriors"),
        "dallas cowboys" to listOf("cowboys"),
    )

    /** Ambiguous words that show up in many channel names and shouldn't be treated as the team. */
    private val AMBIGUOUS_FIRST_WORD = setOf(
        "real", "city", "sport", "sports", "live", "team", "club", "next",
        "united", "inter", "first", "last", "home", "away", "sky", "star",
        "super", "best", "news", "football", "soccer", "channel",
        "fc", "ac", "sc", "la", "el", "red", "blue", "white", "black",
        "north", "south", "east", "west", "new", "st", "san", "los", "de",
    )

    private val US_LEAGUES = setOf("mlb", "nba", "nfl", "nhl", "wnba", "mls")

    internal fun isAmbiguousFirstWord(word: String): Boolean = word in AMBIGUOUS_FIRST_WORD

    /** Returns candidate search keys (lowercased, normalized) for a team display name. */
    fun expand(name: String, league: String = ""): List<String> {
        val normalized = ChannelText.normalize(ChannelText.stripQualitySuffix(name))
        if (normalized.isEmpty()) return emptyList()
        val keys = LinkedHashSet<String>()
        keys.add(normalized)

        // Trailing club suffix: "Real Betis FC" -> "Real Betis"
        val noTrailing = TRAILING_CLUB.replace(normalized, "").trim()
        if (noTrailing != normalized && noTrailing.isNotEmpty()) keys.add(noTrailing)

        // Leading club prefix: "FC Barcelona" -> "Barcelona"
        val noLeading = LEADING_CLUB.replace(noTrailing, "").trim()
        if (noLeading.isNotEmpty() && noLeading != noTrailing) keys.add(noLeading)

        // First word fallback (e.g. "Manchester" -> "manchester"), unless ambiguous.
        val firstWord = noTrailing.split(" ").firstOrNull()
        if (firstWord != null && firstWord.length >= 4 && !isAmbiguousFirstWord(firstWord)) {
            keys.add(firstWord)
        }

        // Last token fallback for US pro teams: "New York Yankees" -> "yankees"
        if (league.lowercase() in US_LEAGUES) {
            val tokens = noTrailing.split(" ")
            val last = tokens.lastOrNull()
            if (last != null && last.length >= 4 && last != firstWord) {
                keys.add(last)
            }
        }

        // Manual aliases for well-known shorthand ("Man City", "Wolves", "Red Sox").
        MANUAL_ALIASES[normalized]?.forEach { keys.add(it) }
        return keys.toList()
    }
}