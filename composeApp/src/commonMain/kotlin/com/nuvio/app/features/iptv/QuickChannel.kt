package com.nuvio.app.features.iptv

data class QuickChannel(
    val displayName: String,
    val aliases: List<String>,
    val regions: List<String>,
    val tags: List<String>
)
