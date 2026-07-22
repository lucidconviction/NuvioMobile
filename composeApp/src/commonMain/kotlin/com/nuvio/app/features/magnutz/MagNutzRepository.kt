package com.nuvio.app.features.magnutz

import kotlinx.coroutines.flow.StateFlow

expect object MagNutzRepository {
    val uiState: StateFlow<MagNutzUiState>
    fun ensureLoaded()
    fun consumePendingMagnet()
    fun addMagnet(magnetUri: String, title: String? = null)
    fun addFromAddon(infoHash: String, fileIdx: Int?, filename: String?, magnetUri: String?, title: String?, trackers: List<String>)
    fun pauseDownload(id: String)
    fun resumeDownload(id: String)
    fun cancelDownload(id: String)
    fun playDownload(item: MagNutzItem): com.nuvio.app.features.player.PlayerLaunch?
    fun clearError()
    fun setFilter(filter: MagNutzFilter)
    fun setSearchQuery(query: String)
    fun setAddingMagnet(adding: Boolean)
    fun setMagnetInputText(text: String)
    var pendingMagnetFromExternal: String?
    fun getSaveLocationDisplayPath(): String
    fun setSaveLocationUri(uri: String?)
    fun requestPickSaveLocation()
}
