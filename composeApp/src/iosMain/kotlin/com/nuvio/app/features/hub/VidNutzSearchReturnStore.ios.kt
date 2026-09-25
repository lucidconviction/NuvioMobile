package com.nuvio.app.features.hub

import platform.Foundation.NSUserDefaults

internal actual object VidNutzSearchReturnStore {
    private const val KEY_STATE = "nuvio_vidnutz_search_state"

    actual fun save(state: VidNutzUiState) {
        val json = VidNutzSearchReturnStoreImpl.saveStateJson(state)
        NSUserDefaults.standardUserDefaults.setObject(json, forKey = KEY_STATE)
    }

    actual fun restore(): VidNutzUiState? =
        VidNutzSearchReturnStoreImpl.restoreStateJson(NSUserDefaults.standardUserDefaults.stringForKey(KEY_STATE))

    actual fun clear() {
        NSUserDefaults.standardUserDefaults.removeObjectForKey(KEY_STATE)
    }
}