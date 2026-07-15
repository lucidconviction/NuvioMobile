package com.nuvio.app

import android.content.Context
import android.os.Build
import android.provider.MediaStore
import java.io.File

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()

internal actual val isIos: Boolean = false

internal var appContext: Context? = null

internal actual fun readUriContent(uri: String): String? {
    val ctx = appContext ?: return null
    return try {
        val parsedUri = android.net.Uri.parse(uri)
        ctx.contentResolver.openInputStream(parsedUri)?.use { stream ->
            stream.bufferedReader().readText()
        }
    } catch (e: Exception) { null }
}

internal actual fun readFileText(path: String): String? {
    var lastError: String? = null

    try {
        val result = File(path).readText()
        android.util.Log.i("BackupManager", "Direct file read OK: ${result.length} chars")
        return result
    } catch (e: Exception) {
        lastError = "direct: ${e.message}"
    }

    val ctx = appContext
    if (ctx == null) {
        android.util.Log.w("BackupManager", "appContext is null")
        return null
    }
    android.util.Log.i("BackupManager", "appContext available, filesDir=${ctx.filesDir}")

    try {
        val filesFile = File(ctx.filesDir, "nuvio-backup.json")
        if (filesFile.exists()) {
            val result = filesFile.readText()
            android.util.Log.i("BackupManager", "filesDir read OK: ${result.length} chars")
            return result
        }
        android.util.Log.i("BackupManager", "filesFile does not exist at ${filesFile.absolutePath}")
    } catch (e: Exception) {
        lastError = "filesDir: ${e.message}"
    }

    try {
        val externalDir = ctx.getExternalFilesDir(null)
        if (externalDir != null) {
            val externalFile = File(externalDir, "nuvio-backup.json")
            if (externalFile.exists()) {
                val result = externalFile.readText()
                android.util.Log.i("BackupManager", "externalFilesDir read OK: ${result.length} chars")
                return result
            }
            android.util.Log.i("BackupManager", "externalFile does not exist at ${externalFile.absolutePath}")
        }
    } catch (e: Exception) {
        lastError = "externalDir: ${e.message}"
    }

    try {
        val cacheDir = ctx.cacheDir
        val cacheFile = File(cacheDir, "nuvio-backup.json")
        if (cacheFile.exists()) {
            val result = cacheFile.readText()
            android.util.Log.i("BackupManager", "cacheDir read OK: ${result.length} chars")
            return result
        }
        android.util.Log.i("BackupManager", "cacheFile does not exist at ${cacheFile.absolutePath}")
    } catch (e: Exception) {
        lastError = "cacheDir: ${e.message}"
    }

    android.util.Log.w("BackupManager", "All read attempts failed. Last error: $lastError. Path: $path")
    return null
}