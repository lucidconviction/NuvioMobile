package com.nuvio.app.features.magnutz

import platform.Foundation.NSUserDefaults
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains

internal actual object MagNutzStorage {
    private const val KEY_ITEMS = "nuvio_magnutz_items"
    private const val KEY_SAVE_LOCATION = "nuvio_magnutz_save_location"

    actual fun loadItems(): List<MagNutzItem> {
        val json = NSUserDefaults.standardUserDefaults.stringForKey(KEY_ITEMS) ?: return emptyList()
        return MagNutzJson.fromJson(json)
    }

    actual fun saveItems(items: List<MagNutzItem>) {
        NSUserDefaults.standardUserDefaults.setObject(MagNutzJson.toJson(items), forKey = KEY_ITEMS)
    }

    actual fun loadSaveLocationUri(): String? {
        return NSUserDefaults.standardUserDefaults.stringForKey(KEY_SAVE_LOCATION)
    }

    actual fun saveSaveLocationUri(uri: String?) {
        NSUserDefaults.standardUserDefaults.setObject(uri, forKey = KEY_SAVE_LOCATION)
    }

    fun getSaveDir(): String {
        val uri = loadSaveLocationUri()
        if (uri != null && uri.isNotBlank()) return uri
        val paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, true, true)
        return "${paths.first()}/magnutz"
    }
}
