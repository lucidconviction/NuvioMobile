package com.nuvio.app.features.iptv

internal expect object DeviceFingerprint {
    fun getDeviceId(): String
}
