package com.nuvio.app.features.addons

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.utils.io.readRemaining
import io.ktor.utils.io.readText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.isSuccess
import kotlinx.coroutines.runBlocking
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.network_empty_response_body
import nuvio.composeapp.generated.resources.network_request_failed_http
import org.jetbrains.compose.resources.getString
import platform.Foundation.NSUserDefaults

actual object AddonStorage {
    private const val addonUrlsKey = "installed_manifest_urls"
    private const val addonEnabledStatesKey = "installed_manifest_enabled_states"

    actual fun loadInstalledAddonUrls(profileId: Int): List<String> =
        NSUserDefaults.standardUserDefaults
            .stringForKey("${addonUrlsKey}_$profileId")
            .orEmpty()
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toList()

    actual fun saveInstalledAddonUrls(profileId: Int, urls: List<String>) {
        NSUserDefaults.standardUserDefaults.setObject(
            urls.joinToString(separator = "\n"),
            forKey = "${addonUrlsKey}_$profileId",
        )
    }

    actual fun loadAddonEnabledStates(profileId: Int): Map<String, Boolean> =
        NSUserDefaults.standardUserDefaults
            .stringForKey("${addonEnabledStatesKey}_$profileId")
            .orEmpty()
            .lineSequence()
            .mapNotNull(::parseEnabledStateLine)
            .toMap()

    actual fun saveAddonEnabledStates(profileId: Int, states: Map<String, Boolean>) {
        val payload = states.entries.joinToString(separator = "\n") { (url, enabled) ->
            "$url\t$enabled"
        }
        NSUserDefaults.standardUserDefaults.setObject(
            payload,
            forKey = "${addonEnabledStatesKey}_$profileId",
        )
    }

    actual fun hasSeededDefaultAddons(): Boolean =
        NSUserDefaults.standardUserDefaults.boolForKey("default_addons_seeded")

    actual fun markDefaultAddonsSeeded() {
        NSUserDefaults.standardUserDefaults.setObject(true, forKey = "default_addons_seeded")
    }
}

private fun parseEnabledStateLine(line: String): Pair<String, Boolean>? {
    val url = line.substringBefore("\t").trim().takeIf { it.isNotEmpty() } ?: return null
    val rawEnabled = line.substringAfter("\t", "true").trim().lowercase()
    val enabled = when (rawEnabled) {
        "false" -> false
        else -> true
    }
    return url to enabled
}

private val addonHttpClient = HttpClient(Darwin) {
    install(HttpTimeout) {
        requestTimeoutMillis = 60_000
        connectTimeoutMillis = 60_000
        socketTimeoutMillis = 60_000
    }
    expectSuccess = false
}

actual suspend fun httpGetText(url: String): String =
    addonHttpClient
        .get(url) {
            accept(ContentType.Application.Json)
        }
        .let { response ->
            val payload = response.bodyAsText()
            if (!response.status.isSuccess()) {
                error(runBlocking { getString(Res.string.network_request_failed_http, response.status.value) })
            }
            if (payload.isBlank()) {
                throw IllegalStateException(runBlocking { getString(Res.string.network_empty_response_body) })
            }
            payload
        }

actual suspend fun httpPostJson(url: String, body: String): String =
    addonHttpClient
        .post(url) {
            accept(ContentType.Application.Json)
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(body)
        }
        .let { response ->
            val payload = response.bodyAsText()
            if (!response.status.isSuccess()) {
                error(runBlocking { getString(Res.string.network_request_failed_http, response.status.value) })
            }
            if (payload.isBlank()) {
                throw IllegalStateException(runBlocking { getString(Res.string.network_empty_response_body) })
            }
            payload
        }

actual suspend fun httpGetTextWithHeaders(
    url: String,
    headers: Map<String, String>,
): String =
    addonHttpClient
        .get(url) {
            accept(ContentType.Application.Json)
            headers.forEach { (key, value) ->
                header(key, value)
            }
        }
        .let { response ->
            val payload = response.bodyAsText()
            if (!response.status.isSuccess()) {
                error(runBlocking { getString(Res.string.network_request_failed_http, response.status.value) })
            }
            if (payload.isBlank()) {
                throw IllegalStateException(runBlocking { getString(Res.string.network_empty_response_body) })
            }
            payload
        }

private suspend fun readResponseBodyLimited(
    response: HttpResponse,
    maxBytes: Int,
): String {
    val channel = response.bodyAsChannel()
    var total = 0
    var out = ""
    while (total < maxBytes && !channel.isClosedForRead) {
        val remaining = (maxBytes - total).coerceAtMost(8192)
        val packet = channel.readRemaining(remaining.toLong()) ?: break
        val text = packet.readText()
        if (text.isEmpty()) break
        out += text
        total += text.encodeToByteArray().size
    }
    channel.cancel(CancelledCause())
    return out
}

private class CancelledCause : Throwable()

actual suspend fun httpGetTextWithHeadersLimited(
    url: String,
    headers: Map<String, String>,
    maxBytes: Int,
): String =
    addonHttpClient
        .get(url) {
            accept(ContentType.Application.Json)
            headers.forEach { (key, value) ->
                header(key, value)
            }
        }
        .let { response ->
            val payload = readResponseBodyLimited(response, maxBytes)
            if (!response.status.isSuccess()) {
                error(runBlocking { getString(Res.string.network_request_failed_http, response.status.value) })
            }
            if (payload.isBlank()) {
                throw IllegalStateException(runBlocking { getString(Res.string.network_empty_response_body) })
            }
            payload
        }

actual suspend fun httpGetTextChunked(
    url: String,
    headers: Map<String, String>,
    onChunk: suspend (String) -> Boolean,
) {
    addonHttpClient.prepareGet(url) {
        headers.forEach { (key, value) -> header(key, value) }
    }.execute { response ->
        val channel = response.bodyAsChannel()
        while (!channel.isClosedForRead) {
            val packet = channel.readRemaining(8192)
            if (packet != null) {
                val text = packet.readText()
                if (!onChunk(text)) break
            }
        }
    }
}

actual suspend fun httpPostJsonWithHeaders(
    url: String,
    body: String,
    headers: Map<String, String>,
): String =
    addonHttpClient
        .post(url) {
            accept(ContentType.Application.Json)
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            headers.forEach { (key, value) ->
                header(key, value)
            }
            setBody(body)
        }
        .let { response ->
            val payload = response.bodyAsText()
            if (!response.status.isSuccess()) {
                error(runBlocking { getString(Res.string.network_request_failed_http, response.status.value) })
            }
            if (payload.isBlank()) {
                throw IllegalStateException(runBlocking { getString(Res.string.network_empty_response_body) })
            }
            payload
        }

actual suspend fun httpRequestRaw(
    method: String,
    url: String,
    headers: Map<String, String>,
    body: String,
    followRedirects: Boolean,
): RawHttpResponse =
    addonHttpClient
        .request {
            url(url)
            this.method = HttpMethod.parse(method.uppercase())
            headers.forEach { (key, value) ->
                header(key, value)
            }
            if (this.method == HttpMethod.Post || this.method == HttpMethod.Put || this.method == HttpMethod.Patch) {
                setBody(body)
            }
        }
        .let { response ->
            RawHttpResponse(
                status = response.status.value,
                statusText = response.status.description,
                url = response.call.request.url.toString(),
                body = response.bodyAsText(),
                headers = response.headers.entries().associate { (name, values) ->
                    name.lowercase() to values.joinToString(",")
                },
            )
        }
