package com.nuvio.app.features.iptv

import android.content.Context
import android.content.SharedPreferences
import java.io.File

actual object IptvStorage {
    private const val PREFS_NAME = "nuvio_iptv"
    private const val KEY_SETTINGS = "iptv_settings"
    private const val EPG_CACHE_FILE = "epg_cache.json"

    private var preferences: SharedPreferences? = null
    private var cacheDir: File? = null

    fun initialize(context: Context) {
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        cacheDir = context.cacheDir
    }

    actual fun loadSettings(): String? =
        preferences?.getString(KEY_SETTINGS, null)

    actual fun saveSettings(payload: String) {
        preferences?.edit()?.putString(KEY_SETTINGS, payload)?.apply()
    }

    actual fun loadEpgCache(): String? {
        val dir = cacheDir ?: return null
        val file = File(dir, EPG_CACHE_FILE)
        return if (file.exists()) file.readText() else null
    }

    actual fun saveEpgCache(data: String) {
        val dir = cacheDir ?: return
        val file = File(dir, EPG_CACHE_FILE)
        file.writeText(data)
    }
}
