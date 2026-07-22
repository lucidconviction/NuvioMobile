package com.nuvio.app.features.hub

import android.content.Context
import android.content.SharedPreferences

internal actual object TeleNutzStorage {
    private const val PREFS_NAME = "nuvio_telenutz"
    private const val KEY_BOOKMARKS = "telenutz_bookmarks"
    private const val KEY_DOWNLOADS = "telenutz_downloads"

    private fun getPrefs(): SharedPreferences? {
        val ctx = com.nuvio.app.appContext ?: return null
        return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    actual fun loadBookmarksPayload(): String? =
        getPrefs()?.getString(KEY_BOOKMARKS, null)

    actual fun saveBookmarksPayload(payload: String) {
        getPrefs()?.edit()?.putString(KEY_BOOKMARKS, payload)?.apply()
    }

    actual fun loadDownloadsPayload(): String? =
        getPrefs()?.getString(KEY_DOWNLOADS, null)

    actual fun saveDownloadsPayload(payload: String) {
        getPrefs()?.edit()?.putString(KEY_DOWNLOADS, payload)?.apply()
    }
}
