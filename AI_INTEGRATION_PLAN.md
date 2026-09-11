# AI Integration Plan — freellmapi (Auto-Select Model)

## Overview

Integrate freellmapi's free cloud LLMs into Nuvio for AI-powered features across all hubs.
Model selection is handled by freellmapi's auto-router — no user-facing model picker.

**Key decisions:**
- Enable toggle: ON by default
- Global AI Chat FAB: bottom-right floating, accessible from all hubs
- AI channel search for events: inline search bar in SportNutz event detail → LIVE tab
- Scope: SportNutz first, then expand to all hubs + main app

---

## Architecture

### New Files

```
src/commonMain/kotlin/com/nuvio/app/features/ai/
├── AiModels.kt                  — shared data classes
├── AiSettingsStore.kt           — expect/actual persisted settings
├── AiClient.kt                  — thin wrapper around httpPostJson → freellmapi
├── AiStore.kt                   — conversation state, rate limiting, loading
└── AiChannelSearchStore.kt      — expect/actual event-specific AI channel search

src/commonMain/kotlin/com/nuvio/app/features/settings/
└── AiSettingsPage.kt            — Settings UI page (URL + test connection)

src/androidMain/kotlin/com/nuvio/app/features/ai/
├── AiSettingsStore.android.kt
├── AiStore.android.kt
└── AiChannelSearchStore.android.kt

src/iosMain/kotlin/com/nuvio/app/features/ai/
├── AiSettingsStore.ios.kt
├── AiStore.ios.kt
└── AiChannelSearchStore.ios.kt
```

### Modified Files

```
src/commonMain/kotlin/com/nuvio/app/features/sports/SportsScreen.kt
  — AI search bar in event detail LIVE tab
  — AI channel search results display

src/commonMain/kotlin/com/nuvio/app/features/hub/RobbdeezeNutzHubScreen.kt
  — Global AI Chat FAB (bottom-right floating)
  — AiChatSheet composable

src/commonMain/kotlin/com/nuvio/app/features/settings/SettingsRootPage.kt
  — Add "AI & Chat" navigation row in General section

src/commonMain/kotlin/com/nuvio/app/features/settings/AdvancedSettingsPage.kt
  — (no change — AI enable toggle lives in AI & Chat settings page)
```

---

## Phase 1: Foundation (Core AI Infrastructure)

### 1a. AiModels.kt

```kotlin
package com.nuvio.app.features.ai

enum class AiRole { System, User, Assistant }

data class AiMessage(val role: AiRole, val content: String)

data class AiResponse(
    val text: String,
    val model: String? = null,
    val latencyMs: Long = 0L,
)

data class AiUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val conversation: List<AiMessage> = emptyList(),
)

data class AiChannelSearchResult(
    val query: String,
    val explanation: String,
    val filteredChannels: List<MatchedChannel>,
    val suggestedPortals: List<String>,
    val latencyMs: Long = 0L,
)
```

### 1b. AiSettingsStore.kt (commonMain expect)

```kotlin
internal expect object AiSettingsStore {
    fun isEnabled(): Boolean           // default: true
    fun setEnabled(enabled: Boolean)
    fun getBaseUrl(): String           // default: "https://freellmapi.com/v1"
    fun setBaseUrl(url: String)
}
```

**Android actual** — `SharedPreferences` with key `"nuvio_ai_settings"`.

**iOS actual** — `UserDefaults` with key `"nuvio_ai_settings"`.

### 1c. AiClient.kt

```kotlin
object AiClient {
    const val DEFAULT_BASE_URL = "https://freellmapi.com/v1"

    suspend fun chatCompletion(
        messages: List<AiMessage>,
        systemPrompt: String = "",
        baseUrl: String = AiSettingsStore.getBaseUrl(),
    ): AiResponse

    suspend fun chatCompletionStreaming(
        messages: List<AiMessage>,
        systemPrompt: String = "",
        onChunk: (String) -> Unit,
        baseUrl: String = AiSettingsStore.getBaseUrl(),
    ): AiResponse
}
```

**Implementation:**
- Builds JSON body with `model = "auto"`, `temperature = 0.7`, `max_tokens = 1024`
- Calls `httpPostJson("$baseUrl/chat/completions", body)` (reuses existing OkHttp layer)
- Parses response via `kotlinx.serialization`
- For streaming: reads chunks from OkHttp response body directly

