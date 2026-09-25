package com.nuvio.app.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PageState(
    val scrollIndex: Int = 0,
    val scrollOffset: Int = 0,
    val searchQuery: String = "",
    val filters: Map<String, String> = emptyMap(),
    val selectedPage: String = "",
    val selectedHub: String = "",
    val extra: Map<String, Any> = emptyMap(),
)

object PageStateStore {
    private val _states = MutableStateFlow<Map<String, PageState>>(emptyMap())
    val states: StateFlow<Map<String, PageState>> = _states.asStateFlow()

    fun save(key: String, state: PageState) {
        _states.value = _states.value + (key to state)
    }

    fun restore(key: String): PageState? = _states.value[key]

    fun clear(key: String) {
        _states.value = _states.value - key
    }

    fun clearAll() {
        _states.value = emptyMap()
    }
}