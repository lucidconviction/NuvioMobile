package com.nuvio.app.features.iptv

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.JsonPrimitive

internal expect object CustomQuickChannelStore {
    fun loadChannels(): List<QuickChannel>
    fun saveChannels(channels: List<QuickChannel>)
    fun addChannel(channel: QuickChannel)
    fun removeChannel(displayName: String)
}

fun QuickChannel.toJson(): JsonObject = kotlinx.serialization.json.buildJsonObject {
    put("displayName", JsonPrimitive(displayName))
    put("aliases", JsonArray(aliases.map { JsonPrimitive(it) }))
    put("regions", JsonArray(regions.map { JsonPrimitive(it) }))
    put("tags", JsonArray(tags.map { JsonPrimitive(it) }))
}

fun fromJsonQuickChannel(obj: JsonObject): QuickChannel {
    return QuickChannel(
        displayName = obj["displayName"]?.jsonPrimitive?.content ?: "",
        aliases = obj["aliases"]?.jsonArray?.map { it.jsonPrimitive?.content ?: "" } ?: emptyList(),
        regions = obj["regions"]?.jsonArray?.map { it.jsonPrimitive?.content ?: "" } ?: emptyList(),
        tags = obj["tags"]?.jsonArray?.map { it.jsonPrimitive?.content ?: "" } ?: emptyList(),
    )
}