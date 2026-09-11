package com.nuvio.app.features.hub

import android.content.Context
import android.content.SharedPreferences

internal actual object VidNutzHubConfigStore {
    private const val PREFS_NAME = "nuvio_vidnutz_hub_config"
    private const val KEY_CONFIG = "config_json"

    private fun getPrefs(): SharedPreferences? {
        val ctx = com.nuvio.app.appContext ?: return null
        return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    actual fun load(): String? =
        getPrefs()?.getString(KEY_CONFIG, null)

    actual fun save(json: String) {
        getPrefs()?.edit()?.putString(KEY_CONFIG, json)?.apply()
    }
}
