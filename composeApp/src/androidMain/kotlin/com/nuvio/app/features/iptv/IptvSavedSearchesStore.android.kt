package com.nuvio.app.features.iptv

import android.content.Context
import android.content.SharedPreferences

internal actual object IptvSavedSearchesStore {
    private const val PREFS_NAME = "nuvio_iptv"
    private const val KEY_SAVED = "saved_searches"
    private const val MAX_ITEMS = 20

    private fun getPrefs(): SharedPreferences? {
        val ctx = com.nuvio.app.appContext ?: return null
        return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    actual fun load(): List<String> {
        val raw = getPrefs()?.getString(KEY_SAVED, null) ?: return emptyList()
        return raw.split("\n").filter { it.isNotBlank() }
    }

    actual fun save(searches: List<String>) {
        val trimmed = searches.take(MAX_ITEMS)
        getPrefs()?.edit()?.putString(KEY_SAVED, trimmed.joinToString("\n"))?.apply()
    }
}