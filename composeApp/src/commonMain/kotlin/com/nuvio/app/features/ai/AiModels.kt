package com.nuvio.app.features.ai

import com.nuvio.app.features.sports.MatchedChannel

enum class AiRole { System, User, Assistant }

data class AiMessage(val role: AiRole, val content: String)

data class AiResponse(
    val text: String,
    val model: String? = null,
    val latencyMs: Long = 0L,
)

data class AiUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val conversation: List<AiMessage> = emptyList(),
)

data class AiChannelSearchResult(
    val query: String,
    val explanation: String,
    val filteredChannels: List<MatchedChannel> = emptyList(),
    val suggestedPortals: List<String> = emptyList(),
    val latencyMs: Long = 0L,
)

internal data class AiChatCompletionRequest(
    val model: String = "auto",
    val messages: List<Map<String, String>>,
    val temperature: Double = 0.7,
    val maxTokens: Int = 1024,
)

internal data class AiChatCompletionResponse(
    val choices: List<AiChoice>,
    val model: String? = null,
)

internal data class AiChoice(
    val message: AiMessageResponse,
)

internal data class AiMessageResponse(
    val role: String,
    val content: String,
)
