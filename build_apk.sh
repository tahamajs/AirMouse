#!/bin/bash
# Build Air Mouse APK without Android Studio
export ANDROID_HOME=~/android-sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools
cd android
chmod +x gradlew
./gradlew clean
./gradlew assembleDebug
echo "APK created at android/app/build/outputs/apk/debug/app-debug.apk"