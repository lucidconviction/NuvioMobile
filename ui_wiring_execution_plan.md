# UI Wiring Execution Plan — DVR & TeleBackup Features

> **Target:** RNutz Nuvio (Nuvio_Robbdeeze)  
> **Goal:** Wire DVR and TeleBackup features into the app's navigation and player UI  
> **Est. time:** ~1 hour  
> **For:** Agent tasked with UI integration

---

## Files Already Built (do not create these)

| File | Purpose |
|---|---|
| `features/dvr/DvrModels.kt` | All DVR data types (Recording, RecordingSchedule, DvrSettings, etc.) |
| `features/dvr/DvrRepository.kt` | Central DVR state management with persistence |
| `features/dvr/DvrStorage.kt` | Expect declarations for DVR persistence |
| `features/dvr/DvrSettingsRepository.kt` | DVR settings state with sync support |
| `features/dvr/DvrTimeshiftService.kt` | Expect class for timeshift buffer |
| `features/dvr/DvrRecordingEngine.kt` | Expect class for recording engine |
| `features/dvr/DvrTelegramUploader.kt` | Expect class for TDLib upload |
| `features/dvr/DvrPlatformFileOps.kt` | File operation abstraction |
| `features/dvr/DvrLibraryScreen.kt` | DVR library UI (recordings list) |
| `features/dvr/TimeshiftControls.kt` | Timeshift seek bar + live indicator overlay |
| `features/backup/TeleBackupModels.kt` | Backup record models |
| `features/backup/TeleBackupEngine.kt` | Expect class + TeleBackupEngineProvider |
| `features/backup/TeleBackupStorage.kt` | Expect object for backup metadata |
| `features/backup/TeleBackupScreen.kt` | Full backup/restore UI |
| `androidMain/.../dvr/DvrStorage.android.kt` | SharedPreferences persistence |
| `androidMain/.../dvr/DvrTimeshiftService.android.kt` | ExoPlayer CacheDataSource buffer |
| `androidMain/.../dvr/DvrRecordingEngine.android.kt` | File capture for recordings |
| `androidMain/.../dvr/DvrTelegramUploader.android.kt` | TDLib upload implementation |
| `androidMain/.../dvr/DvrPlatformFileOps.android.kt` | Android file ops |
| `androidMain/.../backup/TeleBackupEngine.android.kt` | TDLib upload/download for backups |
| `androidMain/.../backup/TeleBackupStorage.android.kt` | SharedPreferences backup metadata |

---

## Task 1 — Wire TeleBackupScreen into Settings Navigation

### File to modify: `commonMain/.../features/settings/SettingsRootPage.kt`

**What exists:** There's already an `onBackupRestoreClick` callback on the settings root page (line 80) and a button wired to it (line 161). You just need to wire it to a navigation route.

**What to do:**
1. Find where `SettingsRootPage` is called in `App.kt` (or the navigation setup)
2. When `onBackupRestoreClick` fires, navigate to the `TeleBackupScreen`

### File to modify: `commonMain/.../App.kt`

**Add route and nav destination:**

```kotlin
// Add to your navigation setup (find where other settings screens are routed)
// Look for the existing pattern — something like:
// composable("settings/backup") { TeleBackupScreen() }
// or a nav callback list

// Add a route constant:
const val ROUTE_TELEBACKUP = "telebackup"

// Add the nav entry:
composable(ROUTE_TELEBACKUP) {
    TeleBackupScreen()
}

// In the settings screen invocation, wire:
onBackupRestoreClick = { navController.navigate(ROUTE_TELEBACKUP) }
```

**Verify:** Settings → Backup/Restore → opens TeleBackup screen → shows Upload button, Scan button, Restore Latest button, and backup list

---

## Task 2 — Wire DvrLibraryScreen into Navigation

### File to modify: `commonMain/.../App.kt`

**Add route:**

```kotlin
const val ROUTE_DVR_LIBRARY = "dvr/library"

composable(ROUTE_DVR_LIBRARY) {
    DvrLibraryScreen(
        onBack = { navController.popBackStack() },
        onPlayRecording = { recording ->
            // Navigate to player with recording's local path or Telegram file
            val launch = PlayerLaunch(
                sourceUrl = recording.localTempPath 
                    ?: "tg://${recording.telegramFileId}",
                streamTitle = recording.channelName,
                providerName = "DVR",
                parentMetaId = recording.channelId,
                parentMetaType = "channel",
            )
            val launchId = PlayerLaunchStore.put(launch)
            navController.navigate("player/$launchId")
        },
    )
}
```

### File to modify: `commonMain/.../features/hub/RobbdeezeNutzHubScreen.kt`

**Add a DVR hub card:**

```kotlin
// Find where other hub cards are defined (IPTVNutz, MultiNutz, etc.)
// Add a DVR card:

// At the top with other onClick callbacks:
onDvrClick: () -> Unit,

// In the hub cards grid:
DvrNutzCard(
    onClick = onDvrClick,
)
```

**Create `DvrNutzCard` composable (or inline it):**

