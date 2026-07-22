package com.nuvio.app.features.magnutz

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal expect object MagNutzStorage {
    fun loadItems(): List<MagNutzItem>
    fun saveItems(items: List<MagNutzItem>)
    fun loadSaveLocationUri(): String?
    fun saveSaveLocationUri(uri: String?)
}

internal object MagNutzJson {
    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun toJson(items: List<MagNutzItem>): String = json.encodeToString(items)
    fun fromJson(jsonStr: String): List<MagNutzItem> =
        runCatching { json.decodeFromString<List<MagNutzItem>>(jsonStr) }.getOrDefault(emptyList())
}
