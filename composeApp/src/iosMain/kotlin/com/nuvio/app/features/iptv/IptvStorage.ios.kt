@file:OptIn(ExperimentalForeignApi::class)

package com.nuvio.app.features.iptv

import kotlinx.cinterop.ExperimentalForeignApi
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

    private fun channelCachePath(sourceUrl: String): String {
        val paths = NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, true)
        val dir = paths.first() as String
        return "$dir/ch_cache_${sourceUrl.hashCode()}.json"
    }

    actual fun loadChannelCache(sourceUrl: String): String? {
        val path = channelCachePath(sourceUrl)
        return NSString.stringWithContentsOfFile(path, encoding = NSUTF8StringEncoding, error = null) as? String
    }

    actual fun saveChannelCache(sourceUrl: String, data: String) {
        val path = channelCachePath(sourceUrl)
        (data as NSString).writeToFile(path, atomically = true, encoding = NSUTF8StringEncoding, error = null)
    }

    actual fun invalidateChannelCache(sourceUrl: String) {
        val path = channelCachePath(sourceUrl)
        val fileManager = platform.Foundation.NSFileManager.defaultManager
        if (fileManager.fileExistsAtPath(path)) {
            fileManager.removeItemAtPath(path, error = null)
        }
    }

    private fun deadUrlsPath(): String {
        val paths = NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, true)
        val dir = paths.first() as String
        return "$dir/dead_urls.json"
    }

    actual fun loadDeadUrls(): String? {
        val path = deadUrlsPath()
        return NSString.stringWithContentsOfFile(path, encoding = NSUTF8StringEncoding, error = null) as? String
    }

    actual fun saveDeadUrls(data: String) {
        val path = deadUrlsPath()
        (data as NSString).writeToFile(path, atomically = true, encoding = NSUTF8StringEncoding, error = null)
    }

    private fun epgSourcePath(id: String): String {
        val paths = NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, true)
        val dir = paths.first() as String
        return "$dir/epg_src_${id.hashCode()}.xml"
    }

    actual fun loadEpgSourceContent(id: String): String? {
        val path = epgSourcePath(id)
        return NSString.stringWithContentsOfFile(path, encoding = NSUTF8StringEncoding, error = null) as? String
    }

    actual fun saveEpgSourceContent(id: String, content: String) {
        val path = epgSourcePath(id)
        (content as NSString).writeToFile(path, atomically = true, encoding = NSUTF8StringEncoding, error = null)
    }

    actual fun deleteEpgSourceContent(id: String) {
        val path = epgSourcePath(id)
        val fileManager = platform.Foundation.NSFileManager.defaultManager
        if (fileManager.fileExistsAtPath(path)) {
            fileManager.removeItemAtPath(path, error = null)
        }
    }
}
