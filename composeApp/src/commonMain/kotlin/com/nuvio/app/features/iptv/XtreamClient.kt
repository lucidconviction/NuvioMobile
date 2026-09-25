package com.nuvio.app.features.iptv

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object XtreamClient {
    private val json = Json { ignoreUnknownKeys = true }

    fun parseAccountInfo(response: String): PortalAccountInfo? {
        try {
            val element = json.parseToJsonElement(response)
            if (element !is JsonObject) return null
            val info = element["user_info"]?.jsonObject ?: element
            if (!element.containsKey("user_info") && info["auth"]?.jsonPrimitive?.contentOrNull != "1") return null

            fun str(key: String): String? = info[key]?.jsonPrimitive?.contentOrNull
            fun parseExpDate(raw: String): Long? {
                if (raw.isBlank() || raw == "null") return null
                val asLong = raw.trim().toLongOrNull()
                if (asLong != null) {
                    return if (asLong > 10_000_000_000L) asLong else asLong * 1000L
                }
                return null
            }
            return PortalAccountInfo(
                expDate = str("exp_date")?.let { parseExpDate(it) },
                maxConnections = str("max_connections")?.toIntOrNull(),
                activeConnections = str("active_connections")?.toIntOrNull(),
                status = str("status"),
                isTrial = str("is_trial")?.let { it == "1" || it.equals("true", true) },
            )
        } catch (_: Exception) { }
        return null
    }

    fun parseCategories(response: String): List<XtreamCategory> {
        val categories = mutableListOf<XtreamCategory>()
        try {
            val arr = json.decodeFromString<JsonArray>(response)
            for (element in arr) {
                val obj = element.jsonObject
                val id = obj["category_id"]?.jsonPrimitive?.content ?: continue
                val name = obj["category_name"]?.jsonPrimitive?.content ?: continue
                categories.add(XtreamCategory(id = id, name = name))
            }
        } catch (_: Exception) { }
        return categories
    }

    fun parseChannels(
        response: String,
        sourceId: String,
        server: String,
        username: String,
        password: String,
    ): List<IptvChannel> {
        val channels = mutableListOf<IptvChannel>()
        try {
            val arr = json.decodeFromString<JsonArray>(response)
            for (element in arr) {
                val obj = element.jsonObject
                val streamId = obj["stream_id"]?.jsonPrimitive?.content
                    ?: obj["stream_id"]?.jsonPrimitive?.content?.toLongOrNull()?.toString()
                    ?: continue
                val name = obj["name"]?.jsonPrimitive?.content ?: obj["stream_type"]?.jsonPrimitive?.content ?: "Unknown"
                val logo = obj["stream_icon"]?.jsonPrimitive?.content
                val categoryId = obj["category_id"]?.jsonPrimitive?.content
                val epgChannelId = obj["epg_channel_id"]?.jsonPrimitive?.content

                val url = buildXtreamUrl(server, username, password, streamId)

                channels.add(
                    IptvChannel(
                        id = streamId,
                        name = name,
                        logo = logo?.ifBlank { null },
                        group = categoryId,
                        url = url,
                        epgChannelId = epgChannelId?.ifBlank { null },
                        sourceType = SourceType.Xtream,
                        sourceId = sourceId,
                    )
                )
            }
        } catch (_: Exception) { }
        return channels
    }

    private fun buildXtreamUrl(server: String, username: String, password: String, streamId: String): String {
        val base = server.trimEnd('/')
        return "$base/live/$username/$password/$streamId.ts"
    }

    fun parseVodStreams(
        response: String,
        server: String,
        username: String,
        password: String,
    ): List<XtreamMovie> {
        val movies = mutableListOf<XtreamMovie>()
        try {
            val arr = json.decodeFromString<JsonArray>(response)
            for (element in arr) {
                val obj = element.jsonObject
                val streamId = obj["stream_id"]?.jsonPrimitive?.content ?: continue
                val name = obj["name"]?.jsonPrimitive?.content ?: "Unknown"
                val containerExtension = obj["container_extension"]?.jsonPrimitive?.content ?: "mp4"
                val url = buildXtreamMovieUrl(server, username, password, streamId, containerExtension)
                movies.add(
                    XtreamMovie(
                        id = streamId,
                        name = name,
                        streamId = streamId,
                        cover = obj["stream_icon"]?.jsonPrimitive?.content?.ifBlank { null },
                        backdrop = obj["backdrop_path"]?.jsonPrimitive?.content?.ifBlank { null },
                        plot = obj["plot"]?.jsonPrimitive?.content?.ifBlank { null },
                        releaseDate = obj["release_date"]?.jsonPrimitive?.content?.ifBlank { null },
                        cast = obj["cast"]?.jsonPrimitive?.content?.ifBlank { null },
                        director = obj["director"]?.jsonPrimitive?.content?.ifBlank { null },
                        genre = obj["genre"]?.jsonPrimitive?.content?.ifBlank { null },
                        rating = obj["rating"]?.jsonPrimitive?.content?.ifBlank { null },
                        year = obj["releasedate"]?.jsonPrimitive?.content?.ifBlank { null },
                        duration = obj["duration"]?.jsonPrimitive?.content?.ifBlank { null },
                        categoryId = obj["category_id"]?.jsonPrimitive?.content?.ifBlank { null },
                        videoUrl = url,
                    )
                )
            }
        } catch (_: Exception) { }
        return movies
    }

    fun parseSeries(
        response: String,
        server: String,
        username: String,
        password: String,
    ): List<XtreamSeries> {
        val series = mutableListOf<XtreamSeries>()
        try {
            val arr = json.decodeFromString<JsonArray>(response)
            for (element in arr) {
                val obj = element.jsonObject
                val seriesId = obj["series_id"]?.jsonPrimitive?.content ?: continue
                val name = obj["name"]?.jsonPrimitive?.content ?: "Unknown"
                val seasonsJson = obj["seasons"]?.jsonArray
                val seasons = seasonsJson?.mapNotNull { sEl ->
                    val sObj = sEl.jsonObject
                    val seasonId = sObj["id"]?.jsonPrimitive?.content ?: return@mapNotNull null
                    val seasonName = sObj["name"]?.jsonPrimitive?.content ?: ""
                    val seasonNum = sObj["season_number"]?.jsonPrimitive?.content?.toIntOrNull()
                    val episodesJson = sObj["episodes"]?.jsonArray
                    val episodes = episodesJson?.mapNotNull { eEl ->
                        val eObj = eEl.jsonObject
                        val epId = eObj["id"]?.jsonPrimitive?.content ?: return@mapNotNull null
                        val epName = eObj["name"]?.jsonPrimitive?.content ?: ""
                        val epNum = eObj["episode_number"]?.jsonPrimitive?.content?.toIntOrNull()
                        val epSeason = eObj["season_number"]?.jsonPrimitive?.content?.toIntOrNull()
                        val epDuration = eObj["duration"]?.jsonPrimitive?.content
                        val epCover = eObj["cover"]?.jsonPrimitive?.content?.ifBlank { null }
                        val epExt = eObj["container_extension"]?.jsonPrimitive?.content ?: "mp4"
                        val epUrl = buildXtreamSeriesUrl(server, username, password, seriesId, seasonId, epId, epExt)
                        XtreamEpisode(
                            id = epId,
                            name = epName,
                            episodeNumber = epNum,
                            seasonNumber = epSeason,
                            duration = epDuration,
                            cover = epCover,
                            videoUrl = epUrl,
                            containerExtension = epExt,
                        )
                    } ?: emptyList()
                    XtreamSeason(
                        id = seasonId,
                        name = seasonName,
                        seasonNumber = seasonNum,
                        cover = sObj["cover"]?.jsonPrimitive?.content?.ifBlank { null },
                        episodes = episodes,
                    )
                }
                series.add(
                    XtreamSeries(
                        id = seriesId,
                        name = name,
                        cover = obj["cover"]?.jsonPrimitive?.content?.ifBlank { null },
                        backdrop = obj["backdrop_path"]?.jsonPrimitive?.content?.ifBlank { null },
                        plot = obj["plot"]?.jsonPrimitive?.content?.ifBlank { null },
                        releaseDate = obj["release_date"]?.jsonPrimitive?.content?.ifBlank { null },
                        cast = obj["cast"]?.jsonPrimitive?.content?.ifBlank { null },
                        genre = obj["genre"]?.jsonPrimitive?.content?.ifBlank { null },
                        seasons = seasons ?: emptyList(),
                        lastModified = obj["last_modified"]?.jsonPrimitive?.content?.ifBlank { null },
                        categoryId = obj["category_id"]?.jsonPrimitive?.content?.ifBlank { null },
                    )
                )
            }
        } catch (_: Exception) { }
        return series
    }

    private fun buildXtreamMovieUrl(server: String, username: String, password: String, streamId: String, extension: String): String {
        val base = server.trimEnd('/')
        return "$base/movie/$username/$password/$streamId.$extension"
    }

    private fun buildXtreamSeriesUrl(server: String, username: String, password: String, seriesId: String, seasonId: String, episodeId: String, extension: String): String {
        val base = server.trimEnd('/')
        return "$base/series/$username/$password/$seasonId/$episodeId.$extension"
    }
}
