package com.nuvio.app.features.iptv

import kotlinx.serialization.Serializable

@Serializable
data class PortalLicenseKey(
    val id: String,
    val exp: Long,
    val maxDev: Int = 1,
    val isAdmin: Boolean = false,
)

enum class LicenseStatus {
    VALID,
    EXPIRED,
    GRACE,
    WRONG_DEVICE,
    INVALID,
    NOT_ACTIVATED,
}
