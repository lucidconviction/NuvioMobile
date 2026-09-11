package com.nuvio.app.features.hub

import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

/**
 * Provides the effective VidNutz hub catalog = immutable Staff Picks hub + default
 * hub catalog overlaid with the user's edits. All mutations are persisted locally.
 * Staff Picks is always present, always first, and protected from add/delete/edit/reorder.
 */
object VidNutzHubManager {

    private val json = Json { ignoreUnknownKeys = true }

    /** The built-in default menu (everything except Staff Picks, before user edits). */
    private val defaults: List<VidNutzHub> = VidNutzHubs.defaultsForEditing

    @Volatile
    private var userHubs: List<VidNutzHub>? = null

    private fun userEdits(): List<VidNutzHub> {
        userHubs?.let { return it }
        val loaded = VidNutzHubConfigStore.load()?.let {
            try { json.decodeFromString<VidNutzHubConfig>(it) } catch (_: Exception) { null }
        }?.hubs ?: defaults
        userHubs = loaded
        return loaded
    }

    /** Effective menu with Staff Picks always first. */
    val menu: List<VidNutzHub>
        get() = listOf(VidNutzStaffPicks.hub) + userEdits()

    fun byId(id: String): VidNutzHub? = menu.firstOrNull { it.id == id }

    private fun persist() {
        userHubs?.let { hubs ->
            try {
                VidNutzHubConfigStore.save(json.encodeToString(VidNutzHubConfig(hubs = hubs)))
            } catch (_: Exception) {}
        }
    }

    // ── Read-side helpers accessed by the edit UI ──
    val editableHubs: List<VidNutzHub>
        get() = userEdits()

    fun reload() {
        userHubs = null
    }

    // ── Mutations (all guarded against Staff Picks) ──

    /** Add a new top-level category hub. */
    fun addHub(displayName: String): VidNutzHub? {
        val name = displayName.trim()
        if (name.isEmpty()) return null
        val id = hubIdFor(name)
        if (menu.any { it.id == id }) return null
        val hub = VidNutzHub(id = id, displayName = name, subs = emptyList())
        userHubs = userEdits().let { it + hub }
        persist()
        return hub
    }

    fun deleteHub(hubId: String): Boolean {
        if (hubId == STAFF_PICKS_HUB_ID) return false
        val updated = userEdits().filterNot { it.id == hubId }
        if (updated.size == userHubs?.size?.let { it } ?: userEdits().size) return false
        userHubs = updated
        persist()
        return true
    }

    /** Move a hub up (to earlier position). Staff Picks always stays first. */
    fun moveHubUp(hubId: String): Boolean {
        if (hubId == STAFF_PICKS_HUB_ID) return false
        val list = userEdits()
        val idx = list.indexOfFirst { it.id == hubId }
        if (idx <= 0) return false
        val moved = list.toMutableList().also {
            val hub = it.removeAt(idx)
            it.add(idx - 1, hub)
        }
        userHubs = moved
        persist()
        return true
    }

    fun moveHubDown(hubId: String): Boolean {
        val list = userEdits()
        val idx = list.indexOfFirst { it.id == hubId }
        if (idx < 0 || idx >= list.size - 1) return false
        val moved = list.toMutableList().also {
            val hub = it.removeAt(idx)
            it.add(idx + 1, hub)
        }
        userHubs = moved
        persist()
        return true
    }

    fun renameHub(hubId: String, newName: String): Boolean {
        if (hubId == STAFF_PICKS_HUB_ID) return false
        val name = newName.trim()
        if (name.isEmpty()) return false
        val updated = userEdits().map { if (it.id == hubId) it.copy(displayName = name) else it }
        userHubs = updated
        persist()
        return true
    }

    // ── Sub-category (rail) mutations ──

    fun addSub(hubId: String, subName: String): VidNutzSub? {
        if (hubId == STAFF_PICKS_HUB_ID) return null
        val name = subName.trim()
        if (name.isEmpty()) return null
        val subId = subIdFor(name)
        if (menu.any { h -> h.subs.any { it.id == subId } }) return null
        val sub = VidNutzSub(id = subId, name = name, count = 0, queries = listOf(name))
        val updated = userEdits().map { hub ->
            if (hub.id == hubId) hub.copy(subs = hub.subs + sub) else hub
        }
        userHubs = updated
        persist()
        return sub
    }

    fun deleteSub(hubId: String, subId: String): Boolean {
        if (hubId == STAFF_PICKS_HUB_ID) return false
        val updated = userEdits().map { hub ->
            if (hub.id == hubId) hub.copy(subs = hub.subs.filterNot { it.id == subId }) else hub
        }
        userHubs = updated
        persist()
        return true
    }

    fun renameSub(hubId: String, subId: String, newName: String): Boolean {
        if (hubId == STAFF_PICKS_HUB_ID) return false
        val name = newName.trim()
        if (name.isEmpty()) return false
        val updated = userEdits().map { hub ->
            if (hub.id == hubId) {
                hub.copy(subs = hub.subs.map { if (it.id == subId) it.copy(name = name, queries = listOf(name)) else it })
            } else hub
        }
        userHubs = updated
        persist()
        return true
    }

    fun moveSubUp(hubId: String, subId: String): Boolean {
        val list = userEdits().firstOrNull { it.id == hubId } ?: return false
        val subs = list.subs
        val idx = subs.indexOfFirst { it.id == subId }
        if (idx <= 0) return false
        val moved = subs.toMutableList().also {
            val s = it.removeAt(idx)
            it.add(idx - 1, s)
        }
        userHubs = userEdits().map { if (it.id == hubId) it.copy(subs = moved) else it }
        persist()
        return true
    }

    fun moveSubDown(hubId: String, subId: String): Boolean {
        val list = userEdits().firstOrNull { it.id == hubId } ?: return false
        val subs = list.subs
        val idx = subs.indexOfFirst { it.id == subId }
        if (idx < 0 || idx >= subs.size - 1) return false
        val moved = subs.toMutableList().also {
            val s = it.removeAt(idx)
            it.add(idx + 1, s)
        }
        userHubs = userEdits().map { if (it.id == hubId) it.copy(subs = moved) else it }
        persist()
        return true
    }

    /** Reset all user edits back to the built-in default catalog. */
    fun resetToDefaults() {
        userHubs = defaults
        persist()
    }

    private fun hubIdFor(name: String): String =
        name.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')

    private fun subIdFor(name: String): String =
        name.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')
}
