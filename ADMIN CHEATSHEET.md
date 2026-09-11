# ADMIN CHEATSHEET — Nuvio (Mobile) & RNutz NuvioTV

Quick reference for the admin/operator. Covers the Telegram bot, portal donation keys, auto-updates, and current plans. The same cheatsheet lives in both the **Nuvio_Robbdeeze** (mobile) and **Nuvio TV Robbdeeze** (TV) project folders.

---

## 1. APPS

| App | Platform | Package (debug) | Version file |
|-----|----------|-----------------|--------------|
| Nuvio Mobile | Android / iOS | `app.robbdeezenutz.nuviodebug` (debug) | `iosApp/Configuration/Version.xcconfig` (`MARKETING_VERSION`/`CURRENT_PROJECT_VERSION`) |
| RNutz NuvioTV | Android TV | `com.nuviodebug.com` (debug, full variant) | `app/build.gradle.kts` (`versionName`/`versionCode`) |

Builds (JVM 17):
- Mobile: `./gradlew :androidApp:assembleFullDebug` → `androidApp/build/outputs/apk/full/debug/androidApp-full-debug.apk`
- TV: `./gradlew :app:assembleFullDebug` → `app/build/outputs/apk/full/debug/app-full-universal-debug.apk`
- Install over ADB: `adb connect 127.0.0.1:5555` then `adb install -r <apk>`

---

## 2. TELEGRAM BOT & GROUP

- **Bot:** `@Nuvio_by_RdNutz_bot` (token `8224209468:AAHcEO7K19cCnHWyqvhk4RfBzLEPq-yP-MQ`)
- **Group:** `@RnutzNuvioUpdates` — *"RNutz Nuvio Mobile/TV Updates"*
  - chat id: `-1004461382401` (forum/supergroup)
  - description: "Get updates and release notes here."

### Topics created (message_thread_id)
| Topic | Thread id |
|-------|-----------|
| 📢 Announcements | 43 |
| 📱 Mobile App Updates | 44 |
| 📺 TV App Updates | 45 |
| 💳 Donations & Portal Access | 46 |
| 🛠️ Help & Support | 47 |
| 🐞 Bug Reports | 48 |
| 🗣️ Feature Requests | 49 |

### Pinned messages
- **Announcements (43):** app names, download link (`https://apps.rdnutz.us/`), portal donation, points to update topics.
- **Donations (46):** donation instructions (see §5).

### Useful Bot API calls
```bash
BOT="bot8224209468:AAHcEO7K19cCnHWyqvhk4RfBzLEPq-yP-MQ"; CHAT="-1004461382401"
# Post to a topic
curl -s "https://api.telegram.org/$BOT/sendMessage" --data-urlencode "chat_id=$CHAT" \
  --data-urlencode "message_thread_id=43" --data-urlencode "text=Hello"
# Pin a message
curl -s "https://api.telegram.org/$BOT/pinChatMessage" --data-urlencode "chat_id=$CHAT" \
  --data-urlencode "message_id=<ID>"
```
> `setChatWelcomeMessage` does not exist in the Bot API — the welcome lives as a pinned Announcements post.

---

## 3. PORTAL DONATION KEYS (shared by BOTH apps)

The same key works on the mobile and TV apps (same HMAC secret + format).

### Generator
- Script: `scripts/generate_portal_key.py` (Python 3 stdlib only)
- Secret: `Rdnutz` (hardcoded in `PortalLicenseManager.kt` in both apps — keep in sync if changed)

### Usage
```bash
# 30-day user key (default)
python scripts/generate_portal_key.py
# custom duration
python scripts/generate_portal_key.py --duration 90
# ADMIN key (unlimited portals, ~100yr expiry)
python scripts/generate_portal_key.py --admin
# multiple keys
python scripts/generate_portal_key.py --count 5
# override secret
NVIO_KEY_SECRET=... python scripts/generate_portal_key.py
```

### Key format
- `NVIO-<base64url(payload.signature)>` — the **full** base64url of `payload.signature` (NOT a truncated 16-char key; those never verified).
- payload JSON: `{"id":"<uuid>","exp":<epochMS>,"maxDev":1,"isAdmin":false}`
- `exp` is in **milliseconds** (apps use `System.currentTimeMillis()`); seconds-based keys read as 1970 and appear "Expired".

### Enforcement in-app
- **3-portal limit** for user keys; **unlimited** for admin keys.
- **Device-bound** (per-device fingerprint UUID); keys can't be transferred between devices.
- **Expiry** (24h grace): on hard expiry the app erases all installed portals.
- Keys entered via the **ACTIVATE** popup (which first shows the donation/payment info).

---

## 4. AUTO-UPDATES

- Update source: **`https://apps.rdnutz.us/`** — plain directory listing of dated APKs:
  - Mobile: `Nuvio-Mobile-YYYY-MM-DD.apk`
  - TV: `RNutz-NuvioTV-YYYY-MM-DD.apk`
- The app fetches the listing, picks the newest date, and only prompts when it's newer than the **last-alerted date** stored locally (so users aren't nagged).
- TV downloads the APK **in-app** (`ApkDownloader`) and installs via the system installer.

---

## 5. DONATIONS / PORTAL ACCESS

- Cost: **$8/month per device**
- USDT: `0xf42b556E240b5a3820365414cE91BCaCd5bBA287`
- PayPal: `https://paypal.me/robbdeeze`
- **Donors must include their email WITH the donation** so the key can be issued.

---

## 6. BRANDING / STRINGS (TV)
- App name (label): **RNutz NuvioTV** (`strings.xml` in every locale)
- Portal section header: **RdNutz TV**
- Active banner: **"RdNutz TV Access Active"**
- Search button: **"Search"** (no "Portals")

---

## 7. PLANS / ROADMAP
- **Portal paywall + donation key system** (HMAC keys, device binding, grace/expiry, admin keys) — v0.20.0 (mobile).
- **Unified scored sport-channel matching** (`ChannelScorer`) + EPG current-program signal + stream validation — v0.19.0 (mobile).
- **Portal search speedup + account info** (goal-oriented verify, 1h cache, expiry/connections) — v0.18.3 (mobile).
- **TV: pull EPG + account expiration + channel count** from added portals; shown on playlist cards.
- **Persistent addons/plugins**: both apps seed the same default addon set on every fresh install; mobile also seeds default plugin repositories.

---

## 8. VERSION HISTORY
See `version.md` in each project folder for the full changelog.
