package com.nuvio.app.features.hub

import android.content.Context
import android.content.SharedPreferences

internal actual object VidNutzSearchReturnStore {
    private const val PREFS_NAME = "nuvio_vidnutz_search"
    private const val KEY_STATE = "state_json"

    private fun getPrefs(): SharedPreferences? {
        val ctx = com.nuvio.app.appContext ?: return null
        return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    actual fun save(state: VidNutzUiState) {
        val json = VidNutzSearchReturnStoreImpl.saveStateJson(state)
        getPrefs()?.edit()?.putString(KEY_STATE, json)?.apply()
    }

    actual fun restore(): VidNutzUiState? {
        val raw = getPrefs()?.getString(KEY_STATE, null)
        return VidNutzSearchReturnStoreImpl.restoreStateJson(raw)
    }

    actual fun clear() {
        getPrefs()?.edit()?.remove(KEY_STATE)?.apply()
    }
}