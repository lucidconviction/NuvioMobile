package com.nuvio.app

import platform.UIKit.UIDevice

class IOSPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

actual fun getPlatform(): Platform = IOSPlatform()

internal actual val isIos: Boolean = true

internal actual fun readFileText(path: String): String? = null
internal actual fun readUriContent(uri: String): String? = null