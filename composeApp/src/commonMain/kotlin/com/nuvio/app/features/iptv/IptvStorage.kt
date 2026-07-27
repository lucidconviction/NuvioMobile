package com.nuvio.app.features.iptv

internal expect object IptvStorage {
    fun loadSettings(): String?
    fun saveSettings(payload: String)

    fun loadEpgCache(): String?
    fun saveEpgCache(data: String)

    fun loadChannelCache(sourceUrl: String): String?
    fun saveChannelCache(sourceUrl: String, data: String)
    fun invalidateChannelCache(sourceUrl: String)

    fun loadDeadUrls(): String?
    fun saveDeadUrls(data: String)
}
