package com.nuvio.app.features.iptv

import platform.Foundation.NSUserDefaults
import platform.Foundation.NSUUID

internal actual object DeviceFingerprint {
    private const val KEY_DEVICE_ID = "nuvio_device_fingerprint"

    actual fun getDeviceId(): String {
        val defaults = NSUserDefaults.standardUserDefaults
        var id = defaults.stringForKey(KEY_DEVICE_ID)
        if (id == null || id.isEmpty()) {
            id = NSUUID.UUID().UUIDString
            defaults.setObject(id, forKey = KEY_DEVICE_ID)
        }
        return id
    }
}
