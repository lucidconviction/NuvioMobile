package com.nuvio.app.features.iptv

import com.nuvio.app.features.trakt.TraktPlatformClock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

internal expect fun hmacSha256(data: String, secret: String): String
internal expect fun base64UrlDecode(input: String): ByteArray

object PortalLicenseManager {
    private const val GRACE_PERIOD_MS = 24 * 60 * 60 * 1000L
    private const val SECRET = "Rdnutz"

    private val json = Json { ignoreUnknownKeys = true }

    fun verifyKey(key: String): LicenseResult {
        val cleaned = key.trim()
        val encoded = when {
            cleaned.startsWith("NVIO-") -> cleaned.removePrefix("NVIO-")
            cleaned.startsWith("nvio-") -> cleaned.removePrefix("nvio-")
            else -> return LicenseResult.Failure("Invalid key format")
        }
        val raw = try {
            base64UrlDecode(encoded).decodeToString()
        } catch (_: Exception) {
            return LicenseResult.Failure("Invalid key format")
        }

        val parts = raw.split(".", limit = 2)
        if (parts.size != 2) return LicenseResult.Failure("Invalid key format")

        val payloadJson = parts[0]
        val signature = parts[1]

        val expectedSig = hmacSha256(payloadJson, SECRET)
        if (!constantTimeEquals(signature, expectedSig)) {
            return LicenseResult.Failure("Invalid or expired key")
        }

        val payload = try {
            json.decodeFromString<JsonObject>(payloadJson)
        } catch (_: Exception) {
            return LicenseResult.Failure("Invalid key format")
        }

        val id = payload["id"]?.jsonPrimitive?.content ?: return LicenseResult.Failure("Invalid key format")
        val exp = payload["exp"]?.jsonPrimitive?.content?.toLongOrNull() ?: return LicenseResult.Failure("Invalid key format")
        val maxDev = payload["maxDev"]?.jsonPrimitive?.content?.toIntOrNull() ?: 1
        val isAdmin = payload["isAdmin"]?.jsonPrimitive?.content == "true"

        val license = PortalLicenseKey(id = id, exp = exp, maxDev = maxDev, isAdmin = isAdmin)
        return LicenseResult.Success(license)
    }

    fun checkStatus(license: PortalLicenseKey?): LicenseStatus {
        if (license == null) return LicenseStatus.NOT_ACTIVATED

        val now = TraktPlatformClock.nowEpochMs()
        if (now > license.exp) return LicenseStatus.EXPIRED
        if (now > license.exp - GRACE_PERIOD_MS) return LicenseStatus.GRACE

        val savedFingerprint = IptvStorage.loadFingerprint()
        val currentFingerprint = DeviceFingerprint.getDeviceId()

        if (savedFingerprint != null && savedFingerprint != currentFingerprint) {
            return LicenseStatus.WRONG_DEVICE
        }

        return LicenseStatus.VALID
    }

    fun saveActivation(license: PortalLicenseKey) {
        val json = """{"id":"${license.id}","exp":${license.exp},"maxDev":${license.maxDev},"isAdmin":${license.isAdmin}}"""
        IptvStorage.saveLicense(json)
        if (IptvStorage.loadFingerprint() == null) {
            IptvStorage.saveFingerprint(DeviceFingerprint.getDeviceId())
        }
    }

    fun getSavedLicense(): PortalLicenseKey? {
        val raw = IptvStorage.loadLicense() ?: return null
        return try {
            json.decodeFromString<PortalLicenseKey>(raw)
        } catch (_: Exception) {
            null
        }
    }

    fun getRemainingTime(license: PortalLicenseKey): String {
        val now = TraktPlatformClock.nowEpochMs()
        val remaining = license.exp - now
        if (remaining <= 0L) return "Expired"

        val days = remaining / (24 * 60 * 60 * 1000)
        val hours = (remaining % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000)

        return when {
            days > 0L -> "${days}d ${hours}h remaining"
            hours > 0L -> "${hours}h remaining"
            else -> {
                val minutes = (remaining % (60 * 60 * 1000)) / (60 * 1000)
                "${minutes}m remaining"
            }
        }
    }

    fun getExpiryDate(license: PortalLicenseKey): String {
        val epochMs = license.exp
        val secs = epochMs / 1000
        val daysSinceEpoch = secs / 86400
        var year = 1970
        var remainingDays = daysSinceEpoch
        while (remainingDays >= 365) {
            val daysInYear = if (isLeapYear(year)) 366L else 365L
            if (remainingDays < daysInYear) break
            remainingDays -= daysInYear
            year++
        }
        val monthDays = intArrayOf(31, if (isLeapYear(year)) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        var month = 1
        for (md in monthDays) {
            if (remainingDays < md) break
            remainingDays -= md
            month++
        }
        return "$year-${month.toString().padStart(2, '0')}-${(remainingDays + 1).toString().padStart(2, '0')}"
    }

    /** Non-admin users are limited to this many simultaneously installed portals. */
    const val MAX_PORTALS = 3

    fun isPortalName(name: String): Boolean {
        val n = name.trim()
        return n.startsWith("list", ignoreCase = true) || n.startsWith("portal", ignoreCase = true)
    }

    fun canAddPortal(license: PortalLicenseKey?, existingPortalCount: Int): Boolean =
        license?.isAdmin == true || existingPortalCount < MAX_PORTALS

    /**
     * On hard expiry, erase every installed portal. Returns true when a cleanup ran so the UI
     * can surface a message. Admin keys never expire, so this is a no-op for them.
     */
    fun enforceExpiryCleanup(license: PortalLicenseKey?): Boolean {
        if (license == null || license.isAdmin) return false
        val now = TraktPlatformClock.nowEpochMs()
        if (now <= license.exp) return false
        return IptvRepository.removePortalAccounts() > 0
    }

    private fun isLeapYear(year: Int): Boolean = (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)

    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var result = 0
        for (i in a.indices) {
            result = result or (a[i].code xor b[i].code)
        }
        return result == 0
    }
}

sealed class LicenseResult {
    data class Success(val license: PortalLicenseKey) : LicenseResult()
    data class Failure(val message: String) : LicenseResult()
}
