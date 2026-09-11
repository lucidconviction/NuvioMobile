package com.nuvio.app.features.hub

internal expect object PodNutzStore {
    fun loadSaved(): List<Podcast>
    fun saveSaved(saved: List<Podcast>)
}