# Call Capture 1.3.0 — legal and local-data controls candidate

Read APP_OVERVIEW_EN.md for detailed current behavior. English legal text is in app/src/main/assets/legal/documents.json, rendered in the app and provided as HTML/Markdown drafts under legal-documents/.

Implemented: Privacy Policy, Terms, retention/deletion and recording/privacy notice; versioned Terms receipt; separate feature consent; refusal with saved-data access; withdrawal/pause; confirmed local deletion with partial-failure reporting; publication configuration guards; no Accessibility; no Internet permission or legacy APK updater; no destructive Room migration fallback.

Current release blockers: real developer identity/contact and public policy/Terms URLs are missing. Android build/lint/device tests and store declarations are not completed. No APK was built or published.

Set verified values in app/src/main/res/values/legal_config.xml, then run `python3 release-tools/verify_release_config.py --export`. Host the exact configured HTML. The Gradle preReleaseBuild hook also validates required publication fields. These guards do not verify live hosting or grant Play approval.

The source is based on the recovered reviewed backup plus changes; it is not the lost latest design project. Earlier notes under history/ are historical and may describe superseded behavior.
