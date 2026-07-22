package com.nuvio.app.features.hub

import platform.Foundation.NSUserDefaults

internal actual object TeleNutzStorage {
    private const val KEY_BOOKMARKS = "telenutz_bookmarks"
    private const val KEY_DOWNLOADS = "telenutz_downloads"

    actual fun loadBookmarksPayload(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(KEY_BOOKMARKS)

    actual fun saveBookmarksPayload(payload: String) {
        NSUserDefaults.standardUserDefaults.setObject(payload, forKey = KEY_BOOKMARKS)
    }

    actual fun loadDownloadsPayload(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(KEY_DOWNLOADS)

    actual fun saveDownloadsPayload(payload: String) {
        NSUserDefaults.standardUserDefaults.setObject(payload, forKey = KEY_DOWNLOADS)
    }
}
