package com.nuvio.app.features.backup

import co.touchlab.kermit.Logger
import com.nuvio.app.core.auth.AuthStorage
import com.nuvio.app.core.ui.CardDepthStyleStorage
import com.nuvio.app.core.ui.PosterCardStyleStorage
import com.nuvio.app.core.sync.SyncClientIdentityStorage
import com.nuvio.app.features.collection.CollectionMobileSettingsStorage
import com.nuvio.app.features.collection.CollectionStorage
import com.nuvio.app.features.debrid.DebridSettingsStorage
import com.nuvio.app.features.details.MetaScreenSettingsStorage
import com.nuvio.app.features.downloads.DownloadsStorage
import com.nuvio.app.features.home.HomeCatalogSettingsStorage

import com.nuvio.app.features.addons.AddonStorage
import com.nuvio.app.features.iptv.IptvStorage
import com.nuvio.app.features.library.LibraryStorage
import com.nuvio.app.features.mdblist.MdbListSettingsStorage
import com.nuvio.app.features.notifications.EpisodeReleaseNotificationsStorage

import com.nuvio.app.features.player.PlayerSettingsStorage
import com.nuvio.app.features.profiles.AvatarStorage
import com.nuvio.app.features.profiles.MAX_PROFILES
import com.nuvio.app.features.profiles.ProfilePinCacheStorage
import com.nuvio.app.features.profiles.ProfileStorage

import com.nuvio.app.features.search.SearchHistoryStorage
import com.nuvio.app.features.settings.ThemeSettingsStorage
import com.nuvio.app.features.streams.StreamBadgeSettingsStorage
import com.nuvio.app.features.tmdb.TmdbSettingsStorage
import com.nuvio.app.features.trakt.TraktAuthStorage
import com.nuvio.app.features.trakt.TraktCommentsStorage
import com.nuvio.app.features.trakt.TraktLibraryStorage
import com.nuvio.app.features.trakt.TraktSettingsStorage
import com.nuvio.app.features.watched.WatchedStorage
import com.nuvio.app.features.watchprogress.ContinueWatchingPreferencesStorage
import com.nuvio.app.features.watchprogress.WatchProgressStorage
import com.nuvio.app.features.trakt.TraktPlatformClock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

