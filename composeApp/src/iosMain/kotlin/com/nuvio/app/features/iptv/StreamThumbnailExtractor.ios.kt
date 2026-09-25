package com.nuvio.app.features.iptv

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import platform.AVFoundation.AVAsset
import platform.AVFoundation.AVAssetImageGenerator
import platform.AVFoundation.AVURLAsset
import platform.AVFoundation.CMTime
import platform.AVFoundation.CMTimeMake
import platform.Foundation.NSError
import platform.Foundation.NSURL
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.UIKit.UIImage
import java.io.File
import java.util.UUID

actual class StreamThumbnailExtractorFactory {
    actual companion object {
        actual fun create(): StreamThumbnailExtractor = IosStreamThumbnailExtractor()
    }
}

private class IosStreamThumbnailExtractor : StreamThumbnailExtractor {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override suspend fun extractThumbnail(
        streamUrl: String,
        headers: Map<String, String> = emptyMap(),
    ): ThumbnailResult = scope.completableDeferred().apply { deferred ->
        scope.launch {
            try {
                val url = NSURL(string = streamUrl)
                val options = if (headers.isNotEmpty()) {
                    val dict = platform.Foundation.NSMutableDictionary()
                    headers.forEach { (key, value) ->
                        dict.setValue(value, forKey = key)
                    }
                    dict
                } else null

                val asset = AVURLAsset.assetWithURL(url, options = options)
                val generator = AVAssetImageGenerator.assetImageGeneratorWithAsset(asset)
                generator.appliesPreferredTrackTransform = true
                generator.requestedTimeToleranceBefore = CMTimeMake(100, 1000)
                generator.requestedTimeToleranceAfter = CMTimeMake(100, 1000)

                val seekTime = CMTimeMake(5, 1)

                val result = generator.copyCGImageAtTime(seekTime, actualTime = null, error = null)
                if (result != null) {
                    val image = UIImage.imageWithCGImage(result)
                    val tempDir = NSTemporaryDirectory()
                    val fileName = "stream_thumb_${UUID.randomUUID()}.jpg"
                    val filePath = "$tempDir/$fileName"
                    val fileUrl = NSURL.fileURLWithPath(filePath)

                    val imageData = image.jpegData(compressionQuality = 0.8)
                    if (imageData != nil) {
                        imageData?.writeToFile(filePath, atomically = true)
                        val uri = "file://$filePath"
                        deferred.complete(ThumbnailResult.success(uri))
                    } else {
                        deferred.complete(ThumbnailResult.failure("Failed to create JPEG data"))
                    }
                } else {
                    deferred.complete(ThumbnailResult.failure("Failed to generate image from stream"))
                }
            } catch (e: Exception) {
                deferred.complete(ThumbnailResult.failure(e.message ?: "Unknown error"))
            }
        }
    }.await()
}