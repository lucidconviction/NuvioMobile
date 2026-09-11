@file:OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)

package com.nuvio.app.features.iptv

import com.nuvio.app.features.addons.httpGetText
import com.nuvio.app.features.addons.httpGetTextWithHeaders
import com.nuvio.app.features.addons.httpGetTextWithHeadersLimited
import com.nuvio.app.features.trakt.TraktPlatformClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlin.io.encoding.Base64
import kotlin.math.min

data class PortalNutzEntry(
    val label: String,
    val url: String,
    val username: String,
    val password: String,
    val channelCount: Int,
    val domain: String,
    val expDate: Long? = null,
    val maxConnections: Int? = null,
    val activeConnections: Int? = null,
    val status: String? = null,
    val isTrial: Boolean? = null,
)

object PortalNutzScraper {

    private var currentJob: Job? = null

    data class PendingSearch(
        val searchTerm: String,
        val job: Job,
        val onEvent: (ScrapeEvent) -> Unit,
    )
    private var pendingSearches = mutableMapOf<String, PendingSearch>()

    fun cancel() {
        currentJob?.cancel()
        currentJob = null
        pendingSearches.values.forEach { it.job.cancel() }
        pendingSearches.clear()
    }

    fun cancelSearch(searchTerm: String) {
        pendingSearches.remove(searchTerm)?.job?.cancel()
    }

    fun isSearching(searchTerm: String): Boolean = pendingSearches.containsKey(searchTerm)

    /** Cancel only the active foreground scrape (no search term path), leaving background searches intact. */
    fun cancelCurrentJob() {
        currentJob?.cancel()
        currentJob = null
    }

    private val json = Json { ignoreUnknownKeys = true }

    private val UA = "Mozilla/5.0 (Linux; Android 11; NuvioTV) AppleWebKit/537.36"

    private const val FETCH_TIMEOUT_MS = 2_500L
    private const val MAX_PARALLEL_FETCHES = 6
    private const val MAX_COUNT_PARALLEL = 6
    private const val MAX_VERIFY_BYTES = 2 * 1024 * 1024
    private const val MAX_COUNT_BYTES = 8 * 1024 * 1024
    private const val MAX_RAW_FILE_BYTES = 8 * 1024 * 1024
    private const val CANDIDATE_POOL_SIZE = 150
    private const val CACHE_TTL_MS = 60 * 60 * 1000L
    private const val CACHE_MAX = 100
    internal const val MAX_SEARCH_RESULTS = 20
    private const val SEARCH_BATCH_SIZE = 30

    private data class Portal(val url: String, val username: String, val password: String, val source: String)
    private data class VerifiedPortal(
        val portal: Portal,
        val name: String,
        val domain: String,
        val expDate: Long? = null,
        val maxConnections: Int? = null,
        val activeConnections: Int? = null,
        val status: String? = null,
        val isTrial: Boolean? = null,
    )
    private data class CachedPortal(
        val url: String,
        val username: String,
        val password: String,
        val name: String,
        val domain: String,
        val channelCount: Int,
        val expDate: Long?,
        val maxConnections: Int?,
        val activeConnections: Int?,
        val status: String?,
        val isTrial: Boolean?,
        val verifiedAt: Long,
    )
    private data class CountedPortal(val verified: VerifiedPortal, val count: Int)

    private class PortalCache {
        private val map = LinkedHashMap<String, CachedPortal>(CACHE_MAX, 0.75f, true)

        @Synchronized
        fun retrieve(key: String): CachedPortal? {
            sweepInternal()
            val p = map[key] ?: return null
            if (TraktPlatformClock.nowEpochMs() - p.verifiedAt > CACHE_TTL_MS) {
                map.remove(key)
                return null
            }
            return p
        }

        @Synchronized
        fun put(key: String, portal: CachedPortal) {
            map[key] = portal
            while (map.size > CACHE_MAX) {
                val it = map.keys.iterator()
                it.next().let { k -> it.remove(); map.remove(k) }
            }
        }

        @Synchronized
        fun putAll(entries: List<Pair<String, CachedPortal>>) {
            entries.forEach { (k, p) -> map[k] = p }
            while (map.size > CACHE_MAX) {
                val it = map.keys.iterator()
                it.next().let { k -> it.remove(); map.remove(k) }
            }
        }

        @Synchronized
        fun snapshot(): List<CachedPortal> {
            sweepInternal()
            return map.values.toList()
        }

        private fun sweepInternal() {
            val now = TraktPlatformClock.nowEpochMs()
            val expired = map.filterValues { now - it.verifiedAt > CACHE_TTL_MS }.keys
            expired.forEach { map.remove(it) }
        }
    }

    private val portalCache = PortalCache()

    private data class GitHubRepo(
        val owner: String,
        val repo: String,
        val path: String,
        val json: Boolean = false,
        val fallbackFiles: List<String> = emptyList(),
    )

    private val WORLD_REPO_FALLBACK = listOf(
        "25.txt", "71.txt", "ABN.txt", "DOV.txt",
        "%5BK_B_W_%20Client%5D.txt", "br.txt",
        "channels_fulltime%20(OR).txt", "channels_fulltime.txt",
        "kgen%20(4).txt", "kgen.txt", "rg.txt", "x.txt",
        "%7BAllTelegram%7D2.txt",
    )

    private val TELEGRAM_CHANNELS = listOf(
        "xtreamcodes", "xtream_iptv_code", "satglobaltv", "IPTVXTREAMPRO",
        "m3u86", "iptvgratuitfr0", "extremeportals",
    )

    private val REDDIT_SUBREDDITS = listOf("IPTV_ZONENEW", "xml2")

    private val PASTE_DOMAINS = listOf(
        "paste.sh", "pastebin.com", "justpaste.it", "controlc.com",
        "pastes.dev", "text.is", "rentry.co",
    )

    private val RAW_PASTE = Regex("""https?://(?:${PASTE_DOMAINS.joinToString("|")})/[a-zA-Z0-9#_=\-]+""", RegexOption.IGNORE_CASE)

    private val B64_RE = Regex("""aHR0c[a-zA-Z0-9+/=]{10,}""")

    private val REDDIT_RSS_HOSTS = listOf("old.reddit.com", "www.reddit.com")

