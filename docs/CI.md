# Continuous integration

Two GitHub Actions workflows live in `.github/workflows/`.

## `ci.yml` — runs on every push and PR

Two parallel jobs:

- **Android** — sets up JDK 21, runs `:app:testDebugUnitTest` + `:app:assembleDebug`,
  then Android Lint (non-blocking for now). Uploads the debug APK, the unit-test
  report, and the lint report as build artifacts.
- **Firestore rules** — boots the Firestore emulator and runs the mocha suite in
  `firebase/test/` against it, so a rules change that would lock users out (or let
  the wrong user write) fails the build.

Neither job ever sees the real Firebase key: `app/google-services.json` is
gitignored, and CI writes a throwaway placeholder that's structurally valid and
matches the `com.duren` package — enough to compile and test.

## `release.yml` — runs when you push a `v*` tag

Builds the APK and attaches it to a GitHub Release for sideloading.

A published APK has to reach the real Firebase backend, so this workflow looks for
a repo secret **`GOOGLE_SERVICES_JSON`** (the real file, base64-encoded) and decodes
it before building. To set it:

```bash
base64 -w0 app/google-services.json    # copy the output
# GitHub → Settings → Secrets and variables → Actions → New repository secret
# Name: GOOGLE_SERVICES_JSON   Value: <the base64 string>
```

Without the secret the APK still builds (handy as a smoke test) but won't connect
to Firebase. The APK is debug-signed so it installs without a release keystore; add
the keystore as secrets and switch to `assembleRelease` when you're ready for Play.

To cut a release:

```bash
git tag v1.0.11
git push origin v1.0.11
```
