# Upstream Merge Audit — lucidconviction/NuvioMobile

**Generated:** August 2, 2026  
**Base commit:** `b3be03fb` (our last common ancestor)  
**Upstream branch:** `cmp-rewrite`  
**Total commits since fork:** ~60 (non-merge)

---

## Color Key

| Label | Meaning |
|-------|---------|
| ✅ **Safe** | No file conflicts with our custom features. Cherry-pick directly. |
| ⚠️ **Low Risk** | Touches a file we modified but changes are in an area we didn't touch. Manual merge needed. |
| 🔴 **Conflict** | Touches files we heavily modified (App.kt, SettingsRootPage, hub files, etc.). Requires careful manual merge. |
| ❌ **Skip** | Already have our own version, or irrelevant to our fork (version bumps, translations, Simkl if unused). |

---

## Recommended Cherry-Picks

### ✅ Safe — No Conflicts

| Commit | Description | Files |
|--------|-------------|-------|
| `ad1b8b79` | fix: handle torrent cache pressure on Android | `P2pStreamingEngine.android.kt` — we may benefit from this fix since we use P2p/TorrServer |
| `ebbc9709` | Take videoid from meta for track services in progress items | `WatchProgressMetadataProjection.kt` — isolated change, no conflict |
| `d92b4d4c` | fix(search): remove recent history icons | `SearchScreen.kt` — UI cleanup only |
| `abbfb62b` | Add badges to TmdbEntityBrowser | `TmdbEntityBrowseScreen.kt`, `DetailPosterRailSection.kt` — isolated UI enhancement |
| `0e1e12a8` | Fix "mark as unwatched" for whole series | `WatchedRepository.kt`, `WatchingActions.kt` — bug fix, no overlap with our changes |
| `6f4c69f5` | fix(ui): clip CardDepthEffect sheen overlay to shape path | `CardDepthEffect.kt` — small UI clip fix |
| `44fc3fa6` | fix(tracking): close early scrobble sessions | `PlayerScreenRuntimePlaybackActions.kt` — tracking fix, no conflict |
| `d55ed7ae` | fix(auth): sanitize authentication errors | `AuthRepository.kt` — isolated auth fix |
| `289ea2c4` | Remove redundant xml declaration | `strings.xml` (Slovak) — translation file only |
| `82159fea` | fix: next episode calculation resolves anime watched via fallback | `MetaDetailsScreen.kt`, `SeriesPlaybackResolver.kt`, `WatchedRepository.kt` — bug fix |
| `d8ffd18c` | fix(simkl): pace requests after completion | `SimklApiClient.kt` — API rate limiting fix |
| `085b136e` | fix(simkl): hide on-hold items from continue watching | `SimklApplicationAdapters.kt`, `SimklPlaybackReconciliation.kt` |
| `327d49d3` | fix(simkl): refresh hidden progress immediately | `HomeScreen.kt`, `SimklApplicationAdapters.kt`, `WatchProgressRepository.kt` — touches HomeScreen but on Simkl-specific paths |
| `94ec538c` | fix(simkl): reconcile completed and dropped progress | `HomeScreen.kt`, Simkl files — Simkl-specific |

### ⚠️ Low Risk — Manual Merge Needed

| Commit | Description | Risk |
|--------|-------------|------|
| `8937dc7b` | Update Play account and foreground service compliance | Touches `SettingsRootPage.kt` (we added Telegram groups) and `AppFeaturePolicy.kt` (not modified by us). The SettingsRootPage changes are likely in a separate section from our footer edits. |
| `b21b36f8` | fix: bulk watched badge resolution with sibling expansion and anime fallback | Touches `HomeScreen.kt`, `WatchedRepository.kt`. If we haven't modified HomeScreen heavily, this should merge cleanly. |
| `e51d2b04` | Add a setting to let user select anime ids | Touches `LibraryScreen.kt`, `TrackingSettingsPage.kt`, `WatchedRepository.kt`. New feature — no overlap. |
| `f2bf839e` | feat: Simkl anime tracking — per-season MAL/Kitsu ID resolution | Touches `WatchedRepository.kt`, `WatchedEpisodeActions.kt`, `WatchedModels.kt`, `WatchingState.kt`. If we haven't touched these, clean merge. |
| `7f254a9f` | refactor: remove catalog header accent line | Touches MANY files (`HomeScreen.kt`, `HomeCatalogSection.kt`, `SearchScreen.kt`, `LibraryScreen.kt`, `SettingsScreen.kt`, etc.). This is a large refactor. Low risk if we cherry-pick carefully. |

