# বর্তমান আপডেট — Legal / Privacy / Data controls

চারটি বিস্তারিত ইংরেজি document: Privacy Policy, Terms of Use, Data Retention and Deletion, Recording and Privacy Notice। একই JSON থেকে অ্যাপ পড়বে; HTML/Markdown draft-ও আছে। APP_OVERVIEW_EN.md-এ সম্পূর্ণ app behavior লেখা আছে।

প্রথম launch-এ Terms checkbox unchecked। Terms গ্রহণ করলেই sensitive feature চালু নয়; প্রতিটি feature-এর disclosure/consent আলাদা। না মানলেও saved data দেখা ও delete করা যায়। Local Terms version/time receipt, feature consent receipts এবং agreement withdrawal আছে।

Manage local data থেকে pause all, withdraw agreement, confirmed delete saved recordings/messages। Pending recording save শেষ হওয়ার অপেক্ষা, private directory containment এবং partial-failure feedback আছে। Android permission নিজে revoke হয় না; source chat/exported copy মুছে না। No app account; nonexistent web account deletion দাবি নেই।

পুরোনো GitHub APK updater সরানো ও INTERNET permission বাদ। Cleartext traffic disabled। Room destructive migration fallback সরানো, যাতে ভবিষ্যতের অজানা version change-এ silent data loss না হয়। SDK37 ও আগে অনুমোদিত notification-based architecture বহাল।

এখনও developer name, privacy-contact email, public HTTPS privacy URL ও Terms URL দেওয়া নেই। legal_config.xml-এ বাস্তব values বসাতে হবে। Python publication checker expected NOT READY দেখায়; Gradle release guard যোগ হয়েছে, কিন্তু Gradle চালিয়ে পরীক্ষা হয়নি। সব placeholders পূরণ ও hosted content verify না করে release নয়।

পাস: ৭টি SQLite regression, legal JSON/XML/TOML, version/text consistency, consent/data-control wiring, no-network/updater manifest scope, source archive checks। Kotlin/Android compilation, real UI/device deletion/recording tests হয়নি। APK বানানো হয়নি।

সরবরাহটি উদ্ধার করা পুরোনো backup-ভিত্তিক পূর্ণ source; হারানো latest dashboard source নয়। সম্পূর্ণ Google compliance বা legal protection নিশ্চিত বলা হয়নি। RELEASE_CHECKLIST.md অনুসরণ করুন।
