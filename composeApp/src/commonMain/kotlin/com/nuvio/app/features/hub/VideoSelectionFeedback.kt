package com.nuvio.app.features.hub

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * App-wide "a video selection is being prepared" feedback signal.
 *
 * Any video-capable screen calls [start] when the user taps a video (before stream
 * resolution completes) and [stop] once the player launches or resolution fails.
 * A single global [BufferingOverlay] observes [state] and shows buffering feedback
 * so the user always knows their selection was registered.
 */
object VideoSelectionFeedback {

    data class SelectionState(
        val active: Boolean = false,
        val label: String = "",
        val subLabel: String = "",
    )

    var state by mutableStateOf(SelectionState())
        private set

    /** Show buffering feedback. [label] is the item being opened (title/name). */
    fun start(label: String = "", subLabel: String = "") {
        state = SelectionState(active = true, label = label, subLabel = subLabel)
    }

    /** Hide the buffering feedback (player launched, resolution done, or failed). */
    fun stop() {
        state = SelectionState()
    }
}
