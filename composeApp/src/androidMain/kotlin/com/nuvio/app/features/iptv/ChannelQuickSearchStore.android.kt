package com.nuvio.app.features.iptv

import android.content.Context
import android.content.SharedPreferences

internal actual object ChannelQuickSearchStore {
    private const val PREFS_NAME = "nuvio_iptv"
    private const val KEY_TERMS = "saved_searches"
    private const val MAX_TERMS = 20

    private fun getPrefs(): SharedPreferences? {
        val ctx = com.nuvio.app.appContext ?: return null
        return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    actual fun loadTerms(): List<String> {
        val raw = getPrefs()?.getString(KEY_TERMS, null) ?: return emptyList()
        return raw.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
    }

    actual fun addTerm(term: String) {
        val normalized = term.trim()
        if (normalized.isEmpty()) return
        val updated = (loadTerms().filterNot { it.equals(normalized, ignoreCase = true) } + normalized).take(MAX_TERMS)
        getPrefs()?.edit()?.putString(KEY_TERMS, updated.joinToString("\n"))?.apply()
    }

    actual fun removeTerm(term: String) {
        val updated = loadTerms().filterNot { it.equals(term, ignoreCase = true) }
        getPrefs()?.edit()?.putString(KEY_TERMS, updated.joinToString("\n"))?.apply()
    }
}