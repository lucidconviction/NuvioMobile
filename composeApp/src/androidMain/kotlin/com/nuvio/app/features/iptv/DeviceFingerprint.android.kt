package com.nuvio.app.features.iptv

import android.content.Context
import java.util.UUID

internal actual object DeviceFingerprint {
    private const val PREFS_NAME = "nuvio_device"
    private const val KEY_DEVICE_ID = "device_fingerprint"

    private var context: Context? = null

    fun initialize(context: Context) {
        this.context = context.applicationContext
    }

    actual fun getDeviceId(): String {
        val ctx = context ?: return "unknown"
        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var id = prefs.getString(KEY_DEVICE_ID, null)
        if (id == null) {
            id = UUID.randomUUID().toString()
            prefs.edit().putString(KEY_DEVICE_ID, id).apply()
        }
        return id
    }
}
