package com.nuvio.app.features.hub

internal expect object TeleNutzStorage {
    fun loadBookmarksPayload(): String?
    fun saveBookmarksPayload(payload: String)
    fun loadDownloadsPayload(): String?
    fun saveDownloadsPayload(payload: String)
}