    private val ADULT_TERMS = setOf(
        "xxx", "adult", "porn", "sex", "erotic", "18+", "onlyfans", "cam", "nude",
        "playboy", "penthouse", "hustler", "brazzers", "bangbros",
        "pornhub", "xvideos", "xhamster", "xnxx", "redtube",
        "milf", "ebony", "lesbian", "gay", "shemale", "tranny",
        "sexo", "porno", "adultos", "fetish", "bdsm", "hardcore",
    )

    private val SPORTS_KEYWORDS = setOf(
        "espn", "nfl", "nba", "mlb", "nhl", "ufc", "dazn", "bein sport",
        "premier league", "laliga", "serie a", "bundesliga", "ligue 1",
        " champions ", "europa league", "world cup", "fifa", "ncaa",
        "nascar", "formula 1", "f1", "motogp", "wwe", "aew",
        "tennis", "us open", "wimbledon", "australian open",
        "nfl network", "nba tv", "mlb network", "nhl network",
        "golf", "pga", "masters", "cricket", "ipl",
        "boxing", "mma", "one championship", "wrc", "world rally",
        "olympics", "super bowl", "stanley cup", "world series",
        "ncaa football", "ncaa basketball", "march madness",
        "sports", "sport", "deporte", "deportes",
    )

    sealed class ScrapeEvent {
        data class Progress(val message: String) : ScrapeEvent()
        data class Result(val portals: List<PortalNutzEntry>) : ScrapeEvent()
        data class Error(val message: String) : ScrapeEvent()
    }

    private fun cleanPortalUrl(raw: String): String {
        var clean = raw.replace("\\s+".toRegex(), "")
        val qIdx = clean.indexOf('?')
        if (qIdx >= 0) clean = clean.substring(0, qIdx)
        clean = clean.replace(Regex("/+(?:get|live|portal|c|index|playlist|player_api|xmltv|index\\.php|portal\\.php)\\.php\$", RegexOption.IGNORE_CASE), "")
        while (clean.endsWith('/')) clean = clean.substring(0, clean.length - 1)
        if (!clean.startsWith("http")) clean = "http://$clean"
        return clean
    }

    private fun extractDomain(url: String): String {
        val cleaned = url
            .removePrefix("https://")
            .removePrefix("http://")
        val slashIdx = cleaned.indexOf('/')
        return if (slashIdx >= 0) cleaned.substring(0, slashIdx) else cleaned
    }

    private fun domainKey(url: String): String =
        url
            .removePrefix("https://")
            .removePrefix("http://")
            .substringBefore('/')
            .substringBefore(':')
            .lowercase()

    private fun extractPortals(text: String, source: String): List<Portal> {
        if (text.length < 15) return emptyList()
        val cleaned = text
            .replace("&amp;".toRegex(), "&")
            .replace("&quot;".toRegex(), "\"")
            .replace(Regex("<(?:p|br|div|li|h\\d)[^>]*>"), "\n")
            .replace(Regex("<[^>]+>"), "")

        val seen = mutableSetOf<String>()
        val portals = mutableListOf<Portal>()

        val urlParamRegex = Regex("""(https?://[^?\s"'<]+)\?(?:[^\s"'<]*?&)?(?:username|user)=([^&\s"'<]+)\s*&(?:password|pass)=([^&\s"'<]+)""", RegexOption.IGNORE_CASE)
        for (m in urlParamRegex.findAll(cleaned)) {
            val url = cleanPortalUrl(m.groupValues[1])
            val user = m.groupValues[2].trim()
            val pass = m.groupValues[3].trim()
            if (url.isNotEmpty() && user.length >= 3 && pass.length >= 3 && !user.contains("http") && !pass.contains("http")) {
                val key = "$url|$user|$pass"
                if (seen.add(key)) portals.add(Portal(url, user, pass, source))
            }
        }

        val labelRegex = Regex("""(?:Portal|Host(?:\s*URL)?|Panel|Real|URL|🔗|Url)\W*?(https?://[^<\s"']+)[\s\S]{1,500}?(?:Username|User|Usu[áa]rio|Usuario|👤|Identifiant)\W*?([^\s|<"'\n]+)[\s\S]{1,200}?(?:Password|Pass|Senha|Contrase[ñn]a|🔑|Mot\s*de\s*[Pp]asse)\W*?([^\s|<"'\n]+)""", RegexOption.IGNORE_CASE)
        for (m in labelRegex.findAll(cleaned)) {
            val url = cleanPortalUrl(m.groupValues[1])
            val user = m.groupValues[2].trim()
            val pass = m.groupValues[3].trim()
            if (url.isNotEmpty() && user.length >= 3 && pass.length >= 3 && !user.contains("http") && !pass.contains("http")) {
                val key = "$url|$user|$pass"
                if (seen.add(key)) portals.add(Portal(url, user, pass, source))
            }
        }

        return portals
    }

    private fun isAdultText(text: String): Boolean {
        val lower = text.lowercase()
        return ADULT_TERMS.any { lower.contains(it) }
    }

    private fun hasNonLatinScript(text: String): Boolean {
        return Regex("""[\u0400-\u04FF\u0500-\u052F]""").containsMatchIn(text) ||
            Regex("""[\u0600-\u06FF]""").containsMatchIn(text) ||
            Regex("""[\u4E00-\u9FFF]""").containsMatchIn(text) ||
            Regex("""[\u3040-\u30FF]""").containsMatchIn(text)
    }

    private suspend fun fetchText(url: String): String? = withTimeoutOrNull(FETCH_TIMEOUT_MS) {
        try {
            httpGetText(url)
        } catch (_: Exception) { null }
    }

    private suspend fun fetchTextWithHeaders(url: String, headers: Map<String, String>): String? =
        withTimeoutOrNull(FETCH_TIMEOUT_MS) {
            try {
                httpGetTextWithHeaders(url, headers)
            } catch (_: Exception) { null }
        }

    private suspend fun fetchTextWithHeadersLimited(url: String, headers: Map<String, String>, maxBytes: Int): String? =
        withTimeoutOrNull(FETCH_TIMEOUT_MS) {
            try {
                httpGetTextWithHeadersLimited(url, headers, maxBytes)
            } catch (_: Exception) { null }
        }

    private fun decodeBase64(raw: String): String? {
        val u = raw.replace(Regex("""[^A-Za-z0-9+/=]"""), "")
        val padded = u + "=".repeat((4 - u.length % 4) % 4)
        return try {
            Base64.decode(padded).decodeToString()
        } catch (_: Exception) { null }
    }

