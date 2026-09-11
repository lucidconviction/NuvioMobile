package com.nuvio.app.features.ai

import com.nuvio.app.features.addons.httpPostJson
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object AiClient {
    const val DEFAULT_BASE_URL = "https://freellmapi.com/v1"

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun chatCompletion(
        messages: List<AiMessage>,
        systemPrompt: String = "",
        baseUrl: String = AiSettingsStore.getBaseUrl(),
    ): AiResponse {
        val allMessages = buildList {
            if (systemPrompt.isNotBlank()) {
                add(mapOf("role" to "system", "content" to systemPrompt))
            }
            addAll(messages.map { msg ->
                mapOf("role" to msg.role.name.lowercase(), "content" to msg.content)
            })
        }

        val request = AiChatCompletionRequest(messages = allMessages)
        val body = json.encodeToString(request)
        val start = System.currentTimeMillis()

        val raw = httpPostJson("$baseUrl/chat/completions", body)
        val latencyMs = System.currentTimeMillis() - start

        return try {
            val response = json.decodeFromString<AiChatCompletionResponse>(raw)
            val text = response.choices.firstOrNull()?.message?.content ?: ""
            AiResponse(text = text, model = response.model, latencyMs = latencyMs)
        } catch (e: Exception) {
            AiResponse(text = raw.take(500), latencyMs = latencyMs)
        }
    }

    suspend fun chatCompletionStreaming(
        messages: List<AiMessage>,
        systemPrompt: String = "",
        onChunk: (String) -> Unit,
        baseUrl: String = AiSettingsStore.getBaseUrl(),
    ): AiResponse {
        val allMessages = buildList {
            if (systemPrompt.isNotBlank()) {
                add(mapOf("role" to "system", "content" to systemPrompt))
            }
            addAll(messages.map { msg ->
                mapOf("role" to msg.role.name.lowercase(), "content" to msg.content)
            })
        }

        val request = AiChatCompletionRequest(messages = allMessages)
        val body = json.encodeToString(request)
        val start = System.currentTimeMillis()

        val raw = httpPostJson("$baseUrl/chat/completions", body)
        val latencyMs = System.currentTimeMillis() - start

        return try {
            val response = json.decodeFromString<AiChatCompletionResponse>(raw)
            val text = response.choices.firstOrNull()?.message?.content ?: ""
            if (text.isNotEmpty()) onChunk(text)
            AiResponse(text = text, model = response.model, latencyMs = latencyMs)
        } catch (e: Exception) {
            AiResponse(text = "", latencyMs = latencyMs)
        }
    }

    suspend fun testConnection(baseUrl: String): TestConnectionResult {
        val start = System.currentTimeMillis()
        return try {
            val body = json.encodeToString(AiChatCompletionRequest(messages = listOf(
                mapOf("role" to "user", "content" to "Reply with only: OK")
            )))
            val raw = httpPostJson("$baseUrl/chat/completions", body)
            val latencyMs = System.currentTimeMillis() - start
            val response = runCatching { json.decodeFromString<AiChatCompletionResponse>(raw) }
            val model = response.getOrNull()?.model ?: "unknown"
            TestConnectionResult(ok = true, latencyMs = latencyMs, model = model)
        } catch (e: Exception) {
            TestConnectionResult(ok = false, error = e.message ?: "Connection failed", latencyMs = System.currentTimeMillis() - start)
        }
    }
}

data class TestConnectionResult(
    val ok: Boolean,
    val latencyMs: Long = 0L,
    val model: String? = null,
    val error: String? = null,
)
