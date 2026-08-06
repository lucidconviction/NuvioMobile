# IPTVNutz DVR Feature — Code Analysis & Execution Plan (Telegram Storage Edition)

> **Project:** RNutz Nuvio (Nuvio_Robbdeeze)  
> **Goal:** Add DVR capabilities to IPTVNutz, using **Telegram as the cloud storage backend**  
> **Date:** 2026-08-03  
> **Key Insight:** Telegram storage reuses existing `:tdlib-java` integration. Unlimited storage, files up to 2 GB, streaming playback already works.

---

## Table of Contents

1. [Current Architecture Overview](#1-current-architecture-overview)
2. [Why Telegram Storage?](#2-why-telegram-storage)
3. [DVR Feature Scope (Telegram Edition)](#3-dvr-feature-scope-telegram-edition)
4. [Phase 1 — Local Timeshift Buffer](#4-phase-1--local-timeshift-buffer)
5. [Phase 2 — Telegram Upload Engine](#5-phase-2--telegram-upload-engine)
6. [Phase 3 — Recording & Upload Pipeline](#6-phase-3--recording--upload-pipeline)
7. [Phase 4 — Scheduled Recording from EPG](#7-phase-4--scheduled-recording-from-epg)
8. [Phase 5 — Playback & Recordings Library](#8-phase-5--playback--recordings-library)
9. [Phase 6 — TeleNutz Integration & Provider Catch-up](#9-phase-6--telenutz-integration--provider-catch-up)
10. [Cross-Cutting Concerns](#10-cross-cutting-concerns)
11. [Risk Assessment & Dependencies](#11-risk-assessment--dependencies)
12. [Appendix: Key Files & Their Roles](#12-appendix-key-files--their-roles)

---

## 1. Current Architecture Overview

### 1.1 Tech Stack

| Layer | Technology |
|---|---|
| UI | Jetpack Compose Multiplatform (Kotlin Multiplatform) |
| Player | ExoPlayer (Android), libmpv (Android fallback), native iOS MPV |
| IPTV Sources | M3U playlists, Xtream Codes API, Stalker Portal |
| EPG | XMLTV format parser with streaming chunked loading |
| Persistence (local) | JSON files via `expect`/`actual` platform storage |
| Telegram | `:tdlib-java` module — full TDLib 2.0 client integrated via TeleNutz |
| Cloud | Supabase (PostgREST, Auth, Realtime) |

### 1.2 What Already Exists for Telegram

| Component | File | Purpose |
|---|---|---|
| TDLib engine | `androidMain/.../TelegramTdEngine.kt` | Full TDLib client lifecycle |
| Auth flow | `commonMain/.../TelegramAuth.kt` | Login with phone/QR |
| TeleNutz models | `commonMain/.../TeleNutzModels.kt` | Telegram → IPTV channel mapping |
| TeleNutz storage | `commonMain/.../TeleNutzStorage.kt` | Persistent chat/channel config |
| TeleNutz streaming | `androidMain/.../TeleNutzStorage.android.kt` | Downloads video from Telegram to local file, feeds to player |

**Key insight:** TeleNutz already does the *download* side — it reads an IPTV stream URL from a Telegram message, downloads the video file via TDLib, and feeds it to ExoPlayer. DVR just adds the *upload* side — record a stream, upload it to a Telegram chat, store the file ID.

### 1.3 The DVR-Telegram Pipeline

```
Live Stream (HLS/TS)
  → Local buffer (torrent-style ring buffer)
    → Optional: watch with timeshift
      → Press Record → mark start position
        → On Stop → finalize local file
          → Upload to Telegram (Saved Messages or DVR channel)
            → Store mapping: metadata ↔ Telegram file_id
              → Delete local file
                → Play back from Telegram file_id (existing TeleNutz pipeline)
```

---

## 2. Why Telegram Storage?

### 2.1 Comparison: Local vs Telegram

| Concern | Local Storage | Telegram Storage |
|---|---|---|
| Capacity | Limited by device (32-128 GB typical) | Unlimited per account |
| Cross-device access | One device only | Access from any Telegram client |
| Backup | Manual or none | Telegram handles redundancy |
| File size limit | Device filesystem limit | 2 GB per file (TG Premium/account) |
| Streaming | Direct file I/O | Already works (TeleNutz does this) |
| Setup cost | Nothing | Already have TDLib |
| Speed | Instant | Upload time after recording |
| Offline playback | Yes | Need to download first |

### 2.2 What Stays Local

| Item | Why Local |
|---|---|
| Timeshift buffer | Too latency-sensitive for Telegram |
| Active recording temp file | Must capture before upload |
| Metadata database | Recording ↔ Telegram file_id mapping |
| Recently watched recordings cache | For offline playback |

### 2.3 Telegram API Limits & How to Handle Them

| Limit | Value | Mitigation |
|---|---|---|
| Max file upload | 50 MB (bots) / 2 GB (user account) | Split recordings into ≤2 GB chunks |
| Upload rate limit | ~1 MB/s (varies) | Show progress, queue uploads |
| Download rate limit | ~1-5 MB/s (varies, better than upload) | Stream directly via TDLib |
| FloodWait | Per-session, temporary | TDLib handles this automatically |
| Active sessions | Unlimited | Use Saved Messages or private channel |

---

## 3. DVR Feature Scope (Telegram Edition)

| Feature | Description | Storage | Difficulty |
|---|---|---|---|
| **Timeshift** | Pause/rewind live TV | Local ring buffer | Medium |
| **Manual Recording** | Record current channel → upload to Telegram | Local → Telegram | Medium-High |
| **Scheduled Recording** | EPG-based auto-record from Telegram | Local → Telegram | High |
| **Recordings Library** | Browse recordings, stored in Telegram | Telegram metadata | Medium |
| **Catch-up (Provider)** | Detect & use provider catch-up APIs | Stream direct | Low-Medium |

---

## 4. Phase 1 — Local Timeshift Buffer

### What It Does

- Continuous circular buffer of the live stream on device
- User can pause for up to ~30 minutes, seek back, then resume
- **Never uploaded to Telegram** — buffer is ephemeral, discarded on channel change
- Separate from recording — recording is when user explicitly saves

### Implementation

#### 4.1 Data Models (`DvrModels.kt`)

```kotlin
// New file: composeApp/.../features/dvr/DvrModels.kt

data class TimeshiftSession(
    val channelId: String,
    val bufferFilePath: String,
    val startedAtMs: Long,
    val durationMs: Long,
    val maxBufferMs: Long = 30 * 60 * 1000L,  // 30 min default
    val isPaused: Boolean = false,
    val pausedAtPositionMs: Long = 0L,
)

data class Recording(
    val id: String,
    val channelId: String,
    val channelName: String,
    val channelLogo: String?,
    val programTitle: String?,
    val programDescription: String?,
    // Telegram storage info
    val telegramFileId: String? = null,       // set after upload completes
    val telegramUniqueId: String? = null,     // for dedup
    val telegramChatId: Long? = null,         // where it's stored
    val telegramMessageId: Long? = null,      // message containing the file
    // Local temp info
    val localTempPath: String? = null,        // while recording / before upload
    val localCachePath: String? = null,       // downloaded for offline playback
    // Timing
    val startedAtMs: Long,
    val endedAtMs: Long? = null,
    val durationMs: Long = 0L,
    val fileSizeBytes: Long = 0L,
    val status: RecordingStatus,
    val epgProgramId: String? = null,          // if scheduled from EPG
    val sourceUrl: String,                     // original stream URL
    val sourceHeaders: Map<String, String> = emptyMap(),
    val chunkCount: Int = 1,                   // multi-chunk recordings
    val chunks: List<RecordingChunk> = emptyList(),
)

data class RecordingChunk(
    val index: Int,
    val localTempPath: String?,
    val telegramFileId: String?,
    val telegramUniqueId: String?,
    val fileSizeBytes: Long,
    val durationMs: Long,
    val uploadProgress: Float = 0f,
    val uploadStatus: UploadStatus = UploadStatus.Pending,
)

enum class RecordingStatus {
    Recording,          // capturing to local temp
    Uploading,          // local done, uploading to Telegram
    Completed,          // stored in Telegram
    Failed,             // error
    Cancelled,          // user cancelled
}

enum class UploadStatus {
    Pending,
    Uploading,
    Completed,
    Failed,
}

data class RecordingSchedule(
    val id: String,
    val channelId: String,
    val channelName: String,
    val epgProgramId: String?,
    val programTitle: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val isRecurring: Boolean = false,
    val daysOfWeek: Set<Int> = emptySet(),
    val enabled: Boolean = true,
)

data class TelegramDvrConfig(
    val storageChatId: Long = 0L,         // 0 = use Saved Messages
    val storageChatTitle: String = "Saved Messages",
    val autoUpload: Boolean = true,        // upload immediately after recording
    val keepLocalCopy: Boolean = false,    // keep local file after upload
    val maxTelegramFileSize: Long = 1_900_000_000L,  // 1.9 GB safe limit
)
```

#### 4.2 Timeshift Buffer Engine (`DvrTimeshiftService.kt`)

Uses ExoPlayer's `CacheDataSource` with a sliding window:

```kotlin
class TimeshiftBufferService(private val context: Context) {
    private val cacheEvictor = LeastRecentlyUsedCacheEvictor(
        maxBytes = 30L * 60 * 1024 * 1024  // 30 min ≈ 1.8 GB
    )
    private val cache = SimpleCache(cacheDir, cacheEvictor)

    fun createTimeshiftMediaSource(url: String, headers: Map<String, String>): MediaSource {
        return HlsMediaSource.Factory(
            CacheDataSource.Factory()
                .setCache(cache)
                .setUpstreamDataSourceFactory(DefaultHttpDataSource.Factory()
                    .setDefaultRequestProperties(headers))
                .setFlags(CacheDataSource.FLAG_BLOCK_ON_CACHE)
        ).createMediaSource(MediaItem.fromUri(url))
    }

    fun getTimeshiftPosition(): Long { /* current position in buffer */ }
    fun getMaxTimeshiftPosition(): Long { /* total buffered duration */ }
    fun clear() { cache.release() }
}
```

#### 4.3 Player Integration

- Add `enableTimeshift()` / `disableTimeshift()` to `PlayerEngineController`
- Add `timeshiftPositionMs`, `timeshiftAvailableMs` to `PlayerPlaybackSnapshot`
- Timeshift controls in `PlayerControls.kt`:
  - Seek bar showing buffered range vs live position
  - "Live" button to jump back to real-time
  - Elapsed: `12:34 behind live / 30:00 buffer`

#### 4.4 New Files for Phase 1

```
features/dvr/DvrModels.kt
features/dvr/DvrTimeshiftService.kt (expect)
features/dvr/TimeshiftControls.kt
androidMain/.../dvr/DvrTimeshiftService.android.kt (actual — CacheDataSource)
```

#### 4.5 Modified Files

```
features/player/PlayerEngine.kt         ← add timeshift methods
features/player/PlayerModels.kt         ← add timeshift snapshot fields
features/player/PlayerControls.kt       ← add timeshift seek bar + Live button
features/player/PlayerPlaybackOverlays.kt  ← timeshift overlay
features/player/PlayerScreenRuntimeState.kt  ← timeshift state wiring
```

---

## 5. Phase 2 — Telegram Upload Engine

### What It Does

Core upload infrastructure that uses the existing TDLib integration to upload files to Telegram. This is the foundation for all recording uploads.

### 5.1 Architecture

```kotlin
// New file: features/dvr/DvrTelegramUploader.kt (expect)
// New file: androidMain/.../dvr/DvrTelegramUploader.android.kt (actual)

class DvrTelegramUploader(private val tdEngine: TelegramTdEngine) {

    suspend fun uploadFile(
        localPath: String,
        fileName: String,
        chatId: Long = 0L,           // 0 = Saved Messages
        onProgress: (Float) -> Unit,
    ): TelegramUploadResult {
        // 1. Open file via TDLib
        val fileId = tdEngine.send(OpenFile(localPath)).fileId

        // 2. Send as document to chat
        val sendMessage = SendMessage(
            chatId = chatId.takeIf { it > 0 } ?: tdEngine.myUserId(),
            inputMessageContent = InputMessageDocument(
                document = InputFileLocal(localPath),
                fileName = fileName,
            )
        )
        val message = tdEngine.send(sendMessage) as Message

        // 3. Track upload progress via TDLib updateFile
        //    (TDLib emits UpdateFile with progress)
        return TelegramUploadResult(
            fileId = message.document.document.id,
            fileUniqueId = message.document.document.uniqueId,
            chatId = message.chatId,
            messageId = message.id,
            fileSize = message.document.document.size,
        )
    }
}

data class TelegramUploadResult(
    val fileId: Int,
    val fileUniqueId: String,
    val chatId: Long,
    val messageId: Long,
    val fileSize: Long,
)
```

### 5.2 Upload Queue (`DvrUploadQueue.kt`)

Since Telegram rate-limits uploads and large files take time:

```kotlin
class DvrUploadQueue(private val uploader: DvrTelegramUploader) {
    private val queue = Channel<QueuedUpload>(Channel.UNLIMITED)

    suspend fun enqueue(recordingId: String, localPath: String, title: String) {
        queue.send(QueuedUpload(recordingId, localPath, title))
    }

    suspend fun processQueue() {
        for (upload in queue) {
            val result = uploader.uploadFile(
                localPath = upload.localPath,
                fileName = upload.title,
                onProgress = { progress ->
                    DvrRepository.updateUploadProgress(upload.recordingId, progress)
                }
            )
            DvrRepository.onUploadComplete(upload.recordingId, result)
        }
    }
}
```

### 5.3 Chunked Upload for Long Recordings

Recordings > 90 min (≈1.8 GB @ 1080p) get split:

```kotlin
class DvrChunkManager {
    fun splitRecording(inputPath: String, maxChunkBytes: Long): List<String> {
        // Use MediaMuxer (Android) to split TS file into chunks
        // Each chunk ≤ maxChunkBytes (e.g., 1.9 GB)
        val chunks = mutableListOf<String>()
        var chunkIndex = 0
        // ... splitting logic ...
        return chunks
    }

    fun rebuildPlaylist(chunks: List<String>): String {
        // Generate a local M3U8 playlist referencing all chunks
        // This can be re-uploaded as the recording manifest
        return m3uContent
    }
}
```

### 5.4 Telegram Chat Selection

The user can choose where recordings go:

| Option | Chat ID | Pros | Cons |
|---|---|---|---|
| **Saved Messages** | Your own user ID | Private, always accessible, no clutter | No sharing |
| **Private Channel** | Your DVR channel | Organized, can share, can add others | Need to create channel |
| **Bot DM** | Bot chat | Can be automated further | Limited file size (50 MB) |

Default = **Saved Messages** (no setup needed, always available).

### 5.5 New Files

```
features/dvr/DvrTelegramUploader.kt (expect)
features/dvr/DvrUploadQueue.kt
features/dvr/DvrChunkManager.kt
features/dvr/DvrTelegramConfig.kt
features/dvr/DvrTelegramConfigScreen.kt     ← settings UI for chat selection
androidMain/.../dvr/DvrTelegramUploader.android.kt (actual)
```

---

## 6. Phase 3 — Recording & Upload Pipeline

### What It Does

When user presses Record, the full pipeline runs:
1. Capture stream to local temp file (using the same buffer from Phase 1)
2. When user stops, finalize the file
3. Upload to Telegram (queued, with progress)
4. Store metadata locally
5. Delete local temp file (optional: keep local copy)

### 6.1 Recording Engine

```kotlin
// New file: features/dvr/DvrRecordingEngine.kt (expect)

class DvrRecordingEngine(private val context: Context) {

    private val activeRecordings = mutableMapOf<String, RecordingHandle>()

    fun startRecording(
        channel: IptvChannel,
        programTitle: String? = null,
        epgProgramId: String? = null,
    ): String {
        val recordingId = generateId()
        val outputPath = "${recordingDir}/${sanitizeFileName(channel.name)}_${timestamp()}.ts"

        // Start capturing the HLS stream to local file
        // Uses the same underlying mechanism as timeshift buffer
        // but writes to a permanent (non-circular) file
        val handle = startSegmentCapture(
            streamUrl = channel.url,
            headers = extractHeaders(channel),
            outputPath = outputPath,
        )
        activeRecordings[recordingId] = handle

        DvrRepository.addRecording(
            Recording(
                id = recordingId,
                channelId = channel.id,
                channelName = channel.name,
                channelLogo = channel.logo,
                programTitle = programTitle,
                localTempPath = outputPath,
                startedAtMs = System.currentTimeMillis(),
                status = RecordingStatus.Recording,
                sourceUrl = channel.url,
            )
        )
        return recordingId
    }

    fun stopRecording(recordingId: String) {
        val handle = activeRecordings[recordingId] ?: return
        handle.stop()
        val fileSize = File(handle.outputPath).length()

        DvrRepository.updateRecording(recordingId) {
            it.copy(
                status = RecordingStatus.Uploading,
                endedAtMs = System.currentTimeMillis(),
                durationMs = System.currentTimeMillis() - it.startedAtMs,
                fileSizeBytes = fileSize,
            )
        }

        // Check if chunking needed
        if (fileSize > TelegramDvrConfig.maxTelegramFileSize) {
            val chunks = DvrChunkManager.splitRecording(handle.outputPath)
            // Upload each chunk
            chunks.forEachIndexed { idx, chunkPath ->
                DvrUploadQueue.enqueue(recordingId, chunkPath, "${it.channelName}_part${idx + 1}")
            }
        } else {
            DvrUploadQueue.enqueue(recordingId, handle.outputPath, channelName)
        }
    }
}
```

### 6.2 Recording State Machine

```
                    ┌─────────┐
                    │  Idle   │
                    └────┬────┘
                         │ Press Record
                    ┌────▼─────┐
              ┌─────┤Recording ├─────┐
              │     └────┬─────┘     │
              │  Stop    │           │ Error / Kill
         ┌────▼────┐ ┌──▼──────┐  ┌──▼────┐
         │Uploading│ │ Cancelled│  │Failed │
         └────┬────┘ └─────────┘  └───────┘
              │ Upload done
         ┌────▼────┐
         │Completed│ ←→ Telegram file_id set
         └─────────┘
```

### 6.3 Android Background Recording

```kotlin
// New file: androidMain/.../dvr/DvrRecordingService.kt
class DvrRecordingService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        recordingEngine = DvrRecordingEngine(this)
        // ... start capture loop
        return START_STICKY
    }
}
```

AndroidManifest addition:
```xml
<service
    android:name=".features.dvr.DvrRecordingService"
    android:foregroundServiceType="mediaPlayback"
    android:exported="false" />
```

### 6.4 Player Controls Integration

Add to `PlayerControls.kt`:
- 🔴 Record button (turns into ⏹ Stop when recording)
- Shows recording duration (`12:34 elapsed`)
- After stop → shows upload progress (`Uploading to Telegram: 67%`)
- Toast: "Recording saved to Telegram DVR"

### 6.5 Notification During Upload

On Android, show ongoing notification:
```
┌─────────────────────────────┐
│ 📹 Uploading Recording      │
│ ESPN_20260803_043000.ts     │
│ ████████░░░░ 67%            │
└─────────────────────────────┘
```

### 6.6 New Files

```
features/dvr/DvrRecordingEngine.kt (expect)
features/dvr/DvrRecordingControls.kt
androidMain/.../dvr/DvrRecordingEngine.android.kt (actual)
androidMain/.../dvr/DvrRecordingService.kt
```

### 6.7 Modified Files

```
features/player/PlayerControls.kt    ← Record/Stop/Upload-progress button
features/player/PlayerModels.kt      ← Recording state in PlayerLaunch
features/settings/SettingsRootPage.kt ← DVR settings link
```

---

## 7. Phase 4 — Scheduled Recording from EPG

### 7.1 How It Works

1. User browses EPG, long-presses a future program → "Schedule Recording"
2. At program start time (+ padding), recording begins automatically
3. Same pipeline as Phase 3: capture locally → upload to Telegram
4. This works because the IPTV stream URL is known in advance
5. If network is unavailable at scheduled time, skip (no local storage for recordings)

### 7.2 Scheduling Mechanism

```kotlin
// New file: features/dvr/DvrSchedulingManager.kt (expect)
class DvrSchedulingManager(private val context: Context) {

    fun scheduleRecording(schedule: RecordingSchedule) {
        // Use WorkManager (Android) for reliable scheduling
        val workRequest = OneTimeWorkRequestBuilder<DvrScheduledRecordingWorker>()
            .setInitialDelay(
                schedule.startTimeMs - System.currentTimeMillis(),
                TimeUnit.MILLISECONDS
            )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .addTag("dvr_scheduled")
            .addTag(schedule.id)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "dvr_schedule_${schedule.id}",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    fun cancelScheduledRecording(scheduleId: String) {
        WorkManager.getInstance(context).cancelUniqueWork("dvr_schedule_$scheduleId")
    }
}
```

### 7.3 WorkManager Worker

```kotlin
// New file: androidMain/.../dvr/DvrScheduledRecordingWorker.kt
class DvrScheduledRecordingWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val scheduleId = params.tags.first { it.startsWith("dvr_schedule_") }
            .removePrefix("dvr_schedule_")
        val schedule = DvrRepository.getSchedule(scheduleId) ?: return Result.failure()

        // Get fresh stream URL (in case it expired or rotated)
        val channel = IptvRepository.getChannel(schedule.channelId) ?: return Result.failure()

        // Start recording with padding
        val expectedEndMs = schedule.endTimeMs + paddingEnd
        DvrRecordingEngine.startRecording(
            channel = channel,
            programTitle = schedule.programTitle,
            epgProgramId = schedule.epgProgramId,
            expectedDurationMs = expectedEndMs - System.currentTimeMillis(),
        )

        // Worker keeps alive for the recording duration
        // (actual recording is in a foreground service, but worker supervises)
        val remainingMs = expectedEndMs - System.currentTimeMillis()
        if (remainingMs > 0) delay(remainingMs)

        // DvrRecordingEngine.stopRecording is called by the timer
        return Result.success()
    }
}
```

### 7.4 Boot Receiver

Re-schedule missed recordings after reboot:

```kotlin
// New file: androidMain/.../dvr/DvrBootReceiver.kt
class DvrBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            DvrRepository.getScheduledRecordings().forEach { schedule ->
                if (schedule.startTimeMs > System.currentTimeMillis() && schedule.enabled) {
                    DvrSchedulingManager.scheduleRecording(schedule)
                } else if (schedule.startTimeMs < System.currentTimeMillis() && schedule.enabled) {
                    // Missed — check if it's still worth recording
                    if (schedule.endTimeMs > System.currentTimeMillis()) {
                        // Still in progress, start recording now (late)
                        DvrSchedulingManager.startEmergencyLateRecording(schedule)
                    }
                }
            }
        }
    }
}
```

### 7.5 EPG UI Integration

In `IptvScreen.kt`, add to EPG program long-press:

```
┌─────────────────────────────────┐
│ 📺 Schedule Recording           │
│ "SportsCenter"                  │
│ ESPN · Today 6:00 PM - 7:00 PM │
│                                 │
│ Padding: [2 min early] [5 late] │
│                                 │
│ [📹 Save to Telegram]  [Cancel] │
└─────────────────────────────────┘
```

EPG grid shows status badges:
- ⏰ Scheduled (clock icon)
- 🔴 Recording Now
- ✅ Recorded (check) → links to recordings library
- 📁 Saved to Telegram (cloud icon)

### 7.6 Conflict Detection

```kotlin
fun getConflictingSchedules(schedule: RecordingSchedule): List<RecordingSchedule> {
    // One stream at a time is all we can record
    // But we can upload multiple simultaneously
    return schedules.filter { existing ->
        existing.enabled &&
        existing.id != schedule.id &&
        existing.startTimeMs < schedule.endTimeMs &&
        existing.endTimeMs > schedule.startTimeMs
    }
}
```

Show warning if overlapping. Since there's only one tuner, only one recording at a time.

### 7.7 New Files

```
features/dvr/DvrSchedulingManager.kt (expect)
features/dvr/DvrScheduleUI.kt                    ← schedule list in EPG
androidMain/.../dvr/DvrSchedulingManager.android.kt (actual — WorkManager)
androidMain/.../dvr/DvrScheduledRecordingWorker.kt
androidMain/.../dvr/DvrBootReceiver.kt
```

### 7.8 Modified Files

```
features/iptv/IptvScreen.kt       ← EPG long-press → schedule dialog
features/iptv/IptvModels.kt       ← EPG status badges
AndroidManifest.xml               ← BootReceiver registration
```

---

## 8. Phase 5 — Playback & Recordings Library

### 8.1 How Playback Works

Two paths:

**Direct from Telegram (recommended):**
1. User picks a recording from the library
2. TDLib fetches the file from Telegram by `file_id`
3. TDLib streams it to a local temp file as it downloads
4. The temp file path is fed to ExoPlayer
5. This is **exactly what TeleNutz already does** for channel playback

**From local cache (offline):**
1. User pre-downloaded a recording
2. Direct file path → ExoPlayer

### 8.2 Telegram Streaming (Reusing TeleNutz)

```kotlin
// Leverage existing TeleNutzFileDownloader
// File: androidMain/.../hub/TeleNutzStorage.android.kt

// TeleNutz already does something like:
fun playFromTelegram(fileId: Int, playerView: PlayerView) {
    val localPath = "${cacheDir}/dvr_${fileId}.ts"
    tdEngine.send(DownloadFile(
        fileId = fileId,
        priority = 32,     // high priority
        synchronous = false,
    ))
    // TDLib writes to localPath as it downloads
    // Feed localPath to ExoPlayer
    playerView.player.setMediaItem(MediaItem.fromUri(Uri.fromFile(File(localPath))))
    playerView.player.play()
}
```

For DVR, we wrap this:

```kotlin
// New file: features/dvr/DvrPlaybackManager.kt
class DvrPlaybackManager(private val tdEngine: TelegramTdEngine) {

    fun playRecording(
        recording: Recording,
        onReady: (localPath: String) -> Unit,
    ) {
        if (recording.chunks.size > 1) {
            // Multi-chunk — build local playlist, download all
            playMultiChunkRecording(recording, onReady)
        } else {
            // Single file — download and play
            val fileId = recording.telegramFileId ?: return
            tdEngine.downloadFile(fileId) { localPath ->
                onReady(localPath)
            }
        }
    }
}
```

### 8.3 Recordings Library UI

New file: `features/dvr/DvrLibraryScreen.kt`

```
┌─────────────────────────────────┐
│ 📁 DVR Recordings          ⚙️  │
├─────────────────────────────────┤
│                                 │
│ 🔴 Recording Now (1)           │
│ ├ ESPN Live · 12:34 elapsed ⏹ │
│ │   → Uploading to Telegram:72%│
│                                 │
│ ⏰ Scheduled (3)                │
│ ├ SportsCenter · Today 6PM     │
│ ├ NFL Live · Today 8PM        │
│ └ Morning Show · Tomorrow 7AM  │
│                                 │
│ 📦 Saved to Telegram (18)      │
│ ═══════════════════════════════│
│ │ ESPN_20260803_043000      ▶ │
│ │ 45 min · 1.2 GB         ☁ 🗑│
│ │──────────────────────────────│
│ │ CNN_20260803_050000       ▶ │
│ │ 30 min · 800 MB         ☁ 🗑│
│ │──────────────────────────────│
│ │ NFL_20260802_183000_Chunk_1 │
│ │ NFL_20260802_183000_Chunk_2 │
│ │ 3h 12m · 4.1 GB     ☁ ☁ 🗑 │
└─────────────────────────────────┘
```

Status indicators:
- 🔴 Recording (red dot) — currently capturing
- ⏰ Scheduled (clock) — future recording
- ☁ In Telegram (cloud) — stored off-device
- 📥 Downloaded (download icon) — cached locally
- ▶ Play button

### 8.4 Storage Management UI

```kotlin
// New file: features/dvr/DvrStorageManager.kt
object DvrStorageManager {
    fun getLocalCacheSize(): Long     // files cached for offline
    fun getTelegramStorageCount(): Int // number of files in Telegram
    fun clearLocalCache()             // delete cached downloads
    fun deleteTelegramRecording(recordingId: String) {
        // Delete from Telegram via TDLib
        // Also remove local metadata
    }
}
```

### 8.5 DVR Hub Card

Add "DVR Recordings" card to the RobbdeezeNutz Hub:

```
┌──────────────┐
│ 📁 DVR       │
│ 18 Recordings│
│ 2 Scheduled  │
│ 1 Recording  │
└──────────────┘
```

### 8.6 New Files

```
features/dvr/DvrLibraryScreen.kt
features/dvr/DvrPlaybackManager.kt
features/dvr/DvrStorageManager.kt
features/dvr/DvrHubCard.kt
```

### 8.7 Modified Files

```
features/hub/RobbdeezeNutzHubScreen.kt  ← add DVR card
```

---

## 9. Phase 6 — TeleNutz Integration & Provider Catch-up

### 9.1 TeleNutz as a DVR Content Source

TeleNutz already streams IPTV from Telegram. This means:
- Any Telegram channel that posts video can be a DVR source
- If you have a Telegram channel that re-broadcasts shows, those are already "recorded" in Telegram
- The DVR library can show a "Browse Telegram Channels" view showing available content

### 9.2 Provider Catch-up Detection

```kotlin
// In IptvRepository
data class CatchupInfo(
    val type: CatchupType,
    val maxDays: Int,
    val urlTemplate: String,
)

enum class CatchupType { Timeshift, Flussonic, Xtream, Stalker }

fun getCatchupInfo(channel: IptvChannel): CatchupInfo? {
    return when (channel.sourceType) {
        SourceType.Xtream -> detectXtreamCatchup(channel)
        SourceType.Stalker -> detectStalkerCatchup(channel)
        SourceType.M3U -> detectM3uCatchup(channel)  // catchup-source tag
    }
}

fun buildCatchupUrl(channel: IptvChannel, targetTimeMs: Long): String? {
    // Build provider-specific catchup URL
    // e.g., Xtream: &timeshift=-3600
    // e.g., Stalker: ?utime=1691084400
}
```

### 9.3 UI: "Watch Catch-up" vs "Record to DVR"

In EPG overlay, past programs show:
- **Watch Catch-up** — if provider supports it
- **Save to Telegram DVR** — record from catch-up stream → upload to Telegram

---

## 10. Cross-Cutting Concerns

### 10.1 Telegram-Specific

| Concern | Solution |
|---|---|
| **Upload time** | Show progress; playable from temp file during upload |
| **2 GB file limit** | Auto-split into ≤1.9 GB chunks at ~90 min |
| **Rate limits** | Queue uploads, TDLib handles FloodWait |
| **No internet = no playback** | Cache recently watched recordings locally |
| **Telegram auth required** | Already logged in for TeleNutz |
| **Privacy** | Use Saved Messages (private) or private channel |
| **Multi-chunk playback** | Build local M3U playlist referencing all chunks |
| **File persistence** | Telegram doesn't delete files unless you delete the message |

### 10.2 Storage Math

| Resolution | Bitrate | Per hour | Chunks needed (2h) |
|---|---|---|---|
| 720p | ~3 Mbps | ~1.35 GB | 1 |
| 1080p | ~5 Mbps | ~2.25 GB | 2 (split at ~85 min) |
| 4K | ~15 Mbps | ~6.75 GB | 4 |
| Sports (high motion) | ~8 Mbps | ~3.6 GB | 2 |

### 10.3 Error Handling

| Error | Handling |
|---|---|
| Stream drops mid-recording | Auto-retry, extend recording window |
| Upload fails (no network) | Queue for retry; recording stays in temp |
| Telegram FloodWait | TDLib handles automatically; queue blocks |
| Storage full (local temp) | Warn user, stop recording, save partial |
| App killed during recording | Auto-restart via WorkManager on boot |
| App killed during upload | Resume upload from TDLib (it tracks progress) |
| Telegram message deleted (recording lost) | Detect missing file_id, mark as lost |

### 10.4 Platform-Specific Notes

**Android:**
- Foreground service for recording + upload
- WorkManager for scheduled recordings
- MediaStore integration for temp files (scoped storage)
- BootReceiver for missed schedule recovery
- ExoPlayer CacheDataSource for timeshift buffer

**iOS:**
- TDLib runs natively (already configured)
- BGTaskScheduler for scheduled recordings
- Limited background execution (~30s processing, ~10 min downloads)
- Recommend server-side scheduling for iOS long recordings
- AVPlayer for local playback

---

## 11. Risk Assessment & Dependencies

### Technical Risks

| Risk | Severity | Mitigation |
|---|---|---|
| HLS streams with DRM | High | Detect DRM early, show unsupported |
| Provider URL expires mid-recording | Medium | Re-fetch URL on error |
| Telegram API changes | Low | TDLib maintained by Telegram |
| Very long recordings (6h+) | Medium | Split into chunks, handle playlist |
| iOS background limits | High | Warn user; Android-first feature |

### Dependencies

| Dependency | Already In Project? | Why |
|---|---|---|
| `:tdlib-java` | ✅ Yes | File upload/download, auth |
| `TelegramTdEngine` | ✅ Yes | TDLib lifecycle |
| ExoPlayer `CacheDataSource` | ✅ Yes (via `exoplayer`) | Timeshift buffer |
| Android `WorkManager` | ✅ Yes | Scheduled recordings |
| Android `ForegroundService` | ✅ Yes | Background recording |
| Kotlin Coroutines | ✅ Yes | Async everything |

**No new external dependencies.** Everything needed is already in the project.

### What TeleNutz Already Gives Us

| Capability | TeleNutz Current | DVR Needs | Delta |
|---|---|---|---|
| TDLib client instance | Running | Reuse same instance | 0 |
| Telegram auth | Complete | Reuse same session | 0 |
| File download from Telegram | Implemented | Same logic | 0 |
| File streaming to player | Working | Same path | 0 |
| **File upload to Telegram** | **Not implemented** | New feature | **Add upload** |
| File deletion | Not needed | Delete after watching | Small |
| Chat/message ID tracking | Per-channel | Per-recording metadata | Small |

---

## 12. Appendix: Key Files & Their Roles

### New Files to Create

```
composeApp/src/commonMain/kotlin/com/nuvio/app/features/dvr/
├── DvrModels.kt                     # All DVR data types
├── DvrRepository.kt                 # Central DVR state
├── DvrTelegramConfig.kt             # Telegram chat config
├── DvrTelegramConfigScreen.kt       # Settings: where to save
├── DvrTelegramUploader.kt (expect)  # Upload to Telegram
├── DvrUploadQueue.kt                # Sequential upload queue
├── DvrChunkManager.kt              # Split large recordings
├── DvrTimeshiftService.kt (expect)  # Timeshift buffer engine
├── DvrRecordingEngine.kt (expect)   # Recording engine
├── DvrRecordingControls.kt          # Record/Stop UI controls
├── DvrSchedulingManager.kt (expect) # Schedule management
├── DvrScheduleUI.kt                # Schedule list/overlay
├── DvrPlaybackManager.kt           # Play back from Telegram
├── DvrStorageManager.kt            # Storage usage & cleanup
├── DvrLibraryScreen.kt             # Recordings library
├── DvrHubCard.kt                   # Hub card for RNutz Hub
├── DvrSettingsPage.kt              # DVR settings

composeApp/src/androidMain/kotlin/com/nuvio/app/features/dvr/
├── DvrTelegramUploader.android.kt   # TDLib upload actual
├── DvrTimeshiftService.android.kt   # CacheDataSource actual
├── DvrRecordingEngine.android.kt    # Android recording actual
├── DvrRecordingService.kt           # Foreground service
├── DvrSchedulingManager.android.kt  # WorkManager actual
├── DvrScheduledRecordingWorker.kt   # WorkManager worker
├── DvrBootReceiver.kt              # Boot re-schedule
```

### Existing Files to Modify

```
composeApp/src/commonMain/kotlin/com/nuvio/app/
├── features/player/PlayerEngine.kt           ← Timeshift methods
├── features/player/PlayerModels.kt           ← Timeshift + recording fields
├── features/player/PlayerControls.kt         ← Record/Stop/Upload-progress
├── features/player/PlayerPlaybackOverlays.kt ← Timeshift overlay
├── features/player/PlayerScreenRuntimeState.kt     ← DVR state wiring
├── features/player/PlayerScreenRuntimePlaybackActions.kt ← Record action
├── features/iptv/IptvScreen.kt              ← EPG schedule recording
├── features/iptv/IptvRepository.kt          ← Catch-up detection
├── features/hub/RobbdeezeNutzHubScreen.kt    ← DVR hub card
├── features/settings/SettingsRootPage.kt     ← DVR settings link

composeApp/src/androidMain/
├── AndroidManifest.xml                       ← Service + BootReceiver
├── features/player/PlayerEngine.android.kt   ← CacheDataSource
```

### Existing Code to Reuse (Reference)

| File | What to Steal |
|---|---|
| `DownloadsRepository.kt` | State machine pattern, progress tracking |
| `DownloadsModels.kt` | Status enum pattern, file size tracking |
| `TeleNutzStorage.android.kt` | Telegram file download → player pipeline |
| `TeleNutzStorage.kt` | Chat config persistence |
| `TelegramTdEngine.kt` | TDLib initialization + file operations |
| `IptvRepository.kt` | Channel lookup, stream URL resolution |
| `EpgParser.kt` | EPG data for scheduling |

---

## Execution Roadmap

```
Week 1:    Phase 1 — Timeshift Buffer
           DvrModels → DvrTimeshiftService → PlayerEngine integration
           Basic timeshift seek bar + Live button

Week 2:    Phase 2 — Telegram Upload Engine
           DvrTelegramUploader → DvrUploadQueue → DvrChunkManager
           Test upload to Saved Messages
           DvrTelegramConfigScreen

Week 3:    Phase 3 — Recording Pipeline
           DvrRecordingEngine → DvrRecordingService
           Record button in player → capture → upload flow
           Recording notification with progress
           End-to-end: Press Record → watch → stop → upload → play from Telegram

Week 4:    Phase 3 Continued
           Upload progress in UI
           Error handling (retry, FloodWait, network loss)
           Background recording edge cases
           Multi-chunk recordings

Week 5:    Phase 4 — Scheduled Recording
           DvrSchedulingManager → WorkManager worker
           EPG integration (long-press → schedule)
           BootReceiver
           Conflict detection

Week 6:    Phase 5 — Recordings Library
           DvrLibraryScreen
           DvrPlaybackManager (download + play from Telegram)
           DvrHubCard
           Storage manager (cache, delete)

Week 7:    Phase 6 — Polish & Provider Catch-up
           Catch-up URL detection
           TeleNutz cross-integration
           Settings page
           Polish all UIs

Week 8:    Testing & Edge Cases
           Long recordings (> 3 hours)
           Network interruptions during upload
           App kill + recovery
           iOS basic support
```

---

## Architecture Summary Diagram

```
                     ┌──────────────┐
                     │  Live Stream │
                     │ (HLS / TS)   │
                     └──────┬───────┘
                            │
              ┌─────────────┴─────────────┐
              │                           │
          ╔════▼════╗               ╔════▼══════════╗
          ║Timeshift║               ║ DVR Recording ║
          ║ Buffer  ║               ║ (local temp)  ║
          ║(ephemeral║               ╚════▲══════════╝
          ║ 30 min) ║                    │
          ╚════▲════╝              ╔══════╧══════════╗
               │                   ║ Telegram Upload ║
          ┌────┴────┐              ║   (chunked)     ║
          │ Player  │              ╚══════╤══════════╝
          │ Playback│                     │
          └─────────┘              ╔══════╧══════════╗
                                   ║ Telegram Cloud  ║
                                   ║ (Saved Messages)║
                                   ╚══════╤══════════╝
                                          │
                                    ╔═════╧══════════╗
                                    ║ DVR Library    ║
                                    ║ Browse → Play  ║
                                    ║ (via TDLib DL) ║
                                    ╚════════════════╝
```

---

*End of Analysis Report — Telegram Storage Edition*
