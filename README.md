# ZIPMM MOVIE

Android app project for ZIPMM MOVIE.

## Current project status
This is the initial Android app foundation and automated debug-APK workflow. It is **not yet a production streaming service**. The movie catalogue, authentication, secure admin roles, video hosting/streaming, receipt upload, KBZPay/WavePay payment review, and subscription activation require a connected backend and owner-provided configuration.

## Build
Run the **Android APK** workflow from GitHub Actions, or build locally with Android SDK, JDK 17 and Gradle 8.9:
`gradle assembleDebug`

APK output: `app/build/outputs/apk/debug/app-debug.apk`

## Payment design
Customer transfers using the owner's verified KBZPay or WavePay details, submits the receipt, and the admin manually approves or rejects the request. Never put wallet PINs, OTPs, or personal passwords in this repository.
