# Call Capture 1.2.0 — notification-based recording candidate

This source replaces Accessibility-based call triggers with narrow notification/phone-state signals and an explicit tap fallback. It is a review candidate, not a Play-approved or device-tested release. See README_BN.md and RELEASE_CHECKLIST.md first.

- Records / Messages / Settings, guided setup and Permission health.
- No Accessibility service, screen scraping, system call-audio source or silent activity launch.
- Opt-in notification text history stays on device; pause and deletion controls.
- Call channels are off by default and require renewed disclosure/consent on upgrade.
- Automatic microphone start is attempted only with a visible app activity, microphone permission, an enabled call channel and a connected-call signal. Otherwise the user taps a notification or in-app button to start capture.
- Exact matching notification removal/phone idle stops its recording; manual Stop is always needed as a fallback. Signal reliability and remote audio availability vary.
- compileSdk / targetSdk 37, AGP 9.1.1, Gradle 9.3.1, JDK 17, minSdk 24. No APK is included.

Built from the recovered reviewed backup plus subsequent changes, not the lost latest design source. Historical audit notes are under history/ and do not describe the current architecture.
