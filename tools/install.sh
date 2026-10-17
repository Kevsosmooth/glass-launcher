#!/usr/bin/env bash
# Install an APK from out/ on the TV (adb install -r) and print the installed version.
# Usage: DEVICE=<ip:port> tools/install.sh [-d <ip:port>] [debug|release] [path/to/app.apk]
# Does not launch anything or press any key.
set -euo pipefail
source "$(dirname "$0")/lib.sh"
ROOT=$(cd "$(dirname "$0")/.." && pwd)
gl_parse_device "$@"; set -- ${GL_ARGS[@]+"${GL_ARGS[@]}"}; gl_init
VARIANT=${1:-debug}
case "$VARIANT" in debug|release) ;; *) gl_die "usage: install.sh [-d <ip:port>] [debug|release] [apk]" ;; esac
APK=${2:-${OUT_DIR:-$ROOT/out}/app-$VARIANT.apk}
[[ -f "$APK" ]] || gl_die "no APK at $APK; run tools/build.sh $VARIANT first"

adb_ install -r "$APK"
adb_ shell dumpsys package "$GL_PKG" | tr -d '\r' | grep -m2 -E "versionName|versionCode" | sed 's/^ *//' || true