    private suspend fun fetchPasteContent(url: String): String? {
        return try {
            when {
                url.contains("pastebin.com/") && !url.contains("/raw/") -> {
                    val id = url.substringAfter("pastebin.com/").substringBefore('?').substringBefore('#')
                    fetchTextWithHeaders("https://pastebin.com/raw/$id", mapOf("User-Agent" to UA))
                }
                url.contains("pastes.dev/") -> {
                    val id = url.substringAfter("pastes.dev/").substringBefore('?').substringBefore('#')
                    fetchTextWithHeaders("https://api.pastes.dev/$id", mapOf("User-Agent" to UA))
                }
                url.contains("rentry.co/") && !url.contains("/raw") -> {
                    val id = url.substringAfter("rentry.co/").substringBefore('?').substringBefore('#')
                    fetchTextWithHeaders("https://rentry.co/$id/raw", mapOf("User-Agent" to UA))
                }
                else -> fetchText(url)
            }
        } catch (_: Exception) { null }
    }

    private suspend fun collectPortalsFromText(
        text: String,
        source: String,
        seen: MutableSet<String>,
        portals: MutableList<Portal>,
    ) {
        for (p in extractPortals(text, source)) {
            val key = "${p.url}|${p.username}|${p.password}"
            if (seen.add(key)) portals.add(p)
        }
        for (m in B64_RE.findAll(text)) {
            val decoded = decodeBase64(m.value) ?: continue
            for (p in extractPortals(decoded, "$source:b64")) {
                val key = "${p.url}|${p.username}|${p.password}"
                if (seen.add(key)) portals.add(p)
            }
        }
        for (m in RAW_PASTE.findAll(text)) {
            val content = fetchPasteContent(m.value) ?: continue
            for (p in extractPortals(content, "$source:paste")) {
                val key = "${p.url}|${p.username}|${p.password}"
                if (seen.add(key)) portals.add(p)
            }
        }
    }

    private suspend fun fetchGitHubPortals(onEvent: (ScrapeEvent) -> Unit): List<Portal> {
        onEvent(ScrapeEvent.Progress("Shaking the tree for ripe nutz..."))
        val seen = mutableSetOf<String>()
        val portals = mutableListOf<Portal>()

        val repos = listOf(
            GitHubRepo("akeotaseo", "world_repo", "Updater_Matrix/XML2", fallbackFiles = WORLD_REPO_FALLBACK),
            GitHubRepo("Armiiin", "world_repo", "Updater_Matrix/XML2", fallbackFiles = WORLD_REPO_FALLBACK),
            GitHubRepo("rochana-sadila", "Xtream-Codes-Library", "", json = true),
        )

        for (repo in repos) {
            var files = mutableListOf<Triple<String, String, Long>>()
            try {
                val apiUrl = "https://api.github.com/repos/${repo.owner}/${repo.repo}/contents/${repo.path}?ref=main"
                val jsonText = fetchTextWithHeadersLimited(apiUrl, mapOf("User-Agent" to UA, "Accept" to "application/vnd.github.v3+json"), 2 * 1024 * 1024)
                if (jsonText != null) {
                    val arr = json.parseToJsonElement(jsonText).jsonArray
                    for (item in arr) {
                        val obj = item.jsonObject
                        if (obj["type"]?.jsonPrimitive?.contentOrNull == "file") {
                            val name = obj["name"]?.jsonPrimitive?.contentOrNull ?: ""
                            val downloadUrl = obj["download_url"]?.jsonPrimitive?.contentOrNull ?: ""
                            if (name.endsWith(if (repo.json) ".json" else ".txt") && downloadUrl.isNotEmpty()) {
                                val size = obj["size"]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: Long.MAX_VALUE
                                files.add(Triple(name, downloadUrl, size))
                            }
                        }
                    }
                    files.sortBy { it.third }
                }
            } catch (_: Exception) {}

            if (files.isEmpty()) {
                val base = "https://raw.githubusercontent.com/${repo.owner}/${repo.repo}/main/${repo.path}"
                files = when {
                    repo.fallbackFiles.isNotEmpty() -> repo.fallbackFiles.map { Triple(it, "$base/${it.trimStart('/')}", Long.MAX_VALUE) }.toMutableList()
                    repo.json -> mutableListOf(Triple("xtreams.json", "$base/xtreams.json", Long.MAX_VALUE))
                    else -> mutableListOf()
                }
            }

            for (chunk in files.take(20).chunked(MAX_PARALLEL_FETCHES)) {
                coroutineScope {
                    chunk.map { (name, dlUrl, _) ->
                        async { parseGitHubFile(name, dlUrl, repo) }
                    }.awaitAll()
                }.flatten().forEach { p ->
                    val key = "${p.url}|${p.username}|${p.password}"
                    if (seen.add(key)) portals.add(p)
                }
            }
        }

        onEvent(ScrapeEvent.Progress("Bagged ${portals.size} wild nutz so far..."))
        return portals
    }

    private suspend fun parseGitHubFile(name: String, dlUrl: String, repo: GitHubRepo): List<Portal> {
        val text = fetchTextWithHeadersLimited(dlUrl, mapOf("User-Agent" to UA), MAX_RAW_FILE_BYTES) ?: return emptyList()
        val result = mutableListOf<Portal>()
        if (repo.json || name.endsWith(".json")) {
            try {
                val jsonArr = json.parseToJsonElement(text).jsonArray
                for (entry in jsonArr) {
                    val entryObj = entry.jsonObject
                    val pUrl = entryObj["url"]?.jsonPrimitive?.contentOrNull ?: ""
                    val user = (entryObj["username"]?.jsonPrimitive?.contentOrNull ?: entryObj["user"]?.jsonPrimitive?.contentOrNull ?: "")
                    val pass = (entryObj["password"]?.jsonPrimitive?.contentOrNull ?: entryObj["pass"]?.jsonPrimitive?.contentOrNull ?: "")
                    if (pUrl.isNotEmpty() && user.length >= 3 && pass.length >= 3) {
                        result.add(Portal(cleanPortalUrl(pUrl), user, pass, "github/${repo.repo}"))
                    }
                }
            } catch (_: Exception) {}
        } else {
            result.addAll(extractPortals(text, "github/${repo.repo}:$name"))
        }
        return result
    }

