# Duren

*Ephemeral. Present. Belong.*

Hey — I'm Jayesh. I'm 16, and Duren is the thing I've been building (mostly at night, which fits) since April 2026.

It's an Android app, and honestly it's kind of the opposite of every app I actually use. Instagram wants you to perform. Twitter wants you angry. Discord servers never sleep and never forget anything. Duren is supposed to be the campfire you sit at for a while and then walk away from — nothing you post sticks around, and that's the entire point.

A few ideas I cared about enough to build the whole thing around:

- **The app has closing hours.** Tribes go quiet around 2–3 AM in their own timezone. The fire rests. So should you.
- **Missing things is allowed to matter.** If your tribe lit up at midnight and you weren't there — you weren't there. There's no infinite scroll to "catch up" on.
- **Posts disappear.** An *ember* lasts 48 hours (72 if enough people echo it), then it's gone. The tribe keeps its vibe over time; your personal feed doesn't pile up forever.

## The words

I renamed basically everything on purpose, because the words change how the whole thing feels:

Post → **Ember** · Like → **Echo** · Comment → **Whisper** · Status → **Lantern** · Community → **Tribe** · Friends → **your Nest** · Profile → **My Presence** · Report → **Cold Mark** · Trending → **Burning Now**

## Where it actually is right now

Real talk: this is a working app I test on my own phone, not a finished product. Stuff that works today:

- Sign up / sign in, password reset, deleting your account
- Posting embers — opens straight to the camera (BeReal style), or you can just write text, with photos. Post as yourself, anonymously, or as a *confession* that gets a random poetic name like "The Wandering Star"
- Embers visibly **burn down** — they fade and blur as they run out of time, and there's a little bar that goes teal → amber → red
- Echoing posts, whisper threads underneath (you can whisper anonymously too)
- Finding people, adding them to your Nest, the whole friend-request flow
- Browsing tribes — there are about 24 built in to start (anime, gaming, lo-fi, vent space, a confession booth…)
- Fragment posts that hide the rest until someone echoes to reveal it
- Your profile, an ember signature that rides under your name, settings, accent colors, custom avatar

The rest is in "what's next" below.

## What it's built with

Kotlin + Jetpack Compose (Material 3), Hilt for dependency injection, and Firebase (Auth + Firestore) for the backend. Runs on Android 7.0 and up.

One honest constraint: I'm not on Firebase's paid plan yet, so there are no Cloud Functions, no push notifications, and no image hosting. I worked around all three:

- Ranking, the "temperature" of a post, and the burn-down are all computed on the phone.
- Photos get shrunk down and stored as Base64 right on the post itself — no image server.
- The Firestore queries are written so they don't need any composite indexes, so there's nothing to set up in the console.

The trade-off is that the features that genuinely *need* a server — push notifications, encrypted DMs, the proper ranking algorithm — are waiting on me upgrading the plan. They're on the list.

## Running it yourself

You'll need Android Studio and JDK 17, plus your own Firebase project with Email/Password auth and Firestore turned on.

1. Clone it and open in Android Studio.
2. Grab your own `google-services.json` from Firebase (Android app, package `com.duren`) and drop it into `app/`. Don't commit it — it's gitignored for a reason (it has your keys in it).
3. Build:
   ```
   ./gradlew.bat :app:assembleDebug
   ```
   The APK lands in `app/build/outputs/apk/debug/`.

The Firestore security rules live in `firestore.rules`. If you change them, `firebase deploy --only firestore:rules` pushes them up.

## If you're poking at the UI

Everything sits on near-black `#0A0A0A`. The teal is `#2dd4bf`. Text on a teal button is always `#1A1A1A`, never white — I'm weirdly strict about that one. Spacing sticks to a 4-point grid.

## How the code is laid out

```
app/src/main/java/com/duren/app/
├─ core/       # Result/error types, Hilt modules
├─ data/       # the repositories + models — where Firebase actually lives
│  ├─ auth/    ember/    profile/   nest/
│  ├─ tribe/   lantern/  media/     settings/   signal/   dm/
├─ feature/    # one folder per screen: a ViewModel + a Composable
├─ ui/
│  ├─ theme/        # colors, type, spacing, shapes
│  ├─ animation/    # the reusable motion bits
│  └─ components/   # EmberCard, the splash, the icon set, etc.
firestore.rules     # security rules
```

## What's next

- An easier way to get builds to testers
- Push notifications + real server-side ranking (both need the paid plan)
- More of the night-economy stuff I've designed but haven't built: Ghost Rooms, Mood Canvas, Hearths
- Blue Flame Pro eventually (₹149/mo or ₹999/yr) so the thing can pay for itself

---

If you somehow found this repo and you're into any of this — building it solo, so be nice. 🔥
