package com.nuvio.app.features.discord

internal expect object DiscordPromptStorage {
    fun isDismissed(): Boolean
    fun setDismissed()
}
