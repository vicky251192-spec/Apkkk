# SchoolSphere Android app

Native Android application scaffold built with Kotlin and Jetpack Compose.

## Build
Open this repository in Android Studio (Koala or newer), allow Gradle sync, then select **Build > Build Bundle(s) / APK(s) > Build APK(s)**.

Or run:
```bash
gradle assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`.

A GitHub Actions workflow builds the debug APK on pushes to main/master and uploads it as an Actions artifact.

## Current scope
- Native Android launcher and dashboard
- Student directory and add-student form
- Attendance, academic records and activity portfolio module entry screens

## Important
This is an early UI/build scaffold. Student entries are currently in-memory demo data and reset when the app closes. Do not enter real student data. Secure sign-in, role permissions, persistent database/cloud sync, attendance transactions, marks, file/photo uploads, backups and privacy controls are not implemented yet.
