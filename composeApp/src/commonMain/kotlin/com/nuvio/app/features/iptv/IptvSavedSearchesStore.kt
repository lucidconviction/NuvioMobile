package com.nuvio.app.features.iptv

internal expect object IptvSavedSearchesStore {
    fun load(): List<String>
    fun save(searches: List<String>)
}