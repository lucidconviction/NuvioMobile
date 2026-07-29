package com.nuvio.app.features.hub

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val TAG = "TelegramTdEngine"

class TelegramApiException(message: String) : Exception(message)

actual class TelegramTdEngine {
    private var client: Client? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _authInfo = MutableStateFlow(TelegramAuthInfo())
    val authInfo = _authInfo.asStateFlow()
    private var ready = false
    private lateinit var appCtx: android.content.Context

    private val downloadListeners = mutableMapOf<Int, (TdApi.File) -> Unit>()
    private val fileDownloadStates = ConcurrentHashMap<Int, FileDownloadState>()

    actual suspend fun start() {
        appCtx = com.nuvio.app.appContext ?: run {
            _authInfo.value = TelegramAuthInfo(error = "App context not available")
            return
        }
        val sessionMarker = File(appCtx.filesDir, "tdlib_session_ok")
        if (!sessionMarker.exists()) {
            File(appCtx.filesDir, "tdlib_db").deleteRecursively()
            File(appCtx.filesDir, "tdlib_files").deleteRecursively()
        }
        client = Client.create(
            { update -> onUpdate(update) },
            { e -> e.printStackTrace() },
            { e -> e.printStackTrace() },
        )
        // sendTdlibParameters is called by onUpdate() when AuthorizationStateWaitTdlibParameters is received
    }

    actual suspend fun close() {
        client?.send(TdApi.Close(), null)
        ready = false
        _authInfo.value = TelegramAuthInfo(state = TelegramAuthState.Closed)
    }

    actual fun getAuthInfo(): TelegramAuthInfo = _authInfo.value

    actual suspend fun setPhoneNumber(phone: String) {
        client?.send(TdApi.SetAuthenticationPhoneNumber(phone, null), null)
    }

    actual suspend fun checkAuthCode(code: String) {
        client?.send(TdApi.CheckAuthenticationCode(code), null)
    }

    actual suspend fun checkPassword(password: String) {
        client?.send(TdApi.CheckAuthenticationPassword(password), null)
    }

    actual suspend fun requestQrCode() {
        client?.send(TdApi.RequestQrCodeAuthentication(LongArray(0)), null)
    }

    actual suspend fun searchVideoMessages(query: String, limit: Int): List<TdMessage> {
        if (!ready) throw TelegramApiException("Engine not ready")
        return withTimeoutOrNull(30000L) {
            suspendCancellableCoroutine { cont ->
                val filter = TdApi.SearchMessagesFilterVideo()
                val req = TdApi.SearchMessages()
                req.chatList = null
                req.query = query
                req.offset = ""
                req.limit = limit.coerceAtMost(100)
                req.filter = filter

                client?.send(req, Client.ResultHandler { obj ->
                    if (obj is TdApi.Error) {
                        cont.resumeWithException(TelegramApiException(obj.message))
                        return@ResultHandler
                    }
                    val found = (obj as TdApi.FoundMessages).messages ?: emptyArray()
                    val chatIds = found.map { it.chatId }.distinct()
                    if (chatIds.isEmpty()) {
                        cont.resume(emptyList())
                        return@ResultHandler
                    }
                    val titles = mutableMapOf<Long, String>()
                    var remaining = chatIds.size
                    for (cid in chatIds) {
                        client?.send(TdApi.GetChat(cid), Client.ResultHandler { chatObj ->
                            if (chatObj !is TdApi.Error && chatObj is TdApi.Chat) {
                                titles[cid] = chatObj.title
                            }
                            remaining--
                            if (remaining == 0) {
                                val msgs = found.mapNotNull { msg ->
                                    toTdMessage(msg, titles[msg.chatId] ?: "")
                                }
                                cont.resume(msgs)
                            }
                        })
                    }
                })
            }
        } ?: throw TelegramApiException("Search timed out")
    }

    actual suspend fun downloadVideoFile(fileId: Int, onProgress: ((Float) -> Unit)?): String? {
        if (!ready) throw TelegramApiException("Engine not ready")
        return withTimeoutOrNull(300000L) {
            suspendCancellableCoroutine { cont ->
                val listener: (TdApi.File) -> Unit = { f ->
                    if (f.id == fileId) {
                        val localFile = f.local
                        if (localFile != null) {
                            val downloaded = localFile.downloadedSize.toFloat()
                            val total = if (f.expectedSize > 0L) f.expectedSize.toFloat() else f.size.toFloat()
                            if (total > 0f) {
                                onProgress?.invoke((downloaded / total).coerceIn(0f, 1f))
                            }
                            if (localFile.isDownloadingCompleted) {
                                synchronized(downloadListeners) { downloadListeners.remove(fileId) }
                                if (cont.isActive) cont.resume(localFile.path)
                            }
                        }
                    }
                }
                synchronized(downloadListeners) { downloadListeners[fileId] = listener }

                client?.send(TdApi.DownloadFile(fileId, 1, 0L, 0L, false), Client.ResultHandler { obj ->
                    if (obj is TdApi.Error) {
                        synchronized(downloadListeners) { downloadListeners.remove(fileId) }
                        if (cont.isActive) cont.resumeWithException(TelegramApiException(obj.message))
                    } else if (obj is TdApi.File && obj.local?.isDownloadingCompleted == true) {
                        synchronized(downloadListeners) { downloadListeners.remove(fileId) }
                        if (cont.isActive) cont.resume(obj.local.path)
                    }
                })

                cont.invokeOnCancellation {
                    cancelDownloadInternal(fileId)
                }
            }
        }
    }

    actual suspend fun startProgressiveDownload(fileId: Int): String? {
        if (!ready) throw TelegramApiException("Engine not ready")
        val existing = fileDownloadStates[fileId]
        if (existing != null && existing.path.isNotBlank()) return existing.path

        return suspendCancellableCoroutine { cont ->
            val listener: (TdApi.File) -> Unit = { f ->
                if (f.id == fileId && f.local?.path?.isNotBlank() == true) {
                    val localFile = f.local!!
                    val total = if (f.expectedSize > 0L) f.expectedSize else f.size
                    fileDownloadStates[fileId] = FileDownloadState(
                        path = localFile.path,
                        totalSize = total,
                        downloadedSize = localFile.downloadedSize,
                        isComplete = localFile.isDownloadingCompleted,
                    )
                    if (cont.isActive) cont.resume(localFile.path)
                }
            }
            synchronized(downloadListeners) { downloadListeners[fileId] = listener }

            client?.send(TdApi.DownloadFile(fileId, 1, 0L, 0L, false), Client.ResultHandler { obj ->
                if (obj is TdApi.Error) {
                    synchronized(downloadListeners) { downloadListeners.remove(fileId) }
                    if (cont.isActive) cont.resumeWithException(TelegramApiException(obj.message))
                } else if (obj is TdApi.File) {
                    val localFile = obj.local
                    if (localFile != null && !localFile.path.isNullOrBlank()) {
                        val total = if (obj.expectedSize > 0L) obj.expectedSize else obj.size
                        fileDownloadStates[fileId] = FileDownloadState(
                            path = localFile.path,
                            totalSize = total,
                            downloadedSize = localFile.downloadedSize,
                            isComplete = localFile.isDownloadingCompleted,
                        )
                        if (cont.isActive) cont.resume(localFile.path)
                    }
                }
            })

            cont.invokeOnCancellation {
                synchronized(downloadListeners) {
                    if (downloadListeners.containsKey(fileId)) {
                        downloadListeners.remove(fileId)
                    }
                }
            }
        }
    }

    actual suspend fun getFileDownloadState(fileId: Int): FileDownloadState? {
        val cached = fileDownloadStates[fileId]
        if (cached != null) return cached

        return suspendCancellableCoroutine { cont ->
            client?.send(TdApi.GetFile(fileId), Client.ResultHandler { obj ->
                if (obj is TdApi.File) {
                    val localFile = obj.local
                    if (localFile != null) {
                        val total = if (obj.expectedSize > 0L) obj.expectedSize else obj.size
                        val state = FileDownloadState(
                            path = localFile.path.orEmpty(),
                            totalSize = total,
                            downloadedSize = localFile.downloadedSize,
                            isComplete = localFile.isDownloadingCompleted,
                        )
                        fileDownloadStates[fileId] = state
                        if (cont.isActive) cont.resume(state)
                    } else {
                        if (cont.isActive) cont.resume(null)
                    }
                } else {
                    if (cont.isActive) cont.resume(null)
                }
            })
        }
    }

    actual fun peekFileDownloadState(fileId: Int): FileDownloadState? {
        return fileDownloadStates[fileId]
    }

    actual suspend fun cancelDownload(fileId: Int) {
        cancelDownloadInternal(fileId)
    }

    private fun cancelDownloadInternal(fileId: Int) {
        synchronized(downloadListeners) { downloadListeners.remove(fileId) }
        fileDownloadStates.remove(fileId)
        client?.send(TdApi.CancelDownloadFile(fileId, false), null)
    }

    actual suspend fun deleteVideoFile(filePath: String): Boolean {
        return try {
            val f = File(filePath)
            if (f.exists()) f.delete() else true
        } catch (_: Exception) {
            false
        }
    }

    private fun toTdMessage(msg: TdApi.Message, chatTitle: String): TdMessage? {
        val content = msg.content ?: return null
        if (content !is TdApi.MessageVideo) return null
        val video = content.video ?: return null
        val videoFile = video.video
        val captionText = content.caption?.text.orEmpty()
        val rawThumbPath = video.thumbnail?.file?.local?.path.orEmpty()
        val thumbUrl = if (rawThumbPath.isNotBlank()) "file://$rawThumbPath" else null
        val isCompleted = videoFile?.local?.isDownloadingCompleted == true
        val rawLocalPath = videoFile?.local?.path.orEmpty()
        val localPath = if (isCompleted && rawLocalPath.isNotBlank()) rawLocalPath else null
        val expected = videoFile?.expectedSize ?: 0L
        val actualSize = videoFile?.size ?: 0L
        val size = if (expected > 0L) expected else actualSize

        return TdMessage(
            id = msg.id,
            chatId = msg.chatId,
            chatUsername = "c/${msg.chatId}",
            chatTitle = chatTitle,
            text = captionText,
            hasVideo = true,
            thumbnailUrl = thumbUrl,
            date = msg.date.toString(),
            fileId = videoFile?.id ?: 0,
            localPath = localPath,
            fileSize = size,
        )
    }

    private fun sendTdlibParameters() {
        val dbDir = File(appCtx.filesDir, "tdlib_db").absolutePath
        val filesDir = File(appCtx.filesDir, "tdlib_files").absolutePath
        val p = TdApi.SetTdlibParameters()
        p.apiId = TelegramConfig.API_ID
        p.apiHash = TelegramConfig.API_HASH
        p.databaseDirectory = dbDir
        p.filesDirectory = filesDir
        p.useMessageDatabase = false
        p.useSecretChats = false
        p.systemLanguageCode = "en"
        p.deviceModel = "Android"
        p.applicationVersion = "1.0.0"
        Log.d(TAG, "Sending TdlibParameters: apiId=${TelegramConfig.API_ID}, dbDir=$dbDir")
        client?.send(p, Client.ResultHandler { obj ->
            if (obj is TdApi.Error) {
                Log.e(TAG, "setTdlibParameters failed: ${obj.message}")
            } else {
                Log.d(TAG, "setTdlibParameters succeeded")
            }
        })
    }

    private fun onUpdate(obj: TdApi.Object) {
        when (obj) {
            is TdApi.UpdateFile -> {
                val file = obj.file ?: return
                val localFile = file.local
                if (localFile != null && !localFile.path.isNullOrBlank()) {
                    val total = if (file.expectedSize > 0L) file.expectedSize else file.size
                    fileDownloadStates[file.id] = FileDownloadState(
                        path = localFile.path,
                        totalSize = total,
                        downloadedSize = localFile.downloadedSize,
                        isComplete = localFile.isDownloadingCompleted,
                    )
                }
                val listener = synchronized(downloadListeners) { downloadListeners[file.id] }
                listener?.invoke(file)
            }
            is TdApi.UpdateAuthorizationState -> {
                when (val s = obj.authorizationState) {
                    is TdApi.AuthorizationStateWaitTdlibParameters -> sendTdlibParameters()
                    is TdApi.AuthorizationStateWaitPhoneNumber ->
                        _authInfo.value = TelegramAuthInfo(state = TelegramAuthState.WaitPhoneNumber)
                    is TdApi.AuthorizationStateWaitCode ->
                        _authInfo.value = TelegramAuthInfo(state = TelegramAuthState.WaitCode)
                    is TdApi.AuthorizationStateWaitOtherDeviceConfirmation ->
                        _authInfo.value = TelegramAuthInfo(state = TelegramAuthState.WaitQrCode, qrCodeUrl = s.link)
                    is TdApi.AuthorizationStateWaitPassword ->
                        _authInfo.value = TelegramAuthInfo(state = TelegramAuthState.WaitPassword)
                    is TdApi.AuthorizationStateReady -> {
                        ready = true
                        File(appCtx.filesDir, "tdlib_session_ok").createNewFile()
                        _authInfo.value = TelegramAuthInfo(state = TelegramAuthState.Ready)
                    }
                    is TdApi.AuthorizationStateClosing -> {}
                    is TdApi.AuthorizationStateClosed -> {
                        ready = false
                        File(appCtx.filesDir, "tdlib_session_ok").delete()
                        _authInfo.value = TelegramAuthInfo(state = TelegramAuthState.Closed)
                    }
                }
            }
            is TdApi.Error -> {
                val cur = _authInfo.value
                if (cur.state in listOf(
                        TelegramAuthState.None, TelegramAuthState.WaitPhoneNumber,
                        TelegramAuthState.WaitQrCode, TelegramAuthState.WaitCode,
                        TelegramAuthState.WaitPassword
                    )) {
                    _authInfo.value = cur.copy(error = obj.message)
                }
            }
        }
    }
}
