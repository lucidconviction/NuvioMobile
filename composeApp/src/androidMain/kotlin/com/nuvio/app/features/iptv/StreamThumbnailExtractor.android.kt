package com.nuvio.app.features.iptv

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import co.touchlab.kermit.Logger
import `is`.xyz.mpv.MPV
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume

actual class StreamThumbnailExtractorFactory {
    actual companion object {
        actual fun create(): StreamThumbnailExtractor = AndroidStreamThumbnailExtractor()
    }
}

private class AndroidStreamThumbnailExtractor : StreamThumbnailExtractor {
    private val log = Logger.withTag("StreamThumbnailExtractor")
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var mpv: MPV? = null
    private var initContext: Context? = null

    private fun ensureMpv(context: Context) {
        if (mpv == null) {
            initContext = context.applicationContext
            try {
                mpv = MPV().also {
                    it.create(context.applicationContext)
                    it.init()
                }
            } catch (e: Exception) {
                log.e(e) { "Failed to init MPV" }
            }
        }
    }

    override suspend fun extractThumbnail(
        streamUrl: String,
        headers: Map<String, String>,
    ): ThumbnailResult = suspendCancellableCoroutine { cont ->
        scope.launch {
            val context = initContext ?: run {
                cont.resume(ThumbnailResult(error = "No context"))
                return@launch
            }
            ensureMpv(context)

            val mpvInstance = mpv ?: run {
                cont.resume(ThumbnailResult(error = "MPV not initialized"))
                return@launch
            }

            val tempFile = File(context.cacheDir, "stream_thumb_${System.currentTimeMillis()}.jpg")
            tempFile.deleteOnExit()

            try {
                val requestHeaders = headers.map { "${it.key}: ${it.value}" }.joinToString("\r\n")
                val fullUrl = if (requestHeaders.isNotBlank()) "$streamUrl|$requestHeaders" else streamUrl

                mpvInstance.command("loadfile", fullUrl, "replace")

                // Wait for playback to start and seek to 5 seconds
                var attempts = 0
                while (attempts < 50) {
                    val timePos = mpvInstance.getPropertyDouble("time-pos") ?: 0.0
                    if (timePos >= 5.0) break
                    kotlinx.coroutines.delay(200)
                    attempts++
                }

                mpvInstance.setPropertyBoolean("pause", true)
                kotlinx.coroutines.delay(300)

                val bitmap: Bitmap? = mpvInstance.grabThumbnail(640)
                if (bitmap != null) {
                    tempFile.outputStream().use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
                    }
                    if (tempFile.exists() && tempFile.length() > 0) {
                        cont.resume(ThumbnailResult(thumbnailUri = Uri.fromFile(tempFile).toString()))
                    } else {
                        cont.resume(ThumbnailResult(error = "Failed to save thumbnail"))
                    }
                } else {
                    cont.resume(ThumbnailResult(error = "Could not grab thumbnail"))
                }
            } catch (e: Exception) {
                log.e(e) { "Thumbnail extraction failed" }
                cont.resume(ThumbnailResult(error = e.message ?: "Unknown error"))
            }
        }
    }
}
