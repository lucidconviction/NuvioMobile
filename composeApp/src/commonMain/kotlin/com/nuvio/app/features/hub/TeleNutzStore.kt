package com.nuvio.app.features.hub

import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

object TeleNutzStore {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }

    private var bookmarks: MutableList<TeleNutzVideo> = mutableListOf()
    private var downloads: MutableList<TeleNutzVideo> = mutableListOf()
    private var loaded = false

    fun getBookmarks(): List<TeleNutzVideo> {
        ensureLoaded()
        return bookmarks.toList()
    }

    fun getDownloads(): List<TeleNutzVideo> {
        ensureLoaded()
        return downloads.toList()
    }

    fun isBookmarked(id: Long, chatId: Long): Boolean {
        ensureLoaded()
        return bookmarks.any { it.id == id && it.chatId == chatId }
    }

    fun isDownloaded(id: Long, chatId: Long): Boolean {
        ensureLoaded()
        return downloads.any { it.id == id && it.chatId == chatId && it.isDownloaded }
    }

    fun toggleBookmark(video: TeleNutzVideo): Boolean {
        ensureLoaded()
        val index = bookmarks.indexOfFirst { it.id == video.id && it.chatId == video.chatId }
        val nowBookmarked = if (index >= 0) {
            bookmarks.removeAt(index)
            false
        } else {
            bookmarks.add(video.copy(isBookmarked = true))
            true
        }
        persist()
        return nowBookmarked
    }

    fun saveDownload(video: TeleNutzVideo) {
        ensureLoaded()
        val index = downloads.indexOfFirst { it.id == video.id && it.chatId == video.chatId }
        if (index >= 0) {
            downloads[index] = video
        } else {
            downloads.add(video)
        }
        persist()
    }

    fun removeDownload(id: Long, chatId: Long): TeleNutzVideo? {
        ensureLoaded()
        val index = downloads.indexOfFirst { it.id == id && it.chatId == chatId }
        return if (index >= 0) {
            val removed = downloads.removeAt(index)
            persist()
            removed
        } else null
    }

    private fun ensureLoaded() {
        if (loaded) return
        try {
            val bookmarksJson = TeleNutzStorage.loadBookmarksPayload()
            if (!bookmarksJson.isNullOrBlank()) {
                bookmarks = json.decodeFromString<List<TeleNutzVideo>>(bookmarksJson).toMutableList()
            }
            val downloadsJson = TeleNutzStorage.loadDownloadsPayload()
            if (!downloadsJson.isNullOrBlank()) {
                downloads = json.decodeFromString<List<TeleNutzVideo>>(downloadsJson).toMutableList()
            }
        } catch (_: Exception) {}
        loaded = true
    }

    private fun persist() {
        try {
            TeleNutzStorage.saveBookmarksPayload(json.encodeToString(bookmarks.toList()))
            TeleNutzStorage.saveDownloadsPayload(json.encodeToString(downloads.toList()))
        } catch (_: Exception) {}
    }
}
