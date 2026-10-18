#!/usr/bin/env bash
# The whole Fire TV check in one run: connect (waits for "Allow USB debugging"), device facts,
# install, Glass home screenshot + layout check, the Home button helper page off/on, the helper's
# Home-key test, the full screenshot set, then put the TV back. Writes <out>/summary.md.
# Sends keys and opens apps on the TV: run only after the owner says nobody is watching.
#
# Usage: DEVICE=<ip:port> tools/test-firetv.sh [-d <ip:port>] [options] [out-dir]
#   --apk <file>     APK to install (default out/app-debug.apk; screens need a debug build)
#   --no-install     test what is already installed
#   --no-screens     skip the full screenshot set (tools/screenshots.sh)
#   --keep-helper    leave the Home button helper on afterwards (default: turn it off again)
# Environment: AUTH_WAIT seconds to wait for Allow (default 90), SETTLE (default 2).
set -euo pipefail
TOOLS=$(cd "$(dirname "$0")" && pwd)
source "$TOOLS/lib.sh"
gl_parse_device "$@"; set -- ${GL_ARGS[@]+"${GL_ARGS[@]}"}
[[ -n "$DEVICE" ]] || gl_die "no device: set DEVICE=<ip:port> or pass -d <ip:port>"

APK=${OUT_DIR:-$TOOLS/../out}/app-debug.apk
INSTALL=1; SCREENS=1; KEEP_HELPER=0; OUT=""
while (($#)); do
    case "$1" in
        --apk) [[ $# -ge 2 ]] || gl_die "--apk needs a file"; APK=$2; shift ;;
        --no-install) INSTALL=0 ;;
        --no-screens) SCREENS=0 ;;
        --keep-helper) KEEP_HELPER=1 ;;
        -*) gl_die "unknown option $1 (see the header of $0)" ;;
        *) OUT=$1 ;;
    esac
    shift
done
OUT=${OUT:-shots/firetv-$(date +%Y%m%d-%H%M%S)}
SETTLE=${SETTLE:-2}
# Fire TV players on top of lib.sh's list: never send keys over them.
GL_BUSY_PACKAGES="$GL_BUSY_PACKAGES com.amazon.avod com.amazon.firebat com.amazon.firetv.youtube
    com.disney.disneyplus com.hulu.livingroomplus com.wbd.stream org.xbmc.kodi com.amazon.cloud9"
export GL_BUSY_PACKAGES   # the stage scripts check the same list
mkdir -p "$OUT"
SUMMARY="$OUT/summary.md"; LOG="$OUT/run.log"
echo "# Fire TV test, $(date '+%Y-%m-%d %I:%M %p')" > "$SUMMARY"
FAILED=0
result() { echo "- $1: $2" >> "$SUMMARY"; echo "$1: $2"; [[ $2 == FAIL* ]] && FAILED=$((FAILED + 1)); return 0; }
finish() { echo; echo "$FAILED failed; details in $SUMMARY"; exit $((FAILED > 0)); }

# 1. Connect. A first connection shows "Allow USB debugging?" on the TV; wait for it.
command -v "$ADB" >/dev/null 2>&1 || [[ -x "$ADB" ]] || gl_die "adb not found: set ADB=/path/to/adb"
"$ADB" connect "$DEVICE" >/dev/null 2>&1 || true
waited=0; state=""
while :; do
    state=$("$ADB" -s "$DEVICE" get-state 2>&1 | tr -d '\r' | head -1 || true)
    [[ "$state" == device ]] && break
    if ((waited == 0)); then
        echo "Waiting for the TV ($state). If it asks \"Allow USB debugging?\", tick Always allow and press OK."
    fi
    ((waited >= ${AUTH_WAIT:-90})) && { result connect "FAIL ($state after ${waited}s)"; finish; }
    sleep 3; waited=$((waited + 3))
    "$ADB" connect "$DEVICE" >/dev/null 2>&1 || true
done
result connect PASS

# 2. Device facts (read-only).
prop() { adb_ shell getprop "$1" | tr -d '\r'; }
{
    echo "model: $(prop ro.product.manufacturer) $(prop ro.product.model)"
    echo "fire os: $(prop ro.build.mktg.fireos) (Android $(prop ro.build.version.release), API $(prop ro.build.version.sdk))"
    echo "screen: $(adb_ shell wm size | tr -d '\r' | tail -1), $(adb_ shell wm density | tr -d '\r' | tail -1)"
    echo "ram: $(adb_ shell cat /proc/meminfo | tr -d '\r' | grep -m1 MemTotal || true)"
    echo "home app: $(adb_ shell cmd package resolve-activity --brief -a android.intent.action.MAIN \
        -c android.intent.category.HOME | tr -d '\r' | tail -1)"
} > "$OUT/device.txt"
sed 's/^/  /' "$OUT/device.txt" | tee -a "$SUMMARY"
[[ "$(prop ro.product.manufacturer)" == Amazon ]] || echo "  note: not an Amazon device" | tee -a "$SUMMARY"

