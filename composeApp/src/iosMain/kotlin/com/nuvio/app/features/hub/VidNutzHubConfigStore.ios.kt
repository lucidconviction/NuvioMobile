package com.nuvio.app.features.hub

import platform.Foundation.NSUserDefaults

internal actual object VidNutzHubConfigStore {
    private const val KEY_CONFIG = "nuvio_vidnutz_hub_config"

    actual fun load(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(KEY_CONFIG)

    actual fun save(json: String) {
        NSUserDefaults.standardUserDefaults.setObject(json, forKey = KEY_CONFIG)
    }
}
