package com.nuvio.app

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

internal expect val isIos: Boolean

internal expect fun readFileText(path: String): String?

internal expect fun readUriContent(uri: String): String?