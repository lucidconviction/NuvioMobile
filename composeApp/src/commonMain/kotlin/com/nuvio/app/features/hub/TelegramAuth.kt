package com.nuvio.app.features.hub

enum class TelegramAuthState {
    None,
    WaitPhoneNumber,
    WaitQrCode,
    WaitCode,
    WaitPassword,
    Ready,
    Closed,
}

data class TelegramAuthInfo(
    val state: TelegramAuthState = TelegramAuthState.None,
    val qrCodeUrl: String? = null,
    val error: String? = null,
)

data class TdChannel(
    val id: Long,
    val username: String,
    val title: String,
    val description: String,
)

data class FileDownloadState(
    val path: String,
    val totalSize: Long = 0L,
    val downloadedSize: Long = 0L,
    val isComplete: Boolean = false,
)

data class TdMessage(
    val id: Long,
    val chatId: Long,
    val chatUsername: String,
    val chatTitle: String,
    val text: String,
    val hasVideo: Boolean,
    val thumbnailUrl: String?,
    val date: String,
    val fileId: Int = 0,
    val localPath: String? = null,
    val fileSize: Long = 0L,
)

expect class TelegramTdEngine() {
    suspend fun start()
    suspend fun close()
    fun getAuthInfo(): TelegramAuthInfo
    suspend fun setPhoneNumber(phone: String)
    suspend fun checkAuthCode(code: String)
    suspend fun checkPassword(password: String)
    suspend fun requestQrCode()
    suspend fun searchVideoMessages(query: String, limit: Int = 30): List<TdMessage>
    suspend fun downloadVideoFile(fileId: Int, onProgress: ((Float) -> Unit)? = null): String?
    suspend fun startProgressiveDownload(fileId: Int): String?
    suspend fun getFileDownloadState(fileId: Int): FileDownloadState?
    fun peekFileDownloadState(fileId: Int): FileDownloadState?
    suspend fun cancelDownload(fileId: Int)
    suspend fun deleteVideoFile(filePath: String): Boolean
}

