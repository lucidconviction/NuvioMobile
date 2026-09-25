package com.nuvio.app.features.iptv

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * Tracks which portal keys (url|username) have been installed via the portalnutz scraper UI.
 * Used to show "Install" chips for uninstalled portals and "ADDED" badges for installed ones.
 *
 * Storage is persisted across process death via [IptvStorage] but **not** across app close
 * (caches directory) — same lifetime as the dead-stream cache.
 */
@Serializable
private data class InstalledPortalsPayload(val keys: Set<String> = emptySet())

object PortalInstallStore {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _installed = MutableStateFlow<Set<String>>(emptySet())
    val installed: StateFlow<Set<String>> = _installed.asStateFlow()

    private var loaded = false

    private fun ensureLoaded() {
        if (loaded) return
        loaded = true
        val raw = try { IptvStorage.loadInstalledPortals() } catch (_: Exception) { null }
        val keys = if (raw != null) {
            try { json.decodeFromString<InstalledPortalsPayload>(raw).keys } catch (_: Exception) { emptySet() }
        } else emptySet()
        _installed.value = keys
    }

    private fun persist() {
        scope.launch {
            try {
                IptvStorage.saveInstalledPortals(json.encodeToString(InstalledPortalsPayload(_installed.value)))
            } catch (_: Exception) {}
        }
    }

    /** Key form: `url|username` — matches the key used by [isInstalled]. */
    fun keyOf(url: String, username: String): String = "$url|$username"

    fun isInstalled(url: String, username: String): Boolean {
        ensureLoaded()
        return keyOf(url, username) in _installed.value
    }

    fun markInstalled(url: String, username: String) {
        ensureLoaded()
        if (keyOf(url, username) in _installed.value) return
        _installed.value = _installed.value + keyOf(url, username)
        persist()
    }

    fun markUninstalled(url: String, username: String) {
        ensureLoaded()
        val k = keyOf(url, username)
        if (k !in _installed.value) return
        _installed.value = _installed.value - k
        persist()
    }

    fun clear() {
        ensureLoaded()
        _installed.value = emptySet()
        persist()
    }
}