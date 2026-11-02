# Required before any Play submission

## Developer facts and declarations
- Supply the real developer identity/contact and host the reviewed privacy policy at an active public URL. Link that exact policy inside the app and in Play Console. The in-app Privacy and data page is present; a public policy/contact is not configured by this source delivery.
- Complete Data safety using actual flows. Local-only processing is not automatically off-device collection. Include any release-provider requests, user-directed sharing, and all future third-party SDKs accurately.
- Declare the microphone foreground-service use case in Play Console and provide an accurate video showing start, visible notification, Stop, access denial and failure feedback.
- Do not claim guaranteed two-way recording, universal background automation, protected-message recovery or verified unsend detection. Do not market this as hidden monitoring.
- Verify notification-access/message-history purposes and consent against User Data, Spyware and Device/Network Abuse policies. Removing Accessibility is a risk reduction, not approval.

## Build and data checks — not yet executed
- Install Android platform 37, Build Tools 36.0.0, JDK17 and a compatible Android Studio (AGP9.1.1 / Gradle9.3.1); run Gradle sync, Kotlin compilation and lint without suppressing unsupported-SDK warnings.
- Verify Room2.8.5/KSP2 upgrade preserves existing recording databases. Test update and fresh install; do not use uninstall as an upgrade test if it would destroy user recordings.
- Validate privacy/local-storage statements against the final binary and all dependencies. Confirm no upload of message or audio content.

## Device behavior — not yet executed
- Android24/32/33/34/37; Infinix, Samsung, Xiaomi and stock Android where available.
- Each supported package, incoming/outgoing call, CATEGORY_CALL absent, notification category replacement/removal, group summary and language variation. Missing call notifications must result in no invented detection.
- App visible auto attempt vs background tap fallback. Permission denied/revoked, notification channel disabled, microphone busy and FGS denial.
- Token stale/forged click, repeated notifications, manual Stop followed by repost, simultaneous calls, short clips, empty capture, low storage and process death.
- Confirm app does not automatically start activities, force speakerphone without the user option, retain the microphone indefinitely, or revive a manually stopped session.
- Message opt-in/decline, selected-app filtering, pause/resume, deletion/tombstones, redaction, retention and live-list updates.

## Primary references
https://support.google.com/googleplay/android-developer/answer/16558241
https://support.google.com/googleplay/android-developer/answer/10144311
https://support.google.com/googleplay/android-developer/answer/14745000
https://support.google.com/googleplay/android-developer/answer/13392821
https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start
https://developer.android.com/develop/background-work/services/fgs/service-types

No APK was built or published. Final approval is Google's decision after review.
