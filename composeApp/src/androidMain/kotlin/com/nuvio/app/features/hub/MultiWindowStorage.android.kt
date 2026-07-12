package com.nuvio.app.features.hub

import android.content.Context
import android.content.SharedPreferences

actual object MultiWindowStorage {
    private const val PREFS_NAME = "nuvio_multinutz"
    private const val KEY_BOOKMARKS = "multinutz_bookmarks"

    private var preferences: SharedPreferences? = null

    fun initialize(context: Context) {
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    actual fun loadBookmarks(): String? =
        preferences?.getString(KEY_BOOKMARKS, null)

    actual fun saveBookmarks(payload: String) {
        preferences?.edit()?.putString(KEY_BOOKMARKS, payload)?.apply()
    }
}