    private suspend fun fetchTelegramPortals(onEvent: (ScrapeEvent) -> Unit): List<Portal> {
        onEvent(ScrapeEvent.Progress("Tapping the social vines for nutz..."))
        val seen = mutableSetOf<String>()
        val portals = mutableListOf<Portal>()

        for (chunk in TELEGRAM_CHANNELS.chunked(MAX_PARALLEL_FETCHES)) {
            coroutineScope {
                chunk.map { channel ->
                    async {
                        val channelPortals = mutableListOf<Portal>()
                        try {
                            val url = "https://t.me/s/$channel"
                            val html = fetchTextWithHeaders(url, mapOf("User-Agent" to UA)) ?: return@async emptyList()
                            val msgRegex = Regex("""<div class="tgme_widget_message_text[^"]*"[^>]*>([\s\S]*?)</div>\s*</div>""")
                            for (m in msgRegex.findAll(html)) {
                                val text = m.groupValues[1]
                                    .replace(Regex("<br\\s*/?>"), "\n")
                                    .replace(Regex("<[^>]+>"), "")
                                    .replace("&amp;".toRegex(), "&")
                                    .replace("&lt;".toRegex(), "<")
                                    .replace("&gt;".toRegex(), ">")
                                    .replace("&quot;".toRegex(), "\"")
                                    .trim()
                                if (text.isNotEmpty()) {
                                    val localSeen = mutableSetOf<String>()
                                    collectPortalsFromText(text, "telegram:$channel", localSeen, channelPortals)
                                }
                            }
                        } catch (_: Exception) {}
                        channelPortals
                    }
                }.awaitAll().forEach { channelPortals ->
                    for (p in channelPortals) {
                        val key = "${p.url}|${p.username}|${p.password}"
                        if (seen.add(key)) portals.add(p)
                    }
                }
            }
        }

        onEvent(ScrapeEvent.Progress("Caught ${portals.size} more rolling nutz..."))
        return portals
    }

    private suspend fun fetchRedditPortals(): List<Portal> {
        val seen = mutableSetOf<String>()
        val portals = mutableListOf<Portal>()
        for (chunk in REDDIT_SUBREDDITS.chunked(MAX_PARALLEL_FETCHES)) {
            coroutineScope {
                chunk.map { sub ->
                    async {
                        val subPortals = mutableListOf<Portal>()
                        var rss: String? = null
                        for (host in REDDIT_RSS_HOSTS) {
                            val url = "https://$host/r/$sub/new/.rss"
                            rss = fetchTextWithHeaders(url, mapOf("User-Agent" to "Mozilla/5.0 (compatible; PodcastFeedFetcher/1.0; +http://example.com)"))
                            if (rss != null) break
                        }
                        rss ?: return@async emptyList()
                        try {
                            val itemRegex = Regex("""<item>([\s\S]*?)</item>""")
                            for (item in itemRegex.findAll(rss)) {
                                val itemText = item.groupValues[1]
                                val title = Regex("""<title>([\s\S]*?)</title>""").find(itemText)?.groupValues?.getOrNull(1)
                                val desc = Regex("""<description>([\s\S]*?)</description>""").find(itemText)?.groupValues?.getOrNull(1)
                                val body = "${title.orEmpty()}\n${desc.orEmpty()}"
                                    .replace("&amp;".toRegex(), "&")
                                    .replace("&lt;".toRegex(), "<")
                                    .replace("&gt;".toRegex(), ">")
                                    .replace("&quot;".toRegex(), "\"")
                                if (body.length > 15) {
                                    subPortals.addAll(extractPortals(body, "reddit:$sub"))
                                }
                            }
                        } catch (_: Exception) {}
                        subPortals
                    }
                }.awaitAll().forEach { subPortals ->
                    for (p in subPortals) {
                        val key = "${p.url}|${p.username}|${p.password}"
                        if (seen.add(key)) portals.add(p)
                    }
                }
            }
        }
        return portals
    }

