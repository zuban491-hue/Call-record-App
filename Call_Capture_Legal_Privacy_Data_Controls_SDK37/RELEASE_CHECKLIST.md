# Publication checklist — not completed by adding legal text

1. Supply real developer identity, direct privacy-contact email and public HTTPS Privacy Policy/Terms URLs in legal_config.xml. Match the actual app/store listing. No placeholder/example URLs or PDF policy.
2. Run release-tools/verify_release_config.py --export. Publish the configured HTML, remove any draft-only notice, verify worldwide public read-only access and link exact documents in Play Console. The app resolves the same fields from resources.
3. Read DATA_SAFETY_WORKSHEET.md and complete Data safety for the final binary, transitive SDKs and all active package versions. Do not label local-only access as off-device collection by default or conceal user-directed sharing.
4. Review microphone foreground-service eligibility and provide correct Play declarations/demo videos. Notification history needs specific consent and a policy-compliant purpose. No Accessibility remote call recording, hidden capture or universal audio claims.
5. Verify source ownership, package-name ownership, the original project's license/redistribution rights and dependencies' license notices. The recovered source did not include a top-level LICENSE file; do not assume redistribution rights are settled.
6. Configure appropriate audience/content rating and accurate store text/screenshots. No children-targeted monitoring, invented security guarantees, unverifiable compatibility claims or manipulated review flow.
7. Install SDK37/Build Tools36/JDK17 and run Gradle sync, compilation, lint, tests and final merged-manifest/dependency review. The added preReleaseBuild publication guard has not been run with Gradle in this environment.
8. Test existing DB upgrade without data loss; unknown migrations now fail rather than erase the database. Test Terms accept/decline/withdraw/reaccept, separate feature consents, permission denial/revoke, call detection/tap/Stop, private-path deletion, save timeout, file/DB failure, concurrent delivery, exports and device storage limits.
9. Test this device matrix: API24/32/33/34/37 and stock Android, Infinix, Samsung and Xiaomi where available. Record real limitations in the listing.
10. Do not publish until missing identity/contact/URLs and runtime issues are resolved. No APK or AAB was built here. No approval guarantee is made.

Primary references:
https://support.google.com/googleplay/android-developer/answer/10144311
https://support.google.com/googleplay/android-developer/answer/10787469
https://support.google.com/googleplay/android-developer/answer/13392821
https://support.google.com/googleplay/android-developer/answer/16558241
https://support.google.com/googleplay/android-developer/answer/18265346
