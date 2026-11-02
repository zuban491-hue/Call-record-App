# Call Capture — Data Retention and Deletion

Version 2026-11-03.1 · Effective 3 November 2026

Publication draft: supply real {{DEVELOPER_NAME}}, {{CONTACT_EMAIL}}, {{PRIVACY_URL}} and {{TERMS_URL}} before publication.

## No app account to delete

This build does not create an app account or store your recordings in a developer cloud account. Local-data deletion is available in the app. An account-deletion web request would become necessary if a future version introduces in-app account creation; no nonexistent account-deletion service is claimed here.

## Recordings

Open Records to delete individual or selected recordings. Audio, associated metadata and notes remain until deletion or app removal, except empty/failed captures. Shared/exported copies are outside the app's control.

## Messages

Open Messages > Manage history to pause saving or delete all saved notification text. Individual delete removes the text and retains a temporary one-way identifier to suppress replay from notification bundles. Delete all removes text and those identifiers. Retention is bounded by 30 days and 5,000 entries, with pruning on read/write rather than a timed guarantee.

## Pause or withdraw

Pause all features turns off call channels and message saving and requests that an active recording stop. It does not revoke Android access or erase saved copies. Withdraw Terms also clears the local agreement receipt and prevents new feature capture until review and renewed opt-in. Existing local data remains available to view or delete.

## Delete saved recordings and messages

Settings > Manage local data offers a confirmed deletion action. It pauses capture, waits for an active recording to finish saving and then removes local saved recordings/messages. Settings, document files and the app itself are not removed. If saving times out or a file cannot be deleted, the app reports failure or partial completion; it does not claim successful complete deletion.

## Other copies and device operations

Deletion does not affect the original WhatsApp/Messenger chats, other apps, shared files, recipients, exports, screenshots or backups you made outside this app. Revoking Android permissions prevents future access but does not delete existing copies. Android Clear storage or uninstall removes private app data; Android controls those operations. Physical secure erasure of flash storage is not guaranteed by an ordinary file delete.

## Privacy requests

For questions about the app or a support message you sent, contact {{CONTACT_EMAIL}}. We cannot remotely inspect or delete files in this local-only app. Use the in-app controls, or Android app storage controls, for private local data.
