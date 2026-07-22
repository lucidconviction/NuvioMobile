package com.nuvio.app.features.hub

object MusicNutzPlaylistStore {
    private var _playlists = listOf<MusicNutzPlaylist>()

    fun loadPlaylists(): List<MusicNutzPlaylist> = _playlists
    fun savePlaylists(playlists: List<MusicNutzPlaylist>) { _playlists = playlists }
}

object MusicDownloadStore {
    private var _downloads = listOf<MusicDownload>()

    fun loadDownloads(): List<MusicDownload> = _downloads
    fun saveDownloads(downloads: List<MusicDownload>) { _downloads = downloads }
}

object MusicNutzSavedAlbumsStore {
    private var _savedAlbums = listOf<MusicAlbum>()

    fun loadSavedAlbums(): List<MusicAlbum> = _savedAlbums
    fun saveSavedAlbums(albums: List<MusicAlbum>) { _savedAlbums = albums }
}

object MusicNutzDownloadStorage {
    var downloadDirPath: String = "."
}
