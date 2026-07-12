package com.nuvio.app.features.hub

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object HubReturnStore {
    var subScreen: String by mutableStateOf("Hub")
}
