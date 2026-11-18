# ZIPMM MOVIE

Android application source for ZIPMM MOVIE.

## Build
GitHub Actions workflow: **Android APK**.

Artifacts:
- ZIPMM-MOVIE-debug: debug build for testing.
- ZIPMM-MOVIE-release: release variant.

## Production signing
To create an installable production-signed APK, add these GitHub Actions repository secrets:
- ZIPMM_KEYSTORE_BASE64: base64-encoded Android release keystore.
- ZIPMM_KEYSTORE_PASSWORD
- ZIPMM_KEY_ALIAS
- ZIPMM_KEY_PASSWORD

Without these secrets, the release variant is not production-signed. Do not distribute it as a production app.

## Current app scope
The Android UI includes Movie, India, Korea, Japan and Korean Drama category sections, search, movie detail navigation, watchlist, continue-watching UI and account/payment contact display. Actual catalogue data, authenticated accounts, cloud sync, remote video playback, receipt uploads and admin approval require a connected backend and uploaded licensed movie sources; these are not supplied by the Android shell alone.

Payment display configured by the owner:
- KBZPay: 09456707261
- WavePay: 09456707261
