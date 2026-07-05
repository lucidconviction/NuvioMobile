package com.nuvio.app.features.iptv

import platform.Foundation.NSUserDefaults
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSUserDomainMask
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.writeToFile
import platform.Foundation.stringWithContentsOfFile

actual object IptvStorage {
    private const val KEY_SETTINGS = "iptv_settings"
    private const val EPG_CACHE_FILE = "epg_cache.json"

    private fun cacheFilePath(): String {
        val paths = NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, true)
        val dir = paths.first() as String
        return "$dir/$EPG_CACHE_FILE"
    }

    actual fun loadSettings(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(KEY_SETTINGS)

    actual fun saveSettings(payload: String) {
        NSUserDefaults.standardUserDefaults.setObject(payload, forKey = KEY_SETTINGS)
    }

    actual fun loadEpgCache(): String? {
        val path = cacheFilePath()
        return NSString.stringWithContentsOfFile(path, encoding = NSUTF8StringEncoding, error = null) as? String
    }

    actual fun saveEpgCache(data: String) {
        val path = cacheFilePath()
        (data as NSString).writeToFile(path, atomically = true, encoding = NSUTF8StringEncoding, error = null)
    }
}
