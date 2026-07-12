package com.nuvio.app.features.hub

internal expect object MultiWindowStorage {
    fun loadBookmarks(): String?
    fun saveBookmarks(payload: String)
}