**System prompt (injected automatically):**
```
You are Nuvio's AI assistant. You help users discover sports content, IPTV channels,
movies, music, and podcasts. Be concise, use bullet points for lists, and reference
the user's region-aware channel preferences when relevant.
```

### 1d. AiStore.kt (commonMain object)

```kotlin
object AiStore {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _uiState = MutableStateFlow(AiUiState())
    val uiState: StateFlow<AiUiState> = _uiState.asStateFlow()

    var conversationHistory: MutableList<AiMessage> = mutableListOf(
        AiMessage(AiRole.System, /* system prompt */)
    )

    private val lastRequestTime = AtomicLong(0L)
    private const val RATE_LIMIT_MS = 1000L

    suspend fun sendUserMessage(text: String): Flow<String> = flow {
        throttle()
        conversationHistory.add(AiMessage(AiRole.User, text))
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        try {
            val response = AiClient.chatCompletionStreaming(
                messages = conversationHistory.toList(),
                onChunk = { chunk -> emit(chunk) },
            )
            conversationHistory.add(AiMessage(AiRole.Assistant, response.text))
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                conversation = conversationHistory.filter { it.role != AiRole.System }.toList(),
            )
        } catch (e: Exception) {
            conversationHistory.removeLastOrNull()
            _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "AI request failed")
        }
    }

    fun clearConversation() {
        conversationHistory.clear()
        conversationHistory.add(AiMessage(AiRole.System, /* system prompt */))
        _uiState.value = _uiState.value.copy(conversation = emptyList())
    }

    private suspend fun throttle() {
        val now = System.currentTimeMillis()
        val wait = RATE_LIMIT_MS - (now - lastRequestTime.get())
        if (wait > 0) delay(wait)
        lastRequestTime.set(System.currentTimeMillis())
    }
}
```

### 1e. AiChannelSearchStore.kt (commonMain expect)

```kotlin
internal expect object AiChannelSearchStore {
    fun searchChannels(
        event: EspnProcessedEvent,
        allChannels: List<IptvChannel>,
        matchedChannels: List<MatchedChannel>,
        query: String,
        region: BroadcastRegion,
    ): Flow<AiChannelSearchResult>
}
```

**System prompt for channel search:**
```
You are a sports channel discovery assistant. Given a sporting event, a list of IPTV
channels the user has, and a natural language query, explain which channels best match
and why. Return a JSON object with "explanation" (2-3 sentences) and
"suggested_portals" (list of portal names from the channel groups, or empty if none).
Do NOT return channel URLs — the app already has the channel list.
```

**Android actual:**
- Parses matched channels to extract unique portal/source info
- Calls `AiClient.chatCompletion` with the prompt + event context + query
- Returns `AiChannelSearchResult`

**iOS actual:** Same logic, different storage API.

---

## Phase 2: SportNutz AI Channel Search for Events

### Location
`SportEventDetailPanel` → `EventTab.LIVE` tab

### UI Changes

1. **Add state** inside `SportEventDetailPanel`:
```kotlin
var aiSearchQuery by remember { mutableStateOf("") }
var aiSearchResult by remember { mutableStateOf<AiChannelSearchResult?>(null) }
var aiSearchLoading by remember { mutableStateOf(false) }

LaunchedEffect(activeTab) {
    if (activeTab != EventTab.LIVE) {
        aiSearchQuery = ""
        aiSearchResult = null
    }
}
```

2. **AI Search Bar** — inserted in the LIVE tab header row, below the tab selector:
```kotlin
if (AiSettingsStore.isEnabled()) {
    AiChannelSearchBar(
        query = aiSearchQuery,
        isLoading = aiSearchLoading,
        onQueryChange = { aiSearchQuery = it },
        onSearch = { q ->
            aiSearchLoading = true
            scope.launch {
                val result = AiChannelSearchStore.searchChannels(
                    event = event,
                    allChannels = IptvRepository.getAllChannels(),
                    matchedChannels = matchedChannels,
                    query = q,
                    region = uiState.userRegion,
                ).first()
                aiSearchResult = result
                aiSearchLoading = false
            }
        },
        onClear = { aiSearchQuery = ""; aiSearchResult = null },
    )
    aiSearchResult?.let { result ->
        AiChannelSearchExplanationCard(result = result)
    }
}
```

