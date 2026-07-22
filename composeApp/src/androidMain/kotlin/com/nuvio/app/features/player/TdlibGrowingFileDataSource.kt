package com.nuvio.app.features.player

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import com.nuvio.app.features.hub.FileDownloadState
import com.nuvio.app.features.hub.TelegramTdEngine
import java.io.File
import java.io.RandomAccessFile

@UnstableApi
class TdlibGrowingFileDataSource(
    private val filePath: String,
    private val fileId: Int,
    private val engine: TelegramTdEngine,
    private val pollIntervalMs: Long = 500L,
    private val maxWaitMs: Long = 300_000L,
) : DataSource {
    private var file: RandomAccessFile? = null
    private var uri: Uri? = null
    private var currentPosition: Long = 0
    private var startTimeMs: Long = 0L

    override fun addTransferListener(transferListener: TransferListener) {}

    override fun open(dataSpec: DataSpec): Long {
        uri = dataSpec.uri
        startTimeMs = System.currentTimeMillis()
        val realFile = File(dataSpec.uri.path!!)
        file = RandomAccessFile(realFile, "r")
        file?.seek(dataSpec.position)
        currentPosition = dataSpec.position
        return C.LENGTH_UNSET.toLong()
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        val currentFile = file ?: return C.RESULT_END_OF_INPUT

        val bytesRead = currentFile.read(buffer, offset, length)
        if (bytesRead >= 0) {
            currentPosition += bytesRead
            return bytesRead
        }

        val elapsed = System.currentTimeMillis() - startTimeMs
        if (elapsed > maxWaitMs) {
            return C.RESULT_END_OF_INPUT
        }

        val state = getCurrentDownloadState()
        if (state == null || state.isComplete) {
            return C.RESULT_END_OF_INPUT
        }

        val currentLength = currentFile.length()
        if (currentPosition < currentLength) {
            currentFile.seek(currentPosition)
            val retryRead = currentFile.read(buffer, offset, length)
            if (retryRead >= 0) {
                currentPosition += retryRead
                return retryRead
            }
        }

        try {
            Thread.sleep(pollIntervalMs)
        } catch (_: InterruptedException) {
            return C.RESULT_END_OF_INPUT
        }

        val newLength = currentFile.length()
        if (newLength > currentPosition) {
            currentFile.seek(currentPosition)
            val retryRead = currentFile.read(buffer, offset, length)
            if (retryRead >= 0) {
                currentPosition += retryRead
                return retryRead
            }
        }

        return 0
    }

    override fun getUri(): Uri? = uri

    override fun close() {
        try {
            file?.close()
        } catch (_: Exception) {}
        file = null
    }

    private fun getCurrentDownloadState(): FileDownloadState? {
        return try {
            engine.peekFileDownloadState(fileId)
        } catch (_: Exception) {
            null
        }
    }
}

@UnstableApi
class TdlibAwareDataSourceFactory(
    private val upstreamFactory: DataSource.Factory,
    private val tdlibFileId: Int,
    private val tdlibEngine: TelegramTdEngine,
) : DataSource.Factory {
    override fun createDataSource(): DataSource {
        return TdlibAwareDataSource(
            upstream = upstreamFactory.createDataSource(),
            tdlibFileId = tdlibFileId,
            tdlibEngine = tdlibEngine,
        )
    }
}

@UnstableApi
class TdlibAwareDataSource(
    private val upstream: DataSource,
    private val tdlibFileId: Int,
    private val tdlibEngine: TelegramTdEngine,
) : DataSource {
    private var activeDataSource: DataSource? = null

    private fun resolveDataSource(dataSpec: DataSpec): DataSource {
        val scheme = dataSpec.uri.scheme.orEmpty()
        if (scheme == "tdlib") {
            val filePath = dataSpec.uri.path?.takeIf { it.isNotBlank() } ?: return upstream
            return TdlibGrowingFileDataSource(
                filePath = filePath,
                fileId = tdlibFileId,
                engine = tdlibEngine,
            )
        }
        return upstream
    }

    override fun addTransferListener(transferListener: TransferListener) {
        upstream.addTransferListener(transferListener)
    }

    override fun open(dataSpec: DataSpec): Long {
        val ds = resolveDataSource(dataSpec)
        activeDataSource = ds
        return ds.open(dataSpec)
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        val ds = activeDataSource ?: return upstream.read(buffer, offset, length)
        return ds.read(buffer, offset, length)
    }

    override fun getUri(): Uri? = activeDataSource?.uri ?: upstream.uri

    override fun close() {
        try { activeDataSource?.close() } catch (_: Exception) {}
        try { upstream.close() } catch (_: Exception) {}
    }
}
