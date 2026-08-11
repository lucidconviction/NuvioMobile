package com.nuvio.app.features.hub

import com.nuvio.app.features.player.PlayerLaunch
import com.nuvio.app.features.trakt.TraktPlatformClock
import kotlinx.coroutines.delay

object TeleNutzRepository {
    val engine = TelegramTdEngine()
    var lastSearchQuery: String = ""
    var lastSearchResults: List<TeleNutzVideo> = emptyList()

    suspend fun start() { engine.start() }
    suspend fun close() { engine.close() }
    fun getAuthInfo() = engine.getAuthInfo()
    suspend fun setPhoneNumber(phone: String) { engine.setPhoneNumber(phone) }
    suspend fun checkAuthCode(code: String) { engine.checkAuthCode(code) }
    suspend fun checkPassword(password: String) { engine.checkPassword(password) }
    suspend fun requestQrCode() { engine.requestQrCode() }

    suspend fun searchVideos(query: String): List<TeleNutzVideo> {
        val messages = engine.searchVideoMessages(query)
        val results = messages.map { msg ->
            val bookmarked = TeleNutzStore.isBookmarked(msg.id, msg.chatId)
            val downloaded = TeleNutzStore.isDownloaded(msg.id, msg.chatId)
            msg.toTeleNutzVideo(isBookmarked = bookmarked, isDownloaded = downloaded)
        }
        lastSearchQuery = query
        lastSearchResults = results
        return results
    }

    fun getBookmarkedVideos(): List<TeleNutzVideo> = TeleNutzStore.getBookmarks()
    fun getDownloadedVideos(): List<TeleNutzVideo> = TeleNutzStore.getDownloads()

    fun toggleBookmark(video: TeleNutzVideo): Boolean {
        return TeleNutzStore.toggleBookmark(video)
    }

    suspend fun downloadVideo(
        video: TeleNutzVideo,
        onProgress: (Float) -> Unit,
    ): TeleNutzVideo? {
        if (video.fileId <= 0) return null
        val path = engine.downloadVideoFile(video.fileId, onProgress) ?: return null
        val updated = video.copy(
            localPath = path,
            isDownloaded = true,
            downloadProgress = 1f,
        )
        TeleNutzStore.saveDownload(updated)
        return updated
    }

    suspend fun cancelDownload(video: TeleNutzVideo) {
        if (video.fileId > 0) {
            engine.cancelDownload(video.fileId)
        }
    }

    suspend fun deleteDownload(video: TeleNutzVideo): Boolean {
        val path = video.localPath
        if (!path.isNullOrBlank()) {
            engine.deleteVideoFile(path)
        }
        TeleNutzStore.removeDownload(video.id, video.chatId)
        return true
    }

    suspend fun resolveVideoPlayback(video: TeleNutzVideo): PlayerLaunch? {
        val title = video.text.ifBlank { video.chatTitle }.take(120)
        var playUrl: String? = null

        if (!video.localPath.isNullOrBlank()) {
            playUrl = if (video.localPath.startsWith("file://")) video.localPath else "file://${video.localPath}"
        } else if (video.fileId > 0) {
            try {
                val existing = engine.getFileDownloadState(video.fileId)
                if (existing != null && existing.isComplete && existing.path.isNotBlank()) {
                    playUrl = if (existing.path.startsWith("file://")) existing.path else "file://${existing.path}"
                    TeleNutzStore.saveDownload(video.copy(localPath = existing.path, isDownloaded = true))
                } else {
                    val localPath = engine.startProgressiveDownload(video.fileId)
                    if (!localPath.isNullOrBlank() && waitForTelegramBytes(video.fileId, minBytes = 64L * 1024L, timeoutMs = 15_000L)) {
                        playUrl = "tdlib://${video.fileId}$localPath"
                    }
                }
            } catch (_: Exception) {}
        }

        if (playUrl.isNullOrBlank()) return null

        return PlayerLaunch(
            profileId = 0,
            title = title,
            sourceUrl = playUrl,
            streamTitle = title,
            providerName = "Telegram",
            parentMetaId = "telenutz",
            parentMetaType = "telenutz",
        )
    }

    private suspend fun waitForTelegramBytes(fileId: Int, minBytes: Long, timeoutMs: Long): Boolean {
        val deadline = TraktPlatformClock.nowEpochMs() + timeoutMs
        while (TraktPlatformClock.nowEpochMs() < deadline) {
            val state = try { engine.peekFileDownloadState(fileId) } catch (_: Exception) { null }
            if (state != null && state.downloadedSize >= minBytes) return true
            delay(250L)
        }
        val finalState = try { engine.peekFileDownloadState(fileId) } catch (_: Exception) { null }
        return finalState != null && finalState.downloadedSize >= minBytes
    }

    fun TdMessage.toTeleNutzVideo(isBookmarked: Boolean = false, isDownloaded: Boolean = false): TeleNutzVideo {
        return TeleNutzVideo(
            id = id,
            chatId = chatId,
            chatTitle = chatTitle,
            text = text,
            date = date,
            thumbnailUrl = thumbnailUrl,
            fileId = fileId,
            localPath = localPath,
            fileSize = fileSize,
            isDownloaded = isDownloaded,
            isBookmarked = isBookmarked,
        )
    }
}
