# Call Capture — সোর্স কোড ও প্রকাশযোগ্যতা অডিট

পর্যালোচনার তারিখ: ২ নভেম্বর ২০২৬। ইনপুট: android-call-capture-main.zip।

## সিদ্ধান্ত

বর্তমান সোর্স এবং এই সীমিত cleanup copy—কোনোটিই এখন Google Play production release হিসেবে প্রস্তুত নয়। প্রধান বাধা হলো Accessibility-নির্ভর তৃতীয় পক্ষের কল রেকর্ডিং, অযোগ্য Call Log permission, API 35 target এবং যাচাইহীন দুই পাশের অডিও রেকর্ডিং দাবি। কেবল permission disclosure বা UI বদলালে মূল সমস্যা মিটবে না।

এই রিপোর্ট একটি static code review। ২৯টি Kotlin ফাইল, Manifest, Gradle configuration, database/preferences, XML resources, permissions flow, recorder/service, player/UI এবং README পর্যালোচনা করা হয়েছে। APK চালানো, বাস্তব কল রেকর্ড করা, device compatibility বা Play approval পরীক্ষা করা হয়নি। এটি penetration test বা পূর্ণ লাইসেন্স যাচাইও নয়।

## প্রকৃত কার্যপদ্ধতি

PhoneCallReceiver, VoipAccessibilityService ও VoipNotificationListenerService সম্ভাব্য কলের ঘটনা শনাক্ত করে CallRecordingService শুরু করে। এরপর MediaRecorderEngine মাইক্রোফোনের অডিও AAC/M4A করে অথবা PcmWavRecordingEngine AudioRecord দিয়ে PCM নিয়ে WAV বানায়। Room-এ title, caller, file path, duration, notes ইত্যাদি থাকে। Compose UI ফাইল দেখায়, বাজায়, share/delete করে।

কল শনাক্ত হওয়া, অডিও ফাইল তৈরি হওয়া এবং উভয় বক্তার কথা শোনা যাওয়া আলাদা ফলাফল। VOICE_COMMUNICATION একটি voice-tuned microphone source; WhatsApp/Telegram-এর internal remote audio পাওয়ার API নয়। VOICE_CALL ও VOICE_DOWNLINK-এর system privilege সাধারণ runtime permission দিয়ে পাওয়া যায় না। Speaker audio মাইক্রোফোনে পৌঁছাতে পারে, কিন্তু route, concurrent microphone access ও OS policy অনুযায়ী নীরব বা এক পাশের অডিও হতে পারে। Gain অনুপস্থিত remote audio উদ্ধার করে না। MediaProjection-ও সব VoIP audio ধরার সাধারণ সমাধান নয়; playback capture-এর usage ও source capture policy সীমা আছে। [S1–S3]

## প্রকাশের বাধা

| বিষয় | সোর্সে প্রমাণ | প্রয়োজনীয় সিদ্ধান্ত |
|---|---|---|
| Accessibility call recorder | VoipAccessibilityService ও voip_accessibility_service_config.xml | Google Play remote call audio recording-এর জন্য Accessibility API চাওয়া অনুমোদন করে না। এই কার্যপদ্ধতির redesign দরকার; কেবল নাম পাল্টানো বা consent form যথেষ্ট নয়। [S4] |
| Call Log | Manifest-এর READ_CALL_LOG; PermissionSetupScreen-এর Grant Phone | Google-এর invalid use cases তালিকায় Call recorder আছে। এই অ্যাপের ঘোষিত কাজ দিয়ে permission ন্যায্যতা পায় না। Play সংস্করণ থেকে বাদ দেওয়ার পরিকল্পনা প্রয়োজন। [S5] |
| Target API | app/build.gradle.kts: compileSdk=35, targetSdk=35 | যাচাইকৃত বর্তমান নিয়মে নতুন অ্যাপ/আপডেটে API 36 প্রয়োজন। SDK, toolchain ও behavior migration একসঙ্গে করতে হবে। শুধু সংখ্যা বাড়িয়ে release নয়। [S6] |
| Privacy | setup-এ ছোট permission description আছে; পূর্ণ পৃথক consent/data policy flow নেই | কী অডিও/নাম/notification data ব্যবহৃত হয়, কোথায় থাকে, কীভাবে share/delete হয়—সুস্পষ্ট disclosure, affirmative consent এবং privacy policy প্রয়োজন। [S7] |
| কার্যক্ষমতার দাবি | Settings-এর 100% clarity; README-এর universal/compliant দাবি | নির্দিষ্ট device evidence ছাড়া universal, guaranteed, 100% বা সব Android support দাবি সরাতে হবে। |

