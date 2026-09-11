package com.nuvio.app.features.ai

internal expect object AiSettingsStore {
    fun isEnabled(): Boolean
    fun setEnabled(enabled: Boolean)
    fun getBaseUrl(): String
    fun setBaseUrl(url: String)
}
