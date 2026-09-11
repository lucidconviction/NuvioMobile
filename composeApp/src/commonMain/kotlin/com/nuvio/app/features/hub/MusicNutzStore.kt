package com.nuvio.app.features.hub

import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

private val musicJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

object MusicNutzPlaylistStore {
    private var _playlists = listOf<MusicNutzPlaylist>()

    fun loadPlaylists(): List<MusicNutzPlaylist> = _playlists

    fun savePlaylists(playlists: List<MusicNutzPlaylist>) {
        _playlists = playlists
        persist()
    }

    private fun persist() {
        try {
            val json = musicJson.encodeToString(_playlists)
            MusicNutzStorageHelper.save("playlists", json)
        } catch (_: Exception) {}
    }

    fun loadFromDisk() {
        try {
            val json = MusicNutzStorageHelper.load("playlists")
            if (!json.isNullOrBlank()) {
                _playlists = musicJson.decodeFromString<List<MusicNutzPlaylist>>(json)
            }
        } catch (_: Exception) {}
    }
}

object MusicNutzSavedAlbumsStore {
    private var _savedAlbums = listOf<MusicAlbum>()

    fun loadSavedAlbums(): List<MusicAlbum> = _savedAlbums

    fun saveSavedAlbums(albums: List<MusicAlbum>) {
        _savedAlbums = albums
        persist()
    }

    private fun persist() {
        try {
            val json = musicJson.encodeToString(_savedAlbums)
            MusicNutzStorageHelper.save("saved_albums", json)
        } catch (_: Exception) {}
    }

    fun loadFromDisk() {
        try {
            val json = MusicNutzStorageHelper.load("saved_albums")
            if (!json.isNullOrBlank()) {
                _savedAlbums = musicJson.decodeFromString<List<MusicAlbum>>(json)
            }
        } catch (_: Exception) {}
    }
}

internal expect object MusicNutzStorageHelper {
    fun save(key: String, value: String)
    fun load(key: String): String?
}
