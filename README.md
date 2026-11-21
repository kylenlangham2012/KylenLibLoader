# Kylen Lib Loader

A dark-themed Android Studio starter app for organising APKs and native `.so` libraries belonging to your own projects.

## Current build
- Import APK files through Android's document picker.
- Import native shared-library files (`.so`).
- Search, view, and remove imported entries.
- Retains selected document URI permissions where Android allows it.
- No game code is modified and no APK is installed by this starter build.

## Important limitation
This is a library manager UI, not yet an APK injection/patching engine. There is no universal safe way to inject an arbitrary `.so` into every Android/Unity game: the library must match the target ABI (for example `arm64-v8a`), dependencies, native API, and the game's own loading/initialisation path. Some games also use signature checks or anti-tamper systems. The reference APK contains a bundled signing key; this project deliberately does not copy or reuse that private key. Use your own signing key for your own builds.

## Build
1. Open this folder in Android Studio (recent version).
2. Let Gradle sync and install the Android SDK Platform 35 if prompted.
3. Choose **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
4. The debug APK will be under `app/build/outputs/apk/debug/`.

Package: `com.kylen.libloader`
