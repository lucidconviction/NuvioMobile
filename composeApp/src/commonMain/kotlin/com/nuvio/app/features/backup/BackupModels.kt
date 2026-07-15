package com.nuvio.app.features.backup

import kotlinx.serialization.Serializable

@Serializable
data class BackupData(
    val version: Int = 1,
    val createdAt: String = "",
    val appVersion: String = "",
    val sections: Map<String, String> = emptyMap(),
)
