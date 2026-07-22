package com.nuvio.app.features.player

import com.nuvio.app.features.iptv.EspnProcessedEvent
import com.nuvio.app.features.sports.DaddyLiveEvent

object SportsNowStore {
    var liveEvents: List<EspnProcessedEvent> = emptyList()
    var onSwitchToEvent: ((EspnProcessedEvent) -> Unit)? = null
    var daddyLiveEvents: List<DaddyLiveEvent> = emptyList()
}
