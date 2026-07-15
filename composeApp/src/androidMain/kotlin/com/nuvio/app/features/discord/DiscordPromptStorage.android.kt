package com.nuvio.app.features.discord

import android.content.Context
import android.content.SharedPreferences

actual object DiscordPromptStorage {
    private const val PREFS_NAME = "nuvio_discord_prompt"
    private const val KEY_DISMISSED = "discord_prompt_dismissed"

    private var prefs: SharedPreferences? = null

    fun initialize(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    actual fun isDismissed(): Boolean = prefs?.getBoolean(KEY_DISMISSED, false) ?: false

    actual fun setDismissed() {
        prefs?.edit()?.putBoolean(KEY_DISMISSED, true)?.apply()
    }
}
