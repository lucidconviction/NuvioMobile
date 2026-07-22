package com.nuvio.app.features.magnutz

import android.content.Context
import android.content.SharedPreferences

internal actual object MagNutzStorage {
    private const val PREFS_NAME = "nuvio_magnutz"
    private const val KEY_ITEMS = "items"
    private const val KEY_SAVE_LOCATION = "save_location_uri"

    private var prefs: SharedPreferences? = null
    private var appContext: Context? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    actual fun loadItems(): List<MagNutzItem> {
        val json = prefs?.getString(KEY_ITEMS, null) ?: return emptyList()
        return MagNutzJson.fromJson(json)
    }

    actual fun saveItems(items: List<MagNutzItem>) {
        prefs?.edit()?.putString(KEY_ITEMS, MagNutzJson.toJson(items))?.apply()
    }

    actual fun loadSaveLocationUri(): String? {
        return prefs?.getString(KEY_SAVE_LOCATION, null)?.takeIf { it.isNotBlank() }
    }

    actual fun saveSaveLocationUri(uri: String?) {
        prefs?.edit()?.putString(KEY_SAVE_LOCATION, uri ?: "")?.apply()
    }

    fun getSaveDir(): java.io.File {
        val base = appContext?.filesDir ?: java.io.File(".")
        return java.io.File(base, "downloads/magnutz").apply { mkdirs() }
    }
}
