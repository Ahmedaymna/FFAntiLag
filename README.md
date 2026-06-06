# GIX Tool 🔥

أداة تحسين الألعاب الاحترافية لـ Android — واجهة 3D/4D بـ OpenGL ES

---

## الميزات

| القسم | التفاصيل |
|-------|----------|
| 🎮 تحسين Free Fire | كشف مواصفات الجهاز تلقائيًا + إعدادات مقترحة |
| 🧹 تنظيف RAM | `ActivityManager.killBackgroundProcesses` + GC |
| ⚙️ تحسين CPU | ضبط أولويات الخيوط + تقليل ضغط GC |
| 💾 تنظيف التخزين | حذف الكاش والملفات المؤقتة |
| 📊 مؤشر أداء حيوي | FPS، RAM، CPU%، الحرارة في الوقت الفعلي |
| 🌐 إعلانات AdMob | Banner + Interstitial + Rewarded |
| 🌙 وضع مظلم | مدعوم افتراضيًا |
| 🌍 اللغة | عربي + إنجليزي |

---

## بناء المشروع محليًا

### المتطلبات
- JDK 17
- Android Studio Flamingo أو أحدث
- Android SDK (API 33)

### خطوات البناء
```bash
# استنساخ المشروع
git clone https://github.com/YOUR_USERNAME/GIXTool.git
cd GIXTool

# بناء debug APK
./gradlew assembleDebug

# بناء release APK
./gradlew assembleRelease

# الملف الناتج:
# app/build/outputs/apk/debug/app-debug.apk
# app/build/outputs/apk/release/app-release.apk
```

---

## GitHub Actions (البناء التلقائي)

### الإعداد
1. ارفع المشروع على GitHub
2. أضف الـ Secrets التالية في Settings → Secrets and variables → Actions:

| اسم السر | الوصف |
|----------|-------|
| `KEYSTORE_BASE64` | محتوى ملف keystore.jks مشفرًا بـ Base64 |
| `SIGNING_KEY_ALIAS` | اسم المفتاح (alias) |
| `SIGNING_KEY_PASSWORD` | كلمة مرور المفتاح |
| `SIGNING_STORE_PASSWORD` | كلمة مرور الـ keystore |

### توليد Keystore
```bash
keytool -genkey -v -keystore keystore.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias gixtool

# تشفير لـ GitHub Secrets
base64 keystore.jks
```

### كيف يعمل الـ Workflow
- **عند كل push** → يبني Debug + Release APK
- **يوقع** Release APK تلقائيًا
- **يرفع** APK كـ Artifact
- **ينشئ Release** تلقائيًا على GitHub

---

## بنية المشروع

```
GIXTool/
├── .github/workflows/build.yml     ← GitHub Actions
├── app/
│   ├── build.gradle                ← إعدادات Gradle
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/gixtool/app/
│       │   ├── MainActivity.kt     ← النشاط الرئيسي
│       │   ├── ads/AdManager.kt    ← إدارة AdMob
│       │   ├── renderer/GLRenderer.kt  ← 3D/4D OpenGL ES
│       │   ├── ui/
│       │   │   ├── main/HomeFragment.kt      ← الرئيسية (3D)
│       │   │   ├── optimizer/OptimizerFragment.kt  ← التحسين
│       │   │   └── cleaner/CleanerFragment.kt     ← التنظيف
│       │   ├── viewmodel/
│       │   │   ├── OptimizerViewModel.kt
│       │   │   └── CleanerViewModel.kt
│       │   └── util/
│       │       ├── DeviceUtils.kt       ← قراءة مواصفات الجهاز
│       │       └── PerformanceMonitor.kt ← مؤشر الأداء
│       └── res/
│           ├── layout/     ← تصميمات الشاشات
│           ├── values/     ← الألوان والنصوص والسمات
│           ├── drawable/   ← الأيقونات 3D SVG
│           ├── navigation/ ← خريطة التنقل
│           ├── anim/       ← التحريكات
│           └── menu/       ← قائمة التنقل السفلية
├── build.gradle
├── settings.gradle
├── gradlew
└── gradlew.bat
```

---

## معرّفات AdMob

| النوع | المعرّف |
|-------|--------|
| Banner | `ca-app-pub-6718038985057828/5364644957` |
| Interstitial | `ca-app-pub-6718038985057828/4460277306` |
| Rewarded | `ca-app-pub-6718038985057828/7252441697` |

> ⚠️ استبدل `ca-app-pub-6718038985057828~YOUR_APP_ID` في `AndroidManifest.xml` بـ App ID الفعلي من AdMob Console.

---

## الصلاحيات المطلوبة

```xml
INTERNET                    ← للإعلانات
KILL_BACKGROUND_PROCESSES   ← تنظيف RAM
CLEAR_APP_CACHE             ← تنظيف الكاش
MANAGE_EXTERNAL_STORAGE     ← تنظيف التخزين
KILL_BACKGROUND_PROCESSES   ← إيقاف التطبيقات الخلفية
```

---

## الحد الأدنى من المتطلبات
- **minSdk**: 21 (Android 5.0 Lollipop)
- **targetSdk**: 33 (Android 13)
- **Java**: 17
- **Gradle**: 8.0
- **OpenGL ES**: 3.0
