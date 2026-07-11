package com.nuvio.app.features.hub

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import com.nuvio.app.features.iptv.IptvChannel

data class WindowStream(
    val id: String,
    val channel: IptvChannel,
    val slotIndex: Int,
    val isPlaying: Boolean = false,
)

object MultiWindowStore {
    private const val MAX_SLOTS = 9
    private val streams = mutableStateListOf<WindowStream>()
    private var idCounter = 0L
    private val volumes = mutableStateMapOf<String, Float>()
    private var _audioFocusId: String? = null
    private val playerHandleIds = mutableMapOf<String, Int>()
    private val resizeModes = mutableStateMapOf<String, Int>()
    private val _currentLayout = mutableStateOf<MultiWindowLayout?>(null)
    private val _layoutLocked = mutableStateOf(false)

    val allStreams: List<WindowStream> get() = streams

    fun addToSlot(channel: IptvChannel, slotIndex: Int) {
        removeSlot(slotIndex)
        val existing = streams.indexOfFirst { it.channel.id == channel.id && it.channel.sourceId == channel.sourceId }
        if (existing >= 0) streams.removeAt(existing)
        streams.add(WindowStream(id = "mw_${++idCounter}", channel = channel, slotIndex = slotIndex))
    }

    fun removeSlot(slotIndex: Int) { streams.removeAll { it.slotIndex == slotIndex } }

    fun remove(id: String) {
        streams.removeAll { it.id == id }; volumes.remove(id); playerHandleIds.remove(id)
        if (_audioFocusId == id) _audioFocusId = null
    }

    fun getStreamsForSlot(slotIndex: Int): WindowStream? = streams.find { it.slotIndex == slotIndex }
    fun getOccupiedSlots(): List<Int> = streams.map { it.slotIndex }
    fun isSlotAvailable(slotIndex: Int): Boolean = streams.none { it.slotIndex == slotIndex }
    fun setVolume(streamId: String, volume: Float) { volumes[streamId] = volume.coerceIn(0f, 1f) }
    fun getVolume(streamId: String): Float = volumes[streamId] ?: 0f
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
    fun resolveLayout(count: Int, isPortrait: Boolean): MultiWindowLayout = _currentLayout.value ?: defaultLayout(count, isPortrait)

    fun swapSlots(slotA: Int, slotB: Int) {
        val idxA = streams.indexOfFirst { it.slotIndex == slotA }
        val idxB = streams.indexOfFirst { it.slotIndex == slotB }
        if (idxA < 0 || idxB < 0) return
        val a = streams[idxA]
        val b = streams[idxB]
        streams[idxA] = a.copy(slotIndex = slotB)
        streams[idxB] = b.copy(slotIndex = slotA)
    }

    fun clear() {
        streams.clear(); volumes.clear(); playerHandleIds.clear(); resizeModes.clear()
        _audioFocusId = null; _currentLayout.value = null; _layoutLocked.value = false
    }
}
