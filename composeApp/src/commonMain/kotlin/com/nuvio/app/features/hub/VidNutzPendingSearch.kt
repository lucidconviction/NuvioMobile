package com.nuvio.app.features.hub

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** One-shot query consumed by VidNutz when the hub opens it from SportNutz. */
object VidNutzPendingSearch {
    var query: String? by mutableStateOf(null)
    var isFromSportNutz: Boolean by mutableStateOf(false)
}