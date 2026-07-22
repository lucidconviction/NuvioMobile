package com.nuvio.app.features.hub

import java.io.File

internal actual object TeleNutzStorage {
    private val dataDir: File by lazy {
        val userHome = System.getProperty("user.home") ?: "."
        File(userHome, ".nuvio/telenutz").also { it.mkdirs() }
    }

    private val bookmarksFile: File get() = File(dataDir, "bookmarks.json")
    private val downloadsFile: File get() = File(dataDir, "downloads.json")

    actual fun loadBookmarksPayload(): String? {
        return if (bookmarksFile.exists()) bookmarksFile.readText() else null
    }

    actual fun saveBookmarksPayload(payload: String) {
        bookmarksFile.writeText(payload)
    }

    actual fun loadDownloadsPayload(): String? {
        return if (downloadsFile.exists()) downloadsFile.readText() else null
    }

    actual fun saveDownloadsPayload(payload: String) {
        downloadsFile.writeText(payload)
    }
}
