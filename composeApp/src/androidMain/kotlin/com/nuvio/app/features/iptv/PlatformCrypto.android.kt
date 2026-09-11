package com.nuvio.app.features.iptv

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

internal actual fun hmacSha256(data: String, secret: String): String {
    val mac = Mac.getInstance("HmacSHA256")
    mac.init(SecretKeySpec(secret.toByteArray(), "HmacSHA256"))
    val hash = mac.doFinal(data.toByteArray())
    return hash.joinToString("") { "%02x".format(it) }
}

internal actual fun base64UrlDecode(input: String): ByteArray {
    val std = input.replace('-', '+').replace('_', '/')
    val padded = std + "=".repeat((4 - std.length % 4) % 4)
    return android.util.Base64.decode(padded, android.util.Base64.DEFAULT)
}