```kotlin
@Composable
private fun DvrNutzCard(onClick: () -> Unit) {
    // Match the existing hub card style (glass cards with icons)
    HubGlassCard(
        title = "DVR",
        subtitle = "${DvrRepository.uiState.collectAsState().value.recordings.size} recordings",
        icon = Icons.Rounded.Folder,
        onClick = onClick,
    )
}
```

**Wire in the navigation:**

```kotlin
// Where RobbdeezeNutzHubScreen is called:
onDvrClick = { navController.navigate(ROUTE_DVR_LIBRARY) }
```

**Verify:** Hub → DVR card → opens recordings list → shows active, scheduled, completed recordings → tap any recording to play

---

## Task 3 — Add DVR Settings Page

### File to create/modify: `commonMain/.../features/settings/DvrSettingsPage.kt`

Create a new settings page:

```kotlin
package com.nuvio.app.features.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nuvio.app.features.dvr.DvrSettingsRepository

@Composable
fun DvrSettingsPage(
    onBack: () -> Unit,
) {
    val settings by DvrSettingsRepository.settings.collectAsState()

    // Header with back button
    // Toggle: Timeshift enabled
    // Slider: Max buffer minutes (5-120)
    // Toggle: Auto-upload to Telegram
    // Toggle: Keep local copy after upload
    // Slider: Schedule padding start (0-30 min)
    // Slider: Schedule padding end (0-30 min)
    // Text: Storage used by local recordings
}
```

**Reference pattern:** Copy the style from `PlaybackSettingsPage.kt` or `StreamsSettingsPage.kt`.

**Wire into Settings:**

```kotlin
// In SettingsRootPage.kt or wherever settings pages are defined:
composable("settings/dvr") {
    DvrSettingsPage(onBack = { navController.popBackStack() })
}

// In the settings list:
SettingsItem(
    title = "DVR",
    subtitle = "Timeshift, recordings, Telegram upload",
    onClick = { navController.navigate("settings/dvr") },
)
```

**Verify:** Settings → DVR → shows all toggles and sliders

---

## Task 4 — Add Record Button to Player Controls

### File to modify: `commonMain/.../features/player/PlayerControls.kt`

**Find the PiP button or similar action button row.** Add a Record button next to it:

```kotlin
// Find the row of action icons (around line 300-400, look for PiP or Settings icons)
// Add a Record/Stop button:

// Add state at the top level of the composable:
var isRecording by remember { mutableStateOf(false) }

// Add the button in the actions row:
IconButton(
    onClick = {
        if (isRecording) {
            // Stop recording — the recordingId needs to be shared
            scope.launch {
                DvrRepository.stopRecording(currentRecordingId)
            }
            isRecording = false
        } else {
            // Start recording
            scope.launch {
                val recordingId = DvrRepository.startRecording(
                    channel = /* get current IptvChannel from args */,
                    programTitle = streamTitle,
                )
                if (recordingId != null) {
                    currentRecordingId = recordingId
                    isRecording = true
                }
            }
        }
    },
) {
    Icon(
        imageVector = if (isRecording) Icons.Rounded.StopCircle 
                      else Icons.Rounded.FiberManualRecord,
        contentDescription = if (isRecording) "Stop Recording" else "Record",
        tint = if (isRecording) Color.Red else MaterialTheme.colorScheme.onSurface,
    )
}
```

**Exact code block to insert** (find the `BottomControlsRow` or similar action row):

```kotlin
// Add AFTER the comment about icon button (like PiP or speed controls):
// ── Record button ──
IconButton(onClick = {
    if (isRecording) {
        scope.launch {
            currentRecordingId?.let { DvrRepository.stopRecording(it) }
            currentRecordingId = null
            isRecording = false
        }
    } else {
        scope.launch {
            // Build a minimal IptvChannel from the PlayerLaunch data
            val channel = IptvChannel(
                id = channelIds?.getOrNull(currentChannelIndex) ?: "",
                name = channelNames?.getOrNull(currentChannelIndex) ?: title,
                url = sourceUrl,
                sourceType = SourceType.M3U,
                sourceId = "dvr",
            )
            val id = DvrRepository.startRecording(channel, programTitle = streamTitle)
            if (id != null) {
                currentRecordingId = id
                isRecording = true
            }
        }
    }
}) {
    Icon(
        imageVector = if (isRecording) Icons.Rounded.StopCircle else Icons.AutoMirrored.Filled.FiberManualRecord,
        contentDescription = if (isRecording) "Stop" else "Record",
        tint = if (isRecording) Color(0xFFE53935) else MaterialTheme.colorScheme.onSurface,
    )
}
```

**Add state variables near the top of the composable:**

```kotlin
var isRecording by remember { mutableStateOf(false) }
var currentRecordingId by remember { mutableStateOf<String?>(null) }
```

**Required imports to add:**

```kotlin
import androidx.compose.ui.graphics.Color
import com.nuvio.app.features.dvr.DvrRepository
import com.nuvio.app.features.iptv.IptvChannel
import com.nuvio.app.features.iptv.SourceType
```

**Verify:** Player controls → Record button appears → tap to start recording → button turns red/stop → tap to stop → recording saved to DVR library