3. **`AiChannelSearchBar` composable** — styled like existing `SearchBar` but smaller, with sparkle icon:
```kotlin
@Composable
private fun AiChannelSearchBar(
    query: String,
    isLoading: Boolean,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("AI: e.g. 'ESPN channels in HD', 'my region, no duplicates'",
            color = OnSurfaceVariant.copy(alpha = 0.5f), fontSize = 12.sp) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "AI Search",
                tint = Primary,
                modifier = Modifier.size(16.dp)
            )
        },
        trailingIcon = {
            if (query.isNotBlank()) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Primary,
                    )
                } else {
                    IconButton(onClick = onClear) {
                        Icon(Icons.Default.Close, "Clear", tint = OnSurfaceVariant, modifier = Modifier.size(16.dp))
                    }
                }
            }
        },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Primary.copy(alpha = 0.6f),
            unfocusedBorderColor = SurfaceContainerHighest,
            focusedContainerColor = SurfaceContainer,
            unfocusedContainerColor = SurfaceContainer,
            cursorColor = Primary,
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { if (query.isNotBlank()) onSearch() }),
    )
}
```

4. **`AiChannelSearchExplanationCard` composable:**
```kotlin
@Composable
private fun AiChannelSearchExplanationCard(result: AiChannelSearchResult) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(PrimaryContainer.copy(alpha = 0.2f))
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 6.dp)) {
            Icon(Icons.Default.Star, "AI Insight", tint = Primary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("AI Insight", color = Primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            if (result.latencyMs > 0) {
                Spacer(Modifier.width(4.dp))
                Text("${result.latencyMs}ms", color = OnSurfaceVariant, fontSize = 10.sp)
            }
        }
        Text(result.explanation, color = OnSurface, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
        if (result.suggestedPortals.isNotEmpty()) {
            Text("Suggested portals:", color = OnSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(result.suggestedPortals) { portal ->
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(6.dp))
                            .background(PrimaryContainer.copy(alpha = 0.4f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(portal, color = Primary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}
```

5. **Channel list integration** — when AI search is active, the filtered channels from the result are shown instead of the default matched channels:
```kotlin
val displayChannels = aiSearchResult?.filteredChannels?.map { mc ->
    MatchedChannel(
        channel = mc.channel,
        matchType = mc.matchType,
        sourceName = mc.sourceName,
        region = mc.region,
        providerGroup = mc.providerGroup,
        score = mc.score,
        reasons = mc.reasons,
    )
} ?: matchedChannels
// Pass displayChannels to ChannelListContent instead of matchedChannels
```

---

## Phase 3: Settings UI

### 3a. AiSettingsPage.kt

```kotlin
internal fun LazyListScope.aiSettingsContent(
    isTablet: Boolean,
    enabled: Boolean,
    baseUrl: String,
    onEnabledChange: (Boolean) -> Unit,
    onBaseUrlChange: (String) -> Unit,
    onTestConnection: () -> Unit,
    testResult: String?,
) {
    item {
        SettingsSection(title = "AI & Chat", isTablet = isTablet) {
            SettingsGroup(isTablet = isTablet) {
                SettingsSwitchRow(
                    title = "Enable AI Features",
                    description = "AI-powered channel search, predictions, and chat",
                    checked = enabled,
                    isTablet = isTablet,
                    onCheckedChange = onEnabledChange,
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsNavigationRow(
                    title = "AI Base URL",
                    description = baseUrl,
                    isTablet = isTablet,
                    onClick = { /* show URL input dialog */ },
                )
                if (testResult != null) {
                    Text(
                        text = testResult,
                        color = if (testResult.startsWith("OK")) AccentGreen else ErrorRed,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 8.dp),
                    )
                }
                SettingsNavigationRow(
                    title = "Test Connection",
                    description = "Check freellmapi availability and latency",
                    isTablet = isTablet,
                    onClick = onTestConnection,
                )
            }
        }
    }
}
```

### 3b. SettingsRootPage.kt — add nav row in General section

```kotlin
SettingsGroupDivider(isTablet = isTablet)
SettingsNavigationRow(
    title = stringResource(Res.string.compose_settings_page_ai_chat),
    description = stringResource(Res.string.compose_settings_root_ai_chat_description),
    icon = Icons.Rounded.AutoFixHigh,
    isTablet = isTablet,
    onClick = onAiSettingsClick,
)
```

---

## Phase 4: Global AI Chat FAB

### 4a. RobbdeezeNutzHubScreen.kt — add FAB

