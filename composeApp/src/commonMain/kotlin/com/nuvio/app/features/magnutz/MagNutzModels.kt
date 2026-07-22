package com.nuvio.app.features.magnutz

import kotlinx.serialization.Serializable

@Serializable
data class MagNutzItem(
    val id: String,
    val magnetUri: String,
    val infoHash: String,
    val title: String,
    val fileIdx: Int? = null,
    val fileName: String? = null,
    val totalBytes: Long = 0,
    val downloadedBytes: Long = 0,
    val status: MagNutzStatus = MagNutzStatus.Queued,
    val createdAtEpochMs: Long,
    val completedAtEpochMs: Long? = null,
    val localFilePath: String? = null,
    val errorMessage: String? = null,
    val trackers: List<String> = emptyList(),
    val downloadSpeed: Long = 0,
    val uploadSpeed: Long = 0,
    val peers: Int = 0,
    val seeds: Int = 0,
    val ratio: Float = 0f,
) {
    val progressFraction: Float
        get() = if (totalBytes > 0) (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 1f) else 0f

    val isPlayable: Boolean
        get() = status == MagNutzStatus.Completed && localFilePath != null
}

@Serializable
enum class MagNutzStatus {
    Queued, Downloading, Paused, Seeding, Completed, Failed
}

enum class MagNutzFilter(val label: String) {
    All("All"),
    Downloading("Downloading"),
    Seeding("Seeding"),
    Completed("Completed"),
    Failed("Failed"),
}

data class MagNutzUiState(
    val items: List<MagNutzItem> = emptyList(),
    val filter: MagNutzFilter = MagNutzFilter.All,
    val searchQuery: String = "",
    val isAddingMagnet: Boolean = false,
    val magnetInputText: String = "",
    val error: String? = null,
)

object MagNutzMagnetParser {
    fun parseMagnet(magnetUri: String): MagNutzParsedMagnet? {
        val trimmed = magnetUri.trim()
        if (!trimmed.startsWith("magnet:", ignoreCase = true)) return null
        val infoHash = extractInfoHash(trimmed) ?: return null
        val dn = extractParam(trimmed, "dn")
        val trackers = extractAllParams(trimmed, "tr")
        return MagNutzParsedMagnet(trimmed, infoHash, dn ?: infoHash.take(8), trackers)
    }

    fun extractInfoHash(magnet: String): String? {
        val regex = Regex("btih:([a-fA-F0-9]{40})")
        return regex.find(magnet)?.groupValues?.getOrNull(1)
    }

    private fun extractParam(magnet: String, key: String): String? {
        val regex = Regex("${key}=([^&]+)")
        return regex.find(magnet)?.groupValues?.getOrNull(1)
    }

    private fun extractAllParams(magnet: String, key: String): List<String> {
        val regex = Regex("${key}=([^&]+)")
        return regex.findAll(magnet).map {
            it.groupValues[1]
        }.toList()
    }
}

data class MagNutzParsedMagnet(
    val uri: String,
    val infoHash: String,
    val title: String,
    val trackers: List<String> = emptyList(),
)