object BackupManager {
    private val log by lazy { Logger.withTag("BackupManager") }
    private val json by lazy {
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            prettyPrint = true
        }
    }

    fun exportBackup(appVersion: String = ""): String {
        val sections = mutableMapOf<String, String>()

        // ── Simple storages (loadPayload/savePayload, no params) ──
        putIfPresent(sections, "profile", ProfileStorage::loadPayload)
        putIfPresent(sections, "avatar", AvatarStorage::loadPayload)
        putIfPresent(sections, "collection", CollectionStorage::loadPayload)
        putIfPresent(sections, "collection_mobile", CollectionMobileSettingsStorage::loadPayload)
        putIfPresent(sections, "continue_watching_prefs", ContinueWatchingPreferencesStorage::loadPayload)
        putIfPresent(sections, "downloads", DownloadsStorage::loadPayload)
        putIfPresent(sections, "home_catalog", HomeCatalogSettingsStorage::loadPayload)
        putIfPresent(sections, "meta_screen", MetaScreenSettingsStorage::loadPayload)
        putIfPresent(sections, "poster_card_style", PosterCardStyleStorage::loadPayload)
        putIfPresent(sections, "card_depth_style", CardDepthStyleStorage::loadPayload)
        putIfPresent(sections, "search_history", SearchHistoryStorage::loadPayload)
        putIfPresent(sections, "episode_release_notifications", EpisodeReleaseNotificationsStorage::loadPayload)
        putIfPresent(sections, "trakt_auth", TraktAuthStorage::loadPayload)
        putIfPresent(sections, "trakt_settings", TraktSettingsStorage::loadPayload)
        putIfPresent(sections, "trakt_library", TraktLibraryStorage::loadPayload)

        // ── Settings with exportToSyncPayload ──
        putJsonObj(sections, "theme_settings", ThemeSettingsStorage::exportToSyncPayload)
        putJsonObj(sections, "player_settings", PlayerSettingsStorage::exportToSyncPayload)
        putJsonObj(sections, "stream_badge_settings", StreamBadgeSettingsStorage::exportToSyncPayload)
        putJsonObj(sections, "debrid_settings", DebridSettingsStorage::exportToSyncPayload)
        putJsonObj(sections, "tmdb_settings", TmdbSettingsStorage::exportToSyncPayload)
        putJsonObj(sections, "mdblist_settings", MdbListSettingsStorage::exportToSyncPayload)
        putJsonObj(sections, "trakt_comments_settings", TraktCommentsStorage::exportToSyncPayload)

        // ── IPTV ──
        putIfPresent(sections, "iptv_settings", IptvStorage::loadSettings)
        putIfPresent(sections, "iptv_epg_cache", IptvStorage::loadEpgCache)

        // ── Auth & identity ──
        putIfPresent(sections, "anonymous_user_id", AuthStorage::loadAnonymousUserId)
        putIfPresent(sections, "client_id", SyncClientIdentityStorage::loadClientId)

        // ── Profile-scoped storages (iterate profiles) ──
        for (pid in 1..MAX_PROFILES) {
            val profileKey = "profile_pin_cache_$pid"
            putIfPresent(sections, profileKey) { ProfilePinCacheStorage.loadPayload(pid) }
            val watchedKey = "watched_$pid"
            putIfPresent(sections, watchedKey) { WatchedStorage.loadPayload(pid) }
            val watchProgressKey = "watch_progress_$pid"
            putIfPresent(sections, watchProgressKey) { WatchProgressStorage.loadPayload(pid) }
            val libraryKey = "library_$pid"
            putIfPresent(sections, libraryKey) { LibraryStorage.loadPayload(pid) }
        }

        val data = BackupData(
            version = 1,
            createdAt = TraktPlatformClock.nowEpochMs().toString(),
            appVersion = appVersion,
            sections = sections,
        )
        return json.encodeToString(data)
    }

    fun importBackup(backupJson: String): ImportResult {
        val data = try {
            json.decodeFromString<BackupData>(backupJson)
        } catch (e: Exception) {
            log.e(e) { "Failed to decode backup" }
            return ImportResult(success = false, error = "Invalid backup file: ${e.message}")
        }

        if (data.version != 1) {
            return ImportResult(success = false, error = "Unsupported backup version: ${data.version}")
        }

        return importBackupRaw(data)
    }

    fun importBackupRaw(data: BackupData): ImportResult {

        val errors = mutableListOf<String>()

        // ── Restore simple storages ──
        restoreIfPresent(data.sections, "profile") { ProfileStorage.savePayload(it) }
        restoreIfPresent(data.sections, "avatar") { AvatarStorage.savePayload(it) }
        restoreIfPresent(data.sections, "collection") { CollectionStorage.savePayload(it) }
        restoreIfPresent(data.sections, "collection_mobile") { CollectionMobileSettingsStorage.savePayload(it) }
        restoreIfPresent(data.sections, "continue_watching_prefs") { ContinueWatchingPreferencesStorage.savePayload(it) }
        restoreIfPresent(data.sections, "downloads") { DownloadsStorage.savePayload(it) }
        restoreIfPresent(data.sections, "home_catalog") { HomeCatalogSettingsStorage.savePayload(it) }
        restoreIfPresent(data.sections, "meta_screen") { MetaScreenSettingsStorage.savePayload(it) }
        restoreIfPresent(data.sections, "poster_card_style") { PosterCardStyleStorage.savePayload(it) }
        restoreIfPresent(data.sections, "card_depth_style") { CardDepthStyleStorage.savePayload(it) }
        restoreIfPresent(data.sections, "search_history") { SearchHistoryStorage.savePayload(it) }
        restoreIfPresent(data.sections, "episode_release_notifications") { EpisodeReleaseNotificationsStorage.savePayload(it) }
        restoreIfPresent(data.sections, "trakt_auth") { TraktAuthStorage.savePayload(it) }
        restoreIfPresent(data.sections, "trakt_settings") { TraktSettingsStorage.savePayload(it) }
        restoreIfPresent(data.sections, "trakt_library") { TraktLibraryStorage.savePayload(it) }

        // ── Settings with replaceFromSyncPayload ──
        restoreJsonObj(data.sections, "theme_settings") { ThemeSettingsStorage.replaceFromSyncPayload(it) }
        restoreJsonObj(data.sections, "player_settings") { PlayerSettingsStorage.replaceFromSyncPayload(it) }
        restoreJsonObj(data.sections, "stream_badge_settings") { StreamBadgeSettingsStorage.replaceFromSyncPayload(it) }
        restoreJsonObj(data.sections, "debrid_settings") { DebridSettingsStorage.replaceFromSyncPayload(it) }
        restoreJsonObj(data.sections, "tmdb_settings") { TmdbSettingsStorage.replaceFromSyncPayload(it) }
        restoreJsonObj(data.sections, "mdblist_settings") { MdbListSettingsStorage.replaceFromSyncPayload(it) }
        restoreJsonObj(data.sections, "trakt_comments_settings") { TraktCommentsStorage.replaceFromSyncPayload(it) }

        // ── IPTV ──
        restoreIfPresent(data.sections, "iptv_settings") { IptvStorage.saveSettings(it) }
        restoreIfPresent(data.sections, "iptv_epg_cache") { IptvStorage.saveEpgCache(it) }

        // ── Auth & identity ──
        restoreIfPresent(data.sections, "anonymous_user_id") { AuthStorage.saveAnonymousUserId(it) }
        restoreIfPresent(data.sections, "client_id") { SyncClientIdentityStorage.saveClientId(it) }

        // ── Raw NuvioSync sections ──
        restoreIfPresent(data.sections, "library_raw") { LibraryStorage.savePayload(1, it) }
        restoreIfPresent(data.sections, "watched_raw") { WatchedStorage.savePayload(1, it) }
        restoreIfPresent(data.sections, "progress_raw") { WatchProgressStorage.savePayload(1, it) }
        restoreIfPresent(data.sections, "addon_urls") {
            val urls = json.decodeFromString<List<String>>(it)
            AddonStorage.saveInstalledAddonUrls(1, urls)
        }
        restoreIfPresent(data.sections, "addon_enabled") {
            val states = json.decodeFromString<Map<String, Boolean>>(it)
            AddonStorage.saveAddonEnabledStates(1, states)
        }

        // ── Profile-scoped storages ──
        for (pid in 1..MAX_PROFILES) {
            val pinKey = "profile_pin_cache_$pid"
            restoreIfPresent(data.sections, pinKey) { ProfilePinCacheStorage.savePayload(pid, it) }
            val watchedKey = "watched_$pid"
            restoreIfPresent(data.sections, watchedKey) { WatchedStorage.savePayload(pid, it) }
            val wpKey = "watch_progress_$pid"
            restoreIfPresent(data.sections, wpKey) { WatchProgressStorage.savePayload(pid, it) }
            val libKey = "library_$pid"
            restoreIfPresent(data.sections, libKey) { LibraryStorage.savePayload(pid, it) }
        }

        return ImportResult(success = true, error = if (errors.isNotEmpty()) errors.joinToString("; ") else null)
    }

    data class ImportResult(
        val success: Boolean,
        val error: String? = null,
    )

    // ── Helpers ──

    private fun putIfPresent(sections: MutableMap<String, String>, key: String, loader: () -> String?) {
        try {
            val value = loader()
            if (!value.isNullOrBlank()) {
                sections[key] = value
            }
        } catch (e: Exception) {
            log.w { "Failed to export $key: ${e.message}" }
        }
    }

    private fun putJsonObj(sections: MutableMap<String, String>, key: String, loader: () -> JsonObject) {
        try {
            val obj = loader()
            if (obj.isNotEmpty()) {
                sections[key] = json.encodeToString(JsonObject.serializer(), obj)
            }
        } catch (e: Exception) {
            log.w { "Failed to export $key: ${e.message}" }
        }
    }

    private fun restoreIfPresent(sections: Map<String, String>, key: String, saver: (String) -> Unit) {
        val value = sections[key]
        if (value != null) {
            try {
                saver(value)
            } catch (e: Exception) {
                log.e(e) { "Failed to restore $key" }
            }
        }
    }

    private fun restoreJsonObj(sections: Map<String, String>, key: String, saver: (JsonObject) -> Unit) {
        val value = sections[key]
        if (value != null) {
            try {
                val obj = json.decodeFromString(JsonObject.serializer(), value)
                saver(obj)
            } catch (e: Exception) {
                log.e(e) { "Failed to restore $key" }
            }
        }
    }
}
