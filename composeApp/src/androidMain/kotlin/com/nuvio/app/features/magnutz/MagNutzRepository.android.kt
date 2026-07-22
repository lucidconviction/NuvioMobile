package com.nuvio.app.features.magnutz

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

actual object MagNutzRepository {
    private val _uiState = MutableStateFlow(MagNutzUiState())
    actual val uiState: StateFlow<MagNutzUiState> = _uiState.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var statsJobs = mutableMapOf<String, Job>()
    private var torrentHashes = mutableMapOf<String, String>()
    private var hasLoaded = false
    internal var appContext: android.content.Context? = null
    actual var pendingMagnetFromExternal: String? = null
    val saveLocationPickerTrigger = mutableLongStateOf(0L)
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val torrServerBase: String get() = "http://127.0.0.1:8091"
    private val JSON_TYPE = "application/json".toMediaType()

    private val DEFAULT_TRACKERS = listOf(
        "udp://tracker.opentrackr.org:1337/announce",
        "udp://open.stealth.si:80/announce",
        "udp://tracker.openbittorrent.com:6969/announce",
        "udp://exodus.desync.com:6969/announce",
        "udp://tracker.torrent.eu.org:451/announce",
        "udp://tracker.moeking.me:6969/announce",
        "https://tracker.tamersunion.org:443/announce",
        "wss://tracker.btorrent.xyz",
        "wss://tracker.openwebtorrent.com",
    )

    fun initialize(context: android.content.Context) {
        appContext = context.applicationContext
        MagNutzStorage.initialize(context.applicationContext)
    }

    actual fun ensureLoaded() {
        if (hasLoaded) return
        hasLoaded = true
        val items = MagNutzStorage.loadItems()
        publish(items)
    }

    actual fun consumePendingMagnet() {
        val magnet = pendingMagnetFromExternal ?: return
        pendingMagnetFromExternal = null
        addMagnet(magnet)
    }

    actual fun addMagnet(magnetUri: String, title: String?) {
        val parsed = MagNutzMagnetParser.parseMagnet(magnetUri)
        if (parsed == null) {
            publish(error = "Invalid magnet link")
            return
        }

        val items = _uiState.value.items.toMutableList()
        val existing = items.any { it.infoHash == parsed.infoHash }
        if (existing) {
            publish(error = "Torrent already added")
            return
        }

        val item = MagNutzItem(
            id = "mg_${System.currentTimeMillis()}_${parsed.infoHash.take(8)}",
            magnetUri = parsed.uri,
            infoHash = parsed.infoHash,
            title = title ?: parsed.title,
            trackers = parsed.trackers,
            status = MagNutzStatus.Queued,
            createdAtEpochMs = System.currentTimeMillis(),
        )
        items.add(item)
        publish(items)

        scope.launch { startTorrentDownload(item) }
    }

    actual fun addFromAddon(infoHash: String, fileIdx: Int?, filename: String?, magnetUri: String?, title: String?, trackers: List<String>) {
        val uri = magnetUri ?: MagNutzMagnetParser.run {
            "magnet:?xt=urn:btih:$infoHash" + trackers.joinToString("") { "&tr=${java.net.URLEncoder.encode(it, "UTF-8")}" }
        }
        addMagnet(uri, title ?: filename ?: infoHash.take(8))
    }

    actual fun pauseDownload(id: String) {
        val items = _uiState.value.items.toMutableList()
        val idx = items.indexOfFirst { it.id == id }
        if (idx < 0) return
        items[idx] = items[idx].copy(status = MagNutzStatus.Paused)
        publish(items)
        val hash = torrentHashes[id]
        if (hash != null) stopTorrent(hash)
        statsJobs[id]?.cancel()
    }

    actual fun resumeDownload(id: String) {
        val items = _uiState.value.items.toMutableList()
        val idx = items.indexOfFirst { it.id == id }
        if (idx < 0) return
        val item = items[idx]
        items[idx] = item.copy(status = MagNutzStatus.Queued)
        publish(items)
        scope.launch { startTorrentDownload(item) }
    }

    actual fun cancelDownload(id: String) {
        val items = _uiState.value.items.toMutableList()
        items.removeAll { it.id == id }
        publish(items)
        val hash = torrentHashes[id]
        if (hash != null) dropTorrent(hash)
        statsJobs[id]?.cancel()
        torrentHashes.remove(id)
        deleteLocalFiles(id)
    }

    actual fun playDownload(item: MagNutzItem): com.nuvio.app.features.player.PlayerLaunch? {
        if (!item.isPlayable) return null
        return com.nuvio.app.features.player.PlayerLaunch(
            profileId = 0,
            title = item.title,
            sourceUrl = item.localFilePath ?: return null,
            streamTitle = item.title,
            providerName = "MagNutz",
            parentMetaId = "magnutz",
            parentMetaType = "download",
        )
    }

    actual fun clearError() {
        publish(error = null)
    }

    actual fun setFilter(filter: MagNutzFilter) {
        publish(filter = filter)
    }

    actual fun setSearchQuery(query: String) {
        publish(searchQuery = query)
    }

    actual fun setAddingMagnet(adding: Boolean) {
        publish(isAddingMagnet = adding, magnetInputText = if (adding) _uiState.value.magnetInputText else "")
    }

    actual fun setMagnetInputText(text: String) {
        publish(magnetInputText = text)
    }

    actual fun getSaveLocationDisplayPath(): String {
        val uri = MagNutzStorage.loadSaveLocationUri()
        if (uri != null) {
            return try {
                val parsed = android.net.Uri.parse(uri)
                parsed.path ?: uri
            } catch (_: Exception) { uri }
        }
        val default = java.io.File(appContext?.filesDir, "downloads/magnutz")
        return default.absolutePath
    }

    actual fun setSaveLocationUri(uri: String?) {
        MagNutzStorage.saveSaveLocationUri(uri)
    }

    actual fun requestPickSaveLocation() {
        saveLocationPickerTrigger.longValue = System.currentTimeMillis()
    }

    private suspend fun startTorrentDownload(item: MagNutzItem) {
        try {
            com.nuvio.app.features.p2p.P2pStreamingEngine.startTorrServer()

            val items = _uiState.value.items.toMutableList()
            val idx = items.indexOfFirst { it.id == item.id }
            if (idx < 0) return
            items[idx] = items[idx].copy(status = MagNutzStatus.Downloading)
            publish(items)

            val hash = addTorrent(item.magnetUri, item.title)
            if (hash == null) {
                failItem(item.id, "Failed to add torrent")
                return
            }
            torrentHashes[item.id] = hash

            val resolvedIdx = resolveFileIndex(hash, item.fileIdx, item.fileName)
            startStatsPolling(item.id, hash)

            val downloadDir = getDownloadDir(item.id)
            downloadDir.mkdirs()

            val streamUrl = "$torrServerBase/stream?link=${URLEncoder.encode(item.magnetUri, "UTF-8")}&index=$resolvedIdx&play"
            
            waitForCompletion(item.id, hash, downloadDir, streamUrl)
        } catch (e: Exception) {
            failItem(item.id, e.message ?: "Download error")
        }
    }

    private suspend fun waitForCompletion(id: String, hash: String, downloadDir: File, streamUrl: String) {
        var stalledCount = 0
        var lastBytes = 0L

        while (true) {
            delay(3_000L)
            
            val items = _uiState.value.items.toMutableList()
            val idx = items.indexOfFirst { it.id == id }
            if (idx < 0 || items[idx].status == MagNutzStatus.Paused || items[idx].status == MagNutzStatus.Failed) return

            val stats = getTorrentStats(hash) ?: continue
            val loadedSize = stats.loadedSize
            val torrentSize = stats.torrentSize

            items[idx] = items[idx].copy(
                downloadedBytes = loadedSize,
                totalBytes = torrentSize,
                downloadSpeed = stats.downloadSpeed,
                uploadSpeed = stats.uploadSpeed,
                peers = stats.peers,
                seeds = stats.seeds,
                ratio = if (torrentSize > 0) loadedSize.toFloat() / torrentSize else 0f,
            )
            publish(items)

            if (loadedSize == lastBytes) stalledCount++ else stalledCount = 0
            lastBytes = loadedSize

            if (torrentSize > 0 && loadedSize >= torrentSize) {
                completeDownload(id, hash, downloadDir)
                return
            }

            if (stalledCount > 20 && torrentSize > 0 && loadedSize > 0) {
                completeDownload(id, hash, downloadDir)
                return
            }
        }
    }

    private suspend fun completeDownload(id: String, hash: String, downloadDir: File) {
        dropTorrent(hash)
        statsJobs[id]?.cancel()
        torrentHashes.remove(id)

        val items = _uiState.value.items.toMutableList()
        val idx = items.indexOfFirst { it.id == id }
        if (idx < 0) return

        val localFile = findDownloadedFile(downloadDir)
        val filePath = localFile?.absolutePath
        val title = items[idx].title

        if (filePath != null) {
            val renamed = File(downloadDir, "${sanitizeFileName(title)}.${localFile!!.extension}")
            localFile.renameTo(renamed)
            items[idx] = items[idx].copy(
                status = MagNutzStatus.Completed,
                completedAtEpochMs = System.currentTimeMillis(),
                totalBytes = renamed.length(),
                downloadedBytes = renamed.length(),
                localFilePath = renamed.absolutePath,
            )
        } else {
            items[idx] = items[idx].copy(
                status = MagNutzStatus.Completed,
                completedAtEpochMs = System.currentTimeMillis(),
            )
        }
        publish(items)
    }

    private fun failItem(id: String, message: String) {
        val items = _uiState.value.items.toMutableList()
        val idx = items.indexOfFirst { it.id == id }
        if (idx < 0) return
        items[idx] = items[idx].copy(status = MagNutzStatus.Failed, errorMessage = message)
        publish(items)
        statsJobs[id]?.cancel()
        torrentHashes.remove(id)
    }

    private fun startStatsPolling(id: String, hash: String) {
        statsJobs[id]?.cancel()
        statsJobs[id] = scope.launch {
            while (isActive) {
                delay(2_000L)
                val items = _uiState.value.items.toMutableList()
                val idx = items.indexOfFirst { it.id == id }
                if (idx < 0 || items[idx].status != MagNutzStatus.Downloading) return@launch
                val stats = getTorrentStats(hash) ?: continue
                items[idx] = items[idx].copy(
                    downloadSpeed = stats.downloadSpeed,
                    uploadSpeed = stats.uploadSpeed,
                    peers = stats.peers,
                    seeds = stats.seeds,
                    downloadedBytes = stats.loadedSize,
                    totalBytes = stats.torrentSize,
                    ratio = if (stats.torrentSize > 0) stats.loadedSize.toFloat() / stats.torrentSize else 0f,
                )
                publish(items)
            }
        }
    }

    private suspend fun addTorrent(magnetLink: String, title: String? = null): String? = withContext(Dispatchers.IO) {
        val enhancedLink = if (!DEFAULT_TRACKERS.any { magnetLink.contains("tr=") }) {
            magnetLink + DEFAULT_TRACKERS.joinToString("") { "&tr=${java.net.URLEncoder.encode(it, "UTF-8")}" }
        } else magnetLink
        val body = JSONObject().apply {
            put("action", "add")
            put("link", enhancedLink)
            put("save_to_db", false)
            if (title != null) put("title", title)
        }
        val request = Request.Builder()
            .url("$torrServerBase/torrents")
            .post(body.toString().toRequestBody(JSON_TYPE))
            .build()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val json = JSONObject(response.body?.string() ?: "{}")
                json.optString("hash", "").ifEmpty { null }
            }
        } catch (e: Exception) { null }
    }

    private suspend fun getTorrentStats(hash: String): TorrServerStats? = withContext(Dispatchers.IO) {
        val body = JSONObject().apply { put("action", "get"); put("hash", hash) }
        val request = Request.Builder()
            .url("$torrServerBase/torrents")
            .post(body.toString().toRequestBody(JSON_TYPE))
            .build()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val json = JSONObject(response.body?.string() ?: "{}")
                val files = mutableListOf<TorrServerFile>()
                val fileList = json.optJSONArray("file_stats") ?: JSONArray()
                for (i in 0 until fileList.length()) {
                    val f = fileList.getJSONObject(i)
                    files.add(TorrServerFile(f.optInt("id", i + 1), f.optString("path", ""), f.optLong("length", 0)))
                }
                TorrServerStats(
                    json.optLong("download_speed", 0), json.optLong("upload_speed", 0),
                    json.optInt("active_peers", 0), json.optInt("connected_seeders", 0),
                    json.optLong("preloaded_bytes", 0), json.optLong("loaded_size", 0),
                    json.optLong("torrent_size", 0), files,
                )
            }
        } catch (e: Exception) { null }
    }

    private suspend fun resolveFileIndex(hash: String, requestedIdx: Int?, filename: String?): Int {
        val deadline = System.currentTimeMillis() + 15_000L
        var files: List<TorrServerFile> = emptyList()
        while (System.currentTimeMillis() < deadline) {
            val stats = getTorrentStats(hash)
            files = stats?.files ?: emptyList()
            if (files.isNotEmpty()) break
            delay(1_000L)
        }
        if (files.isEmpty()) return requestedIdx?.plus(1) ?: 1
        if (!filename.isNullOrBlank()) {
            files.firstOrNull { it.path.substringAfterLast('/').equals(filename, ignoreCase = true) }?.let { return it.id }
        }
        if (requestedIdx != null && files.any { it.id == requestedIdx + 1 }) return requestedIdx + 1
        return files.maxByOrNull { it.length }?.id ?: 1
    }

    private fun stopTorrent(hash: String) {
        try {
            val body = JSONObject().apply { put("action", "stop"); put("hash", hash) }
            val request = Request.Builder()
                .url("$torrServerBase/torrents")
                .post(body.toString().toRequestBody(JSON_TYPE))
                .build()
            client.newCall(request).execute().close()
        } catch (_: Exception) {}
    }

    private fun dropTorrent(hash: String) {
        try {
            val body = JSONObject().apply { put("action", "drop"); put("hash", hash) }
            val request = Request.Builder()
                .url("$torrServerBase/torrents")
                .post(body.toString().toRequestBody(JSON_TYPE))
                .build()
            client.newCall(request).execute().close()
        } catch (_: Exception) {}
    }

    private fun getDownloadDir(id: String): File {
        val base = MagNutzStorage.getSaveDir()
        val dir = File(base, id)
        dir.mkdirs()
        return dir
    }

    private fun findDownloadedFile(dir: File): File? {
        return dir.listFiles()?.filter { it.isFile && it.length() > 0 }?.maxByOrNull { it.length() }
            ?: dir.listFiles()?.filter { it.isFile }?.maxByOrNull { it.lastModified() }
    }

    private fun deleteLocalFiles(id: String) {
        val dir = File(MagNutzStorage.getSaveDir(), id)
        if (dir.exists()) dir.deleteRecursively()
    }

    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[:\\\\/*?\"<>|]"), "_").take(100)
    }

    private fun publish(
        items: List<MagNutzItem>? = null,
        filter: MagNutzFilter? = null,
        searchQuery: String? = null,
        isAddingMagnet: Boolean? = null,
        magnetInputText: String? = null,
        error: String? = null,
    ) {
        val current = _uiState.value
        _uiState.value = current.copy(
            items = items ?: current.items,
            filter = filter ?: current.filter,
            searchQuery = searchQuery ?: current.searchQuery,
            isAddingMagnet = isAddingMagnet ?: current.isAddingMagnet,
            magnetInputText = magnetInputText ?: current.magnetInputText,
            error = error ?: current.error,
        )
        if (items != null) MagNutzStorage.saveItems(items)
    }

    private data class TorrServerFile(val id: Int, val path: String, val length: Long)
    private data class TorrServerStats(
        val downloadSpeed: Long, val uploadSpeed: Long, val peers: Int, val seeds: Int,
        val preloadedBytes: Long, val loadedSize: Long, val torrentSize: Long, val files: List<TorrServerFile>,
    )
}

@Composable
fun MagNutzSaveLocationPickerEffect() {
    val triggerValue = MagNutzRepository.saveLocationPickerTrigger.longValue
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri != null) {
            MagNutzRepository.appContext?.contentResolver?.takePersistableUriPermission(
                uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
            MagNutzRepository.setSaveLocationUri(uri.toString())
        }
    }
    LaunchedEffect(triggerValue) {
        if (triggerValue > 0L) {
            launcher.launch(null)
        }
    }
}
