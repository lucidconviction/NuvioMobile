package com.nuvio.app.features.iptv

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import platform Foundation.NSUserDefaults

internal actual object CustomQuickChannelStore {
    private const val KEY_CUSTOM_QC = "custom_quick_channels"
    private val json = Json { ignoreUnknownKeys = true, prettyPrint = false }

    private fun prefs() = NSUserDefaults.standardUserDefaults

    actual fun loadChannels(): List<QuickChannel> {
        val raw = prefs().stringForKey(KEY_CUSTOM_QC) ?: return emptyList()
        return try {
            val arr = json.parseToJsonElement(raw).jsonArray
            arr.map { QuickChannel.fromJson(it.jsonObject) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    actual fun saveChannels(channels: List<QuickChannel>) {
        val arr = kotlinx.serialization.json.buildJsonArray {
            channels.forEach { add(it.toJson()) }
        }
        prefs().setObject(arr.toString(), forKey = KEY_CUSTOM_QC)
    }

    actual fun addChannel(channel: QuickChannel) {
        val updated = loadChannels().toMutableList()
        updated.removeAll { it.displayName.equals(channel.displayName, ignoreCase = true) }
        updated.add(channel)
        saveChannels(updated)
    }

    actual fun removeChannel(displayName: String) {
        val updated = loadChannels().filterNot { it.displayName.equals(displayName, ignoreCase = true) }
        saveChannels(updated)
    }
}