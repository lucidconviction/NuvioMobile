package com.nuvio.app.features.hub

import platform.Foundation.NSUserDefaults

internal actual object VidNutzCacheStore {
    private const val KEY_CACHE = "nuvio_vidnutz_cache"

    actual fun load(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(KEY_CACHE)

    actual fun save(json: String) {
        NSUserDefaults.standardUserDefaults.setObject(json, forKey = KEY_CACHE)
    }
}
