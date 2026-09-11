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
    // Per-slot auto-play queues (e.g. YouTube playlists): next video plays when the current one ends.
    private val slotPlaylists = mutableMapOf<Int, List<IptvChannel>>()
    private val slotPlaylistIdx = mutableMapOf<Int, Int>()

    /** Set the playlist to auto-play through for a slot. Empty clears it. */
    fun setSlotPlaylist(slotIndex: Int, channels: List<IptvChannel>) {
        slotPlaylists[slotIndex] = channels
        slotPlaylistIdx[slotIndex] = -1
    }

    /** Returns the next channel to play in the slot's playlist, or null when done. */
    fun nextInSlot(slotIndex: Int): IptvChannel? {
        val list = slotPlaylists[slotIndex] ?: return null
        val next = slotPlaylistIdx[slotIndex]?.plus(1) ?: 0
        if (next >= list.size) { slotPlaylists.remove(slotIndex); slotPlaylistIdx.remove(slotIndex); return null }
        slotPlaylistIdx[slotIndex] = next
        return list[next]
    }

    /** Mark the slot's current playlist position as playing this channel. */
    fun markSlotCurrent(slotIndex: Int, channelId: String) {
        val list = slotPlaylists[slotIndex] ?: return
        val idx = list.indexOfFirst { it.id == channelId }
        if (idx >= 0) slotPlaylistIdx[slotIndex] = idx
    }

    val allStreams: List<WindowStream> get() = streams

    fun addToSlot(channel: IptvChannel, slotIndex: Int) {
        val id = "mw_${++idCounter}"
        val hasFocus = _audioFocusId == null
        volumes[id] = if (hasFocus) 1f else 0f
        if (hasFocus) _audioFocusId = id
        val existingAtSlot = streams.indexOfFirst { it.slotIndex == slotIndex }
        val existingChannel = streams.indexOfFirst { it.channel.id == channel.id && it.channel.sourceId == channel.sourceId }
        if (existingChannel >= 0 && existingChannel != existingAtSlot) {
            streams.removeAt(existingChannel)
            val adjustedExistingAtSlot = streams.indexOfFirst { it.slotIndex == slotIndex }
            if (adjustedExistingAtSlot >= 0) {
                val oldId = streams[adjustedExistingAtSlot].id
                streams[adjustedExistingAtSlot] = WindowStream(id = id, channel = channel, slotIndex = slotIndex)
                volumes.remove(oldId); playerHandleIds.remove(oldId); paused.remove(oldId); resizeModes.remove(oldId)
            } else {
                streams.add(WindowStream(id = id, channel = channel, slotIndex = slotIndex))
            }
        } else if (existingAtSlot >= 0) {
            val oldId = streams[existingAtSlot].id
            streams[existingAtSlot] = WindowStream(id = id, channel = channel, slotIndex = slotIndex)
            volumes.remove(oldId); playerHandleIds.remove(oldId); paused.remove(oldId); resizeModes.remove(oldId)
        } else {
            streams.add(WindowStream(id = id, channel = channel, slotIndex = slotIndex))
        }
        if (_currentLayout.value == null) {
            _layoutLocked.value = false
        }
    }

    fun addStream(url: String, title: String, poster: String? = null, slotIndex: Int) {
        val id = "mw_${++idCounter}"
        val hasFocus = _audioFocusId == null
        volumes[id] = if (hasFocus) 1f else 0f
        if (hasFocus) _audioFocusId = id
        val dummyChannel = IptvChannel(
            id = "generic_$id",
            name = title,
            url = url,
            logo = poster,
            group = title.take(30),
            sourceType = SourceType.M3U,
            sourceId = "multiview",
        )
        val existingAtSlot = streams.indexOfFirst { it.slotIndex == slotIndex }
        if (existingAtSlot >= 0) {
            val oldId = streams[existingAtSlot].id
            streams[existingAtSlot] = WindowStream(id = id, channel = dummyChannel, slotIndex = slotIndex, playerUrl = url, playerTitle = title, playerPoster = poster)
            volumes.remove(oldId); playerHandleIds.remove(oldId); paused.remove(oldId); resizeModes.remove(oldId)
        } else {
            streams.add(WindowStream(id = id, channel = dummyChannel, slotIndex = slotIndex, playerUrl = url, playerTitle = title, playerPoster = poster))
        }
        if (_currentLayout.value == null) {
            _layoutLocked.value = false
        }
    }

    fun removeSlot(slotIndex: Int) {
        streams.removeAll { it.slotIndex == slotIndex }
        slotPlaylists.remove(slotIndex); slotPlaylistIdx.remove(slotIndex)
    }

    fun remove(id: String) {
        streams.removeAll { it.id == id }; volumes.remove(id); paused.remove(id); playerHandleIds.remove(id)
        if (_audioFocusId == id) _audioFocusId = null
    }

    fun getStreamsForSlot(slotIndex: Int): WindowStream? = streams.find { it.slotIndex == slotIndex }
    fun getOccupiedSlots(): List<Int> = streams.map { it.slotIndex }
    fun isSlotAvailable(slotIndex: Int): Boolean = streams.none { it.slotIndex == slotIndex }

    /** First free slot index, or null when every slot is occupied. */
    fun nextAvailableSlot(): Int? = (0 until MAX_SLOTS).firstOrNull { isSlotAvailable(it) }
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
        slotPlaylists.clear(); slotPlaylistIdx.clear()
        _audioFocusId = null; _currentLayout.value = null; _layoutLocked.value = false
    }
}
