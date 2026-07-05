package com.nuvio.app.features.iptv

internal expect object IptvStorage {
    fun loadSettings(): String?
    fun saveSettings(payload: String)

    fun loadEpgCache(): String?
    fun saveEpgCache(data: String)
}
