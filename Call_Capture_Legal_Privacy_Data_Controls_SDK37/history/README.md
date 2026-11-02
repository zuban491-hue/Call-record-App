# Backup-based permission UI update

This is the restored reviewed backup, not the latest lost design-update project. Read ../README_BN.md. The new permission UI is integrated here; no APK build or Android compilation was run.

# Call Capture — reviewed source copy

**Not release-ready.** Read `AUDIT_BN.md` for the complete Bengali audit and `CHANGES.md` for the exact limited cleanup scope.

This Kotlin/Jetpack Compose project contains legacy cellular/VoIP call detection, a microphone recording service, Room recording metadata, playback, notes, search and sharing. Detecting a call does not guarantee capture of its remote audio. The legacy Accessibility-based call recording workflow and Call Log permission are Google Play blockers for this product design.

New recording-channel and speaker-assistance defaults are OFF. Existing stored preferences are preserved. The unsafe size/duration-only cleanup action has been removed, single deletion asks for confirmation, sensitive backup rules are narrowed, and FileProvider only exposes the internal recordings folder when a URI grant is explicitly issued.

## Build status

No successful build or device test was completed in this review. The Gradle wrapper distribution download was blocked by unavailable network access. Do not label this archive as a tested APK or approved Play release.

The existing configuration targets/compiles API 35, uses Java 17 and has minimum SDK 24. It requires an Android SDK and a compatible Android Studio/JDK/Gradle setup. API 36 migration and corresponding toolchain verification are outstanding for current Play submission requirements.

On a configured development machine, run from this project root:

```bash
./gradlew assembleDebug lint
./gradlew bundleRelease
```

These are intended validation commands, not commands that passed in this session. Release signing must be configured with your own upload key; do not commit credentials or keystores. Complete the product/policy redesign and release checks in the audit before publishing.

Screenshots are upstream reference images and have not been regenerated for the modified source. Confirm ownership/license and replace upstream identity/update configuration before distribution.
