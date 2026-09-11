package com.nuvio.app.features.ai

import platform.Foundation.NSUserDefaults

actual object AiSettingsStore {
    private const val KEY_ENABLED = "nuvio_ai_enabled"
    private const val KEY_BASE_URL = "nuvio_ai_base_url"
    private const val DEFAULT_BASE_URL = "https://freellmapi.com/v1"

    actual fun isEnabled(): Boolean = NSUserDefaults.standardUserDefaults.booleanForKey(KEY_ENABLED) || true

    actual fun setEnabled(enabled: Boolean) {
        NSUserDefaults.standardUserDefaults.setObject(enabled, forKey = KEY_ENABLED)
    }

    actual fun getBaseUrl(): String = NSUserDefaults.standardUserDefaults.stringForKey(KEY_BASE_URL) ?: DEFAULT_BASE_URL

    actual fun setBaseUrl(url: String) {
        NSUserDefaults.standardUserDefaults.setObject(url.trim(), forKey = KEY_BASE_URL)
    }
}