নীতির উদ্ধৃতি থেকে এই অ্যাপের প্রকাশযোগ্যতা সম্পর্কে সিদ্ধান্ত আমার মূল্যায়ন; Google Play review-এর পূর্বানুমোদন নয়। সব ধরনের call recorder নিষিদ্ধ—এমন সাধারণ সিদ্ধান্ত এই রিপোর্ট দিচ্ছে না। এই সোর্সের নির্দিষ্ট API/permission ব্যবহারই সমস্যা।

## প্রধান engineering findings

নিচের file/line উল্লেখ মূল ZIP-এর সোর্স অনুসারে; পরিবর্তিত copy-তে line সরে যেতে পারে। P0 = প্রকাশের বাধা, P1 = ডেটা/স্থায়িত্বের গুরুতর সমস্যা, P2 = ব্যবহারযোগ্যতা/রক্ষণাবেক্ষণ।

| ID | অগ্রাধিকার ও অবস্থান | পাওয়া সমস্যা ও প্রভাব | করণীয়/অবস্থা |
|---|---|---|---|
| 01 | P0; Accessibility service | তৃতীয় পক্ষের call recording trigger হিসেবে ব্যবহার | policy-compliant product flow redesign; বাকি আছে |
| 02 | P0; Manifest/PermissionSetupScreen | READ_CALL_LOG call-recorder use case | Play build থেকে অপসারণ; বাকি আছে |
| 03 | P0; app/build.gradle.kts | target API 35 | API 36 migration ও regression test; বাকি আছে |
| 04 | P1; AudioRecorderEngine.kt:115,295 | success শুধু size>0 বা >44; silent audio-ও সফল হতে পারে | container playable কি না ও captured audio quality আলাদা ফলাফল হিসেবে দেখানো; বাকি আছে |
| 05 | P1; CallRecordingService.kt:164 | microphone FGS SecurityException হলে type=0 দিয়ে আবার চেষ্টা | এটিকে বৈধ workaround ধরে নেওয়া যাবে না। অনুমোদিত user action flow এবং স্পষ্ট failure UI দরকার; বাকি আছে [S8] |
| 06 | P1; VoipNotificationListenerService | startRecording() call-এ exception guard নেই | OS-rejected start অ্যাপ ব্যর্থ করতে পারে; failure state ও UI দরকার; বাকি আছে |
| 07 | P1; CallRecordingService.kt:215–278 | async stop/save চলার সময় shared mutable caller/type/timestamp বদলাতে পারে; পুরোনো coroutine নতুন session-এর মধ্যে stopSelf করতে পারে | immutable session snapshot ও serial lifecycle design; বাকি আছে |
| 08 | P1; AudioRecorderEngine.kt:262 | read thread-কে stop-এর আগে সর্বোচ্চ ১ সেকেন্ড join; timeout-এর পর writer চলতে পারে | shutdown ও WAV finalization completion নিশ্চিত করার lifecycle design; বাকি আছে |
| 09 | P1; AudioRecorderEngine.kt:195 | FileOutputStream তৈরি thread-এর try block-এর বাইরে | storage failure uncaught হতে পারে; resource handling ও failure reporting; বাকি আছে |
| 10 | P1; AudioRecorderEngine.kt:200 | read() negative error values আলাদাভাবে handle হয় না | failure loop ও misleading active state ঠেকানো; বাকি আছে |
| 11 | P1; MediaRecorderEngine.stop | stop ব্যর্থ হলেও size দেখে success; stop ছুড়লে reset/release বাদ পড়তে পারে | finally-based release ও valid-file status; বাকি আছে |
| 12 | P1; engine.start failure path | আংশিক recorder/file রেখে ব্যর্থ হওয়ার ঝুঁকি; service-এ engine assignment কেবল সফল start-এর পরে | partial resource cleanup ও explicit error result; বাকি আছে |
| 13 | P1; CallRecordingService.onDestroy | active audio stop হয়, কিন্তু স্বাভাবিক database insert পথ চলে না | interrupted recording status/recovery ও orphan reconciliation; বাকি আছে |
| 14 | P1; serviceScope | Job cancel নেই; database insert exception/IO failure সেভের flow ভাঙতে পারে | supervised scope, bounded finalization এবং error state; বাকি আছে |
| 15 | P1; PreferenceManager | recording এবং speaker default true | নতুন/অনুপস্থিত preference-এ false করা হয়েছে; বিদ্যমান saved setting বদলায়নি |
| 16 | P1; VoIP service settings | Signal/Viber/Meet ইত্যাদি settings-এ toggle ছাড়াই else=true | else=false করা হয়েছে; অসমর্থিত channel স্বয়ংক্রিয়ভাবে চালু হবে না |
| 17 | P1; MainViewModel.kt:297 | file<10000 bytes অথবা duration<4 sec হলে “empty” ধরে delete; signal যাচাই নেই, UI confirmation নেই | destructive cleanup menu ও method সরানো হয়েছে |
| 18 | P1; HomeScreen | single-item Delete সঙ্গে সঙ্গে স্থায়ী delete | confirmation dialog যুক্ত করা হয়েছে |
| 19 | P1; MainViewModel.delete* | file.delete() ব্যর্থ হলেও DB entry মুছে যায় | delete result যাচাই, failure report ও retry; বাকি আছে |
| 20 | P1; AppDatabase.kt:49 | fallbackToDestructiveMigration ভবিষ্যৎ schema upgrade-এ metadata মুছতে পারে | explicit migrations ও schema export; বাকি আছে |
| 21 | P1; backup XML | cloud-এ audio বাদ কিন্তু metadata/preferences বাদ নয়; device transfer-এ audio include | audio, database ও shared preferences দুটো route-এই exclude করা হয়েছে; restore/transfer test বাকি |
| 22 | P1; logs/filenames/notification | caller name/number logs ও file name-এ; lockscreen notification-এও দেখা যেতে পারে | PII redaction, opaque file IDs, private lockscreen display; বাকি আছে |
| 23 | P1; private audio storage | app-private files আছে, কিন্তু app-level encryption বা biometric gate নেই | threat model অনুযায়ী Keystore-ভিত্তিক encryption ও optional app lock; বাকি আছে। Android sandbox থাকা মানে encryption নেই এমন নয় |
| 24 | P2; file_paths.xml | পুরো cache ও অপ্রয়োজনীয় external-files path share scope-এ | শুধু recordings/ path রাখা হয়েছে; নিজে থেকে সব cache public হতো এমন দাবি নয় |
| 25 | P2; MainViewModel.playRecording:111 | main thread-এ synchronous prepare; error হলে user message নেই, local player resource leak হতে পারে | async player lifecycle, error state, audio focus/noisy route handling; বাকি আছে |
| 26 | P2; search/filter | search চালু হলে selected tab উপেক্ষিত; WhatsApp filter-এ Business নেই; notes search README-তে আছে, SQL-এ নেই | composable combined query ও সঠিক copy; বাকি আছে |
| 27 | P2; PermissionSetupScreen | READ_CONTACTS runtime request নেই; notification listener declared হলেও enable flow নেই; phone status refresh শুধু PHONE_STATE দেখে | feature-ভিত্তিক minimum permissions ও consistent status; বাকি আছে; Play redesign-এ অপ্রয়োজনীয় access বাদ দিতে হবে |
| 28 | P2; VoIP detectors | English UI strings ও broad package substring-এর ওপর নির্ভর; ringing/answer action-কে active call ধরে; direction notification থেকে সবসময় INCOMING | ভুল metadata ও অনাকাঙ্ক্ষিত শুরু হওয়ার ঝুঁকি; অনুমানকে verified fact হিসেবে UI-তে না দেখানো; বাকি আছে |
| 29 | P2; ContactHelper | latest call log helper কখনো name, কখনো number ফেরায়; phoneNumber field-এ name যেতে পারে; 30-sec lookup unrelated call ধরতে পারে | typed metadata, unknown state ও data minimization; বাকি আছে |
| 30 | P2; AudioRecorderEngine timing | System.currentTimeMillis দিয়ে duration; clock change হলে ভুল হতে পারে | elapsedRealtime timing; বাকি আছে |
| 31 | P2; WAV/PCM | default gain 3.5x, nonlinear boost/hard clipping; amplitude দিয়ে remote/local speaker নির্ভরযোগ্যভাবে চেনা যায় না | noise/distortion ও limitation স্পষ্ট করা; বাকি আছে |
| 32 | P2; SettingsViewModel | অন্য repository-এর GitHub update URL, hardcoded v1.0.0, 404-কে UpToDate; disconnect finally-তে নেই | নিজের release identity ও Play update channel; 404-কে unknown/error; বাকি আছে |
| 33 | P2; BootReceiver | শুধু log লেখে; কোনো পুনরুদ্ধার কাজ করে না | README-এর reboot/reliability দাবি সংশোধন; permission প্রয়োজনীয়তা পুনর্মূল্যায়ন |
| 34 | P2; HomeScreen/player/layout | fixed waveform/control widths ছোট screen/বড় font-এ জায়গা কমাতে পারে; কিছু tap target ছোট; collectAsState lifecycle-aware নয় | responsive layout, বড় font/TalkBack পরীক্ষা; বাকি আছে |
| 35 | P2; localization | অধিকাংশ লেখা hardcoded English; resourceConfigurations শুধু en | সব copy string resource-এ; বাংলা/ইংরেজি; বাকি আছে |
| 36 | P2; project packaging | app ID generic; ZIP-এ স্পষ্ট LICENSE file নেই; GitHub/creator identity hardcoded | নিজের package/branding, ownership/license যাচাই এবং notices; বাকি আছে |
| 37 | P2; build/test | test source নেই; release signing setup ZIP-এ নেই; wrapper download যাচাই হয়নি | controlled build, signing ও meaningful tests; বাকি আছে |
| 38 | P2; AudioWaveformVisualizer | animation sine-wave থেকে; রেকর্ড করা signal-এর বাস্তব waveform নয় | decorative animation হিসেবে চিহ্নিত, quality evidence হিসেবে ব্যবহার নয় |

