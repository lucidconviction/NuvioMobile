package com.nuvio.app.features.hub

data class MusicTrack(
    val id: Long,
    val title: String,
    val artistName: String,
    val albumName: String,
    val albumCover: String,
    val durationSeconds: Int,
    val previewUrl: String?,
)

data class MusicAlbum(
    val id: Long,
    val title: String,
    val artistName: String,
    val coverUrl: String,
    val releaseDate: String,
    val trackCount: Int,
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

enum class MusicNutzMode { TRACKS, ALBUMS }

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
)
