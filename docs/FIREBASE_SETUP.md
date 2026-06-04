# Firebase Setup — Phase 0

This is the manual checklist Jayesh must run **once** before the app can build and connect. Everything Claude built assumes a real `google-services.json` will exist at `app/google-services.json`. Without it, Gradle's `google-services` plugin will fail the build with a clear error.

---

## Step 1 — Create the Firebase project

1. Go to <https://console.firebase.google.com>.
2. Click **Add project** → name it `duren-dev`.
3. Disable Google Analytics for now (optional for Phase 0; can enable later).
4. Choose region **`asia-south1`** (Mumbai) for India latency. **This is irreversible** — pick carefully.

## Step 2 — Register the Android app

1. In project overview, click the Android icon → **Add app**.
2. Android package name: `com.duren.app`
3. App nickname: `Duren Dev`
4. SHA-1: leave blank for now (we'll add it in Step 4).
5. Click **Register app**.
6. **Download `google-services.json`** → save it to:

   ```
   C:\Users\dines\AndroidStudioProjects\Duren\app\google-services.json
   ```

   This file is gitignored. Do NOT commit it.

## Step 3 — Enable Email/Password authentication

1. Firebase Console → **Authentication** → **Get started**.
2. Sign-in method tab → **Email/Password** → toggle **Enable** → Save.

## Step 4 — Add SHA-1 fingerprint

From PowerShell at the project root:

```powershell
.\gradlew signingReport
```

Look for the `debug` variant block, copy the line that starts with `SHA1:`.

Then in Firebase Console → Project Settings → Your Apps → Duren Dev → **Add fingerprint** → paste the SHA-1.

(Why? Required for any future Google Sign-In, Phone Auth, and App Check.)

## Step 5 — Create Firestore database

1. Firebase Console → **Firestore Database** → **Create database**.
2. Mode: **Production mode** (locked by default; we ship rules in Step 6).
3. Region: **`asia-south1`** (must match Step 1).

## Step 6 — Deploy Security Rules

Install Firebase CLI if you haven't:

```powershell
npm install -g firebase-tools
firebase login
```

Then from the project root:

```powershell
firebase deploy --only firestore:rules
```

This pushes `firestore.rules` from the repo to production. Re-run any time you change the rules file.

## Step 7 — Enable Crashlytics

1. Firebase Console → **Crashlytics** → **Enable Crashlytics**.
2. Run the app once on an emulator/device — Crashlytics auto-instruments on first launch.
3. To verify wiring, add a temporary "Force crash" button later (Phase 1) and confirm reports arrive within ~5 min.

---

## Running the Firestore rules smoke test

Once the Firebase CLI is installed:

```powershell
cd firebase\test
npm install
cd ..\..
firebase emulators:exec --only firestore "npm --prefix firebase/test test"
```

Expected: 5 passing tests (owner can write own profile, others can't, etc.).

---

## When something breaks

- **Build fails: "File google-services.json is missing"** — you skipped Step 2 download. Drop the file in `app/`.
- **Sign-up fails silently** — Step 3 (Email/Password) not enabled.
- **Rules deploy rejected** — syntax error in `firestore.rules`. Run the emulator smoke test locally first.
- **App can't reach Firebase** — check device has network; check `applicationId` in `app/build.gradle.kts` matches the package name registered in Firebase Console exactly (`com.duren.app`, plus `.debug` suffix on debug builds — register both `com.duren.app` and `com.duren.app.debug` as separate apps in Firebase Console, or remove the `applicationIdSuffix` in `buildTypes.debug`).

---

## Production project (later)

When you're ready to launch publicly, repeat Steps 1–7 with `duren-prod` as the project ID. Use Gradle product flavors to swap `google-services.json` per build variant. That's a Phase 4 concern — don't do it yet.
