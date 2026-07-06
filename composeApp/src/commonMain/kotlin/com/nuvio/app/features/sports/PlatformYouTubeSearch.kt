package com.nuvio.app.features.sports

expect suspend fun platformYouTubeSearch(query: String): List<YouTubeVideo>?

expect suspend fun platformResolveYouTubeStream(videoId: String): StreamResult?