## যেসব ভিত্তি কাজে লাগানো যাবে

Kotlin, Compose Material 3, Room Flow, R8 release configuration, app-private recordings directory, FileProvider-এর temporary URI grant, foreground notification ও Stop action, search, notes, star এবং batch confirmation আছে। এগুলো ভালো শুরুর ভিত্তি। পর্যালোচিত app source-এ audio upload বা remote-command implementation পাইনি। তবে GitHub release check internet request করে, তাই সম্পূর্ণ network-free দাবি করা যাবে না। এটি malware-free certification নয়।

## Product ও user-friendly redesign

Play সংস্করণের জন্য user-started microphone recorder এবং recording library একটি বাস্তবসম্মত দিক। অন্য apps-এর সব call রেকর্ড হওয়ার প্রতিশ্রুতি বাদ দিতে হবে। কল যদি নিজের VoIP service-এর ভেতর হয়, আলাদা consent-ভিত্তিক recording design করা যায়; তার মানে WhatsApp/Telegram কল স্বয়ংক্রিয়ভাবে রেকর্ড হবে না। শেষ পর্যন্ত Play review প্রযোজ্য থাকবে।

Onboarding-এ প্রথমেই capability/limitation বোঝাতে হবে। প্রয়োজনের সময় microphone permission, তারপর স্পষ্ট Start/Stop এবং দৃশ্যমান recording state থাকবে। Permission না দিলে library/player ব্যবহার করা সম্ভব হওয়া উচিত। Settings থেকে audio source-এর technical menu দিয়ে ব্যবহারকারীকে trial-and-error করানো কমাতে হবে।

