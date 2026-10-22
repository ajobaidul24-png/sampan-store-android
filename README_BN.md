# সাম্পান স্টোর Android অ্যাপ — Auto Backup & Restore যুক্ত প্রজেক্ট

এই Android Studio project-এ WebView-এর জন্য native backup সুবিধা যোগ করা হয়েছে। **এটি এখনো APK নয়** এবং আপনার সম্পূর্ণ HTML অ্যাপটি এখানে স্বয়ংক্রিয়ভাবে যুক্ত হয়নি।

## ১) আপনার HTML ফাইল যোগ করুন

1. `app/src/main/assets/` ফোল্ডারে আপনার সম্পূর্ণ HTML অ্যাপটি `index.html` নামে রাখুন।
2. `docs/backup-integration.js` ফাইলের সম্পূর্ণ কোড কপি করে আপনার `index.html`-এর শেষের `</body>` ট্যাগের ঠিক আগে একটি `<script> ... </script>` ট্যাগের মধ্যে বসান। উদাহরণস্বরূপ, ফাইলটির কোড সরাসরি HTML-এ যোগ করতে হবে; আলাদা `.js` ফাইলের পথ ব্যবহার করতে চাইলে সেটি `assets` ফোল্ডারে রেখে `<script src="backup-integration.js"></script>` লিখতে পারেন।
3. `docs/backup-buttons.html`-এর দুটি বাটন আপনার অ্যাপের সেটিংস বা রিপোর্ট অংশে বসান। চাইলে আপনার ডিজাইনের সঙ্গে মিলিয়ে বাটনের CSS পরিবর্তন করুন।

## ২) ব্যাকআপ কীভাবে কাজ করবে

- **Auto backup:** অ্যাপের `localStorage`-এ পরিবর্তন হলে সামান্য বিরতির পর ব্যাকআপ অ্যাপের নিজস্ব private storage-এ লেখা হবে।
- **Auto recovery:** অ্যাপের `localStorage` খালি থাকলে private storage-এর সর্বশেষ ব্যাকআপ ফিরিয়ে আনার চেষ্টা করবে।
- **Export:** “ব্যাকআপ ডাউনলোড করুন” চাপলে Android ফাইল সেভ করার উইন্ডো খুলবে। ফাইলটি অন্য নিরাপদ জায়গায় কপি করে রাখুন।
- **Restore:** “ব্যাকআপ রিস্টোর করুন” চাপলে JSON ব্যাকআপ ফাইল বেছে নিতে পারবেন। রিস্টোরে বর্তমান `localStorage`-এর ডেটা প্রতিস্থাপিত হবে, তাই আগে বর্তমান হিসাবেরও ব্যাকআপ নিন।

## ৩) APK তৈরি

1. ZIP Extract করুন এবং Android Studio-তে `SampanStoreAndroid` ফোল্ডারটি Open করুন।
2. Gradle Sync শেষ হলে **Build > Build Bundle(s) / APK(s) > Build APK(s)** চাপুন।
3. APK সাধারণত `app/build/outputs/apk/debug/app-debug.apk`-এ থাকবে।

## জরুরি সীমাবদ্ধতা

- এই ZIP একটি সোর্স প্রজেক্ট; **এটি তৈরি APK নয়**।
- অটো ব্যাকআপ একই অ্যাপের private storage-এ থাকে। অ্যাপ uninstall বা app data clear করলে এই private backup-ও মুছে যেতে পারে। তাই নিয়মিত JSON ফাইল Export করে অন্য জায়গায় রাখুন।
- প্রথমবার চালুর আগে HTML-এ integration script ও বাটন যোগ করা জরুরি।
- রিস্টোর করার পর অ্যাপের মোট/দৈনিক হিসাব যাচাই করুন। গুরুত্বপূর্ণ হিসাবের একমাত্র কপি হিসেবে এই ব্যাকআপের ওপর নির্ভর করবেন না।
- এই Android project-টি Android Studio-তে build ও বাস্তব ডিভাইসে পরীক্ষা করা প্রয়োজন।
