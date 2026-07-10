package com.nuvio.app.features.player

import com.nuvio.app.features.iptv.EspnProcessedEvent

object SportsNowStore {
    var liveEvents: List<EspnProcessedEvent> = emptyList()
    var onSwitchToEvent: ((EspnProcessedEvent) -> Unit)? = null
}