Home-এ Ready, Recording, Finalizing, Saved, Interrupted এবং Failed আলাদা status; storage remaining; privacy control; import/export; notes/search; এবং ব্যবহারকারী বুঝতে পারে এমন error থাকবে। “Saved” শুধু file/metadata সেভ বোঝাবে; অপর পক্ষের অডিও নিশ্চিতভাবে এসেছে এমন অর্থ নয়। Silence বা decode failure হলে আলাদা সতর্ক status দরকার, স্বয়ংক্রিয় delete নয়।

Settings-এ consent/privacy, storage/retention, optional app lock, notification privacy, language এবং help থাকবে। Export user-initiated হবে; কোন app-এ share হচ্ছে তা ব্যবহারকারী বেছে নেবে। Cloud backup প্রয়োজন হলে তা আলাদা opt-in design, বর্তমান cleanup-এ যোগ করা হয়নি।

Storage হিসাব: বর্তমান mono 16 kHz/16-bit WAV প্রায় 115.2 MB/hour; raw→WAV conversion-এ সাময়িকভাবে প্রায় দ্বিগুণ জায়গা লাগতে পারে। AAC 64 kbps payload প্রায় 28.8 MB/hour, container overhead আলাদা। free-space guard ও storage-full message প্রয়োজন।

## উন্নয়ন ক্রম ও release gate

