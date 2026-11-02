# Call Capture — Detailed App Overview

Version 1.3.0, source candidate. Updated 3 November 2026.

This document describes the implemented source, not a claim of successful Android compilation, device testing or Google Play approval. The app is based on the recovered reviewed project, not the lost latest design project.

## Purpose

Call Capture is an Android app for managing local microphone recordings and an optional history of selected messaging notifications. Its bottom navigation contains Records, Messages and Settings. It prioritizes explicit choices, visible recording controls, understandable permission status and direct deletion of local copies.

It is not a messaging client, telecommunications provider, remote monitoring service, encrypted vault, deleted-chat recovery tool or source of guaranteed two-way call audio. It has no app account, login, developer cloud archive, advertising SDK, analytics SDK, in-app billing or subscription in this build.

## First launch and agreement

Before optional features are enabled, users can read the English Privacy Policy and Terms of Use. A checkbox is unchecked by default. Agreeing creates only a local receipt containing the Terms version and acceptance time. This does not grant Android access or turn on capture.

Users who do not agree can open existing saved data and use deletion controls. New capture is gated while the current Terms version is not accepted. On startup without current agreement, call channels and message saving are paused. A changed Terms version requires renewed review. Feature activation remains a separate action after that review.

The guided setup presents one access step at a time. Android prompts are launched only after a user tap. Runtime results and return from Android Settings update status automatically. Denial, permanent denial, optional skip and later review have explicit routes. No Accessibility service or screen-reading permission is present.

## Records and call signals

Call channels are disabled by default. Enabling each channel displays a specific explanation and requires agreement to its data use. Selected WhatsApp, WhatsApp Business, Messenger/Lite or Telegram packages can provide call notifications. Only supported packages and the public call category are used for this path; ambiguous social/message notifications do not start recording. Cellular detection uses phone-state events without requesting contacts, call-log or outgoing-call interception permissions.

An automatic microphone start is attempted only when the app has a visible Activity, the channel is enabled, microphone access is granted and a connected-call signal is observed. Android can still reject the foreground service or microphone capture. With the app in the background, the app presents a Tap to record notification where notification permissions allow it. That explicit tap opens the app; the app validates the current call-offer token before starting. A matching offer also appears in Records with a microphone-review or start button.

Audio is ordinary microphone input. Other apps may hold the microphone, block capture, suppress relevant notifications or keep the remote participant inaudible. A successful start does not prove that both sides were recorded. The user must obtain whatever notices or agreement are legally required before recording.

Live status distinguishes Starting, Recording with an elapsed timer, Saving, Saved and Error. A saved row is reported only after the database insert succeeds. Successful nonempty short clips are retained; failed or empty output may be discarded. Saved recordings can be played, searched, filtered, starred, annotated, shared by the user's deliberate action, or deleted.

A matching call notification removal/category change or cellular idle signal can stop the associated recording. These signals are imperfect. The user can always use Stop, and the existing watchdog is a fallback rather than a guarantee. An explicit user Stop suppresses automatic restart from reposts of the same call key until that signal is removed.

## Messages

Message history has separate consent and is off by default. Android notification access is different from permission to display this app's own notifications. Although Android may deliver a wider notification stream, the history store filters to supported selected WhatsApp/Messenger packages.

Only newly eligible notification text, sender/conversation labels, source package and timestamps are saved. No old active-notification import or chat-database access is performed. The parser uses AndroidX compatibility support. Stable message identities reduce duplicate saves while allowing distinct messages with the same body. A local copy can remain after the source sender deletes a message, but the app does not know whether that message was actually unsent.

Search, selectable message text, per-app switches, pause, individual delete and delete-all are provided. The list reacts to successful writes; a latest-messages button helps when the user has scrolled. Android redaction, muted/hidden notifications, source-app choices and device background limits can prevent capture. Images, voice notes and old protected/deleted conversations are not recovered.

## Permission health

Settings explains which access is configured, which enabled feature lacks access and whether the notification listener is connected. Permissions are checked on foreground/resume, with settings observation and a foreground-only fallback check. A permission tick does not certify service health, phone security or capture compatibility. Battery exemption is optional troubleshooting and does not override platform microphone restrictions.

## Privacy, local storage and network boundaries

Recordings use app-private files and a Room database. Notification history uses a private no-backup SQLite database. Backup exclusions cover audio files, databases and preferences. Android sandboxing and device security protect ordinary access; this app does not implement a separate encryption vault or promise forensic secure erasure.

No Internet permission, automatic uploader, in-app APK downloader, external updater, ad SDK or analytics SDK is included. Published policy links and contact email actions open other apps only on request. Those apps control their own networking and privacy practices. Sharing an audio file uses a temporary Android FileProvider grant and cannot be recalled by deleting the original copy here.

The app does not read contacts, call logs, location, camera, SMS messages or other apps' private storage. Source call-notification labels may still contain personal information, which is covered by the disclosures.

## Retention, consent withdrawal and deletion

Audio and metadata remain until deletion or app removal. Message entries are limited to 5,000 and a 30-day window, pruned on reads/writes rather than by an exact-time scheduler. Individual deletion retains a temporary one-way replay identifier without the body; bulk clear also deletes identifiers.

Manage local data can pause all capture, withdraw the local Terms receipt, or confirm deletion of saved recordings/messages. Capture is paused first; deletion waits for recording finalization before touching audio files. File removal is restricted to the app's private recordings directory. Failures or timeouts are reported as incomplete operations rather than full success. Existing settings and Terms receipts are retained by the saved-copy deletion action unless separately withdrawn or cleared through Android storage controls.

There is no account to delete in this build. If accounts or cloud storage are introduced later, both in-app and external account/data-deletion workflows must be implemented before launch.

## Required before publication

Supply a real developer identity, privacy contact email and public HTTPS Privacy Policy and Terms URLs in `legal_config.xml`. The release publication task and configuration checker reject missing values. Export the configured documents, host them, and verify they are publicly accessible and match the app. Complete Data safety, foreground-service declarations, target-audience/content-rating questions, demo material and source/dependency licensing review honestly.

Android compilation, lint, migration testing, device audio tests, consent/refusal flows and end-to-end deletion tests remain required. SDK37 and legal documents do not guarantee approval or compatibility.
