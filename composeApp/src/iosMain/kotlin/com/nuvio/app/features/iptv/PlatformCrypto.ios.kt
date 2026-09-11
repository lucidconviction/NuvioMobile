@file:OptIn(ExperimentalForeignApi::class)

package com.nuvio.app.features.iptv

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.refTo
import kotlinx.cinterop.toKString
import kotlinx.cinterop.usePinned
import platform.CommonCrypto.CCHmac
import platform.CommonCrypto.CC_SHA256_DIGEST_LENGTH
import platform.CommonCrypto.kCCHmacAlgSHA256
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.posix.free

internal actual fun hmacSha256(data: String, secret: String): String {
    val dataBytes = data.encodeToByteArray()
    val secretBytes = secret.encodeToByteArray()

    val mac = ByteArray(CC_SHA256_DIGEST_LENGTH.toInt())
    mac.usePinned { pinned ->
        CCHmac(
            algorithm = kCCHmacAlgSHA256,
            key = secretBytes.refTo(0).getPointer(pinned.pointed).readBytes(secretBytes.size),
            keyLength = secretBytes.size.toULong(),
            data = dataBytes.refTo(0).getPointer(pinned.pointed).readBytes(dataBytes.size),
            dataLength = dataBytes.size.toULong(),
            macOut = mac.refTo(0).getPointer(pinned.pointed),
        )
    }

    return mac.joinToString("") { "%02x".format(it) }
}

internal actual fun base64UrlDecode(input: String): ByteArray {
    val std = input.replace('-', '+').replace('_', '/')
    val padded = std + "=".repeat((4 - std.length % 4) % 4)
    val nsString = NSString.create(string = padded)
    val data = nsString.dataUsingEncoding(NSUTF8StringEncoding) ?: return ByteArray(0)
    return ByteArray(data.length.toInt()).also { arr ->
        data.bytes?.let { ptr ->
            arr.usePinned { pinned ->
                kotlinx.cinterop.memcpy(pinned.addressOf(0), ptr, data.length)
            }
        }
    }
}
