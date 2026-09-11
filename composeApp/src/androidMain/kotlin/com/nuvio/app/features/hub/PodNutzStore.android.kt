package com.nuvio.app.features.hub

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.json.Json

internal actual object PodNutzStore {
    private const val PREFS_NAME = "nuvio_podnutz"
    private const val KEY_SAVED = "saved_podcasts"
    private const val MAX_ITEMS = 50
    private val json = Json { ignoreUnknownKeys = true }

    private fun getPrefs(): SharedPreferences? {
        val ctx = com.nuvio.app.appContext ?: return null
        return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    actual fun loadSaved(): List<Podcast> {
        val raw = getPrefs()?.getString(KEY_SAVED, null) ?: return emptyList()
        return runCatching { json.decodeFromString<List<Podcast>>(raw) }.getOrDefault(emptyList())
    }

    actual fun saveSaved(saved: List<Podcast>) {
        val trimmed = saved.take(MAX_ITEMS)
        getPrefs()?.edit()?.putString(KEY_SAVED, json.encodeToString(trimmed))?.apply()
    }
}