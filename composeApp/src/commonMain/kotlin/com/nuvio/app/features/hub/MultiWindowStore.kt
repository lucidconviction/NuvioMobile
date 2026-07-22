package com.nuvio.app.features.hub

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import com.nuvio.app.features.iptv.IptvChannel
import com.nuvio.app.features.iptv.SourceType

data class WindowStream(
    val id: String,
    val channel: IptvChannel,
    val slotIndex: Int,
    val isPlaying: Boolean = false,
    /** Generic stream URL for non-IPTV content (movies, shows, sports, etc.) */
    val playerUrl: String? = null,
    /** Display title for generic streams */
    val playerTitle: String? = null,
    /** Poster/channel logo URL for generic streams */
    val playerPoster: String? = null,
)

object MultiWindowStore {
    private const val MAX_SLOTS = 9
    private val streams = mutableStateListOf<WindowStream>()
    private var idCounter = 0L
    private val volumes = mutableStateMapOf<String, Float>()
    private val paused = mutableStateMapOf<String, Boolean>()
    private var _audioFocusId: String? = null
    private val playerHandleIds = mutableMapOf<String, Int>()
    private val resizeModes = mutableStateMapOf<String, Int>()
    private val _currentLayout = mutableStateOf<MultiWindowLayout?>(null)
    private val _layoutLocked = mutableStateOf(false)

    val allStreams: List<WindowStream> get() = streams

    fun addToSlot(channel: IptvChannel, slotIndex: Int) {
        // Replace in-place to avoid list size changes that trigger layout recalculation
        val existingAtSlot = streams.indexOfFirst { it.slotIndex == slotIndex }
        val existingChannel = streams.indexOfFirst { it.channel.id == channel.id && it.channel.sourceId == channel.sourceId }
        // Remove duplicate channel entry if exists at a different slot
        if (existingChannel >= 0 && existingChannel != existingAtSlot) {
            streams.removeAt(existingChannel)
            // Adjust index if removal shifted our target
            val adjustedExistingAtSlot = streams.indexOfFirst { it.slotIndex == slotIndex }
            if (adjustedExistingAtSlot >= 0) {
                streams[adjustedExistingAtSlot] = WindowStream(
                    id = streams[adjustedExistingAtSlot].id,
                    channel = channel,
                    slotIndex = slotIndex,
                )
            } else {
                streams.add(WindowStream(id = "mw_${++idCounter}", channel = channel, slotIndex = slotIndex))
            }
        } else if (existingAtSlot >= 0) {
            // Replace existing at slot in-place — preserves player key
            streams[existingAtSlot] = WindowStream(
                id = streams[existingAtSlot].id,
                channel = channel,
                slotIndex = slotIndex,
            )
        } else {
            streams.add(WindowStream(id = "mw_${++idCounter}", channel = channel, slotIndex = slotIndex))
        }
        // Stay auto by default — don't lock layout when adding streams
        if (_currentLayout.value == null) {
            _layoutLocked.value = false
        }
    }

    /** Add any video stream (movie, show, sports, etc.) to a slot. */
    fun addStream(url: String, title: String, poster: String? = null, slotIndex: Int) {
        val dummyChannel = IptvChannel(
            id = "generic_${idCounter}",
            name = title,
            url = url,
            logo = poster,
            group = title.take(30),
            sourceType = SourceType.M3U,
            sourceId = "multiview",
        )
        // Replace in-place to avoid list size changes
        val existingAtSlot = streams.indexOfFirst { it.slotIndex == slotIndex }
        if (existingAtSlot >= 0) {
            streams[existingAtSlot] = WindowStream(
                id = streams[existingAtSlot].id,
                channel = dummyChannel,
                slotIndex = slotIndex,
                playerUrl = url,
                playerTitle = title,
                playerPoster = poster,
            )
        } else {
            streams.add(WindowStream(
                id = "mw_${++idCounter}",
                channel = dummyChannel,
                slotIndex = slotIndex,
                playerUrl = url,
                playerTitle = title,
                playerPoster = poster,
            ))
        }
        // Stay auto by default — don't lock layout when adding streams
        if (_currentLayout.value == null) {
            _layoutLocked.value = false
        }
    }

    fun removeSlot(slotIndex: Int) { streams.removeAll { it.slotIndex == slotIndex } }

    fun remove(id: String) {
        streams.removeAll { it.id == id }; volumes.remove(id); paused.remove(id); playerHandleIds.remove(id)
        if (_audioFocusId == id) _audioFocusId = null
    }

    fun getStreamsForSlot(slotIndex: Int): WindowStream? = streams.find { it.slotIndex == slotIndex }
    fun getOccupiedSlots(): List<Int> = streams.map { it.slotIndex }
    fun isSlotAvailable(slotIndex: Int): Boolean = streams.none { it.slotIndex == slotIndex }
    fun setVolume(streamId: String, volume: Float) { volumes[streamId] = volume.coerceIn(0f, 1f) }
    fun getVolume(streamId: String): Float = volumes[streamId] ?: 0f
    fun setPaused(streamId: String, isPaused: Boolean) { paused[streamId] = isPaused }
    fun isPaused(streamId: String): Boolean = paused[streamId] ?: false
    fun setAudioFocus(streamId: String) { _audioFocusId = streamId }
    fun isAudioFocused(streamId: String): Boolean = _audioFocusId == streamId
    fun storePlayerHandle(streamId: String, handleId: Int) { playerHandleIds[streamId] = handleId }
    fun getPlayerHandleId(streamId: String): Int? = playerHandleIds[streamId]
    fun setResizeMode(streamId: String, mode: Int) { resizeModes[streamId] = mode }
    fun getResizeMode(streamId: String): Int = resizeModes[streamId] ?: RESIZE_FIT
    fun currentLayout(): MultiWindowLayout? = _currentLayout.value
    fun isLayoutLocked(): Boolean = _layoutLocked.value
    fun setLayout(layout: MultiWindowLayout) { _currentLayout.value = layout; _layoutLocked.value = true }
    fun setAutoLayout() { _currentLayout.value = null; _layoutLocked.value = false }
    fun resolveLayout(count: Int, isPortrait: Boolean, isTablet: Boolean = false): MultiWindowLayout = _currentLayout.value ?: defaultLayout(count, isPortrait, isTablet)

    fun swapSlots(slotA: Int, slotB: Int) {
        val idxA = streams.indexOfFirst { it.slotIndex == slotA }
        val idxB = streams.indexOfFirst { it.slotIndex == slotB }
        if (idxA < 0 || idxB < 0) return
        val a = streams[idxA]
        val b = streams[idxB]
        // Swap both list positions and slotIndex values so the grid (which renders by list order) visually updates
        streams[idxA] = b.copy(slotIndex = slotA)
        streams[idxB] = a.copy(slotIndex = slotB)
    }

    fun clear() {
        streams.clear(); volumes.clear(); paused.clear(); playerHandleIds.clear(); resizeModes.clear()
        _audioFocusId = null; _currentLayout.value = null; _layoutLocked.value = false
    }
}
