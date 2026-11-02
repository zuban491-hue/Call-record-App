# Limited cleanup changelog

This is a separate reviewed copy of the uploaded source. It is NOT Play Store-ready.

- New/missing preference defaults: all four recording channels and speakerphone assistance are OFF. Existing stored preferences remain unchanged.
- Channels without an explicit settings control now remain disabled in both VoIP listeners.
- Removed size/duration-only “Clean Empty / Short Recordings” deletion action and its ViewModel method. Legitimate short recordings must not be treated as empty.
- Added confirmation before single-recording deletion.
- Excluded recordings, database and preferences from configured cloud backup and device transfer routes. Users must deliberately export files; OEM restore behavior still requires verification.
- Narrowed FileProvider to internal recordings directory.
- Replaced 100% remote-audio clarity claim and clarified privileged audio-source labels and empty-state limitations.
- Replaced README with accurate review status, build instructions and limitations; detailed Bengali audit included.

Not implemented: Play-compliant product redesign, permission removal/migration, API 36 migration, lifecycle/audio reliability fixes, encryption, localization, release signing, or device verification. Existing accessibility call detection remains a blocker. No silent capture, protection bypass, remote upload or capture-capability enhancements were added.

Validation: XML parsing and change-scope/ZIP integrity checks only. Gradle wrapper failed to download its distribution due to network unavailability before compilation. No Kotlin build, lint, automated Android tests or physical-device testing completed.
