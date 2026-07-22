package com.nuvio.app.features.iptv

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
actual fun rememberFilePickerLauncher(onContent: (String, String) -> Unit): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    val fileName: String
                    val cursor = context.contentResolver.query(uri, null, null, null, null)
                    if (cursor != null) {
                        try {
                            val nameIdx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                            fileName = if (nameIdx >= 0 && cursor.moveToFirst()) cursor.getString(nameIdx) ?: "uploaded_playlist.m3u" else "uploaded_playlist.m3u"
                        } finally {
                            cursor.close()
                        }
                    } else {
                        fileName = "uploaded_playlist.m3u"
                    }

                    val content = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: ""
                    }
                    if (content.isNotBlank()) {
                        onContent(fileName, content)
                    }
                } catch (_: Exception) {}
            }
        }
    }

    return { launcher.launch(arrayOf("text/plain", "*/*", "audio/x-mpegurl", "application/vnd.apple.mpegurl")) }
}
