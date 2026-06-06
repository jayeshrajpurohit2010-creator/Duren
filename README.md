# 🌙 Duren

> **Ephemeral. Present. Belong.**
> A digital campfire for synchronous presence — not permanent identity.

Duren is a **time-bound, tribe-based social network built natively for Android** in Kotlin + Jetpack Compose, backed by Firebase. It's built for people tired of Instagram's performance pressure, Twitter's outrage, and Discord's permanent communities. Duren is the first social network designed to **let you go**, not to keep you.

Three things make it different:

1. **The platform has opening hours.** Tribes go quiet 2–3 AM in their home timezone. The campfire rests.
2. **Absence is meaningful.** Missing your tribe's peak hour means something. The fire burned. You weren't there.
3. **Community memory without individual archive.** Posts (*embers*) expire in 6h–7d. Tribes accumulate identity over nights; your individual content does not accumulate.

---

## 🗣 The Vocabulary

Duren deliberately renames everything — the words shape the feeling.

| Generic | Duren |
|---|---|
| Post | **Ember** |
| Like | **Echo** |
| Comment / reply | **Whisper** |
| Feed | **The Clearing** (for lone campers) / **The State** |
| Status | **Lantern** |
| Community / server | **Tribe** |
| Friend | **Nest member** |
| Friends space | **The Nest** |
| Profile | **My Presence** |
| Report | **Cold Mark** |
| Trending | **Burning Now** |
| Hot post | **Drum Circle** (20+ echoes) |

---

## ✅ Current status

This repo is a **working MVP**, well past the "skeleton" stage. What's implemented and shipping in the debug APK:

- **Auth** — email/password sign-up + sign-in, TOCTOU-safe username reservation, show/hide password, "forgot password" reset email, anti-enumeration messaging.
- **Embers** — compose with photo, named / anonymous / confess modes, expiry windows, live countdown with a "fading" flicker under 30 min.
- **Echoes** — one-tap with a heart-bounce animation; **Drum Circle** gold pulse at 20+ echoes; temperature/heat tiers on the card border.
- **Whispers** — inline comment threads per ember, including anonymous ("A Soul").
- **The Nest** — find people by name/@username, view public profiles, send/accept friend requests, mutual-membership list.
- **Tribes** — browse, create from templates, tribe detail pages.
- **Lanterns** — lightweight presence/status.
- **Media** — pinch-to-zoom full-screen image viewer (1×–5×).
- **Settings** — accent color, light mode, avatar (photo or color), privacy toggles, account identity, change password, **delete account** (re-auth guarded), About.
- **Cold Marks** — report flow.

See the [roadmap](#-roadmap) for what's next.

---

## 🏗 Tech stack

| Layer | Choice |
|---|---|
| Language | Kotlin 2.2 |
| UI | Jetpack Compose + Material 3 |
| DI | Hilt (KSP) |
| Navigation | Navigation Compose, type-safe `@Serializable` routes |
| Auth | Firebase Authentication (email/password) |
| Database | Cloud Firestore |
| Images | Coil 3 |
| Min / Target SDK | 24 / 35 |

**Deliberate constraints (no paid plan yet):**
- **No Cloud Functions / no FCM / no Firebase Storage.** Ranking, temperature, and heat are computed **client-side**.
- **Media is inline Base64** — photos are downscaled → JPEG → `data:` URI stored on the ember document, decoded on-device. (No external image host.)
- Firestore queries avoid composite indexes (single-field equality + on-device sort/filter), so there's no console index setup.

---

## 📁 Project structure

```
app/src/main/java/com/duren/app/
├─ core/          # Result/DomainError, Hilt modules (FirebaseModule)
├─ data/          # repositories + models (the source of truth boundary)
│  ├─ auth/       # AuthRepository — sign up/in/out, reset, delete account
│  ├─ ember/      # EmberRepository (+ echoes, whispers, cold marks), models
│  ├─ profile/    # ProfileRepository — profile + people search
│  ├─ nest/       # NestRepository — friend requests + mutual membership
│  ├─ tribe/      # TribeRepository
│  ├─ lantern/    # LanternRepository
│  ├─ media/      # MediaUploadRepository (Base64 pipeline)
│  └─ settings/   # SettingsRepository
├─ feature/       # one package per screen: ViewModel + Composable
│  ├─ auth/ feed/ compose/ profile/ search/ nest/ mynest/
│  ├─ whisper/ tribes/ settings/ tabs/ theme/ nav/
├─ ui/
│  ├─ theme/      # DurenColors, DurenType, DurenSpacing, DurenShapes, DurenTheme
│  ├─ animation/  # pressableCard (A3), toast, modal, shimmer, empty-state, springs
│  └─ components/ # EmberCard, DurenAvatar, ExpiryTimer, FullScreenImageViewer, …
firestore.rules   # security rules (deployed via Firebase CLI)
firebase.json     # CLI config → firestore.rules
```

---

## 🚀 Getting started

### Prerequisites
- Android Studio (latest), JDK 17
- A Firebase project with **Authentication (Email/Password)** and **Cloud Firestore** enabled

### Setup
1. **Clone** and open in Android Studio.
2. **Add `google-services.json`** — download it from your Firebase project (Android app, package `com.duren`) and drop it into `app/`. *This file is gitignored and must never be committed.*
3. **Build & run:**
   ```powershell
   ./gradlew.bat :app:assembleDebug
   ```
   The APK lands at `app/build/outputs/apk/debug/app-debug.apk`.

### Deploying Firestore rules
Rules live in `firestore.rules` and deploy via the Firebase CLI (one-time `firebase login`, then):
```powershell
firebase deploy --only firestore:rules
```
`firebase.json` and `.firebaserc` are already wired to the project.

---

## 🎨 Design system (quick reference)

| Token | Value |
|---|---|
| Background | `#0A0A0A` (cinematic deep black) |
| Teal accent | `#41CBBF` |
| Green secondary | `#2BB673` |
| Button text on teal | `#1A1A1A` — **always**, never white |
| Spacing | 4-point grid (4 / 8 / 12 / 16 / 20 / 24 / 32 / 40 …) |
| Cards | 16dp radius · Buttons/inputs 12dp · Pills 999dp · Avatars circle |

**Motion discipline:** max 3 simultaneous animations per screen; 60fps is non-negotiable.

Full specs live in the Notion **Design System v2.0** and **Animation Bible v2.1** (55+ animations: 39 numbered A1–A39 + 6 signature moments).

---

## 🗺 Roadmap

- [ ] Firebase App Distribution for one-tap tester updates
- [ ] Push notifications (needs FCM + Cloud Functions → paid plan)
- [ ] Server-side ranking & anti-gaming (Algorithm Spec v1.0, 7-layer)
- [ ] Ghost Rooms, Mood Canvas, Hearths, Whisper Circles (PRD Phase 3+)
- [ ] Blue Flame Pro (₹149/mo · ₹999/yr)

---

## 🔐 Security

- `app/google-services.json` is **gitignored** — never commit it.
- Firestore rules enforce per-user ownership; cross-user writes are denied by default.
- Security model follows the Notion **Security & Privacy v1.0** doc (OWASP Mobile Top 10 mapping, DPDP/GDPR alignment).

---

## 👤 Founder

**Jayesh Rajpurohit** — 16, solo founder, building Duren since May 2026.
Engineering with Claude Code · Strategy with Claude · Design with Claude Design.

*Source of truth: the 8-document Notion master workspace. If code conflicts with Notion, the code is wrong.*
