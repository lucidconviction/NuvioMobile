package com.nuvio.app.features.hub

import kotlinx.serialization.Serializable

@Serializable
data class MusicTrack(
    val id: Long,
    val title: String,
    val artistName: String,
    val albumName: String,
    val albumCover: String,
    val durationSeconds: Int,
    val previewUrl: String? = null,
)

@Serializable
data class MusicAlbum(
    val id: Long,
    val title: String,
    val artistName: String,
    val coverUrl: String,
    val releaseDate: String,
    val trackCount: Int,
)

@Serializable
data class MusicNutzPlaylist(
    val id: String,
    val name: String,
    val tracks: List<MusicTrack> = emptyList(),
    val createdAtEpochMs: Long = 0L,
) {
    val trackCount: Int get() = tracks.size
    val durationSeconds: Int get() = tracks.sumOf { it.durationSeconds }
}

@Serializable
data class MusicDownload(
    val trackId: Long,
    val title: String,
    val artistName: String,
    val albumCover: String = "",
    val localFilePath: String? = null,
    val isDownloading: Boolean = false,
    val progress: Float = 0f,
    val error: String? = null,
)

enum class MusicNutzCategory(val displayName: String) {
    TRENDING("Trending"),
    NEW_RELEASES("New Releases"),
    ROCK("Rock"),
    HIP_HOP("Hip-Hop"),
    ELECTRONIC("Electronic"),
    POP("Pop"),
    R_AND_B("R&B"),
    JAZZ("Jazz"),
    CLASSICAL("Classical"),
    COUNTRY("Country"),
    METAL("Metal"),
    INDIE("Indie"),
}

enum class MusicNutzMode { TRACKS, ALBUMS, PLAYLISTS, DOWNLOADS, SAVED_ALBUMS }

data class MusicNutzUiState(
    val selectedCategory: MusicNutzCategory = MusicNutzCategory.TRENDING,
    val tracks: List<MusicTrack> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val currentPage: Int = 1,
    val hasMore: Boolean = true,
    val searchQuery: String = "",
    val searchResults: List<MusicTrack>? = null,
    val searchCurrentPage: Int = 1,
    val searchHasMore: Boolean = true,
    val mode: MusicNutzMode = MusicNutzMode.TRACKS,
    val albums: List<MusicAlbum> = emptyList(),
    val albumResults: List<MusicAlbum>? = null,
    val albumPage: Int = 1,
    val albumHasMore: Boolean = true,
    val isLoadingAlbums: Boolean = false,
    val selectedAlbum: MusicAlbum? = null,
    val albumTracks: List<MusicTrack> = emptyList(),
    val isLoadingAlbumTracks: Boolean = false,
    val playlists: List<MusicNutzPlaylist> = emptyList(),
    val downloads: List<MusicDownload> = emptyList(),
    val currentPlaylist: MusicNutzPlaylist? = null,
    val showAddToPlaylist: MusicTrack? = null,
    val newPlaylistName: String = "",
    val savedAlbums: List<MusicAlbum> = emptyList(),
    val isPlayingTrack: Boolean = false,
    val streamError: String? = null,
)
