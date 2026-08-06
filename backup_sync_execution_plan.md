# Full Device Backup & Sync — Execution Plan

> **Project:** RNutz Nuvio (Nuvio_Robbdeeze)  
> **Goal:** Share full app backups between devices — IPTV sources, addons, plugins, settings, everything  
> **Date:** 2026-08-03

---

## Table of Contents

1. [What Already Exists](#1-what-already-exists)
2. [The Gap: What's Missing](#2-the-gap-whats-missing)
3. [Approach Comparison](#3-approach-comparison)
4. [Recommended Approach: Telegram Relay + Supabase Realtime](#4-recommended-approach-telegram-relay--supabase-realtime)
5. [Phase 1 — TeleBackup Engine](#5-phase-1--telebackup-engine)
6. [Phase 2 — Backup Lifecycle & Versioning](#6-phase-2--backup-lifecycle--versioning)
7. [Phase 3 — Cross-Device Restore Flow](#7-phase-3--cross-device-restore-flow)
8. [Phase 4 — Supabase Full-Config Sync (Optional Follow-up)](#8-phase-4--supabase-full-config-sync-optional-follow-up)
9. [AlterSend Approach (Alternative)](#9-altersend-approach-alternative)
10. [WhatsApp/Direct Share Approach](#10-whatsappdirect-share-approach)
11. [Cross-Cutting Concerns](#11-cross-cutting-concerns)
12. [Appendix: Data Coverage Map](#12-appendix-data-coverage-map)

---

## 1. What Already Exists

The project already has **three independent backup/sync systems**. This is the foundation.

### 1.1 BackupManager — Full Export/Import

**File:** `commonMain/.../features/backup/BackupManager.kt`

This is a comprehensive full-app backup system. It exports every data category as a single JSON blob:

| Section | What's Exported |
|---|---|
| `profile` | All profiles, display names, avatars |
| `avatar` | Profile avatar data |
| `collection` | User collections and their items |
| `collection_mobile` | Collection mobile layout settings |
| `continue_watching_prefs` | Continue watching preferences |
| `downloads` | Download metadata (not files themselves) |
| `home_catalog` | Home screen catalog settings |
| `meta_screen` | Meta/details screen settings |
| `poster_card_style` | Poster card style settings |
| `card_depth_style` | Card depth/shadow style |
| `search_history` | Search history |
| `episode_release_notifications` | Episode release notification prefs |
| `trakt_auth` | Trakt auth tokens & credentials |
| `trakt_settings` | Trakt sync settings |
| `trakt_library` | Trakt library cache |
| `theme_settings` | Theme, accent colors, AMOLED mode |
| `player_settings` | All player settings (engines, audio, subs, etc.) |
| `stream_badge_settings` | Stream badge filter rules |
| `debrid_settings` | Debrid provider settings |
| `tmdb_settings` | TMDB API settings |
| `mdblist_settings` | MDBList settings |
| `trakt_comments_settings` | Trakt comments preferences |
| **`iptv_settings`** | **ALL IPTV sources — M3U URLs, Xtream accounts, Stalker accounts, EPG sources, favorites, history, channel groups** |
| **`iptv_epg_cache`** | Cached EPG data |
| `anonymous_user_id` | Anonymous auth identifier |
| `client_id` | Sync client identity |
| `profile_pin_cache_N` | Profile PIN cache per profile |
| `watched_N` | Watched items per profile |
| `watch_progress_N` | Watch progress per profile |
| `library_N` | Library items per profile |
| **`addon_urls_N`** | Installed addon URLs per profile |
| **`addon_enabled_N`** | Addon enabled states per profile |

**This is already a complete backup.** It's just manual only — export to file, transfer the file, import.

### 1.2 BackupScreen — UI

**File:** `commonMain/.../features/backup/BackupScreen.kt`

Already has:
- Export button → saves JSON to device
- Import button → file picker → reads JSON → restores
- NuvioSync format converter

### 1.3 Supabase ProfileSettingsSync — Real-time Settings Sync

**File:** `commonMain/.../core/sync/ProfileSettingsSync.kt`

Already auto-syncs (push + pull) via Supabase PostgREST RPC:
- Theme settings
- Poster card / card depth styles
- Player settings
- Stream badge settings
- Debrid settings
- TMDB / MDBList settings
- Meta screen settings
- Collection mobile settings
- Continue watching preferences
- Trakt settings and comments
- Episode release notifications

This is **debounced (1.5s) and real-time** — change a setting on one device, it shows up on another.

### 1.4 SyncManager — Full Supabase Pull

**File:** `commonMain/.../core/sync/SyncManager.kt`

Pulls from Supabase on demand: addons, plugins, profile settings, trakt credentials, library, collections, home catalog.

### 1.5 Supabase Realtime Invalidation

**File:** `commonMain/.../core/sync/RealtimeSyncInvalidationService.kt`

Receives Supabase Realtime channel events telling the app to pull updates for specific surfaces.

### 1.6 TDLib (Telegram Client)

- `:tdlib-java` module
- `TelegramTdEngine.kt` — full Telegram client lifecycle
- `TelegramAuth.kt` — phone/QR authentication
- TeleNutz uploads/downloads files from Telegram already

---

## 2. The Gap: What's Missing

| Capability | Status |
|---|---|
| **Export full backup as JSON** | ✅ Done (`BackupManager.kt`) |
| **Import full backup from JSON** | ✅ Done (`BackupManager.kt`) |
| **Settings auto-sync (live)** | ✅ Partial (via Supabase, limited data types) |
| **IPTV sources sync between devices** | ❌ Not synced — only in manual backup |
| **Addon URLs/enabled states sync** | ❌ Pulled from Supabase per profile but not pushed |
| **One-tap "restore from other device"** | ❌ Not built |
| **Backup stored in cloud** | ❌ Manual file only |
| **Auto-backup on schedule** | ❌ Not built |
| **Selective restore (IPTV only, etc.)** | ❌ All-or-nothing import |

---

## 3. Approach Comparison

### 3.1 Options Matrix

| Approach | Mechanism | Async? | Requires Both Online? | Complexity | Privacy |
|---|---|---|---|---|---|
| **Telegram Relay** | Export → upload to Saved Messages → download on other device | ✅ Yes | ❌ No | Low (TDLib exists) | Medium (Telegram servers) |
| **Supabase Sync** | Extend existing PostgREST RPC to include all data types | ✅ Yes | ❌ No | Medium (extend existing) | Low-Medium (Supabase) |
| **AlterSend P2P** | Use AlterSend app to transfer backup file manually | ❌ Manual | ✅ Yes | Very Low (manual) | High (direct P2P) |
| **AlterSend Embedded** | Rewrite Hyperswarm protocol in Kotlin/JVM | ✅ Yes | ✅ Yes | Very High | High |
| **WhatsApp/Share** | System share sheet → any app | ❌ Manual | ❌ No | Trivial (already works) | Varies |
| **Local Wi-Fi Direct** | Android Wi-Fi P2P | ✅ Yes | ✅ Yes | High | High |

### 3.2 Recommendation

**Primary: Telegram Relay — lowest effort, biggest win.** TDLib already works, BackupManager already works. The bridge is ~1 week of work.

**Secondary (after): Extend Supabase sync** to cover IPTV and addons for real-time multi-device. Higher effort but better UX.

---

## 4. Recommended Approach: Telegram Relay + Supabase Realtime

### Architecture

```
┌──────────────┐          ┌──────────────────┐          ┌──────────────┐
│  Device A    │          │  Telegram Cloud   │          │  Device B    │
│              │          │  (Saved Messages) │          │              │
│  BackupMgr   │          │                   │          │  TDLib       │
│  export()    │─────────►│  backup_20260803  │◄─────────│  download()  │
│  ───────────►│  TDLib  │  .json            │  TDLib   │  ───────────►│
│  TDLib       │  upload  │                   │  download │  BackupMgr   │
│  sendMessage │          │  ┌─────────────┐  │          │  import()    │
│  (Document)  │          │  │ Changelog    │  │          │              │
│              │          │  │ msgs:        │  │          │              │
│              │          │  │ "Pushed new  │  │          │              │
│              │          │  │ IPTV source" │  │          │              │
│              │          │  └─────────────┘  │          │              │
└──────────────┘          └──────────────────┘          └──────────────┘
```

### 4.1 Dual Sync Strategy

| Sync Type | Backend | Data | Frequency |
|---|---|---|---|
| **Live settings sync** | Supabase PostgREST | Theme, player, debrid, TMDB, etc. | Real-time (debounced 1.5s) |
| **Full config backup** | Telegram Saved Messages | IPTV sources, addons, plugins, library, progress | On-demand / periodic |
| **Realtime invalidation** | Supabase Realtime | "New data available" signal | Instant |

This means:
- **Settings changes** propagate instantly between devices via Supabase (already works)
- **Full backup/restore** happens via Telegram when you switch devices or want a snapshot
- **Restore flow:** Send backup to Telegram → Install app on new device → Login → Pull from Telegram → Import

---

## 5. Phase 1 — TeleBackup Engine

### 5.1 What It Does

Wraps the existing `BackupManager` with Telegram upload/download:

```
BackupManager.export() → JSON string → DvrTelegramUploader
    → TDLib sendMessage (document) to Saved Messages
    → Store message_id + file_id locally

On restore:
    → Query Saved Messages for backup documents
    → TDLib downloadFile → JSON string
    → BackupManager.import()
```

### 5.2 New File: `TeleBackupEngine.kt`

```kotlin
// New file: features/backup/TeleBackupEngine.kt (expect)
// Leverages existing DvrTelegramUploader from DVR plan

class TeleBackupEngine(private val tdEngine: TelegramTdEngine) {

    /**
     * Exports full backup and uploads to Telegram.
     * Returns metadata for the uploaded backup.
     */
    suspend fun pushBackup(
        label: String = "",           // optional label like "Before IPTV reconfig"
        onProgress: (Float) -> Unit,
    ): BackupUploadResult {
        // 1. Generate backup JSON
        val json = BackupManager.exportBackup(
            appVersion = AppVersionConfig.VERSION_NAME
        )

        // 2. Write to temp file
        val tempFile = createTempFile("nuvio_backup_", ".json")
        tempFile.writeText(json)

        // 3. Upload to Saved Messages via TDLib
        val result = uploadFileToTelegram(
            localPath = tempFile.absolutePath,
            fileName = buildBackupFilename(label),
            caption = buildBackupCaption(label),
        )

        // 4. Store backup metadata locally
        TeleBackupStorage.recordPush(
            TeleBackupRecord(
                id = result.messageId.toString(),
                telegramMessageId = result.messageId,
                telegramChatId = result.chatId,
                telegramFileId = result.fileId,
                exportedAtMs = System.currentTimeMillis(),
                appVersion = AppVersionConfig.VERSION_NAME,
                label = label,
                sizeBytes = json.length.toLong(),
                dataTypes = getExportedDataTypes(),
            )
        )

        // 5. Clean up temp file
        tempFile.delete()

        return result
    }

    /**
     * Finds the most recent backup in Saved Messages and imports it.
     */
    suspend fun pullLatestBackup(
        onProgress: (Float) -> Unit,
    ): BackupManager.ImportResult {
        // 1. Search for backup documents in Saved Messages
        val records = TeleBackupStorage.getPushHistory()
        if (records.isEmpty()) {
            // First time — search Saved Messages for existing backups
            val found = searchSavedMessagesForBackups()
            TeleBackupStorage.mergeFound(found)
        }

        // 2. Pick latest
        val latest = TeleBackupStorage.getLatest() ?: return ImportResult(
            success = false, error = "No backup found in Telegram"
        )

        // 3. Download from Telegram via TDLib
        val localPath = downloadFileFromTelegram(
            fileId = latest.telegramFileId,
            onProgress = onProgress,
        ) ?: return ImportResult(
            success = false, error = "Failed to download backup"
        )

        // 4. Read and import
        val json = File(localPath).readText()
        val result = BackupManager.importBackup(json)

        // 5. Cleanup
        File(localPath).delete()

        return result
    }

    /**
     * Lists all backups found in Telegram (for manual selection).
     */
    suspend fun listRemoteBackups(): List<TeleBackupRecord> {
        return TeleBackupStorage.getAll().sortedByDescending { it.exportedAtMs }
    }
}
```

### 5.3 Backup Filename Convention

```
NuvioBackup_20260803_043000.json                ← plain
NuvioBackup_20260803_043000_Before_IPTV_reconfig.json  ← with label
```

Caption on the Telegram message:
```
📦 Nuvio Full Backup
📅 2026-08-03 04:30 UTC
📱 App v0.2.20
📋 42 sections • IPTV, Addons, Settings, Library, Progress
```

### 5.4 Metadata Storage

Small local DB to track what's where in Telegram:

```kotlin
data class TeleBackupRecord(
    val id: String,
    val telegramMessageId: Long,
    val telegramChatId: Long,
    val telegramFileId: Int,
    val telegramUniqueId: String,
    val exportedAtMs: Long,
    val appVersion: String,
    val label: String,
    val sizeBytes: Long,
    val dataTypes: List<String>,  // ["iptv", "addons", "settings", "library", ...]
)

object TeleBackupStorage {
    fun recordPush(record: TeleBackupRecord)
    fun getPushHistory(): List<TeleBackupRecord>
    fun getLatest(): TeleBackupRecord?
    fun getAll(): List<TeleBackupRecord>
    fun mergeFound(records: List<TeleBackupRecord>)
    fun deleteRecord(id: String)
}
```

### 5.5 New Files

```
features/backup/TeleBackupEngine.kt (expect)
features/backup/TeleBackupStorage.kt (expect)
features/backup/TeleBackupModels.kt
features/backup/TeleBackupScreen.kt      ← New UI for backup management
androidMain/.../backup/TeleBackupEngine.android.kt (actual — TDLib upload/download)
androidMain/.../backup/TeleBackupStorage.android.kt (actual)
iosMain/.../backup/TeleBackupEngine.ios.kt (actual)
iosMain/.../backup/TeleBackupStorage.ios.kt (actual)
```

### 5.6 Modified Files

```
features/backup/BackupScreen.kt   ← Add "Back up to Telegram" / "Restore from Telegram" buttons
features/settings/SettingsRootPage.kt ← Add backup sync section
```

---

## 6. Phase 2 — Backup Lifecycle & Versioning

### 6.1 Backup Types

| Type | Trigger | Retention |
|---|---|---|
| **Manual** | User taps "Back up now" | Keep latest 10 |
| **Pre-update** | App update detected (version changed) | Keep 1 pre-update |
| **On-signout** | Before logging out / wiping data | Keep 1 signout |
| **Scheduled** | Periodic (every 7 days) | Keep latest 5 |

### 6.2 Backup Versioning

The Telegram message serves as the version marker. Each backup's caption includes the `exportedAtMs` timestamp. The app can diff versions by comparing `TeleBackupRecord.exportedAtMs`.

**Changelog approach:** After each push, also send a small "changelog" message to the same chat:

```
📋 Backup Changelog
─────────────────
🕐 2026-08-03 04:30 — Full backup (42 sections)
🕐 2026-08-02 22:15 — IPTV sources updated
🕐 2026-08-01 10:00 — Addons changed: +3, -1
```

These changelogs let the user browse what changed without downloading the full backup.

### 6.3 Selective Backup

Add granular export to `BackupManager`:

```kotlin
// Add to BackupManager
fun exportBackup(
    appVersion: String = "",
    includeIptv: Boolean = true,
    includeAddons: Boolean = true,
    includeSettings: Boolean = true,
    includeLibrary: Boolean = true,
    includeProgress: Boolean = true,
    includeCollections: Boolean = true,
    includeDownloads: Boolean = false,  // metadata only
): String
```

The Telegram UI lets users choose what to back up:

```
┌──────────────────────────────┐
│ 📦 Backup to Telegram        │
│                              │
│ ☑ IPTV Sources (3 M3U, 2    │
│    Xtream, 1 Stalker)       │
│ ☑ Addons (12 installed)     │
│ ☑ Settings (theme, player,  │
│    debrid, etc.)            │
│ ☑ Library (247 items)       │
│ ☑ Watch Progress             │
│ ☑ Collections (8)           │
│ ☐ Downloads (metadata only) │
│                              │
│ Label: [Before reconfig____] │
│                              │
│ [📤 Back up to Telegram]    │
└──────────────────────────────┘
```

### 6.4 Pre-Update Safety Net

```kotlin
// In App.kt or a startup observer
fun onAppVersionChanged(oldVersion: String, newVersion: String) {
    if (oldVersion.isNotBlank() && oldVersion != newVersion) {
        // Auto-backup before update takes effect
        scope.launch {
            TeleBackupEngine.pushBackup(
                label = "Pre-update $oldVersion → $newVersion",
            )
        }
    }
}
```

---

## 7. Phase 3 — Cross-Device Restore Flow

### 7.1 New Device Setup Flow

When a user installs Nuvio on a new device and logs into Telegram:

```
1. First launch → Auth → Telegram login (restores TeleNutz)
2. App detects: no IPTV sources configured
   → "Found backups in Telegram. Restore?"
3. Shows list of available backups:
   ┌─────────────────────────────────────┐
   │ 📦 Found 5 backups in Telegram     │
   │                                     │
   │ [Latest] 2026-08-03 04:30 UTC       │
   │   App v0.2.20 · 42 sections · 247KB │
   │─────────────────────────────────────│
   │ 2026-08-02 22:15 UTC                │
   │   "Before IPTV reconfig" · 38 sect  │
   │─────────────────────────────────────│
   │ 2026-08-01 10:00 UTC                │
   │   "Pre-update v0.2.19 → v0.2.20"   │
   │                                     │
   │ [Restore Latest] [Browse All] [Skip]│
   └─────────────────────────────────────┘
4. User taps "Restore Latest"
5. Download from Telegram → import via BackupManager
6. App restarts with all IPTV sources, addons, settings, library, progress
```

### 7.2 Backup Discovery

On first launch (or when no backups exist locally), search Saved Messages:

```kotlin
suspend fun discoverBackupsInTelegram(): List<TeleBackupRecord> {
    // Use TDLib to search messages in Saved Messages chat
    val messages = tdEngine.send(SearchChatMessages(
        chatId = myUserId,         // Saved Messages uses your own user ID
        query = "NuvioBackup",     // search for backup files
        filter = SearchMessagesFilterDocument(),
        limit = 50,
    ))

    return messages.map { msg ->
        TeleBackupRecord(
            telegramMessageId = msg.id,
            telegramChatId = msg.chatId,
            telegramFileId = msg.document.document.id,
            telegramUniqueId = msg.document.document.uniqueId,
            // ... parse caption for metadata
        )
    }
}
```

### 7.3 In-App Sync Trigger

Add a "Sync from other device" button in the backup screen that:
1. Checks Telegram for backups
2. Downloads the latest
3. Imports it
4. Shows diff: "Updated 3 IPTV sources, 2 addons"

### 7.4 New Files

```
features/backup/TeleBackupRestoreFlow.kt  ← New device setup wizard
features/backup/TeleBackupDiscovery.kt    ← Search Saved Messages for backups
```

### 7.5 Modified Files

```
App.kt                                    ← First-launch backup detection
features/backup/BackupScreen.kt           ← Restore from Telegram options
```

---

## 8. Phase 4 — Supabase Full-Config Sync (Optional Follow-up)

### 8.1 What's Already Synced (via Supabase)

| Data | Synced? | Mechanism |
|---|---|---|
| Theme settings | ✅ | ProfileSettingsSync RPC |
| Player settings | ✅ | ProfileSettingsSync RPC |
| Debrid settings | ✅ | ProfileSettingsSync RPC |
| TMDB settings | ✅ | ProfileSettingsSync RPC |
| Addons | ⚠️ Pull only | AddonRepository.pullFromServer() |
| Plugins | ⚠️ Pull only | PluginRepository.pullFromServer() |
| Library | ⚠️ Pull only | LibraryRepository.pullFromServer() |
| IPTV sources | ❌ | Not synced |
| Collections | ⚠️ Pull only | CollectionSyncService.pullFromServer() |

### 8.2 Extending IPTV Source Sync

The aim is to add push/pull for IPTV sources so they sync like settings do.

**Supabase schema addition:**

```sql
-- Already has a sync_push_profile_settings_blob RPC
-- Add IPTV-specific fields to the blob or create a separate RPC

-- Option A: Add iptv to existing MobileProfileSettingsBlob
-- Option B: Separate RPC (sync_push_iptv_settings)

-- Option A is simpler since the infra already exists:
```

Extend `MobileProfileSettingsBlob`:

```kotlin
@Serializable
private data class MobileProfileSettingsBlob(
    val version: Int = 4,  // bump version
    val features: MobileProfileSettingsFeatures = MobileProfileSettingsFeatures(),
)

@Serializable
private data class MobileProfileSettingsFeatures(
    // ... existing fields ...
    
    // NEW:
    @SerialName("iptv_settings_payload")
    val iptvSettingsPayload: String = "",
    
    @SerialName("addon_urls_payload")
    val addonUrlsPayload: String = "",
    
    @SerialName("addon_enabled_payload")
    val addonEnabledPayload: String = "",
)
```

Add to `exportSettingsBlob()`:
```kotlin
iptvSettingsPayload = IptvStorage.loadSettings().orEmpty().trim(),
addonUrlsPayload = json.encodeToString(
    AddonStorage.loadInstalledAddonUrls(profileId)
),
addonEnabledPayload = json.encodeToString(
    AddonStorage.loadAddonEnabledStates(profileId)
),
```

Add to `applyRemoteBlob()`:
```kotlin
if (blob.features.iptvSettingsPayload.isNotBlank()) {
    IptvStorage.saveSettings(blob.features.iptvSettingsPayload)
}
if (blob.features.addonUrlsPayload.isNotBlank()) {
    // parse and save
}
```

### 8.3 Modified Files

```
core/sync/ProfileSettingsSync.kt          ← Extend blob with IPTV + addons
core/sync/SyncManager.kt                  ← Add IPTV pull step
features/iptv/IptvStorage.kt             ← Already has load/save
features/addons/AddonStorage.kt           ← Already has load/save
```

### 8.4 Tradeoffs

| Pro | Con |
|---|---|
| Real-time sync (1.5s debounce) | IPTV credentials stored on Supabase |
| No manual step needed | Requires Supabase RPC changes |
| Same mechanism as settings | Larger push payloads |
| Selective: change IPTV on one device, other sees it | Credentials in cloud = privacy concern |

Since IPTV credentials are **sensitive** (your provider login), making them opt-in with a warning is recommended:

```
┌──────────────────────────────────┐
│ ⚠️ Sync IPTV Sources to Cloud   │
│                                  │
│ Your IPTV provider credentials   │
│ will be stored on our Supabase   │
│ server. Only your devices can    │
│ pull them, but the data passes   │
│ through our cloud.               │
│                                  │
│ [Enable IPTV Cloud Sync] [Cancel]│
└──────────────────────────────────┘
```

---

## 9. AlterSend Approach (Alternative)

### 9.1 What It Is

AlterSend (`altersend-main/` in your projects folder) is an open-source P2P file transfer app built on:
- **Electron** (desktop) + **React Native / Expo** (mobile)
- **Hyperswarm DHT** for peer discovery
- **Noise protocol** for end-to-end encryption
- **Hyperdrive** + custom `drive` channel for chunked file transfer

### 9.2 Integration Options

#### Option A: Manual Transfer (Trivial, ▲ Best for now)

**How it works:**
1. Open Nuvio → Backup → Export
2. Share via AlterSend (system share sheet → AlterSend)
3. On other device → AlterSend receives file
4. Open in Nuvio → Backup → Import

**Effort:** Zero. System share sheet handles this.

**Why this works already:**
- Android has a system share sheet
- AlterSend registers as a file receiver on both platforms
- Nuvio's `BackupScreen` already uses `ActivityResultContracts.OpenDocument` for import

#### Option B: Embed Hyperswarm in Nuvio (Very High Effort)

**What it would take:**
- Hyperswarm is a Node.js library built on `@hyperswarm/network` (UDP DHT, TCP)
- No Kotlin/JVM port exists
- Would need to either:
  - Run a Node.js process alongside the app (possible via J2V8 or similar)
  - Rewrite the DHT protocol in Kotlin (months of work)
  - Use the Rust implementation (hyperswarm-rs) via JNI

**Not recommended** unless you have a strong reason to avoid any cloud intermediary.

#### Option C: Use as an Intent/Deep Link (Low Effort)

```kotlin
// In BackupScreen, add a "Send via AlterSend" button
val sendIntent = Intent(Intent.ACTION_SEND).apply {
    type = "application/json"
    putExtra(Intent.EXTRA_STREAM, backupFileUri)
    `package` = "com.altersend.mobile"  // target AlterSend directly
}
startActivity(Intent.createChooser(sendIntent, "Send backup via..."))
```

This skips the file picker and goes straight to share.

### 9.3 Verdict

**Use AlterSend as a manual transport** — export backup, share via AlterSend, import on other device. Zero code changes needed. It's a better UX than email/Drive because it's P2P and works with large files, but it's still manual.

For automatic sync, **Telegram is the better path** because TDLib is already in the app.

---

## 10. WhatsApp/Direct Share Approach

### 10.1 What It Does

The simplest possible approach — just use the system share sheet. The app already has a file picker for import.

### 10.2 How to Enable It

```kotlin
// In BackupScreen, after export succeeds:
fun shareBackup(jsonText: String) {
    val tempFile = File(context.cacheDir, "nuvio_backup_${timestamp()}.json")
    tempFile.writeText(jsonText)
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
    
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/json"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share backup"))
}
```

**Pros:**
- Works with any app (WhatsApp, Telegram as chat, email, Drive, AlterSend)
- Zero new code beyond the share button
- User chooses the transport

**Cons:**
- Manual every time
- No auto-backup
- No versioning

### 10.3 Recommendation

**Add this as the first step** — it's a few lines of code and gives immediate value while the Telegram engine is being built.

---

## 11. Cross-Cutting Concerns

### 11.1 Privacy

| Data | Sensitivity | Recommendation |
|---|---|---|
| IPTV credentials | 🔴 High | Encrypt before upload; warn user |
| Trakt tokens | 🟡 Medium | Already in backup (rotate on import) |
| Debrid API keys | 🔴 High | Encrypt before upload |
| Watch history | 🟢 Low | Fine to sync |
| Settings | 🟢 Low | Fine to sync |

**Encryption layer:** Before uploading to Telegram, encrypt the JSON with a device-generated key:

```kotlin
suspend fun pushBackupEncrypted(password: String) {
    val json = BackupManager.exportBackup()
    val encrypted = encryptAes(json, password)  // AES-256-GCM
    uploadToTelegram(encrypted)
}

suspend fun pullAndDecrypt(password: String): BackupManager.ImportResult {
    val encrypted = downloadFromTelegram()
    val json = decryptAes(encrypted, password)
    return BackupManager.importBackup(json)
}
```

This gives end-to-end encryption even though the backup passes through Telegram's servers.

### 11.2 Backup Size

| Data | Typical Size |
|---|---|
| Full backup (no EPG cache) | 50-300 KB |
| With EPG cache | 1-5 MB |
| With downloads metadata | + a few KB |
| **Without EPG cache (recommended)** | **50-300 KB** |

Telegram handles files up to 2 GB, and these backups are tiny in comparison. Including or excluding EPG cache doesn't matter for Telegram but matters for speed.

### 11.3 Conflict Resolution

When restoring on a device that already has data:

| Scenario | Strategy |
|---|---|
| First-time restore on new device | Import everything, no conflicts |
| Restore over existing data | Last-write-wins (backup overwrites current) |
| Partial restore (IPTV only) | Only overwrite selected sections |
| Merge (keep both) | Rename conflicts, keep both sets |

### 11.4 IPTV Credential Validity After Restore

IPTV source URLs and credentials can expire. After restore:
- M3U URLs: verify with HEAD request
- Xtream accounts: test API endpoint
- Stalker: test MAC binding
- Show warnings if any sources are stale

### 11.5 Platform Differences

**Android:**
- TDLib fully functional for upload/download
- Backup file can be written to temp directory
- FileProvider for share sheet
- WorkManager for scheduled backups

**iOS:**
- TDLib works but with stricter background limits
- Backup file in app's Documents directory
- Share sheet via UIActivityViewController
- BGTaskScheduler for scheduled backups

---

## 12. Appendix: Data Coverage Map

### What BackupManager Currently Covers

```
✅ profile                  → Profile names, avatars, display settings
✅ avatar                   → Avatar image data
✅ collection               → User-created content collections
✅ collection_mobile        → Collection layout settings
✅ continue_watching_prefs  → Continue watching behavior
✅ downloads                → Download metadata (what's been downloaded)
✅ home_catalog             → Home screen catalog configuration
✅ meta_screen              → Detail screen settings
✅ poster_card_style        → Poster card appearance
✅ card_depth_style         → Card shadow/depth
✅ search_history           → Recent searches
✅ episode_release_notifications → Release alert prefs
✅ trakt_auth               → Trakt login tokens
✅ trakt_settings           → Trakt sync configuration
✅ trakt_library            → Trakt library cache
✅ theme_settings           → Theme, accent color, AMOLED
✅ player_settings          → Player engine, audio, subs, resize
✅ stream_badge_settings    → Stream badge filters
✅ debrid_settings          → Debrid provider config + API keys
✅ tmdb_settings            → TMDB API key
✅ mdblist_settings         → MDBList API key
✅ trakt_comments_settings  → Comments preferences
✅ iptv_settings            → ALL M3U/Xtream/Stalker sources + groups + favorites + history
✅ iptv_epg_cache           → Cached EPG data (optional)
✅ anonymous_user_id        → Auth identity
✅ client_id                → Sync client identity
✅ profile_pin_cache       → Per-profile PIN cache
✅ watched                  → Watched items per profile
✅ watch_progress           → Watch progress per profile
✅ library                  → Library items per profile
✅ addon_urls              → Installed addon URLs per profile
✅ addon_enabled           → Addon enabled/disabled states per profile
```

### What ProfileSettingsSync Currently Covers (Auto-Sync)

```
✅ theme_settings           → Theme selection
✅ amoled_mode              → AMOLED dark mode
✅ liquid_glass_tab_bar     → Tab bar style
✅ poster_card_style        → Poster card settings
✅ card_depth_style         → Card depth settings
✅ player_settings          → All playback settings
✅ stream_badge_settings    → Stream badge config
✅ debrid_settings          → Debrid settings
✅ tmdb_settings            → TMDB settings
✅ mdblist_settings         → MDBList settings
✅ meta_screen              → Detail screen settings
✅ collection_mobile        → Collection mobile layout
✅ continue_watching        → Continue watching prefs
✅ trakt_settings           → Trakt settings
✅ trakt_comments           → Comments preferences
✅ episode_release_alerts   → Release notification pref
```

### What Would Be Added with Phase 4 (Supabase Extension)

```
⬜ iptv_settings            → M3U/Xtream/Stalker sources (opt-in)
⬜ addon_urls               → Installed addon URLs
⬜ addon_enabled            → Addon enabled states
```

---

## Summary Execution Roadmap

```
Week 1:   Phase 1 — TeleBackup Engine
          TeleBackupEngine.kt (upload/download via TDLib)
          TeleBackupStorage.kt (metadata persistence)
          Integrate with existing BackupManager

Week 2:   Phase 1 Continued + Phase 2
          Backup lifecycle (manual, pre-update, scheduled)
          Selective backup (choose what to include)
          Backup screen UI → "Back up to Telegram" / "Restore from Telegram"
          Share sheet integration (works with AlterSend, WhatsApp, etc.)

Week 3:   Phase 3 — Cross-Device Restore Flow
          Backup discovery in Saved Messages
          New device setup wizard
          In-app sync trigger
          Encryption layer (AES-256-GCM for sensitive data)

Week 4:   Phase 4 — Supabase Full-Config Sync (Optional)
          Extend MobileProfileSettingsBlob with IPTV + addons
          Privacy warning for IPTV credential sync
          Test multi-device IPTV sync

Week 5:   Polish & Edge Cases
          Backup diff display
          Stale credential detection after restore
          Error handling (upload fails, corrupt backup, version mismatch)
          iOS compatibility
```

---

## Quick Win (Do Today)

The absolute fastest path to device-to-device backup sharing requires **zero code changes** to the core app. Just add a share button to the existing `BackupScreen`:

```kotlin
// 5 lines in BackupScreen.kt, after export succeeds:
val shareIntent = Intent(Intent.ACTION_SEND).apply {
    type = "application/json"
    putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", backupFile))
}
context.startActivity(Intent.createChooser(shareIntent, "Send backup"))
```

This instantly lets you:
- Share via **AlterSend** (P2P, encrypted, no size limit)
- Share via **Telegram** (to Saved Messages or any chat)
- Share via **WhatsApp**, email, Drive, etc.

From there, the Telegram Relay approach in Phase 1 makes it automatic — one-tap backup/restore without leaving the app.

---

*End of Backup Sync Execution Plan*