### 🔴 Conflict Risk — Touches Heavily-Modified Files

| Commit | Description | Why It Conflicts |
|--------|-------------|------------------|
| `db87075b` | Remove realtime sync support | Touches `App.kt` (we added TeleNutz, MW button, hub navigation), `build.gradle.kts` (our build config), `SupabaseProvider.kt`, `SyncManager.kt`, `gradle/libs.versions.toml` |
| `b3f22a6b` | feat(auth): register official client devices | Touches `App.kt`, `DeviceSessionRegistration.android.kt`. Our App.kt has extensive customizations. Need manual merge. |
| `1b05c222` | feat(library): add incremental delta sync | Touches `LibraryRepository.kt`, `LibraryStoragePayload.kt`, `LibrarySyncReconciler.kt` — if we haven't modified library files, low risk. |

### ❌ Skip

| Commit | Reason |
|--------|--------|
| `3ba9cfd4` / `88d3cbdf` | Version bumps — we have our own versioning |
| `2de02d53` / `c53b2173` | Slovak translations only |
| `e4911b77` / `c2fc3732` / `c96cda92` / `247afcbf` / `2ebaf3b5` / `807f561f` / `085b136e` | Simkl-only features — skip unless you use Simkl |
| `e9b40053` / `15ec2330` / `232a54fc` / `e356e373` / `a234646f` / `a1e8c77d` / `5bdbc353` | Merge commits — already covered by individual commits |

---

## Recommended Merge Order

### Batch 1 — High Value, Zero Risk
```bash
git cherry-pick ad1b8b79  # torrent cache pressure fix
git cherry-pick ebbc9709  # videoid for track services
git cherry-pick d92b4d4c  # search history icons
git cherry-pick abbfb62b  # TmdbEntityBrowser badges
git cherry-pick 0e1e12a8  # mark as unwatched fix
git cherry-pick 6f4c69f5  # CardDepthEffect clip
git cherry-pick 44fc3fa6  # close early scrobble sessions
git cherry-pick d55ed7ae  # sanitize auth errors
git cherry-pick 82159fea  # next episode anime fallback
git cherry-pick d8ffd18c  # simkl pace requests
```

### Batch 2 — Manual Review Required
```bash
git cherry-pick 8937dc7b  # Play account compliance (check SettingsRootPage.kt diff)
git cherry-pick b21b36f8  # watched badge resolution (check HomeScreen.kt diff)
git cherry-pick b3f22a6b  # device registration (manual merge App.kt)
git cherry-pick db87075b  # remove realtime sync (manual merge App.kt + build.gradle.kts)
git cherry-pick 1b05c222  # delta sync (check LibraryRepository.kt diff)
```

### Batch 3 — Simkl (if used)
```git cherry-pick
e51d2b04 c2fc3732 e4911b77 c96cda92 f2bf839e
```

---

## File Conflict Matrix

| File | Our Modifications | Upstream Changes | Risk |
|------|-------------------|------------------|------|
| `App.kt` | TeleNutz hub, MW button, hub navigation, settings wiring | Realtime sync removal, device registration | 🔴 |
| `SettingsRootPage.kt` | Added Telegram groups, version, removed GitHub | Play account compliance text changes | ⚠️ |
| `HomeScreen.kt` | Quick Channels section (HomeQuickChannelsSection) | Catalog refactor, Simkl progress fixes | ⚠️ |
| `P2pStreamingEngine.android.kt` | TorrServer start method | Torrent cache pressure fix | ✅ (small change) |
| `WatchedRepository.kt` | None (same as upstream) | Multiple bug fixes | ✅ |
| `build.gradle.kts` | Build config, native libs, version gen | Realtime sync dependency removal | 🔴 |
| `gradle/libs.versions.toml` | Not modified | Realtime sync version removal | 🔴 |

---

## Tools

To preview a cherry-pick without applying it:
```bash
git cherry-pick --no-commit <commit-hash>
git diff --cached
git cherry-pick --abort
```

To see what a commit changed:
```bash
git show --stat <commit-hash>
git show <commit-hash>
```
