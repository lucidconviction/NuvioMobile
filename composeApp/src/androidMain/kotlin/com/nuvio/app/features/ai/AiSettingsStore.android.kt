package com.nuvio.app.features.ai

import android.content.Context

actual object AiSettingsStore {
    private const val PREFS_NAME = "nuvio_ai_settings"
    private const val KEY_ENABLED = "ai_enabled"
    private const val KEY_BASE_URL = "ai_base_url"
    private const val DEFAULT_BASE_URL = "https://freellmapi.com/v1"

    private fun getPrefs(): android.content.SharedPreferences? {
        val ctx = com.nuvio.app.appContext ?: return null
        return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    actual fun isEnabled(): Boolean = getPrefs()?.getBoolean(KEY_ENABLED, true) ?: true

    actual fun setEnabled(enabled: Boolean) {
        getPrefs()?.edit()?.putBoolean(KEY_ENABLED, enabled)?.apply()
    }

    actual fun getBaseUrl(): String = getPrefs()?.getString(KEY_BASE_URL, null) ?: DEFAULT_BASE_URL

    actual fun setBaseUrl(url: String) {
        getPrefs()?.edit()?.putString(KEY_BASE_URL, url.trim())?.apply()
    }
}
