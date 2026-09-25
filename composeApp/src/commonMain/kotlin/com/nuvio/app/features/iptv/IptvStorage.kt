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

    fun loadEpgSourceContent(id: String): String?
    fun saveEpgSourceContent(id: String, content: String)
    fun deleteEpgSourceContent(id: String)

    fun loadLicense(): String?
    fun saveLicense(data: String)

    fun loadFingerprint(): String?
    fun saveFingerprint(data: String)

    fun hasSeededDefaultM3u(): Boolean
    fun markDefaultM3uSeeded()

    fun loadLastRefresh(key: String): Long?
    fun saveLastRefresh(key: String, timestamp: Long)

    fun loadSearchQuery(): String?
    fun saveSearchQuery(query: String)

    fun loadInstalledPortals(): String?
    fun saveInstalledPortals(data: String)
}
