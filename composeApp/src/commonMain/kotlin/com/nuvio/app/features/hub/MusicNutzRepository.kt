package com.nuvio.app.features.hub

import com.nuvio.app.features.addons.httpGetText
import com.nuvio.app.features.sports.StreamResult
import com.nuvio.app.features.sports.YouTubeStreamResolver
import com.nuvio.app.features.sports.platformYouTubeSearch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object MusicNutzRepository {
    private const val BASE = "https://api.deezer.com"
    private val json = Json { ignoreUnknownKeys = true }

    private val categoryQueries = mapOf(
        MusicNutzCategory.ROCK to listOf("rock", "classic rock", "alternative rock", "rock hits", "indie rock"),
        MusicNutzCategory.HIP_HOP to listOf("hip hop", "rap", "trap", "hip hop hits", "underground rap"),
        MusicNutzCategory.ELECTRONIC to listOf("electronic", "EDM", "house music", "techno", "dubstep"),
        MusicNutzCategory.POP to listOf("pop", "pop hits", "top 40", "pop music", "popsongs"),
        MusicNutzCategory.R_AND_B to listOf("r&b", "rnb", "soul", "neo soul", "rhythm and blues"),
        MusicNutzCategory.JAZZ to listOf("jazz", "smooth jazz", "bebop", "jazz fusion", "cool jazz"),
        MusicNutzCategory.CLASSICAL to listOf("classical", "orchestra", "piano", "symphony", "classical music"),
        MusicNutzCategory.COUNTRY to listOf("country", "country hits", "americana", "bluegrass", "country music"),
        MusicNutzCategory.METAL to listOf("metal", "heavy metal", "death metal", "thrash metal", "metalcore"),
        MusicNutzCategory.INDIE to listOf("indie", "indie rock", "indie pop", "lo-fi", "bedroom pop"),
    )

    private val categoryPageOffsets = mutableMapOf<MusicNutzCategory, Int>()

    suspend fun fetchTrending(page: Int = 1): List<MusicTrack> {
        val index = ((page - 1) * 20) + ((1..50).random())
        return fetchTracks("$BASE/chart/0/tracks?limit=20&index=$index")
    }

    suspend fun search(query: String, page: Int = 1): List<MusicTrack> {
        if (query.isBlank()) return emptyList()
        val encoded = encodeUrl(query)
        val index = (page - 1) * 20
        return fetchTracks("$BASE/search/track?q=$encoded&limit=20&index=$index")
    }

    suspend fun fetchByCategory(category: MusicNutzCategory, page: Int = 1): List<MusicTrack> {
        val offset = categoryPageOffsets.getOrPut(category) { (1..10).random() }
        val randomPage = page + offset
        return when (category) {
            MusicNutzCategory.TRENDING -> fetchTrending(randomPage)
            MusicNutzCategory.NEW_RELEASES -> fetchNewReleases(randomPage)
            else -> {
                val variants = categoryQueries[category] ?: return emptyList()
                val queryIndex = (page - 1) % variants.size
                val query = variants[queryIndex]
                val encoded = encodeUrl("$query music")
                val index = ((randomPage - 1) * 20) + ((1..5).random())
                fetchTracks("$BASE/search/track?q=$encoded&limit=20&index=$index")
            }
        }
    }

    private suspend fun fetchNewReleases(page: Int): List<MusicTrack> {
        val index = (page - 1) * 20
        return try {
            val response = httpGetText("$BASE/chart/0/albums?limit=20&index=$index")
            val parsed = json.decodeFromString<DeezerAlbumList>(response)
            parsed.data.mapNotNull { album ->
                try {
                    val albumResp = httpGetText("$BASE/album/${album.id}/tracks?limit=1")
                    val albumParsed = json.decodeFromString<DeezerTrackList>(albumResp)
                    albumParsed.data.firstOrNull()?.let { track ->
                        MusicTrack(id = track.id, title = track.title, artistName = track.artist.name,
                            albumName = album.title, albumCover = album.cover_medium,
                            durationSeconds = track.duration, previewUrl = track.preview)
                    }
                } catch (_: Exception) { null }
            }
        } catch (_: Exception) { emptyList() }
    }

    private suspend fun fetchTracks(url: String): List<MusicTrack> {
        return try {
            val response = httpGetText(url)
            val parsed = json.decodeFromString<DeezerTrackList>(response)
            parsed.data.map { track ->
                MusicTrack(id = track.id, title = track.title, artistName = track.artist.name,
                    albumName = track.album.title, albumCover = track.album.cover_medium,
                    durationSeconds = track.duration, previewUrl = track.preview)
            }
        } catch (_: Exception) { emptyList() }
    }

    suspend fun fetchTrendingAlbums(page: Int = 1): List<MusicAlbum> {
        val index = (page - 1) * 20
        return fetchAlbums("$BASE/chart/0/albums?limit=20&index=$index")
    }

    suspend fun searchAlbums(query: String, page: Int = 1): List<MusicAlbum> {
        if (query.isBlank()) return emptyList()
        val encoded = encodeUrl(query)
        val index = (page - 1) * 20
        return fetchAlbums("$BASE/search/album?q=$encoded&limit=20&index=$index")
    }

    suspend fun fetchAlbumsByCategory(category: MusicNutzCategory, page: Int = 1): List<MusicAlbum> {
        val offset = categoryPageOffsets.getOrPut(category) { (1..10).random() }
        val randomPage = page + offset
        return when (category) {
            MusicNutzCategory.TRENDING, MusicNutzCategory.NEW_RELEASES -> fetchTrendingAlbums(randomPage)
            else -> {
                val variants = categoryQueries[category] ?: return emptyList()
                val queryIndex = (page - 1) % variants.size
                val query = variants[queryIndex]
                val encoded = encodeUrl("$query album")
                val index = ((randomPage - 1) * 20) + ((1..5).random())
                fetchAlbums("$BASE/search/album?q=$encoded&limit=20&index=$index")
            }
        }
    }

    suspend fun fetchAlbumTracks(albumId: Long): List<MusicTrack> = fetchTracks("$BASE/album/$albumId/tracks")

    private suspend fun fetchAlbums(url: String): List<MusicAlbum> {
        return try {
            val response = httpGetText(url)
            val parsed = json.decodeFromString<DeezerAlbumList>(response)
            parsed.data.map { album ->
                MusicAlbum(id = album.id, title = album.title, artistName = album.artist.name,
                    coverUrl = album.cover_medium, releaseDate = album.release_date, trackCount = 0)
            }
        } catch (_: Exception) { emptyList() }
    }

    suspend fun resolveStream(track: MusicTrack): StreamResult? {
        // Always try YouTube first for full-length audio
        try {
            val query = "${track.title} ${track.artistName} audio"
            val searchResults = platformYouTubeSearch(query)
            if (searchResults != null && searchResults.isNotEmpty()) {
                val stream = YouTubeStreamResolver.resolveStream(searchResults.first().videoId)
                if (stream != null) return stream
            }
        } catch (_: Exception) { }
        // Fallback: Deezer 30s preview
        if (track.previewUrl != null) {
            return StreamResult(url = track.previewUrl, headers = emptyMap())
        }
        return null
    }

    private fun encodeUrl(s: String): String {
        return s.replace(" ", "+").replace(",", "%2C").replace("&", "%26").replace("?", "%3F").replace("#", "%23")
    }

    @Serializable
    data class DeezerTrackData(val id: Long = 0, val title: String = "", val duration: Int = 0,
        val preview: String? = null, val artist: DeezerArtist = DeezerArtist(), val album: DeezerAlbum = DeezerAlbum())

    @Serializable data class DeezerArtist(val name: String = "")
    @Serializable data class DeezerAlbum(val title: String = "", val cover_medium: String = "")
    @Serializable data class DeezerTrackList(val data: List<DeezerTrackData> = emptyList())

    @Serializable
    data class DeezerAlbumData(val id: Long = 0, val title: String = "", val cover_medium: String = "",
        val artist: DeezerArtist = DeezerArtist(), val release_date: String = "")

    @Serializable data class DeezerAlbumList(val data: List<DeezerAlbumData> = emptyList())
}
