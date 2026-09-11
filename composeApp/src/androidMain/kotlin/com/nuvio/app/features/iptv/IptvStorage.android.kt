package com.nuvio.app.features.iptv

import android.content.Context
import android.content.SharedPreferences
import java.io.File
import java.io.IOException
import co.touchlab.kermit.Logger

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
        return if (file.exists()) try { file.readText() } catch (e: IOException) { Logger.e(e) { "Failed to read EPG cache" }; null } else null
    }

    actual fun saveEpgCache(data: String) {
        val dir = cacheDir ?: return
        val file = File(dir, EPG_CACHE_FILE)
        try { file.writeText(data) } catch (e: IOException) { Logger.e(e) { "Failed to write EPG cache" } }
    }

    actual fun loadChannelCache(sourceUrl: String): String? {
        val dir = cacheDir ?: return null
        val file = File(dir, "ch_cache_${sourceUrl.hashCode()}.json")
        return if (file.exists()) try { file.readText() } catch (e: IOException) { Logger.e(e) { "Failed to read channel cache" }; null } else null
    }

    actual fun saveChannelCache(sourceUrl: String, data: String) {
        val dir = cacheDir ?: return
        val file = File(dir, "ch_cache_${sourceUrl.hashCode()}.json")
        try { file.writeText(data) } catch (e: IOException) { Logger.e(e) { "Failed to write channel cache" } }
    }

    actual fun invalidateChannelCache(sourceUrl: String) {
        val dir = cacheDir ?: return
        val file = File(dir, "ch_cache_${sourceUrl.hashCode()}.json")
        if (file.exists()) try { file.delete() } catch (e: SecurityException) { Logger.e(e) { "Failed to delete channel cache" } }
    }

    actual fun loadDeadUrls(): String? {
        val dir = cacheDir ?: return null
        val file = File(dir, "dead_urls.json")
        return if (file.exists()) try { file.readText() } catch (e: IOException) { Logger.e(e) { "Failed to read dead URLs" }; null } else null
    }

    actual fun saveDeadUrls(data: String) {
        val dir = cacheDir ?: return
        val file = File(dir, "dead_urls.json")
        try { file.writeText(data) } catch (e: IOException) { Logger.e(e) { "Failed to write dead URLs" } }
    }

    private fun epgSourcePath(id: String): File? {
        val dir = cacheDir ?: return null
        return File(dir, "epg_src_${id.hashCode()}.xml")
    }

    actual fun loadEpgSourceContent(id: String): String? {
        val file = epgSourcePath(id) ?: return null
        return if (file.exists()) try { file.readText() } catch (e: IOException) { Logger.e(e) { "Failed to read EPG source $id" }; null } else null
    }

    actual fun saveEpgSourceContent(id: String, content: String) {
        val file = epgSourcePath(id) ?: return
        try { file.writeText(content) } catch (e: IOException) { Logger.e(e) { "Failed to write EPG source $id" } }
    }

    actual fun deleteEpgSourceContent(id: String) {
        val file = epgSourcePath(id) ?: return
        if (file.exists()) try { file.delete() } catch (e: SecurityException) { Logger.e(e) { "Failed to delete EPG source $id" } }
    }

    actual fun loadLicense(): String? =
        preferences?.getString("portal_license", null)

    actual fun saveLicense(data: String) {
        preferences?.edit()?.putString("portal_license", data)?.apply()
    }

    actual fun loadFingerprint(): String? =
        preferences?.getString("portal_fingerprint", null)

    actual fun saveFingerprint(data: String) {
        preferences?.edit()?.putString("portal_fingerprint", data)?.apply()
    }
}
