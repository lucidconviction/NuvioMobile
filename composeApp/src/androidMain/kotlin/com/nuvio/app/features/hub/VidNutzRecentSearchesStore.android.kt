package com.nuvio.app.features.hub

import android.content.Context
import android.content.SharedPreferences

internal actual object VidNutzRecentSearchesStore {
    private const val PREFS_NAME = "nuvio_vidnutz"
    private const val KEY_RECENT = "recent_searches"
    private const val MAX_ITEMS = 10

    private fun getPrefs(): SharedPreferences? {
        val ctx = com.nuvio.app.appContext ?: return null
        return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    actual fun load(): List<String> {
        val raw = getPrefs()?.getString(KEY_RECENT, null) ?: return emptyList()
        return raw.split("\n").filter { it.isNotBlank() }
    }

    actual fun save(searches: List<String>) {
        val trimmed = searches.take(MAX_ITEMS)
        getPrefs()?.edit()?.putString(KEY_RECENT, trimmed.joinToString("\n"))?.apply()
    }
}