---

## Task 5 — Add Timeshift Overlay to Player

### File to modify: `commonMain/.../features/player/PlayerPlaybackOverlays.kt`

**Find where the main overlay content is rendered.** The `TimeshiftControls` composable is in `features/dvr/TimeshiftControls.kt`. Add it as an overlay when watching live IPTV:

```kotlin
// Find the Box scope where overlays are rendered (look for BoxScope.PlayerPlaybackOverlays)
// Add the timeshift controls near the bottom of the overlay stack:

// Near the bottom of the Box content, before the gradient overlays:
if (showTimeshiftControls && /* is live IPTV source */) {
    TimeshiftControls(
        timeshiftService = dvrTimeshiftService,
        isActive = activeTimeshiftSession != null,
        isPaused = isTimeshiftPaused,
        currentPositionMs = timeshiftPositionMs,
        maxPositionMs = maxTimeshiftPositionMs,
        bufferedDurationMs = bufferedTimeshiftDuration,
        onPlayPauseToggle = { /* toggle pause/play */ },
        onJumpToLive = { /* seek to latest position */ },
        onSeek = { fraction -> /* seek to position in buffer */ },
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 120.dp) // above the main controls
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    )
}
```

**Add state to the composable parameters:**

```kotlin
// Add these parameters to PlayerPlaybackOverlays:
val showTimeshiftControls: Boolean = false,
val activeTimeshiftSession: Boolean = false,
val isTimeshiftPaused: Boolean = false,
val timeshiftPositionMs: Long = 0L,
val maxTimeshiftPositionMs: Long = 0L,
val bufferedTimeshiftDuration: Long = 0L,
```

**Wire in the screen that calls this overlay:**

```kotlin
// In PlayerScreenRuntimeUi.kt or wherever PlayerPlaybackOverlays is called:
showTimeshiftControls = dvrSettings.timeshiftEnabled && isIptvSource,
```

**Verify:** Watching live IPTV channel → timeshift bar appears at bottom → shows buffer range → pause works → "Live" button jumps to current position

---

## Task 6 — Wire Upload Progress After Recording

### File to modify: `commonMain/.../features/player/PlayerScreenRuntimeUi.kt`

**After a recording stops and upload starts, show upload progress:**

```kotlin
// In the composable that manages overlay state, add:
val dvrState by DvrRepository.uiState.collectAsState()

// Pass upload progress to the loading overlay or notification
when (dvrState.activeUploadProgress) {
    0f -> { /* no upload */ }
    else -> { /* show uploading indicator in player */ }
}
```

**Minimal implementation — just show in the existing snackbar/status area:**

```kotlin
// Where you show loading/saving indicators:
if (dvrState.activeRecordingId != null) {
    Text(
        text = if (dvrState.activeUploadProgress > 0f) 
            "Uploading to Telegram: ${(dvrState.activeUploadProgress * 100).toInt()}%" 
        else "Recording...",
        style = MaterialTheme.typography.bodySmall,
        color = Color(0xFFE53935),
    )
}
```

---

## Task 7 — Initialize DVR + TeleBackup on App Start

### File to modify: `commonMain/.../App.kt`

**Add initialization at app startup:**

```kotlin
// Find the initialization block (look for LaunchedEffect(Unit) or similar)
// Add:
LaunchedEffect(Unit) {
    DvrRepository.ensureLoaded()
    DvrSettingsRepository.ensureLoaded()
}

// For TeleBackup, find where TelegramTdEngine is initialized:
// TeleBackupEngineProvider.setEngine(telegramTdEngine)
```

**In `androidMain/.../MainActivity.kt` or similar:**

```kotlin
// After TelegramTdEngine is initialized:
TeleBackupEngineProvider.setEngine(telegramTdEngine)
```

---

## Summary: What Each File Needs

### Files to Modify

| # | File | Change |
|---|---|---|
| 1 | `App.kt` | Add 2 nav routes (telebackup, dvr/library), init DVR on start |
| 2 | `RobbdeezeNutzHubScreen.kt` | Add DVR hub card + onClick callback |
| 3 | `SettingsRootPage.kt` | Add DVR settings item |
| 4 | `PlayerControls.kt` | Add Record/Stop button with state |
| 5 | `PlayerPlaybackOverlays.kt` | Add TimeshiftControls overlay |
| 6 | `PlayerScreenRuntimeUi.kt` | Wire upload progress display |
| 7 | `MainActivity.kt` | Set TeleBackupEngineProvider engine |

### Files to Create

| # | File | Purpose |
|---|---|---|
| 8 | `features/settings/DvrSettingsPage.kt` | DVR settings UI (toggles + sliders) |

### Pre-existing Navigation Pattern

To find where to add routes, look for existing patterns like:

```kotlin
// In App.kt, find where screens like PlayerScreen, IptvScreen, etc. are navigated
// Usually a NavHost composable with composable("route") { Screen() } entries
// OR a sealed class/object representing nav routes
// Look for examples like "settings", "player/{launchId}", etc.
```

---

*End of UI wiring execution plan*
