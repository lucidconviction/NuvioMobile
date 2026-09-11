package com.nuvio.app.features.hub

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Scrim = Color(0xCC000000)
private val Accent = Color(0xFF4A90D9)

/**
 * Full-screen buffering feedback shown while a video selection is being prepared
 * (before the player opens). Driven by [VideoSelectionFeedback] so every video hub
 * gives the user immediate visual confirmation their selection was made.
 */
@Composable
fun BufferingOverlay() {
    val feedback = VideoSelectionFeedback.state

    AnimatedVisibility(
        visible = feedback.active,
        enter = fadeIn(androidx.compose.animation.core.tween(180)),
        exit = fadeOut(androidx.compose.animation.core.tween(180)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Scrim),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        color = Accent,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(56.dp),
                    )
                }
                Spacer(Modifier.height(20.dp))
                Text(
                    "Opening video…",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                if (feedback.label.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        feedback.label,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 32.dp),
                    )
                }
                if (feedback.subLabel.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        feedback.subLabel,
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 32.dp),
                    )
                }
            }
        }
    }
}
