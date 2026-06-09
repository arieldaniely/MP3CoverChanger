# 🎵 MP3 Cover Changer — אפליקציית אנדרואיד

אפליקציה לשינוי תמונות כיסוי (Album Art) של קבצי MP3 על אנדרואיד.

## פיצ'רים
- טעינת קבצי MP3 מהמכשיר
- הצגת תמונת הכיסוי הנוכחית
- שינוי תמונת כיסוי מהגלריה
- הסרת תמונת כיסוי
- תמיכה ב-ID3v2 tags
- דחיסת תמונות אוטומטית (מקס 500KB)

## דרישות מערכת
- Android Studio Hedgehog (2023.1.1) ומעלה
- Android SDK 26+
- Java 8+

## הוראות הפעלה

### 1. פתח ב-Android Studio
```
File → Open → בחר את תיקיית MP3CoverChanger
```

### 2. סנכרן Gradle
לחץ על "Sync Now" כאשר מופיעה ההודעה

### 3. הפעל על מכשיר
- חבר מכשיר אנדרואיד (עם USB Debugging מופעל)
- לחץ Run ▶

## מבנה הפרויקט
```
app/
├── src/main/
│   ├── java/com/mp3cover/
│   │   ├── MainActivity.kt      ← מסך ראשי
│   │   ├── Mp3FileItem.kt       ← קריאה/כתיבה של MP3
│   │   └── Mp3FileAdapter.kt    ← רשימת קבצים
│   ├── res/
│   │   ├── layout/
│   │   │   ├── activity_main.xml
│   │   │   └── item_mp3_file.xml
│   │   └── values/
│   └── AndroidManifest.xml
└── build.gradle
```

## ספריות בשימוש
- **mp3agic** — קריאה/כתיבה של ID3 tags
- **MediaMetadataRetriever** — קריאת מטא-דאטה
- **Material Components** — ממשק משתמש
