package com.nuvio.app.features.iptv

internal expect object ChannelQuickSearchStore {
    fun loadTerms(): List<String>
    fun addTerm(term: String)
    fun removeTerm(term: String)
}