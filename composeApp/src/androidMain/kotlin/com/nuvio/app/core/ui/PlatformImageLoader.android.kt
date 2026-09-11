package com.nuvio.app.core.ui

import android.os.Build
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.gif.AnimatedImageDecoder
import coil3.gif.GifDecoder
import okio.Path.Companion.toOkioPath

internal actual fun ImageLoader.Builder.configurePlatformImageLoader(): ImageLoader.Builder =
    components {
        if (Build.VERSION.SDK_INT >= 28) {
            add(AnimatedImageDecoder.Factory())
        } else {
            add(GifDecoder.Factory())
        }
    }
    .diskCache {
        val dir = com.nuvio.app.appContext?.cacheDir?.resolve("image_cache")
        if (dir != null) {
            DiskCache.Builder()
                .directory(dir.toOkioPath())
                .maxSizeBytes(250L * 1024 * 1024)
                .build()
        } else {
            null
        }
    }