package com.nuvio.app.features.hub

internal expect object VidNutzRecentSearchesStore {
    fun load(): List<String>
    fun save(searches: List<String>)
}
