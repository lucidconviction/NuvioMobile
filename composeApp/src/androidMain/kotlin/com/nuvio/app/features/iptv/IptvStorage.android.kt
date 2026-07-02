package com.nuvio.app.features.iptv

import android.content.Context
import android.content.SharedPreferences

actual object IptvStorage {
    private const val PREFS_NAME = "nuvio_iptv"
    private const val KEY_SETTINGS = "iptv_settings"

    private var preferences: SharedPreferences? = null

    fun initialize(context: Context) {
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    actual fun loadSettings(): String? =
        preferences?.getString(KEY_SETTINGS, null)

    actual fun saveSettings(payload: String) {
        preferences?.edit()?.putString(KEY_SETTINGS, payload)?.apply()
    }
}
