package com.nuvio.app.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Typed layer key for a screen the user has visited. Two screens with the same key are
 * considered the same layer — re-selecting the same screen does **not** push a duplicate.
 *
 * The key is intentionally a stable string built by the *caller* from its own typed route
 * object, so this package stays a leaf (no dependency on feature modules). Callers pass
 * a [ScreenLayer] built via the helpers below.
 *
 * In-memory only (lost on process death), per the Feature 7 requirement.
 */
sealed interface ScreenLayer {
    val key: String

    /** Root tab shell — never popped; back falls through to exit confirmation. */
    data object Tabs : ScreenLayer {
        override val key: String = "Tabs"
    }

    /** Hub home screen (the RobbdeezeNutz hub grid). Never popped either. */
    data object HubHome : ScreenLayer {
        override val key: String = "Hub:Home"
    }

    /** A hub sub-screen (Iptv / Sports / VidNutz / Music / Pod / Multi). Key includes the name. */
    data class HubSub(val name: String) : ScreenLayer {
        override val key: String = "Hub:$name"
    }

    /** A detail screen opened off the tab stack: `type:id`. */
    data class Detail(val type: String, val id: String) : ScreenLayer {
        override val key: String = "Detail:$type:$id"
    }

    /** A settings leaf page. */
    data class Settings(val pageName: String) : ScreenLayer {
        override val key: String = "Settings:$pageName"
    }

    /** A sports page (LIVE / EVENT / STANDINGS / UPCOMING / ...). */
    data class Sport(val page: String) : ScreenLayer {
        override val key: String = "Sport:$page"
    }

    /** A team detail screen. */
    data class Team(val teamName: String, val sport: String) : ScreenLayer {
        override val key: String = "Team:$teamName:$sport"
    }

    /** The full-screen player. */
    data class Player(val launchId: Long) : ScreenLayer {
        override val key: String = "Player:$launchId"
    }

    companion object {
        /** Build a [ScreenLayer] from a Navigation3 [AppRoute]-like value. */
        fun fromRoute(route: Any?): ScreenLayer = when (route) {
            null -> Tabs
            is ScreenLayer -> route
            else -> {
                val s = route.toString()
                Detail(type = s.substringBefore('('), id = s)
            }
        }
    }
}

/**
 * Records every visited screen layer so the back button can return one layer at a time
 * instead of exiting the app. Root layers ([ScreenLayer.Tabs], [ScreenLayer.HubHome]) are
 * never popped — back there falls through to the existing exit-confirmation behavior.
 *
 * In-memory only.
 */
object ScreenBackStore {
    private val _layers = MutableStateFlow<List<ScreenLayer>>(emptyList())
    val layers: StateFlow<List<ScreenLayer>> = _layers.asStateFlow()

    fun push(layer: ScreenLayer) {
        val current = _layers.value
        if (current.lastOrNull() == layer) return
        _layers.value = current + layer
    }

    /** Pop the top layer. Returns the new top layer (or null when the stack is empty). */
    fun pop(): ScreenLayer? {
        val current = _layers.value
        if (current.isEmpty()) return null
        _layers.value = current.dropLast(1)
        return current.lastOrNull()
    }

    fun peek(): ScreenLayer? = _layers.value.lastOrNull()

    fun canGoBack(): Boolean = _layers.value.size > 1

    /** Replace the top layer (used when re-selecting the same screen at the same depth). */
    fun replaceTop(layer: ScreenLayer) {
        val current = _layers.value
        if (current.isEmpty()) { _layers.value = listOf(layer); return }
        _layers.value = current.dropLast(1) + layer
    }

    fun clear() { _layers.value = emptyList() }
}