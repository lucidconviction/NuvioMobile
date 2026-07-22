package com.nuvio.app.features.iptv

import androidx.compose.runtime.Composable

@Composable
expect fun rememberFilePickerLauncher(onContent: (String, String) -> Unit): () -> Unit