```kotlin
// Inside the root Box, after all hub content:
if (AiSettingsStore.isEnabled()) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
        FloatingActionButton(
            onClick = { showAiChat = true },
            modifier = Modifier.padding(end = 16.dp, bottom = 100.dp),
            containerColor = Primary,
            contentColor = Color(0xFF00363A),
        ) {
            Icon(Icons.Default.Chat, "AI Chat", modifier = Modifier.size(24.dp))
        }
    }
}

if (showAiChat) {
    AiChatSheet(onDismiss = { showAiChat = false })
}
```

### 4b. AiChatSheet.kt

Full-screen bottom sheet with:
- Conversation message list (user right-aligned, assistant left-aligned)
- Input field + send button
- Clear conversation button (top bar)
- Error banner (if AI request fails)
- Streaming text animation (character-by-character reveal for long responses)
- Auto-scroll to bottom on new messages

---

## Phase 5: SportNutz Additional AI Features

### 5a. AI Match Predictions Card

Shown above the channel list in the LIVE tab when `matchedChannels.isEmpty()`:
- Prompt: "Predict the outcome of [awayTeam] vs [homeTeam] in [league]. Give 3 key factors and a predicted score."
- Displays as a card with "AI Prediction" badge

### 5b. AI Game Summary (NEWS tab)

Button "AI Summary" above the news list:
- Prompt: "Summarize the key moments of [event title] in 3 bullet points."
- Shows collapsible card with summary text

---

## Phase 6: Hub-Wide AI Features

| Hub | Feature | Implementation |
|-----|---------|---------------|
| **VidNutz** | AI Search Suggestions | Below search input: show 3 AI-generated related queries based on recent search history |
| **MusicNutz** | AI Mood Picker | Chips row: "Chill", "Workout", "Focus", "Party" → AI suggests playlists/artists |
| **PodNutz** | AI Episode Summaries | Per-episode chip: "AI Summary" → tooltip with 2-line summary |
| **IPTV** | AI Channel Search | In PortalForm: "AI Search" toggle → natural language filter for channel lists |
| **Home** | AI Recommendations | Top-of-home card: "AI picked this for you" based on watch history |
| **Search** | AI Query Enhancement | Transform natural language → structured addon query |

---

## Technical Notes

- **Network**: Reuses existing `httpPostJson` from `AddonPlatform.android.kt` — no new HTTP library
- **Model**: Always sends `"model": "auto"` — freellmapi handles routing across 34+ providers
- **Rate limiting**: 1 request/second via atomic timestamp check in `AiStore`
- **Offline degradation**: All AI features hidden when disabled; buttons don't crash
- **Region-aware**: `AiChannelSearchStore` receives `BroadcastRegion` for localized suggestions
- **Conversation scope**: Global shared conversation — same chat accessible from any hub
- **Storage**: Settings persisted via `SharedPreferences` (Android) / `UserDefaults` (iOS)
- **Streaming**: Supports both non-streaming (`chatCompletion`) and streaming (`chatCompletionStreaming`)
- **Error handling**: Network errors shown in UI; conversation history preserved across failures

---

## Implementation Order

1. **Phase 1** — `AiModels.kt`, `AiSettingsStore.kt`, `AiClient.kt`, `AiStore.kt`, `AiChannelSearchStore.kt` (+ Android/iOS actuals)
2. **Phase 2** — SportNutz AI Channel Search (search bar + explanation card in event detail)
3. **Phase 3** — Settings UI (`AiSettingsPage.kt`, wire into `SettingsRootPage.kt`)
4. **Phase 4** — Global AI Chat FAB + `AiChatSheet.kt`
5. **Phase 5** — AI Match Predictions + AI Game Summary in SportNutz
6. **Phase 6** — AI features in VidNutz, MusicNutz, PodNutz, IPTV, Home, Search

---

## Build & Test

```bash
export JAVA_HOME=/usr/local/Cellar/openjdk@17/17.0.17/libexec/openjdk.jdk/Contents/Home
./gradlew :androidApp:assembleFullDebug
# Install to 192.168.1.172:5555
adb -s 192.168.1.172:5555 install androidApp/build/outputs/apk/full/debug/androidApp-full-debug.apk
```

Test checklist:
- [ ] AI toggle ON by default in Settings → AI & Chat
- [ ] AI channel search bar appears in SportNutz event detail → LIVE tab
- [ ] Typing a natural language query returns AI explanation + suggested portals
- [ ] Global AI Chat FAB visible on all hub screens
- [ ] Chat conversation persists across hub switches
- [ ] Clear conversation button resets chat
- [ ] Test Connection button shows OK + latency
- [ ] AI features hidden when toggle is OFF
- [ ] No crashes when freellmapi is unreachable
