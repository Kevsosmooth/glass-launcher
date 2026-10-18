#!/usr/bin/env bash
# Device check for the Home button helper. Presses Home from Glass and from Android Settings and
# watches the app in front for up to WATCH_SECONDS. PASS when Glass comes to the front; reports how
# long that took and whether the stock home screen showed first (a flash), and saves a screenshot.
# Fire OS (Android 9) holds back app starts for about 5 s after Home, so expect ~5.5 s there. Needs the helper
# on (tools/home-helper.sh --apply). Keys sent: Home only, never Select.
#
# Usage: DEVICE=<ip:port> tools/test-home-helper.sh [-d <ip:port>] [out-dir]
#   WATCH_SECONDS (default 10) to wait for Glass per press, SETTLE (2) seconds after opening a screen.
set -euo pipefail
source "$(dirname "$0")/lib.sh"
gl_parse_device "$@"; set -- ${GL_ARGS[@]+"${GL_ARGS[@]}"}; gl_init

OUT=${1:-shots/home-helper-$(date +%Y-%m-%d)}
WATCH_MS=$(( ${WATCH_SECONDS:-10} * 1000 )); SETTLE=${SETTLE:-2}
STOCK="com.amazon.tv.launcher com.google.android.apps.tv.launcherx com.google.android.tvlauncher"
SERVICE="$GL_PKG/$GL_PKG.homebutton.HomeButtonService"

services=$(adb_ shell settings get secure enabled_accessibility_services | tr -d '\r')
tr ':' '\n' <<<"$services" | grep -qx -e "$SERVICE" -e "$GL_PKG/.homebutton.HomeButtonService" \
    || gl_die "the Home button helper is off on $DEVICE: run tools/home-helper.sh --apply first"
gl_ensure_idle
mkdir -p "$OUT"

PASSED=0; FAILED=0
# press_home <name>: Home, then watch what is in front.
press_home() {
    local name=$1 seen="" top="" flash=no start ms=0
    start=$(date +%s%N)
    key_home
    # Leave once Glass is back after something else showed, or on timeout.
    while ((ms < WATCH_MS)); do
        top=$(gl_top_package)
        [[ "$seen" == *" ${top:-?}" ]] || seen+=" ${top:-?}"
        in_list "$top" "$STOCK" && flash=yes
        ms=$(( ($(date +%s%N) - start) / 1000000 ))
        [[ "$top" == "$GL_PKG" && "$seen" != " $GL_PKG" ]] && break
    done
    screencap_to "$OUT/$name.png"
    if [[ "$top" == "$GL_PKG" ]]; then
        PASSED=$((PASSED + 1))
        echo "PASS $name: Glass in front after ${ms} ms; stock screen showed first: $flash; seen:$seen"
    else
        FAILED=$((FAILED + 1)); echo "FAIL $name: ${top:-unknown} in front after ${ms} ms; seen:$seen"
        adb_ shell dumpsys activity activities | tr -d '\r' | grep -m1 -E "ResumedActivity" | sed 's/^ */    /' || true
    fi
}

glass_home; sleep "$SETTLE"
press_home from-glass
adb_ shell am start -a android.settings.SETTINGS >/dev/null; sleep "$SETTLE"
press_home from-settings

echo "$PASSED passed, $FAILED failed; screenshots in $OUT"
exit $((FAILED > 0))