    private suspend fun fetchAmzPortals(onEvent: (ScrapeEvent) -> Unit): List<Portal> {
        onEvent(ScrapeEvent.Progress("Digging through the deep stash..."))
        val seen = mutableSetOf<String>()
        val portals = mutableListOf<Portal>()

        try {
            for (page in 1..5) {
                val html = fetchTextWithHeaders("https://iptv.tutoje.cz/list/?page=$page", mapOf("User-Agent" to UA)) ?: break
                val hashes = Regex("""\?data=([a-f0-9]{32})""").findAll(html).map { it.groupValues[1] }.distinct().toList()
                if (hashes.isEmpty()) break
                for (hash in hashes.take(20)) {
                    val decoded = fetchTextWithHeaders("https://iptv.tutoje.cz/?data=$hash", mapOf("User-Agent" to UA)) ?: continue
                    val serverMatch = Regex("""name="server_url"\s+value="([^"]*)"""").find(decoded)
                    val userMatches = Regex("""<span[^>]*class="[^"]*font-black text-3xl[^"]*"[^>]*>([^<]+)</span>""").findAll(decoded).toList()
                    val server = serverMatch?.groupValues?.getOrNull(1) ?: continue
                    val username = userMatches.getOrNull(0)?.groupValues?.getOrNull(1)?.trim() ?: continue
                    val password = userMatches.getOrNull(1)?.groupValues?.getOrNull(1)?.trim() ?: ""
                    val key = "$server|$username|$password"
                    if (seen.add(key)) portals.add(Portal(server, username, password, "amziptv"))
                }
            }
        } catch (_: Exception) {}

        onEvent(ScrapeEvent.Progress("Unearthed ${portals.size} hidden nutz..."))
        return portals
    }

    private suspend fun verifyPortal(p: Portal): VerifiedPortal? {
        if (isAdultText(p.url)) return null
        return verifyViaPlayerApi(p) ?: verifyViaM3U(p)
    }

    private suspend fun verifyViaPlayerApi(p: Portal): VerifiedPortal? {
        try {
            val url = "${p.url}/player_api.php?username=${p.username}&password=${p.password}"
            val body = fetchTextWithHeaders(url, mapOf("User-Agent" to "VLC/3.0.20")) ?: return null
            try {
                val element = json.parseToJsonElement(body)
                if (element is JsonObject) {
                    val info = element["user_info"]?.jsonObject ?: element
                    val auth = info["auth"]?.jsonPrimitive?.contentOrNull ?: ""
                    val status = info["status"]?.jsonPrimitive?.contentOrNull ?: ""
                    if (auth == "1" || status == "active" || element.containsKey("user_info")) {
                        val name = info["username"]?.jsonPrimitive?.contentOrNull ?: p.username
                        val accountInfo = XtreamClient.parseAccountInfo(body)
                        return VerifiedPortal(
                            portal = p,
                            name = name,
                            domain = extractDomain(p.url),
                            expDate = accountInfo?.expDate,
                            maxConnections = accountInfo?.maxConnections,
                            activeConnections = accountInfo?.activeConnections,
                            status = accountInfo?.status,
                            isTrial = accountInfo?.isTrial,
                        )
                    }
                }
            } catch (_: Exception) {}
        } catch (_: Exception) {}
        return null
    }

    private suspend fun verifyViaM3U(p: Portal): VerifiedPortal? {
        try {
            val url = "${p.url}/get.php?username=${p.username}&password=${p.password}&type=m3u_plus"
            val text = fetchTextWithHeadersLimited(url, mapOf("User-Agent" to "VLC/3.0.20"), MAX_VERIFY_BYTES) ?: return null
            if (Regex("#EXTM3U", RegexOption.IGNORE_CASE).containsMatchIn(text) &&
                !Regex("<html|<head|<body", RegexOption.IGNORE_CASE).containsMatchIn(text)) {
                val urlCount = text.lines().count { it.startsWith("http") }
                if (urlCount >= 5) {
                    val adultRatio = text.lines().count { isAdultText(it) }.toFloat() / text.lines().size.coerceAtLeast(1)
                    if (adultRatio < 0.15f) {
                        return VerifiedPortal(p, p.username, extractDomain(p.url))
                    }
                }
            }
        } catch (_: Exception) {}
        return null
    }

    private suspend fun getChannelCount(p: Portal): Int {
        try {
            val url = "${p.url}/player_api.php?username=${p.username}&password=${p.password}&action=get_live_streams"
            val text = fetchTextWithHeadersLimited(url, mapOf("User-Agent" to "VLC/3.0.20"), MAX_COUNT_BYTES) ?: return 0
            try {
                val arr = json.parseToJsonElement(text).jsonArray
                return arr.size
            } catch (_: Exception) {}
        } catch (_: Exception) {}

        try {
            val url = "${p.url}/get.php?username=${p.username}&password=${p.password}&type=m3u_plus"
            val text = fetchTextWithHeadersLimited(url, mapOf("User-Agent" to "VLC/3.0.20"), MAX_COUNT_BYTES) ?: return 0
            return text.lines().count { it.startsWith("http") }
        } catch (_: Exception) {}
        return 0
    }

    /**
     * Fast channel search for a single portal. Returns up to maxMatches matching channel names.
     */
    private suspend fun searchChannelsForPortal(
        p: Portal,
        termLower: String,
        maxMatches: Int = 3,
    ): List<String> {
        if (termLower.isEmpty()) return emptyList()
        val matches = mutableListOf<String>()
        try {
            val url = "${p.url}/player_api.php?username=${p.username}&password=${p.password}&action=get_live_streams"
            val text = fetchTextWithHeadersLimited(url, mapOf("User-Agent" to "VLC/3.0.20"), MAX_COUNT_BYTES) ?: return emptyList()
            try {
                val arr = json.parseToJsonElement(text).jsonArray
                for (elem in arr) {
                    if (matches.size >= maxMatches) break
                    val obj = elem.jsonObject
                    val title = obj["name"]?.jsonPrimitive?.contentOrNull ?: continue
                    if (title.lowercase().contains(termLower)) matches.add(title)
                }
            } catch (_: Exception) {}
        } catch (_: Exception) {}
        if (matches.isEmpty()) {
            try {
                val url = "${p.url}/player_api.php?username=${p.username}&password=${p.password}&action=get_vod_streams"
                val text = fetchTextWithHeadersLimited(url, mapOf("User-Agent" to "VLC/3.0.20"), MAX_COUNT_BYTES) ?: return matches
                try {
                    val arr = json.parseToJsonElement(text).jsonArray
                    for (elem in arr) {
                        if (matches.size >= maxMatches) break
                        val obj = elem.jsonObject
                        val title = obj["name"]?.jsonPrimitive?.contentOrNull ?: continue
                        if (title.lowercase().contains(termLower)) matches.add(title)
                    }
                } catch (_: Exception) {}
            } catch (_: Exception) {}
        }
        return matches
    }

    private fun cacheKey(vp: VerifiedPortal): String =
        "${domainKey(vp.portal.url)}|${vp.portal.username}|${vp.portal.password}".lowercase()

    private fun CountedPortal.toCachedPortal(): CachedPortal = CachedPortal(
        url = verified.portal.url,
        username = verified.portal.username,
        password = verified.portal.password,
        name = verified.name,
        domain = verified.domain,
        channelCount = min(count, 500),
        expDate = verified.expDate,
        maxConnections = verified.maxConnections,
        activeConnections = verified.activeConnections,
        status = verified.status,
        isTrial = verified.isTrial,
        verifiedAt = TraktPlatformClock.nowEpochMs(),
    )

    private fun CachedPortal.toEntry(label: String): PortalNutzEntry = PortalNutzEntry(
        label = label,
        url = url,
        username = username,
        password = password,
        channelCount = channelCount,
        domain = domain,
        expDate = expDate,
        maxConnections = maxConnections,
        activeConnections = activeConnections,
        status = status,
        isTrial = isTrial,
    )

    private suspend fun verifyGoalOriented(
        candidates: List<Portal>,
        maxResults: Int,
        onTested: (Int) -> Unit,
        onVerified: (VerifiedPortal) -> Unit,
    ): Int {
        if (candidates.isEmpty() || maxResults <= 0) return 0
        val workers = min(MAX_PARALLEL_FETCHES, candidates.size)
        var accepted = 0
        val work = Channel<Portal>(Channel.UNLIMITED)
        val out = Channel<VerifiedPortal?>(Channel.UNLIMITED)
        coroutineScope {
            val workerJobs = List(workers) {
                launch { for (p in work) out.send(verifyPortal(p)) }
            }
            launch {
                candidates.forEach { work.send(it) }
                work.close()
            }
            var tested = 0
            for (v in out) {
                tested++
                onTested(tested)
                if (v != null) {
                    onVerified(v)
                    accepted++
                    if (accepted >= maxResults) break
                }
                if (tested >= candidates.size) break
            }
            coroutineContext.cancelChildren()
            work.close()
        }
        return accepted
    }

    private suspend fun countPortals(
        verified: List<VerifiedPortal>,
        onProgress: (Int, Int) -> Unit,
    ): List<CountedPortal> {
        val result = mutableListOf<CountedPortal>()
        var done = 0
        for (chunk in verified.chunked(MAX_COUNT_PARALLEL)) {
            val counted = coroutineScope {
                chunk.map { vp -> async { CountedPortal(vp, getChannelCount(vp.portal)) } }.awaitAll()
            }
            result.addAll(counted)
            done += counted.size
            onProgress(done, verified.size)
        }
        return result
    }

    private fun presentEntries(
portals: List<CountedPortal>,
          maxPortals: Int,
          minChannels: Int,
          noAdult: Boolean,
          adultOnly: Boolean,
          englishOnly: Boolean,
          sportsOnly: Boolean,
          searchTerm: String = "",
      ): List<PortalNutzEntry> {
           var num = 0
           val termLower = searchTerm.trim().lowercase()
           return portals
               .filter { it.count >= minChannels }
               .filter { if (noAdult) !isAdultText(it.verified.name) else true }
               .filter { if (adultOnly) isAdultText(it.verified.name) else true }
               .filter { if (englishOnly) !hasNonLatinScript(it.verified.name) else true }
               .filter { if (sportsOnly) SPORTS_KEYWORDS.any { k -> it.verified.name.lowercase().contains(k) || it.verified.domain.lowercase().contains(k) } else true }
               .filter { termLower.isEmpty() || it.verified.name.lowercase().contains(termLower) || it.verified.domain.lowercase().contains(termLower) }
               .sortedByDescending { it.count }
               .take(maxPortals)
            .map { cp ->
                num++
                PortalNutzEntry(
                    label = "list$num",
                    url = cp.verified.portal.url,
                    username = cp.verified.portal.username,
                    password = cp.verified.portal.password,
                    channelCount = min(cp.count, 500),
                    domain = cp.verified.domain,
                    expDate = cp.verified.expDate,
                    maxConnections = cp.verified.maxConnections,
                    activeConnections = cp.verified.activeConnections,
                    status = cp.verified.status,
                    isTrial = cp.verified.isTrial,
                )
            }
    }

    fun scrape(
        englishOnly: Boolean = true,
        noAdult: Boolean = true,
        sportsOnly: Boolean = false,
        adultOnly: Boolean = false,
        excludeServers: Set<String> = emptySet(),
        searchTerm: String = "",
        unlimited: Boolean = false,
        onEvent: (ScrapeEvent) -> Unit,
        onComplete: () -> Unit = {},
    ) {
        cancel()
        if (searchTerm.trim().isNotEmpty()) {
            // Use smart channel-aware search when a term is provided
            val scope = CoroutineScope(Dispatchers.Default + Job())
            currentJob = scope.launch {
                try {
                    doSearchPortals(
                        englishOnly = englishOnly,
                        noAdult = noAdult,
                        sportsOnly = sportsOnly,
                        adultOnly = adultOnly,
                        excludeServers = excludeServers,
                        searchTerm = searchTerm,
                        unlimited = unlimited,
                        onEvent = onEvent,
                    )
                } catch (e: kotlinx.coroutines.CancellationException) { throw e }
                catch (e: Exception) { onEvent(ScrapeEvent.Error("Dropped a nutz! ${e.message ?: "Something went wrong"}")) }
                finally { onComplete() }
            }
            return
        }
        // Legacy path — no search term, just return top cached portals
        cancel()
        val scope = CoroutineScope(Dispatchers.Main.immediate + Job())
        currentJob = scope.launch {
            try {
                 val maxPortals = if (unlimited) Int.MAX_VALUE else if (sportsOnly) 10 else 5
                 val candidatePool = if (unlimited) CANDIDATE_POOL_SIZE * 3 else CANDIDATE_POOL_SIZE
                val excludedDomains = excludeServers.mapTo(mutableSetOf()) { domainKey(it) }

                // 1. Fast path — 1-hour verified cache already has good portals
                val cached = portalCache.snapshot()
                    .filter { domainKey(it.url) !in excludedDomains }
                    .filter { if (noAdult) !isAdultText(it.name) else true }
                    .filter { if (adultOnly) isAdultText(it.name) else true }
                    .filter { if (englishOnly) !hasNonLatinScript(it.name) else true }
                    .filter { if (sportsOnly) SPORTS_KEYWORDS.any { k -> it.name.lowercase().contains(k) || it.domain.lowercase().contains(k) } else true }
                    .sortedByDescending { it.channelCount }
                    .take(maxPortals)

                if (cached.isNotEmpty()) {
                    onEvent(ScrapeEvent.Progress("Serving fresh nutz from the stash..."))
                    onEvent(ScrapeEvent.Result(cached.mapIndexed { i, cp -> cp.toEntry("list${i + 1}") }))
                    return@launch
                }

                // 2. Harvest candidates from all sources, in parallel
                onEvent(ScrapeEvent.Progress("Throwing nutz at portals..."))
                val fetched = coroutineScope {
                    listOf(
                        async { fetchGitHubPortals(onEvent) },
                        async { fetchTelegramPortals(onEvent) },
                        async { fetchRedditPortals() },
                        async { fetchAmzPortals(onEvent) },
                    ).awaitAll()
                }
                val raw = fetched.flatten()

                onEvent(ScrapeEvent.Progress("Cracking ${raw.size} shells to find the good ones..."))

                val byCreds = mutableMapOf<String, Portal>()
                for (p in raw) byCreds.putIfAbsent("${p.username}|${p.password}".lowercase(), p)

                val freshPool = byCreds.values
                    .filter { domainKey(it.url) !in excludedDomains }
                    .toList()
                    .shuffled()
                    .take(candidatePool)

                // 3. Goal-oriented verify — emit results as they come in
                onEvent(ScrapeEvent.Progress("Testing nutz... 0/${freshPool.size}"))
                val accumulated = mutableListOf<PortalNutzEntry>()
                val verifiedList = mutableListOf<VerifiedPortal>()
                verifyGoalOriented(freshPool, maxPortals, { tested ->
                    onEvent(ScrapeEvent.Progress("Testing nutz... $tested/${freshPool.size}"))
                }) { vp ->
                    verifiedList.add(vp)
                    accumulated.addAll(presentEntries(listOf(CountedPortal(vp, 0)), maxPortals = maxPortals, minChannels = 0,
                        noAdult = noAdult, adultOnly = adultOnly, englishOnly = englishOnly, sportsOnly = sportsOnly, searchTerm = searchTerm))
                    onEvent(ScrapeEvent.Result(accumulated.toList()))
                }
                if (verifiedList.isEmpty()) {
                    onEvent(ScrapeEvent.Error("No ripe nutz found — try different filters!"))
                    return@launch
                }

                // 4. Background: fetch channel counts, then cache + re-emit with real counts
                val counted = countPortals(verifiedList) { done, total ->
                    onEvent(ScrapeEvent.Progress("Counting channels... $done/$total"))
                }
                portalCache.putAll(counted.map { cacheKey(it.verified) to it.toCachedPortal() })
                val full = presentEntries(counted, maxPortals = maxPortals, minChannels = 5,
                    noAdult = noAdult, adultOnly = adultOnly, englishOnly = englishOnly, sportsOnly = sportsOnly, searchTerm = searchTerm)
                if (full.isNotEmpty()) {
                    onEvent(ScrapeEvent.Result(full.toList()))
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                onEvent(ScrapeEvent.Error("Dropped a nutz! ${e.message ?: "Something went wrong"}"))
            }
        }
    }

    fun scrapeInBackground(
        englishOnly: Boolean = true,
        noAdult: Boolean = true,
        sportsOnly: Boolean = false,
        adultOnly: Boolean = false,
        excludeServers: Set<String> = emptySet(),
        searchTerm: String = "",
        unlimited: Boolean = false,
        onEvent: (ScrapeEvent) -> Unit,
    ) {
        if (pendingSearches.containsKey(searchTerm)) return
        val scope = CoroutineScope(Dispatchers.Default + Job())
        val job = scope.launch {
            val searchJob = coroutineContext[Job]!!
            val pending = PendingSearch(searchTerm, searchJob, onEvent)
            pendingSearches[searchTerm] = pending
            try {
                scrape(
                    englishOnly = englishOnly,
                    noAdult = noAdult,
                    sportsOnly = sportsOnly,
                    adultOnly = adultOnly,
                    excludeServers = excludeServers,
                    searchTerm = searchTerm,
                    unlimited = unlimited,
                    onEvent = onEvent,
                )
            } finally {
                pendingSearches.remove(searchTerm)
            }
        }
    }

    /**
     * Smart channel-aware portal search. Finds portals whose actual channel lists contain
     * the search term, not just portals named after the term.
     */
    fun searchPortals(
        englishOnly: Boolean = true,
        noAdult: Boolean = true,
        sportsOnly: Boolean = false,
        adultOnly: Boolean = false,
        excludeServers: Set<String> = emptySet(),
        searchTerm: String = "",
        unlimited: Boolean = false,
        onEvent: (ScrapeEvent) -> Unit,
    ) {
        if (pendingSearches.containsKey(searchTerm)) return
        val scope = CoroutineScope(Dispatchers.Default + Job())
        val job = scope.launch {
            val searchJob = coroutineContext[Job]!!
            val pending = PendingSearch(searchTerm, searchJob, onEvent)
            pendingSearches[searchTerm] = pending
            try {
                doSearchPortals(
                    englishOnly = englishOnly,
                    noAdult = noAdult,
                    sportsOnly = sportsOnly,
                    adultOnly = adultOnly,
                    excludeServers = excludeServers,
                    searchTerm = searchTerm,
                    unlimited = unlimited,
                    onEvent = onEvent,
                )
            } finally {
                pendingSearches.remove(searchTerm)
            }
        }
    }

    private suspend fun doSearchPortals(
        englishOnly: Boolean,
        noAdult: Boolean,
        sportsOnly: Boolean,
        adultOnly: Boolean,
        excludeServers: Set<String>,
        searchTerm: String,
        unlimited: Boolean,
        onEvent: (ScrapeEvent) -> Unit,
    ) {
        val maxResults = if (unlimited) Int.MAX_VALUE else MAX_SEARCH_RESULTS
        val termLower = searchTerm.trim().lowercase()
        val termTokens = termLower.split(" ").filter { it.isNotBlank() }.toSet()
        val excludedDomains = excludeServers.mapTo(mutableSetOf()) { domainKey(it) }
        val accumulated = mutableListOf<PortalNutzEntry>()
        val seenUrls = mutableSetOf<String>()

        fun addToAccumulated(entry: PortalNutzEntry) {
            if (entry.url !in seenUrls) {
                seenUrls.add(entry.url)
                if (!noAdult && isAdultText(entry.domain)) return
                if (adultOnly && !isAdultText(entry.domain)) return
                if (englishOnly && hasNonLatinScript(entry.domain)) return
                if (sportsOnly && !SPORTS_KEYWORDS.any { k -> entry.domain.lowercase().contains(k) || entry.label.lowercase().contains(k) }) return
                accumulated.add(entry)
                onEvent(ScrapeEvent.Result(accumulated.toList()))
            }
        }

        // Phase 1: Fast cache hit — check cached portals for channel matches
        if (termLower.isNotEmpty()) {
            onEvent(ScrapeEvent.Progress("Checking cached portals for \"$termLower\"..."))
            val cached = portalCache.snapshot()
                .filter { domainKey(it.url) !in excludedDomains }
                .shuffled()
                .take(50)
            var checked = 0
            for (cp in cached) {
                if (accumulated.size >= maxResults) break
                checked++
                if (checked % 10 == 0) {
                    onEvent(ScrapeEvent.Progress("Scanning cache... $checked/${cached.size}"))
                }
                val matches = searchChannelsForPortal(Portal(cp.url, cp.username, cp.password, "cache"), termLower, maxMatches = 2)
                if (matches.isNotEmpty()) {
                    addToAccumulated(cp.toEntry("cache$checked"))
                }
            }
            if (accumulated.isNotEmpty()) {
                onEvent(ScrapeEvent.Progress("Found ${accumulated.size} matching portals in cache"))
            }
        } else {
            // No search term — just serve top cached portals
            val cached = portalCache.snapshot()
                .filter { domainKey(it.url) !in excludedDomains }
                .sortedByDescending { it.channelCount }
                .take(maxResults)
            if (cached.isNotEmpty()) {
                onEvent(ScrapeEvent.Progress("Serving ${cached.size} cached portals from the stash..."))
                onEvent(ScrapeEvent.Result(cached.mapIndexed { i, cp -> cp.toEntry("list${i + 1}") }))
                return
            }
        }

        // Phase 2: Fetch fresh candidates from all sources
        if (accumulated.size < maxResults) {
            onEvent(ScrapeEvent.Progress("Throwing nutz at portals..."))
            val fetched = coroutineScope {
                listOf(
                    async { fetchGitHubPortals(onEvent) },
                    async { fetchTelegramPortals(onEvent) },
                    async { fetchRedditPortals() },
                    async { fetchAmzPortals(onEvent) },
                ).awaitAll()
            }
            val raw = fetched.flatten()
            onEvent(ScrapeEvent.Progress("Harvested ${raw.size} candidate portals"))

            val byCreds = mutableMapOf<String, Portal>()
            for (p in raw) byCreds.putIfAbsent("${p.username}|${p.password}".lowercase(), p)
            val freshPool = byCreds.values
                .filter { domainKey(it.url) !in excludedDomains }
                .toList()
                .shuffled()
                .take(CANDIDATE_POOL_SIZE)

            // Phase 3: Verify and search channels in parallel batches
            var batchStart = 0
            while (batchStart < freshPool.size && accumulated.size < maxResults) {
                val batchEnd = minOf(batchStart + SEARCH_BATCH_SIZE, freshPool.size)
                val batch = freshPool.subList(batchStart, batchEnd)
                onEvent(ScrapeEvent.Progress("Testing nutz batch ${batchStart / SEARCH_BATCH_SIZE + 1}... ${batchStart}/${freshPool.size}"))

                val verified = mutableListOf<VerifiedPortal>()
                verifyGoalOriented(batch, maxResults * 2, { tested ->
                    if (tested % 10 == 0) onEvent(ScrapeEvent.Progress("Testing nutz... $tested/${freshPool.size}"))
                }) { vp ->
                    verified.add(vp)
                }

                if (termLower.isNotEmpty() && verified.isNotEmpty()) {
                    // Search channels for each verified portal
                    val workers = min(MAX_PARALLEL_FETCHES, verified.size)
                    var verifiedIdx = 0
                    coroutineScope {
                        val channels = Channel<VerifiedPortal>(Channel.UNLIMITED)
                        val results = Channel<Pair<VerifiedPortal, List<String>>>(Channel.UNLIMITED)
                        List(workers) {
                            launch {
                                for (vp in channels) {
                                    val matches = searchChannelsForPortal(vp.portal, termLower, maxMatches = 3)
                                    if (matches.isNotEmpty()) results.send(vp to matches)
                                }
                            }
                        }
                        launch {
                            for (vp in verified) {
                                channels.send(vp)
                                verifiedIdx++
                                if (verifiedIdx % 10 == 0) {
                                    onEvent(ScrapeEvent.Progress("Searching channels... $verifiedIdx/${verified.size}"))
                                }
                            }
                            channels.close()
                        }
                        for ((vp, matches) in results) {
                            if (accumulated.size >= maxResults) break
                            val count = getChannelCount(vp.portal)
                            addToAccumulated(PortalNutzEntry(
                                label = "list${accumulated.size + 1}",
                                url = vp.portal.url,
                                username = vp.portal.username,
                                password = vp.portal.password,
                                channelCount = min(count, 500),
                                domain = vp.domain,
                                expDate = vp.expDate,
                                maxConnections = vp.maxConnections,
                                activeConnections = vp.activeConnections,
                                status = vp.status,
                                isTrial = vp.isTrial,
                            ))
                        }
                        results.close()
                    }
                } else {
                    // No search term — just count and add top verified
                    for (vp in verified) {
                        if (accumulated.size >= maxResults) break
                        val count = getChannelCount(vp.portal)
                        addToAccumulated(PortalNutzEntry(
                            label = "list${accumulated.size + 1}",
                            url = vp.portal.url,
                            username = vp.portal.username,
                            password = vp.portal.password,
                            channelCount = min(count, 500),
                            domain = vp.domain,
                            expDate = vp.expDate,
                            maxConnections = vp.maxConnections,
                            activeConnections = vp.activeConnections,
                            status = vp.status,
                            isTrial = vp.isTrial,
                        ))
                    }
                }

                // Cache the verified portals
                portalCache.putAll(verified.map { vp ->
                    val count = getChannelCount(vp.portal)
                    cacheKey(vp) to CachedPortal(
                        url = vp.portal.url,
                        username = vp.portal.username,
                        password = vp.portal.password,
                        name = vp.name,
                        domain = vp.domain,
                        channelCount = min(count, 500),
                        expDate = vp.expDate,
                        maxConnections = vp.maxConnections,
                        activeConnections = vp.activeConnections,
                        status = vp.status,
                        isTrial = vp.isTrial,
                        verifiedAt = TraktPlatformClock.nowEpochMs(),
                    )
                })

                batchStart = batchEnd
            }
        }

        if (accumulated.isEmpty()) {
            onEvent(ScrapeEvent.Error("No ripe nutz found — try different filters!"))
        }
    }

    fun formatAccountInfoLine(
        expDate: Long?,
        maxConnections: Int?,
        activeConnections: Int?,
        status: String?,
        isTrial: Boolean?,
    ): String? {
        val parts = mutableListOf<String>()
        expDate?.let { exp ->
            val days = ((exp - TraktPlatformClock.nowEpochMs()) / 86_400_000L).toInt()
            parts += when {
                days <= 0 -> "Expired"
                days == 1 -> "Exp 1d"
                else -> "Exp ${days}d"
            }
        }
        if (activeConnections != null && maxConnections != null) {
            parts += "$activeConnections/$maxConnections conns"
        } else if (maxConnections != null) {
            parts += "max ${maxConnections} conns"
        }
        if (isTrial == true) parts += "Trial"
        val statusClean = status?.trim()?.takeIf { it.isNotBlank() && !it.equals("Active", true) }
        if (statusClean != null) parts += statusClean
        return parts.joinToString(" · ").ifBlank { null }
    }
}
