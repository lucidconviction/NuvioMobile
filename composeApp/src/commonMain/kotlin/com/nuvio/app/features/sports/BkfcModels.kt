package com.nuvio.app.features.sports

data class BkfcEvent(
    val title: String,
    val date: String,
    val time: String,
    val location: String,
    val mainEvent: String,
    val fighter1: String = "",
    val fighter2: String = "",
    val slug: String = "",
)
