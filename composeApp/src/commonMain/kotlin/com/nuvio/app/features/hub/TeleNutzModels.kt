package com.nuvio.app.features.hub

import kotlinx.serialization.Serializable

enum class TeleNutzTab { SEARCH, BOOKMARKS, DOWNLOADS }

@Serializable
data class TeleNutzVideo(
    val id: Long,
    val chatId: Long,
    val chatTitle: String,
    val text: String,
    val date: String,
    val thumbnailUrl: String?,
    val fileId: Int = 0,
    val localPath: String? = null,
    val fileSize: Long = 0L,
    val isDownloaded: Boolean = false,
    val isBookmarked: Boolean = false,
    val downloadProgress: Float = 0f,
)

data class TeleNutzUiState(
    val selectedTab: TeleNutzTab = TeleNutzTab.SEARCH,
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val searchResults: List<TeleNutzVideo> = emptyList(),
    val bookmarkedVideos: List<TeleNutzVideo> = emptyList(),
    val downloadedVideos: List<TeleNutzVideo> = emptyList(),
    val activeDownloads: Map<Int, Float> = emptyMap(),
    val error: String? = null,
    val authState: TelegramAuthState = TelegramAuthState.None,
    val authQrUrl: String? = null,
    val authError: String? = null,
    val phoneInput: String = "",
    val codeInput: String = "",
) {
    val needsAuth: Boolean
        get() = authState == TelegramAuthState.WaitPhoneNumber ||
                authState == TelegramAuthState.WaitQrCode ||
                authState == TelegramAuthState.WaitCode ||
                authState == TelegramAuthState.WaitPassword
}
