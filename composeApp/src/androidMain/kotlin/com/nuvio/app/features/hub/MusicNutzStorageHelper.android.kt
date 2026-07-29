package com.nuvio.app.features.hub

import android.content.Context
import android.content.SharedPreferences

internal actual object MusicNutzStorageHelper {
    private const val PREFS_NAME = "nuvio_musicnutz"

    private fun getPrefs(): SharedPreferences? {
        val ctx = com.nuvio.app.appContext ?: return null
        return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    actual fun save(key: String, value: String) {
        getPrefs()?.edit()?.putString(key, value)?.apply()
    }

    actual fun load(key: String): String? {
        return getPrefs()?.getString(key, null)
    }
}
