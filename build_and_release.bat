@echo off
echo ====================================
echo MP3 Cover Changer - Build Script
echo ====================================

echo Cleaning old builds...
call gradlew clean

echo.
echo Building Release APK...
call gradlew assembleRelease

echo.
echo Build Complete!
echo Opening APK folder...
explorer "app\build\outputs\apk\release"

echo.
pause