1. আগে publishable product scope ঠিক করে prohibited Accessibility recording flow ও অযোগ্য Call Log access বাদ দিন। এই cleanup copy-তে সেই বড় redesign সম্পন্ন হয়নি।
2. API 36 migration এবং compatible AGP/Gradle/JDK configuration যাচাই করুন। minimum SDK কী হবে আলাদা সিদ্ধান্ত।
3. Recorder state/session lifecycle ও interrupted-file recovery ঠিক করুন; DB migration এবং delete consistency দিন।
4. Privacy policy URL, in-app disclosure/consent, সঠিক Data safety form, foreground-service declarations ও reviewer instructions প্রস্তুত করুন। শুধু ফোনে local audio থাকলেই Data safety-তে collected বলতে হবে এমন নয়; off-device transmission এবং সব SDK/data flow দেখে উত্তর দিতে হবে।
5. নিজের app ID/branding, version, upload signing key, Play App Signing, AAB, icons/screenshots, content rating, target audience, support details তৈরি করুন। প্রয়োজনীয় permissions-এর eligibility আগে নিষ্পত্তি করুন।
6. Personal developer account ১৩ নভেম্বর ২০২৩-এর পরে তৈরি হলে প্রকাশিত বর্তমান নিয়ম অনুযায়ী অন্তত ১২ জন tester টানা ১৪ দিন closed test-এ opted in থাকতে হয়; তারপর production access আবেদন। [S9]
7. APK/build সফল হওয়া, phone-এ চলা এবং Play approval পাওয়া তিনটি আলাদা gate। এদের কোনোটির জন্য এখানে pass দাবি করা হচ্ছে না।

## প্রয়োজনীয় পরীক্ষা

- Fresh install: কোনো channel/speaker নিজে চালু নয়; permission deny/revoke, permanent denial এবং settings থেকে ফেরার state।
- Library: single/batch delete cancel, delete failure, missing file, playback error, search+tab+notes, WhatsApp Business grouping।
- Privacy: backup/restore ও device transfer-এ audio/metadata/settings বাদ পড়া; FileProvider scope ও URI grant; logs/lockscreen-এ sensitive information।
- Lifecycle: rapid start/stop, interrupted finalization, process death, disk full, invalid audio source, database insert failure—user-visible outcome।
- Compatibility: Android 7 minimum দাবি রাখলে API 24 থেকেও পরীক্ষা; বিশেষ গুরুত্ব API 31/33/34/35/36, Infinix/XOS, Samsung, Xiaomi ও Pixel; speaker/headset/Bluetooth এবং বাংলা/ইংরেজি। বর্তমানে কোনো device verified নয়।
- Release: assembleDebug, lint, relevant automated tests, minified release/AAB এবং physical-device smoke tests।

## এই সেশনে যাচাই

XML ফাইল well-formed কি না parse করে দেখা হয়েছে; cleanup scope ও ZIP integrity পরীক্ষা করা হয়েছে। Gradle wrapper চালানোর চেষ্টা distribution download পর্যায়ে Network is unreachable দিয়ে ব্যর্থ হয়েছে। তাই Kotlin compilation, lint, APK/AAB generation এবং runtime/device tests চালানো যায়নি। wrapper version download URL-টি valid কি না-ও এখানে নিশ্চিত করা হয়নি।

## সরকারি উৎস

[S1] Sharing audio input: https://developer.android.com/media/platform/sharing-audio-input
[S2] AudioSource reference: https://developer.android.com/reference/android/media/MediaRecorder.AudioSource
[S3] Playback capture limits: https://developer.android.com/media/platform/av-capture
[S4] Sensitive APIs / Accessibility policy: https://support.google.com/googleplay/android-developer/answer/16558241?hl=en
[S5] SMS / Call Log policy: https://support.google.com/googleplay/android-developer/answer/10208820?hl=en
[S6] Target API requirement: https://support.google.com/googleplay/android-developer/answer/11926878?hl=en
[S7] User Data: https://support.google.com/googleplay/android-developer/answer/10144311
[S8] Foreground service restrictions: https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start
[S9] Personal account testing: https://support.google.com/googleplay/android-developer/answer/14151465?hl=en

Sources checked ২ নভেম্বর ২০২৬; প্রকাশের সময় Play Console-এর কার্যকর requirements আবার মিলিয়ে নিতে হবে।
