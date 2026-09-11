package com.nuvio.app.features.sports

import platform.Foundation.NSUserDefaults

internal actual object SportsCacheStore {
    private const val KEY_CACHE = "nuvio_sports_cache"

    actual fun load(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(KEY_CACHE)

    actual fun save(json: String) {
        NSUserDefaults.standardUserDefaults.setObject(json, forKey = KEY_CACHE)
    }
}
