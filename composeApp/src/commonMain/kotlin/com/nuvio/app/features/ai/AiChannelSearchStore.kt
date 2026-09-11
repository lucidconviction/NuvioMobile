package com.nuvio.app.features.ai

import com.nuvio.app.features.addons.httpPostJson
import com.nuvio.app.features.sports.BroadcastRegion
import com.nuvio.app.features.iptv.EspnProcessedEvent
import com.nuvio.app.features.iptv.IptvChannel
import com.nuvio.app.features.sports.MatchedChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

internal expect object AiChannelSearchStore {
    fun searchChannels(
        event: EspnProcessedEvent,
        allChannels: List<IptvChannel>,
        matchedChannels: List<MatchedChannel>,
        query: String,
        region: BroadcastRegion,
    ): Flow<AiChannelSearchResult>
}
