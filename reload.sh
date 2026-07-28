#!/usr/bin/env bash
# Rebuild, reinstall and relaunch the app on the running emulator.
# Usage: ./reload.sh [activity]     e.g. ./reload.sh .MyProjectsActivity
set -euo pipefail

PKG=com.example.madproject
ADB="$(grep -m1 '^sdk.dir=' local.properties | cut -d= -f2)/platform-tools/adb"

./gradlew installDebug -q

if [ $# -gt 0 ]; then
    "$ADB" shell am start -n "$PKG/$1" >/dev/null
else
    "$ADB" shell monkey -p "$PKG" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1
fi

echo "launched: $("$ADB" shell dumpsys activity activities | grep -m1 'ResumedActivity:' | sed 's/.*madproject//;s/ .*//')"
