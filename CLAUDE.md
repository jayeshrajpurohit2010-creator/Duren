# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

Duren is a tribe-based **ephemeral** Android social network (Kotlin + Compose + Firebase). `README.md` is the source of truth for the product, vocabulary (Ember/Echo/Whisper/Tribe/Nest…), and design tokens — read it for the "what." This file is the "how": build commands, architecture, and the conventions that will silently break things if ignored.

## Build, run, test

Use the **Bash tool** for Gradle (`./gradlew …`). In a **PowerShell** terminal use `.\gradlew.bat …`.

| Task | Command |
|------|---------|
| Fast compile check (preferred during iteration) | `./gradlew :app:compileDebugKotlin --console=plain` |
| Full debug APK | `./gradlew :app:assembleDebug` |
| Android lint | `./gradlew :app:lintDebug` |
| Unit tests | `./gradlew :app:testDebugUnitTest` |
| A single test | `./gradlew :app:testDebugUnitTest --tests "com.duren.app.SomeTest"` |
| Clean | `./gradlew clean` |

- **APK output:** `app/build/outputs/apk/debug/Duren-<versionName>-debug.apk` (the build script renames the artifact via `applicationVariants` — it is **not** `app-debug.apk`, despite older docs). Bump `versionCode`/`versionName` in `app/build.gradle.kts` per release.
- **Tests are effectively empty.** The only test files are leftover template stubs under the stale `com.example.duren` package (`ExampleUnitTest`, `ExampleInstrumentedTest`) — there is no real suite. New tests go under `com.duren.app`. Instrumented tests need a device/emulator (`./gradlew :app:connectedDebugAndroidTest`).
- `google-services.json` must be present in `app/` for any build to succeed (gitignored — see below).

### Firestore rules
Security rules in `firestore.rules` are deployed with the Firebase CLI (one-time `firebase login`):
```
firebase deploy --only firestore:rules
```
`firebase.json`/`.firebaserc` point at project **duren-78576**. The emulator suite (auth 9099, firestore 8080, UI 4000) is configured but not part of the normal loop.

## Architecture

Single `:app` module, MVVM, package boundaries under `com.duren.app`:

- **`core/`** — `Result`/`DomainError`, Hilt `FirebaseModule` (provides `FirebaseAuth`/`FirebaseFirestore`), and `core/time/NightEconomy` (see below).
- **`data/{domain}/`** — the **only** layer that touches Firestore. Each domain has `{Domain}Repository.kt` + a `model/` package. Repositories expose `Flow`/`StateFlow` (via `callbackFlow` over snapshot listeners) and `suspend` writes returning `Result`. Domains: auth, ember, profile, nest, tribe, lantern, dm, signal, mood, hearth, testimonial, smoke, media, settings.
- **`feature/{name}/`** — one package per screen: a Hilt `@HiltViewModel` + its `@Composable` screen. ViewModels inject repositories and own `StateFlow` UI state; Composables `collectAsStateWithLifecycle()`.
- **`ui/`** — `theme/` (Material3 wrapper `DurenTheme` + tokens), `animation/` (`pressableCard`, toast, modal, shimmer, springs), `components/` (shared widgets, notably `EmberCard`).

**Navigation** is a single Activity → `DurenNavHost` swaps between `AuthGraph` and `MainGraph` reactively from `SessionViewModel.isAuthenticated`. `MainScaffold` hosts the 5-tab bottom bar (`StateTab`/`TribesTab`/`ComposeTab`/`NestTab`/`PresenceTab`) plus stacked routes. All routes are type-safe `@Serializable` objects/data classes in `feature/nav/Destinations.kt` — navigate with the object, e.g. `navController.navigate(ChatRoute(otherUserId))`.

**Two architecture-defining concepts:**
- **Night Economy** (`core/time/NightEconomy`) — Dead Hours (2 AM) / Morning Fade (6 AM) are computed **on-device** from the device or a tribe's `homeTimezone` via `java.util.Calendar`. No server, no per-country job.
- **Signals** (`data/signal/`) — in-app notifications are Firestore docs (a `notifications`-style collection surfaced by `SignalScreen`), **not** FCM push.

## The free-tier law (read before designing anything)

The app runs on the **Firebase Spark (free) plan**: **no Cloud Functions, no FCM, no Firebase Storage, no Blaze.** Every derived value — ranking, temperature/heat tiers, expiry, rate caps, rations — is computed and enforced **client-side**. This makes some rules "soft" (cap races, client-side rations, mutual-spark windows) — that softness is a **known, accepted tradeoff** to be revisited when Cloud Functions arrive. Do not propose a server-side fix as if it were free.

Consequences baked into the code:
- **Media is inline Base64.** Photos are downscaled → JPEG → `data:` URI stored on the ember doc and decoded on-device (`data/media/MediaUploadRepository`). `CloudinaryConfig` exists but its cloud name is bogus/unused — do **not** route uploads there.

## Conventions & gotchas (these fail silently)

**Firestore**
- **Never pair `whereEqualTo(...)` with a server `.orderBy(...)`.** That needs a composite index which is not deployed, so the query returns an **empty list with no error**. Use a single-field query and sort on-device with `.sortedByDescending { it.createdAt }`. (Already documented inline in `EmberRepository`.)
- **Write `Timestamp.now()` for `createdAt`, not `FieldValue.serverTimestamp()`.** `serverTimestamp` resolves to `null` locally until the round-trip, so optimistic local writes sort to the wrong place. (Server timestamps are fine for fields you never sort on locally.)
- **A new collection needs a matching `match` block in `firestore.rules`** (added before the deny-all) and a redeploy, or every read/write is denied.

**Android / Compose**
- **minSdk 24, no desugaring → no `java.time`.** Use `java.util.Calendar` and `java.text.SimpleDateFormat`.
- **Material icons: `material-icons-core` only** (the extended artifact is not a dependency). `Icons.Default.Cameraswitch`, `PhotoCamera`, etc. will not compile — use a core icon name, text/emoji, or the custom set in `ui/components/DurenIcons.kt`.
- Design tokens live in `ui/theme/` (`DurenColors`, `DurenType`, `DurenSpacing`, `DurenShapes`) — treat those files, not hardcoded literals, as the source of truth. Button text on teal is always `#1A1A1A`, never white.

**Editing existing code:** upgrade in place rather than rebuilding from scratch — features deliberately share core files (`EmberRepository`, `TribeRepository`, `EmberCard`, `firestore.rules`), which is also why parallel/worktree edits conflict.

## Working in this repo (operational)

- **Environment:** Windows + PowerShell. The stray `.claude/worktrees/*` directories are abandoned agent worktrees with duplicate `build.gradle.kts`/`firestore.rules` — they are **not** part of the build (`settings.gradle.kts` includes only `:app`). Ignore them; never edit files there.
- **Confirm the repo root before any git operation:** `git rev-parse --show-toplevel` must be `C:/Users/dines/AndroidStudioProjects/Duren`. There is an unrelated stray `C:\.git` repo — leave it untouched.
- **Branch protection:** never commit/push to `main`. Work on a feature branch and open a PR for the user to merge.
- **Commit messages are human-voice:** first person, no `feat()`/`Phase X:` prefixes, no bullet dumps. Keep the `Co-Authored-By` trailer.
- **PowerShell + apostrophes:** apostrophes in a commit message break here-strings. Write the message to a temp file and `git commit -F <file>`, then delete it.

### Never commit (gitignored, and must stay that way)
- `app/google-services.json` (contains the API key)
- `*.jks` keystores and `keystore.properties`
- Any Cloudinary API secret — it must never appear in client code or history.
