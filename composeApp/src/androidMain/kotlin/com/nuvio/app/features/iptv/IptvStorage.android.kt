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

    actual fun loadChannelCache(sourceUrl: String): String? {
        val dir = cacheDir ?: return null
        val file = File(dir, "ch_cache_${sourceUrl.hashCode()}.json")
        return if (file.exists()) file.readText() else null
    }

    actual fun saveChannelCache(sourceUrl: String, data: String) {
        val dir = cacheDir ?: return
        val file = File(dir, "ch_cache_${sourceUrl.hashCode()}.json")
        file.writeText(data)
    }

    actual fun invalidateChannelCache(sourceUrl: String) {
        val dir = cacheDir ?: return
        val file = File(dir, "ch_cache_${sourceUrl.hashCode()}.json")
        if (file.exists()) file.delete()
    }

    actual fun loadDeadUrls(): String? {
        val dir = cacheDir ?: return null
        val file = File(dir, "dead_urls.json")
        return if (file.exists()) file.readText() else null
    }

    actual fun saveDeadUrls(data: String) {
        val dir = cacheDir ?: return
        val file = File(dir, "dead_urls.json")
        file.writeText(data)
    }

    private fun epgSourcePath(id: String): File? {
        val dir = cacheDir ?: return null
        return File(dir, "epg_src_${id.hashCode()}.xml")
    }

    actual fun loadEpgSourceContent(id: String): String? {
        val file = epgSourcePath(id) ?: return null
        return if (file.exists()) file.readText() else null
    }

    actual fun saveEpgSourceContent(id: String, content: String) {
        val file = epgSourcePath(id) ?: return
        file.writeText(content)
    }

    actual fun deleteEpgSourceContent(id: String) {
        val file = epgSourcePath(id) ?: return
        if (file.exists()) file.delete()
    }
}
