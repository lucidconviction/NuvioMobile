package com.nuvio.app.features.ai

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong

object AiStore {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _uiState = MutableStateFlow(AiUiState())
    val uiState: Flow<AiUiState> = _uiState.asStateFlow()

    private val SYSTEM_PROMPT = """
        You are Nuvio's AI assistant. You help users discover sports content, IPTV channels,
        movies, music, and podcasts. Be concise, use bullet points for lists, and reference
        the user's region-aware channel preferences when relevant. When answering about sports
        channels, suggest specific channel names and explain why they match the user's query.
    """.trimIndent()

    val conversationHistory: MutableList<AiMessage> = mutableListOf(
        AiMessage(AiRole.System, SYSTEM_PROMPT),
    )

    private val lastRequestTime = AtomicLong(0L)
    private const val RATE_LIMIT_MS = 1000L

    fun sendUserMessage(text: String): Flow<String> = flow {
        throttle()
        conversationHistory.add(AiMessage(AiRole.User, text))
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        try {
            val response = AiClient.chatCompletion(
                messages = conversationHistory.toList(),
                systemPrompt = "",
            )
            if (response.text.isNotEmpty()) {
                conversationHistory.add(AiMessage(AiRole.Assistant, response.text))
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = null,
                    conversation = conversationHistory
                        .filter { it.role != AiRole.System }
                        .toList(),
                )
                emit(response.text)
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "AI returned an empty response. Try again.",
                )
                conversationHistory.removeLastOrNull()
            }
        } catch (e: Exception) {
            conversationHistory.removeLastOrNull()
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = e.message ?: "AI request failed",
            )
            emit("")
        }
    }

    fun clearConversation() {
        conversationHistory.clear()
        conversationHistory.add(AiMessage(AiRole.System, SYSTEM_PROMPT))
        _uiState.value = _uiState.value.copy(conversation = emptyList())
    }

    fun getConversation(): List<AiMessage> = conversationHistory.filter { it.role != AiRole.System }.toList()

    private suspend fun throttle() {
        val now = System.currentTimeMillis()
        val wait = RATE_LIMIT_MS - (now - lastRequestTime.get())
        if (wait > 0) kotlinx.coroutines.delay(wait)
        lastRequestTime.set(System.currentTimeMillis())
    }
}
