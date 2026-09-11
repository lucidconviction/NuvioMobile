package com.nuvio.app.features.hub

import android.content.Context
import android.content.SharedPreferences

internal actual object VidNutzCacheStore {
    private const val PREFS_NAME = "nuvio_vidnutz_cache"
    private const val KEY_CACHE = "cache_json"

    private fun getPrefs(): SharedPreferences? {
        val ctx = com.nuvio.app.appContext ?: return null
        return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    actual fun load(): String? =
        getPrefs()?.getString(KEY_CACHE, null)

    actual fun save(json: String) {
        getPrefs()?.edit()?.putString(KEY_CACHE, json)?.apply()
    }
}
