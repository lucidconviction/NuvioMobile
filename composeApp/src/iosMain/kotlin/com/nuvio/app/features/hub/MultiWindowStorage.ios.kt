package com.nuvio.app.features.hub

import platform.Foundation.NSUserDefaults

actual object MultiWindowStorage {
    private const val KEY_BOOKMARKS = "multinutz_bookmarks"

    actual fun loadBookmarks(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(KEY_BOOKMARKS)

    actual fun saveBookmarks(payload: String) {
        NSUserDefaults.standardUserDefaults.setObject(payload, forKey = KEY_BOOKMARKS)
        NSUserDefaults.standardUserDefaults.synchronize()
    }
}
