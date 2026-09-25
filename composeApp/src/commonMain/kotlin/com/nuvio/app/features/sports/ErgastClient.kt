package com.nuvio.app.features.sports

import com.nuvio.app.features.addons.httpGetTextWithHeaders
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull

data class ErgastRace(
    val season: Int,
    val round: Int,
    val name: String,
    val circuit: String,
    val date: String,
    val position: Int? = null,
    val driver: String? = null,
)

object ErgastClient {

    private val baseUrl = "https://ergast.com/mrd"
    private val json = Json { ignoreUnknownKeys = true }

    private suspend fun fetchArray(path: String): JsonArray? {
        return try {
            withTimeout(15_000) {
                val body = httpGetTextWithHeaders(
                    "$baseUrl$path",
                    mapOf("User-Agent" to "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36"),
                )
                Json.parseToJsonElement(body).jsonArray
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun parseRaces(arr: JsonArray?, season: Int): List<ErgastRace> {
        if (arr == null) return emptyList()
        val result = mutableListOf<ErgastRace>()
        try {
            for (el in arr) {
                val obj = el.jsonObject
                val round = obj["round"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0
                val name = obj["raceName"]?.jsonPrimitive?.contentOrNull
                    ?: obj["name"]?.jsonPrimitive?.contentOrNull ?: ""
                val circuit = obj["Circuit"]?.jsonObject?.get("circuitName")?.jsonPrimitive?.contentOrNull
                    ?: obj["circuit"]?.jsonPrimitive?.contentOrNull ?: ""
                val date = obj["date"]?.jsonPrimitive?.contentOrNull ?: ""
                result.add(ErgastRace(season = season, round = round, name = name, circuit = circuit, date = date))
            }
        } catch (_: Exception) {}
        return result
    }

    suspend fun fetchResults(season: Int): List<ErgastRace> {
        val arr = fetchArray("/api/results/$season") ?: return emptyList()
        return parseRaces(arr, season)
    }

    suspend fun fetchSprintResults(season: Int): List<ErgastRace> {
        val arr = fetchArray("/api/sprint/$season") ?: return emptyList()
        return parseRaces(arr, season)
    }

    suspend fun fetchStandings(season: Int): List<ErgastRace> {
        val arr = fetchArray("/api/standings/$season") ?: return emptyList()
        if (arr == null) return emptyList()
        val result = mutableListOf<ErgastRace>()
        try {
            for (el in arr) {
                val obj = el.jsonObject
                val round = obj["round"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0
                val driver = obj["Driver"]?.jsonObject?.get("familyName")?.jsonPrimitive?.contentOrNull
                    ?: obj["driver"]?.jsonPrimitive?.contentOrNull
                val position = obj["position"]?.jsonPrimitive?.contentOrNull?.toIntOrNull()
                result.add(
                    ErgastRace(
                        season = season,
                        round = round,
                        name = obj["raceName"]?.jsonPrimitive?.contentOrNull ?: "",
                        circuit = obj["Circuit"]?.jsonObject?.get("circuitName")?.jsonPrimitive?.contentOrNull
                            ?: obj["circuit"]?.jsonPrimitive?.contentOrNull ?: "",
                        date = obj["date"]?.jsonPrimitive?.contentOrNull ?: "",
                        position = position,
                        driver = driver,
                    )
                )
            }
        } catch (_: Exception) {}
        return result
    }

    suspend fun fetchRaces(season: Int): List<ErgastRace> {
        val arr = fetchArray("/api/races/$season") ?: return emptyList()
        return parseRaces(arr, season)
    }
}