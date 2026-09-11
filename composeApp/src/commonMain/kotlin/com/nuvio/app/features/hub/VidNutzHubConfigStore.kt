package com.nuvio.app.features.hub

import kotlinx.serialization.Serializable

/** Persists the user's customized VidNutz hub catalog (categories + sub-categories). */
internal expect object VidNutzHubConfigStore {
    fun load(): String?
    fun save(json: String)
}

/**
 * User-edited hub catalog. "Staff Picks" is never stored here; it is always derived
 * from the immutable built-in catalog and merged at the front.
 */
@Serializable
data class VidNutzHubConfig(
    val hubs: List<VidNutzHub> = emptyList(),
)

/** Whether a hub/sub is protected from user edit/delete (Staff Picks). */
internal const val STAFF_PICKS_HUB_ID = "staff_picks"
