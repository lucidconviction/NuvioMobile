package com.nuvio.app.features.ai

import com.nuvio.app.features.sports.BroadcastRegion
import com.nuvio.app.features.iptv.EspnProcessedEvent
import com.nuvio.app.features.iptv.IptvChannel
import com.nuvio.app.features.sports.MatchedChannel
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

@OptIn(ExperimentalForeignApi::class)
actual object AiChannelSearchStore {
    actual fun searchChannels(
        event: EspnProcessedEvent,
        allChannels: List<IptvChannel>,
        matchedChannels: List<MatchedChannel>,
        query: String,
        region: BroadcastRegion,
    ): Flow<AiChannelSearchResult> = flow {
        // iOS: delegates to native AI framework or returns empty result
        // Full implementation requires KMP NativeAI integration
        emit(AiChannelSearchResult(
            query = query,
            explanation = "AI channel search is not yet available on iOS. Use the SportNutz channel list directly.",
            filteredChannels = matchedChannels,
            suggestedPortals = emptyList(),
            latencyMs = 0L,
        ))
    }
}
