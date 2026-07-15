package com.nuvio.app.features.backup

import com.nuvio.app.features.library.LibraryItem
import com.nuvio.app.features.watched.WatchedItem
import com.nuvio.app.features.watchprogress.WatchProgressEntry
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.contentOrNull

object NuvioSyncConverter {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun convertToBackupData(nuvioSyncJson: String): BackupData? {
        val root = try {
            json.parseToJsonElement(nuvioSyncJson).jsonObject
        } catch (e: Exception) {
            return null
        }

        val original = root["original"]?.jsonObject ?: return null
        val profileId = root["profileIndex"]?.jsonPrimitive?.intOrNull ?: 1

        val sections = mutableMapOf<String, String>()

        val libraryItems = parseLibraryItems(original["library"]?.jsonArray)
        if (libraryItems.isNotEmpty()) {
            sections["library_raw"] = json.encodeToString(StoredLibraryPayload(libraryItems))
        }

        val watchedItems = parseWatchedItems(original["watched"]?.jsonArray, libraryItems)
        if (watchedItems.isNotEmpty()) {
            sections["watched_raw"] = json.encodeToString(StoredWatchedPayload(watchedItems))
        }

        val progressEntries = parseProgressEntries(original["progress"]?.jsonArray)
        if (progressEntries.isNotEmpty()) {
            sections["progress_raw"] = json.encodeToString(StoredWatchProgressPayload(progressEntries))
        }

        val addonUrls = parseAddonUrls(original["addons"]?.jsonArray)
        if (addonUrls.urls.isNotEmpty()) {
            sections["addon_urls"] = json.encodeToString(addonUrls.urls)
            sections["addon_enabled"] = json.encodeToString(addonUrls.enabledStates)
        }

        return BackupData(
            version = 1,
            createdAt = root["createdAt"]?.jsonPrimitive?.contentOrNull ?: "",
            appVersion = "nuviosync-converted",
            sections = sections,
        )
    }

    fun importNuvioSyncBackup(backupJson: String): BackupManager.ImportResult {
        val data = convertToBackupData(backupJson) ?:
            return BackupManager.ImportResult(success = false, error = "Failed to parse NuvioSync backup")

        return BackupManager.importBackupRaw(data)
    }

    private fun parseLibraryItems(array: JsonArray?): List<LibraryItem> {
        if (array == null) return emptyList()
        return array.mapNotNull { element ->
            val obj = element.jsonObject
            val contentId = obj["content_id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            LibraryItem(
                id = contentId,
                type = obj["content_type"]?.jsonPrimitive?.contentOrNull ?: "movie",
                name = obj["name"]?.jsonPrimitive?.contentOrNull ?: "",
                poster = obj["poster"]?.jsonPrimitive?.contentOrNull,
                description = obj["description"]?.jsonPrimitive?.contentOrNull,
                releaseInfo = obj["release_info"]?.jsonPrimitive?.contentOrNull,
                genres = obj["genres"]?.jsonArray?.mapNotNull {
                    it.jsonPrimitive.contentOrNull
                } ?: emptyList(),
                posterShape = when (obj["poster_shape"]?.jsonPrimitive?.contentOrNull) {
                    "LANDSCAPE" -> com.nuvio.app.features.home.PosterShape.Landscape
                    "SQUARE" -> com.nuvio.app.features.home.PosterShape.Square
                    else -> com.nuvio.app.features.home.PosterShape.Poster
                },
                imdbId = contentId.takeIf { it.startsWith("tt") },
                savedAtEpochMs = obj["added_at"]?.jsonPrimitive?.longOrNull ?: 0L,
            )
        }
    }

    private fun parseWatchedItems(array: JsonArray?, libraryItems: List<LibraryItem>): List<WatchedItem> {
        if (array == null) return emptyList()
        val libLookup = libraryItems.associateBy { it.id }
        return array.mapNotNull { element ->
            val obj = element.jsonObject
            val contentId = obj["content_id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            val libItem = libLookup[contentId]
            WatchedItem(
                id = contentId,
                type = obj["content_type"]?.jsonPrimitive?.contentOrNull ?: libItem?.type ?: "movie",
                name = obj["title"]?.jsonPrimitive?.contentOrNull ?: libItem?.name ?: "",
                poster = libItem?.poster,
                releaseInfo = libItem?.releaseInfo,
                season = obj["season"]?.jsonPrimitive?.intOrNull,
                episode = obj["episode"]?.jsonPrimitive?.intOrNull,
                markedAtEpochMs = obj["watched_at"]?.jsonPrimitive?.longOrNull ?: 0L,
            )
        }
    }

    private data class AddonUrls(
        val urls: List<String>,
        val enabledStates: Map<String, Boolean>,
    )

    private fun parseAddonUrls(array: JsonArray?): AddonUrls {
        if (array == null) return AddonUrls(emptyList(), emptyMap())
        val urls = mutableListOf<String>()
        val enabledStates = mutableMapOf<String, Boolean>()
        for (element in array) {
            val obj = element.jsonObject
            val url = obj["url"]?.jsonPrimitive?.contentOrNull ?: continue
            urls.add(url)
            enabledStates[url] = obj["enabled"]?.jsonPrimitive?.booleanOrNull ?: true
        }
        return AddonUrls(urls, enabledStates)
    }

    private fun parseProgressEntries(array: JsonArray?): List<WatchProgressEntry> {
        if (array == null) return emptyList()
        return array.mapNotNull { element ->
            val obj = element.jsonObject
            val videoId = obj["video_id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            val contentId = obj["content_id"]?.jsonPrimitive?.contentOrNull ?: ""
            val contentType = obj["content_type"]?.jsonPrimitive?.contentOrNull ?: "movie"
            val season = obj["season"]?.jsonPrimitive?.intOrNull
            val episode = obj["episode"]?.jsonPrimitive?.intOrNull
            WatchProgressEntry(
                contentType = contentType,
                parentMetaId = contentId,
                parentMetaType = contentType,
                videoId = videoId,
                title = "",
                seasonNumber = season,
                episodeNumber = episode,
                lastPositionMs = obj["position"]?.jsonPrimitive?.longOrNull ?: 0L,
                durationMs = obj["duration"]?.jsonPrimitive?.longOrNull ?: 0L,
                lastUpdatedEpochMs = obj["last_watched"]?.jsonPrimitive?.longOrNull ?: 0L,
                progressKey = obj["progress_key"]?.jsonPrimitive?.contentOrNull,
                source = "local",
            )
        }
    }
}

@Serializable
private data class StoredLibraryPayload(
    val items: List<LibraryItem> = emptyList(),
)

@Serializable
private data class StoredWatchedPayload(
    val items: List<WatchedItem> = emptyList(),
    val fullyWatchedSeriesKeys: Set<String> = emptySet(),
    val lastSuccessfulPushEpochMs: Long = 0L,
    val deltaCursorEventId: Long = 0L,
    val deltaInitialized: Boolean = false,
    val dirtyWatchedKeys: Set<String> = emptySet(),
)

@Serializable
private data class StoredWatchProgressPayload(
    val entries: List<WatchProgressEntry> = emptyList(),
    val lastSuccessfulPushEpochMs: Long = 0L,
    val deltaCursorEventId: Long = 0L,
    val deltaInitialized: Boolean = false,
    val dirtyProgressKeys: Set<String> = emptySet(),
)
