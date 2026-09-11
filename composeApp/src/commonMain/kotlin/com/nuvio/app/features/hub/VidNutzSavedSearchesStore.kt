package com.nuvio.app.features.hub

internal expect object VidNutzSavedSearchesStore {
    fun load(): List<String>
    fun save(searches: List<String>)
}