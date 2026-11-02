# Google Play Data safety — evidence worksheet, not an automatically completed form

Scope: this source candidate only. Review the final binary, merged manifest, transitive SDKs and every active Play-distributed version of the package before answering. The Play form's definitions of collection/sharing are different from simply accessing local data.

| Area | Current implementation | Submission review |
|---|---|---|
| Microphone audio | Optional private recording files; no automatic upload | Explain purpose in Privacy Policy/consent even if local-only processing does not count as off-device collection. |
| Messages | Optional selected notification text/labels/times in a no-backup database | Sensitive local access requires clear disclosure/consent. Do not claim all deleted chats can be recovered. |
| Caller/conversation labels | Used when provided by supported notifications | Personal information still exists locally; it is not contact-book permission or proof of caller identity. |
| Sharing | Only deliberate selection of an audio file and receiving app | Review the official user-initiated transfer exception; do not assume every form of sharing is exempt. |
| App network | INTERNET permission and external APK updater removed | Confirm merged manifest and final dependencies introduce no unreported path. Browser/email links run in separate user-chosen apps. |
| Accounts | None | Do not advertise a nonexistent account-deletion backend. Add both app and web request paths if account creation is introduced. |
| Ads, analytics, identifiers | No advertising/analytics/account SDK in declared dependencies | Audit transitive dependencies and the final release. Never add an SDK without revisiting declarations. |
| Storage protection | Android private storage and backup exclusions; no app-level encryption vault | Do not claim end-to-end encryption or encryption of an uploaded data stream that does not exist. |
| Retention | Audio until deletion; message cap/window pruned on read/write | Explain timing honestly; do not claim exact timed erasure. |
| Deletion | Confirmed local deletion, pause, withdrawal, per-entry controls | Explain exports/source chats are unaffected; report partial failures. |
| Privacy policy | App text and prepared HTML; real public URLs/contact not supplied | Supply verified identity/contact/URLs and host reviewed documents before release. |

Source: https://support.google.com/googleplay/android-developer/answer/10787469
Privacy/User Data: https://support.google.com/googleplay/android-developer/answer/10144311

Do not use the presence of Terms or consent as a substitute for a permitted feature purpose. Final Google review and runtime behavior remain decisive.
