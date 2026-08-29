#!/bin/bash
# سكريبت بناء وتطهير محلي — لا يحتوي على أي توكين
# استخدمه أنت على جهازك بعد تغيير التوكن
set -e

echo "=== Building APK ==="
./gradlew :app:assembleRelease

echo "=== Copying APK to workspace ==="
cp app/build/outputs/apk/release/app-release.apk ~/gas_app_release.apk

echo "=== Cleaning build artifacts ==="
find . -type d -name "build" -exec rm -rf {} + 2>/dev/null || true
find . -type d -name ".gradle" -exec rm -rf {} + 2>/dev/null || true

echo "=== Done ==="
echo "APK: ~/gas_app_release.apk"
echo "Cleaned local build dirs. Source remains."
