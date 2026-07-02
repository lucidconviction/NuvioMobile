package com.nuvio.app.features.iptv

import kotlinx.serialization.Serializable

@Serializable
data class M3uPlaylist(
    val id: String,
    val name: String,
    val url: String,
    val channels: List<IptvChannel> = emptyList(),
)

@Serializable
data class XtreamAccount(
    val id: String,
    val name: String,
    val server: String,
    val username: String,
    val password: String,
    val channels: List<IptvChannel> = emptyList(),
    val categories: List<XtreamCategory> = emptyList(),
)

@Serializable
data class XtreamCategory(
    val id: String,
    val name: String,
)

@Serializable
data class IptvChannel(
    val id: String,
    val name: String,
    val logo: String? = null,
    val group: String? = null,
    val url: String,
    val epgChannelId: String? = null,
    val sourceType: SourceType,
    val sourceId: String,
)

enum class SourceType { M3U, Xtream }

@Serializable
data class EpgSource(
    val id: String,
    val name: String,
    val url: String,
)

@Serializable
data class EpgProgram(
    val channelId: String,
    val title: String,
    val description: String? = null,
    val startTime: Long,
    val endTime: Long,
    val icon: String? = null,
)

data class IptvUiState(
    val m3uPlaylists: List<M3uPlaylist> = emptyList(),
    val xtreamAccounts: List<XtreamAccount> = emptyList(),
    val epgSources: List<EpgSource> = emptyList(),
    val selectedSourceIds: Set<String> = emptySet(),
    val selectedCategory: String? = null,
    val channels: List<IptvChannel> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val epgPrograms: Map<String, List<EpgProgram>> = emptyMap(),
    val epgProgramsByName: Map<String, List<EpgProgram>> = emptyMap(),
    val epgLoading: Boolean = false,
    val epgMatchCount: Int = 0,
    val favoriteChannelIds: Set<String> = emptySet(),
    val searchQuery: String = "",
    val refreshingSourceIds: Set<String> = emptySet(),
    val playlistsExpanded: Boolean = true,
    val channelsExpanded: Boolean = true,
    val favoritesExpanded: Boolean = true,
)

data class IptvPlaylistSettings(
    val m3uPlaylists: List<M3uPlaylist> = emptyList(),
    val xtreamAccounts: List<XtreamAccount> = emptyList(),
    val epgSources: List<EpgSource> = emptyList(),
    val favoriteChannelIds: Set<String> = emptySet(),
    val channelHistory: List<String> = emptyList(),
)
