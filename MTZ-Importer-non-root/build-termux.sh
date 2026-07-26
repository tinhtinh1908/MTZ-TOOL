#!/data/data/com.termux/files/usr/bin/bash
set -e

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$PROJECT_DIR"

if [ -z "${ANDROID_HOME:-}" ]; then
  export ANDROID_HOME="$HOME/android-sdk"
fi
export ANDROID_SDK_ROOT="$ANDROID_HOME"

if [ ! -d "$ANDROID_HOME/platforms/android-35" ]; then
  echo "Thiếu Android SDK Platform 35 tại: $ANDROID_HOME"
  exit 1
fi

AAPT2_BIN="$(command -v aapt2 || true)"
if [ -z "$AAPT2_BIN" ]; then
  echo "Không tìm thấy aapt2 của Termux."
  exit 1
fi

printf 'sdk.dir=%s\n' "$ANDROID_HOME" > local.properties
sed -i '/android.aapt2FromMavenOverride/d' gradle.properties
printf '\nandroid.aapt2FromMavenOverride=%s\n' "$AAPT2_BIN" >> gradle.properties

if [ ! -f gradlew ]; then
  gradle wrapper --gradle-version 8.9
fi

bash gradlew clean assembleRelease --no-daemon --console=plain

APK="$PROJECT_DIR/app/build/outputs/apk/release/app-release-unsigned.apk"
OUT="/sdcard/Download/MTZ-Tool-NonRoot-1.0.0-unsigned.apk"
cp "$APK" "$OUT"

echo
echo "BUILD THÀNH CÔNG"
echo "APK chưa ký: $OUT"
