package com.nuvio.app.features.sports

actual suspend fun platformYouTubeSearch(query: String): List<YouTubeVideo>? = null

actual suspend fun platformResolveYouTubeStream(videoId: String): StreamResult? = null
