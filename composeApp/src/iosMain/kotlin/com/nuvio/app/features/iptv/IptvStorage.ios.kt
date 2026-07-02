package com.nuvio.app.features.iptv

import platform.Foundation.NSUserDefaults

actual object IptvStorage {
    private const val KEY_SETTINGS = "iptv_settings"

    actual fun loadSettings(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(KEY_SETTINGS)

    actual fun saveSettings(payload: String) {
        NSUserDefaults.standardUserDefaults.setObject(payload, forKey = KEY_SETTINGS)
    }
}
