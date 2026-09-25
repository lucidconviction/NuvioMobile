package com.nuvio.app.features.iptv

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal actual object CustomQuickChannelStore {
    private const val PREFS_NAME = "nuvio_iptv"
    private const val KEY_CUSTOM_QC = "custom_quick_channels"
    private val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true; prettyPrint = false }

    private fun getPrefs(): SharedPreferences? {
        val ctx = com.nuvio.app.appContext ?: return null
        return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    actual fun loadChannels(): List<QuickChannel> {
        val raw = getPrefs()?.getString(KEY_CUSTOM_QC, null) ?: return emptyList()
        return try {
            val arr = json.parseToJsonElement(raw).jsonArray
            arr.map { fromJsonQuickChannel(it.jsonObject) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    actual fun saveChannels(channels: List<QuickChannel>) {
        val arr = kotlinx.serialization.json.buildJsonArray {
            channels.forEach { add(it.toJson()) }
        }
        getPrefs()?.edit()?.putString(KEY_CUSTOM_QC, arr.toString())?.apply()
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