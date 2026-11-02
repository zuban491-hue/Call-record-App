# Call Capture — Privacy Policy

Version 2026-11-03.1 · Effective 3 November 2026

Publication draft: supply real {{DEVELOPER_NAME}}, {{CONTACT_EMAIL}}, {{PRIVACY_URL}} and {{TERMS_URL}} before publication.

## About this policy

Call Capture provides optional microphone recording, call-notification or phone-state detection, and a local history of selected messaging notifications. This policy describes this version of the Android app. It is not affiliated with WhatsApp, Meta, Messenger, Telegram or Google. Developer: {{DEVELOPER_NAME}}. Privacy contact: {{CONTACT_EMAIL}}.

## Information processed on your device

When you choose recording, the app processes microphone audio and stores audio files with recording time, duration, size, app/call-type label, and caller or conversation labels supplied by a supported notification. User-entered notes and starred status are also stored. Cellular phone-state signals may indicate ringing, connected or idle, but this build does not request contacts or call-log access and does not promise caller identification.

## Message notification history

After separate affirmative consent and Android notification access, the app saves new notification text, sender/conversation labels, source app and times for the WhatsApp or Messenger packages you select. Android access can expose a broader stream of notifications; this feature filters to its supported selected apps. Call detection separately inspects supported call notifications only when its channel is enabled. The app does not read chat databases, app screens or Accessibility content. It cannot recover old, hidden or protected messages, download chat media, or reliably identify which message was deleted or unsent. Android may redact sensitive text.

## Purposes and optional access

Audio is used to create and play the recordings you enable. Call signals are used for call prompts and supported start/stop behavior. Notification text is used only for the local message-history feature. Permission and listener state are used for feature guidance. Microphone, phone state, app notifications and notification-listener access serve different purposes. Android permission approval and accepting Terms are not substitutes for the separate feature consents. You may decline optional features and still view saved recordings and messages.

## How recording behaves

Automatic microphone start is attempted only while this app has a visible activity, the selected channel is enabled, microphone access is available and a connected-call signal is received. Otherwise an explicit tap-to-record prompt can bring you to the app. Recording captures microphone input; both sides of another app's call may be unavailable, silent or blocked. A matching call-notification removal or phone-idle event may stop the recording. Signals can be missed or inaccurate, so a visible Stop control remains important. Speakerphone routing changes occur only if you enable that separate option.

## Storage and security

Saved audio and metadata use app-private device storage. Message history uses private SQLite storage in the app's no-backup directory. Current backup rules exclude recording files, app databases and preferences. Android sandboxing, device locking and operating-system storage protection help restrict access. This build does not add its own password vault, end-to-end encryption or application-level database encryption. No security control guarantees protection on a compromised, rooted or shared unlocked device. HTTP cleartext traffic is disabled. Sharing uses Android FileProvider with temporary read access to selected recordings.

## Sharing, external links and third parties

There is no feature that automatically uploads audio files, notification messages or caller labels to the developer. This build does not request Android Internet access and contains no in-app updater, advertising SDK, analytics SDK or cloud-sync client. If you deliberately share a recording, the receiving app or recipient receives the selected file and its own policies apply. Exported or shared copies are outside this app's deletion controls. Opening a published policy or support link hands the action to another app, such as your browser or email app; that app may make network requests under its own policies. A support email opens a draft, and you choose whether to send it and what to include. Do not attach sensitive material unless necessary and intended.

## Accounts, advertising and tracking

This build has no app account creation or login, advertising SDK, analytics SDK, cloud-sync account or sale of personal data. Local Terms receipts record the accepted document version and time; feature consents record their version/time locally. These receipts are not uploaded. Introducing accounts, analytics, ads, uploads or a new provider requires updating the implementation, disclosures and store declarations before use.

## Retention

Recordings, recording metadata and notes remain until you delete them or remove the app. Empty or failed captures may be discarded; successful nonempty short recordings can be saved. Saved message entries are limited to 5,000 entries and a 30-day retention window. Older entries are pruned when message history is opened or updated, not by a guaranteed exact-time deletion scheduler. Individual message deletion leaves a one-way deduplication identifier, without the message text, for up to that retention window to prevent replay from re-posted notification bundles. Delete all message history also removes these identifiers. Local settings and consent receipts remain until withdrawal, Android Clear storage or app removal, as applicable.

## Your controls and deletion

Records supports deleting individual or selected audio entries. Messages supports pausing, choosing apps, deleting individual copies and deleting all message history. Settings > Manage local data can pause features, withdraw the Terms receipt, or stop capture and delete saved recordings/messages. A destructive action requires confirmation. If an audio file cannot be removed, the app reports incomplete deletion and keeps its row for retry. Pausing or withdrawing consent does not itself delete saved content. Android settings can revoke permissions at any time. Deleting local content does not delete source chats, recipients' copies, exports or copies saved elsewhere. You can also use Android Clear storage or uninstall to remove this app's private local data; Android controls those operations.

## Children and lawful use

The app is not designed for children or use as a child-monitoring service. Use it only if you meet the applicable age and legal-capacity requirements. Do not install or use it covertly on another person's device. Obtain any notices, agreement or authorization required for recording and handling other people's communications in your location. One user's permission does not remove other people's rights.

## Changes and contact

Policy changes will be reflected by a new document version. Changes to data access or purposes require appropriate updated disclosures and affirmative consent before the changed processing begins. For questions or privacy requests, contact {{CONTACT_EMAIL}} or the developer contact in the official store listing. The published policy URL is {{PRIVACY_URL}}. This local-only build cannot retrieve private recordings from your device remotely; use the in-app deletion controls for local copies. Any support correspondence you choose to send is handled through the contact channel you select.