# 3. Nobody watching? Re-checked before every stage that sends keys.
guard() { ( gl_ensure_idle ) || { result "$1" "FAIL (TV in use or not on Home; stopped)"; finish; }; }
guard idle

# 4. Install.
if ((INSTALL)); then
    [[ -f "$APK" ]] || { result install "FAIL (no APK at $APK; tools/build.sh debug)"; finish; }
    if "$TOOLS/install.sh" -d "$DEVICE" debug "$APK" >> "$LOG" 2>&1; then
        version=$(grep -m1 versionName "$LOG" | tr -d ' ' || true)
        result install "PASS${version:+ ($version)}"
    else
        result install "FAIL (see run.log)"; finish
    fi
fi

# 5. Glass opens; home screenshot + layout check; how long the cold start took.
guard launch
adb_ shell am force-stop "$GL_PKG"
total=$(adb_ shell am start -W -n "$GL_ACTIVITY" -a android.intent.action.MAIN -c android.intent.category.HOME \
    | tr -d '\r' | sed -n 's/^TotalTime: //p')
sleep "$SETTLE"
if [[ "$(gl_top_package)" == "$GL_PKG" ]]; then
    screencap_to "$OUT/glass-home.png"; uidump_to "$OUT/glass-home.xml"
    result launch "PASS (cold start ${total:-?} ms)"
    if [[ -f "$OUT/glass-home.xml" ]]; then
        if python3 "$TOOLS/check_layout.py" "$OUT/glass-home.xml" > "$OUT/glass-home-layout.txt" 2>&1; then
            result layout PASS
        else
            result layout "FAIL ($(grep -c FAIL "$OUT/glass-home-layout.txt") checks; glass-home-layout.txt)"
        fi
    else
        result layout "SKIP (no layout dump)"
    fi
else
    result launch "FAIL ($(gl_top_package) in front)"; finish
fi

# 6. Helper page while off, helper on, page again, then the Home-key test.
helper_page() { start_glass --es open homehelper; sleep "$SETTLE"; screencap_to "$OUT/$1.png"; }
"$TOOLS/home-helper.sh" -d "$DEVICE" --undo >> "$LOG" 2>&1 || true
helper_page helper-page-off
adb_ shell logcat -c || true
if "$TOOLS/home-helper.sh" -d "$DEVICE" --apply >> "$LOG" 2>&1; then
    result "helper on" PASS
else
    result "helper on" "FAIL (see run.log)"; finish
fi
sleep "$SETTLE"
# Fire OS shortens labels here ("Glass Launcher Home b..."), so match the start only.
bound=$(adb_ shell dumpsys accessibility | tr -d '\r' | grep -c "label=Glass Launcher Home" || true)
result "helper bound by the system" "$( ((bound > 0)) && echo PASS || echo "FAIL (not in dumpsys accessibility)")"
helper_page helper-page-on
guard "home key"
if "$TOOLS/test-home-helper.sh" -d "$DEVICE" "$OUT/home-key" > "$OUT/home-key.txt" 2>&1; then
    flashes=$(grep -c "showed first: yes" "$OUT/home-key.txt" || true)
    result "home key" "PASS ($flashes of 2 presses showed Fire TV's screen first)"
else
    why=$(grep -m1 FAIL "$OUT/home-key.txt" || tail -1 "$OUT/home-key.txt")
    result "home key" "FAIL ($why)"
fi
adb_ shell logcat -d -v time | tr -d '\r' \
    | grep -E "glasslauncher|HomeButton|AccessibilityManager|ActivityTaskManager: START" > "$OUT/logcat.txt" || true

# 7. The full screenshot set (debug builds).
if ((SCREENS)); then
    guard screens
    if "$TOOLS/screenshots.sh" -d "$DEVICE" "$OUT/screens" > "$OUT/screens.txt" 2>&1; then
        result screens "PASS ($(ls "$OUT"/screens/*.png 2>/dev/null | wc -l) screenshots)"
    else
        result screens "FAIL ($(tail -1 "$OUT/screens.txt"))"
    fi
fi

# 8. Put the TV back: helper off (unless --keep-helper), Home to Fire TV's own screen.
if ((KEEP_HELPER)); then
    result restore "helper left on (--keep-helper)"
else
    "$TOOLS/home-helper.sh" -d "$DEVICE" --undo >> "$LOG" 2>&1 && result restore "PASS (helper off)" \
        || result restore "FAIL (helper still on: tools/home-helper.sh --undo)"
    in_list "$(gl_top_package)" "$GL_BUSY_PACKAGES" || key_home
fi
finish
