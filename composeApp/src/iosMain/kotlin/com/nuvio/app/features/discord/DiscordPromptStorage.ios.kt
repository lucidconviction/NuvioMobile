package com.nuvio.app.features.discord

import platform.Foundation.NSUserDefaults

actual object DiscordPromptStorage {
    private const val KEY_DISMISSED = "discord_prompt_dismissed"

    actual fun isDismissed(): Boolean =
        NSUserDefaults.standardUserDefaults.boolForKey(KEY_DISMISSED)

    actual fun setDismissed() {
        NSUserDefaults.standardUserDefaults.setBool(true, forKey = KEY_DISMISSED)
    }
}
