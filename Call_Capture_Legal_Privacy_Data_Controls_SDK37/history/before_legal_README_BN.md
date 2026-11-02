# বর্তমান পরিবর্তন ও সীমা

এই version-এ Accessibility-based call recording বাদ। service declaration, service source, screen-reading config এবং UI permission path সরানো হয়েছে। কোনো hidden setting, root/ADB বা privilege bypass নেই।

## ফিচারের প্রকৃত আচরণ

| ফিচার | আচরণ |
|---|---|
| Call detection | নির্বাচিত WhatsApp/Business/Messenger/Lite/Telegram package-এর CATEGORY_CALL notification; cellular-এর জন্য phone-state signal। Ambiguous notification-এ capture শুরু নয়। |
| Automatic start | Channel consent আছে, app-এর Activity visible, microphone granted এবং connected-call signal থাকলেই standard microphone FGS attempt। Android reject করলে tap prompt/error; background নিশ্চয়তা নয়। |
| Tap fallback | User notification tap করলে MainActivity সামনে আসে, current offer token ও access যাচাই করে start। In-app Start microphone recording button-ও আছে। Notification permission off হলে app-এর offer card দেখা যাবে। |
| Stop | Matching notification removal/category replacement বা cellular idle signal। Notification lifecycle call lifecycle-এর নিখুঁত সমতুল্য নয়; miss/false signal হতে পারে। User Stop এবং existing watchdog fallback থাকে। |
| Re-start suppression | User Stop-এর পরে সেই call key-এর repost auto-start করে না; removal পর্যন্ত suppressed। পুরোনো notification action invalid token হলে ignore। |
| Audio | Ordinary microphone input, direct remote call stream নয়। অন্য calling app microphone busy/Android policy-র কারণে silence/failure হতে পারে। সফল short nonempty clip-ও সংরক্ষণ করে। |
| Message history | পৃথক consent, selected WhatsApp/Messenger notifications-এর local copy, real-time list/search/pause/delete। Notification remove saved copy delete করে না। Hidden/old deleted text/media recovery নয়। |
| Permissions | প্রথম launch guided steps, live tick এবং Permission health। Accessibility নেই। Battery troubleshooting optional। |
| Privacy | Settings → Privacy and data। Audio/messages স্বয়ংক্রিয় upload নেই; selected audio user নিজে share করতে পারে। Update check release provider-এর network request করতে পারে। |

আগের call-channel toggles নতুন notification-based purpose-এর সম্মতির জন্য একবার reset to off। Saved audio/messages মুছে ফেলা হয়নি। Existing data/schema migration ও fresh install বাস্তব ফোনে পরীক্ষা জরুরি। New versionCode3/name1.2.0; SDK37 config আগের মতো।

## যা এখনও নিশ্চিত নয়

এটি Play Store অনুমোদনের নিশ্চয়তা নয়। Accessibility-related প্রধান বাধাটি সরানো হয়েছে, কিন্তু remaining message handling, notification access, microphone FGS, store claims, privacy policy/Data safety এবং actual implementation Google review করবে। Public privacy URL/developer contact এবং demo videos আপনাকে বাস্তব তথ্য দিয়ে পূরণ করতে হবে। Notification-based auto-stop ও অডিও compatibility test ছাড়া সব ফোনে কাজ করে বলা যাবে না।

## Validation

SQLite message-history-এর ৬টি regression test pass। XML/TOML parse, manifest scopes, Accessibility/source path absence, narrow package/category filtering, visible-activity gate, pending-intent token and Stop suppression source checks, ZIP integrity pass। Source checks Android runtime test নয়।

চালানো হয়নি: Gradle sync, Kotlin/Android compile, SDK37 lint, UI rendering, call/audio/device delivery tests। এই environment-এ SDK37 build toolchain নেই। APK বানানো হয়নি।

এই folder পূর্ণ project। পুরোনো permission patch script ব্যবহার করবেন না; সেটি Accessibility-ভিত্তিক পুরোনো UI আবার বসাতে পারে। এটির ভিত্তি উদ্ধার করা পুরোনো reviewed source; হারানো সর্বশেষ dashboard source নয়।

পরবর্তী কাজ: RELEASE_CHECKLIST.md।
