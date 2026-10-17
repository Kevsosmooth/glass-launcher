#!/usr/bin/env bash
# Shared helpers for the Glass Launcher device scripts. Source this file; do not run it.
#
# Device:  DEVICE=<ip:port> in the environment, or -d <ip:port> on any script (see gl_parse_device).
#          Example: DEVICE=192.168.1.50:5555 tools/screenshots.sh
# adb:     ADB=/path/to/adb in the environment; otherwise `adb` from PATH.
# Safety:  gl_ensure_idle refuses to act while a video app is in front; keys never include Select
#          unless a script asks for it explicitly.

# Optional per-machine settings (DEVICE, ADB, GL_BUSY_PACKAGES...), kept out of git. Read first so
# the defaults below only fill in what it leaves unset.
GL_LOCAL_CONF=${GL_LOCAL_CONF:-"$(dirname "${BASH_SOURCE[0]}")/local.conf"}
# shellcheck disable=SC1090
[[ -f "$GL_LOCAL_CONF" ]] && source "$GL_LOCAL_CONF"

GL_PKG=${GL_PKG:-com.kevin.glasslauncher}
GL_ACTIVITY=${GL_ACTIVITY:-$GL_PKG/.MainActivity}
ADB=${ADB:-adb}
DEVICE=${DEVICE:-}

# Apps whose presence in front means someone is watching: never send keys over them. Add your
# live-TV and player apps (space-separated package names), e.g. in tools/local.conf.
GL_BUSY_PACKAGES=${GL_BUSY_PACKAGES:-"com.google.android.youtube.tv com.netflix.ninja org.videolan.vlc com.plexapp.android"}
# Launchers (and Glass itself) that are safe to drive. Anything else in front also stops the script
# unless GL_ALLOW_ANY_TOP=1.
GL_HOME_PACKAGES=${GL_HOME_PACKAGES:-"com.google.android.tvlauncher com.google.android.apps.tv.launcherx com.spocky.projengmenu com.amazon.tv.launcher $GL_PKG"}

gl_die() { echo "error: $*" >&2; exit 1; }

# Consumes "-d <device>" from the arguments; the rest is left in GL_ARGS (array).
gl_parse_device() {
    GL_ARGS=()
    while (($#)); do
        case "$1" in
            -d) [[ $# -ge 2 ]] || gl_die "-d needs a device (ip:port)"; DEVICE=$2; shift 2 ;;
            *) GL_ARGS+=("$1"); shift ;;
        esac
    done
}

# Call once after gl_parse_device: checks the device is set and connects network devices.
gl_init() {
    [[ -n "$DEVICE" ]] || gl_die "no device: set DEVICE=<ip:port> or pass -d <ip:port>"
    command -v "$ADB" >/dev/null 2>&1 || [[ -x "$ADB" ]] || gl_die "adb not found: set ADB=/path/to/adb or put it on PATH"
    if [[ "$DEVICE" == *:* ]]; then
        "$ADB" connect "$DEVICE" >/dev/null || gl_die "cannot connect to $DEVICE"
    fi
}

# Network devices drop to "offline" when another machine's adb server also connects; reconnect
# and retry the command once.
adb_() {
    "$ADB" -s "$DEVICE" "$@" && return 0
    local rc=$?
    [[ "$DEVICE" == *:* ]] || return $rc
    "$ADB" -s "$DEVICE" get-state >/dev/null 2>&1 && return $rc
    "$ADB" disconnect "$DEVICE" >/dev/null 2>&1 || true
    "$ADB" connect "$DEVICE" >/dev/null 2>&1 || return $rc
    "$ADB" -s "$DEVICE" wait-for-device >/dev/null 2>&1 || return $rc
    "$ADB" -s "$DEVICE" "$@"
}

in_list() { local needle=$1; shift; local x; for x in $*; do [[ "$x" == "$needle" ]] && return 0; done; return 1; }

# Package of the activity currently in front (empty when unknown).
gl_top_package() {
    adb_ shell dumpsys activity activities | tr -d '\r' \
        | grep -m1 -E "mResumedActivity|topResumedActivity|ResumedActivity" \
        | grep -oE "[a-zA-Z0-9_.]+/" | head -1 | tr -d '/' || true
}

wake() { adb_ shell input keyevent KEYCODE_WAKEUP; }

# Output is captured before grepping: `cmd | grep -q` under pipefail fails when grep exits early
# and the writer gets SIGPIPE.
gl_is_debug_build() {
    local info
    info=$(adb_ shell dumpsys package "$GL_PKG" | tr -d '\r')
    grep -qE "flags=\[.*DEBUGGABLE" <<<"$info"
}

gl_has_leanback() {
    local features
    features=$(adb_ shell pm list features | tr -d '\r')
    grep -qx "feature:android.software.leanback" <<<"$features"
}

gl_is_awake() { [[ "$(adb_ shell dumpsys power | tr -d '\r' | grep -c "mWakefulness=Awake" || true)" -ge 1 ]]; }

# Stop (exit 1) unless the TV is awake on a launcher or Glass. Wakes a sleeping screen once.
gl_ensure_idle() {
    local top
    top=$(gl_top_package)
    if in_list "$top" "$GL_BUSY_PACKAGES"; then
        echo "TV is in use ($top is in front). Stopping." >&2; exit 1
    fi
    if ! gl_is_awake; then
        wake; sleep 2
        gl_is_awake || { echo "TV screen is off and did not wake. Stopping." >&2; exit 1; }
        top=$(gl_top_package)
        if in_list "$top" "$GL_BUSY_PACKAGES"; then
            echo "TV is in use ($top is in front). Stopping." >&2; exit 1
        fi
    fi
    if [[ "${GL_ALLOW_ANY_TOP:-0}" != 1 ]] && ! in_list "$top" "$GL_HOME_PACKAGES"; then
        echo "TV is not on Home (${top:-unknown} is in front). Stopping; set GL_ALLOW_ANY_TOP=1 to override." >&2; exit 1
    fi
}

# --- input ---------------------------------------------------------------------------------------

key() { adb_ shell input keyevent "$@"; }
key_up() { key KEYCODE_DPAD_UP; }
key_down() { key KEYCODE_DPAD_DOWN; }
key_left() { key KEYCODE_DPAD_LEFT; }
key_right() { key KEYCODE_DPAD_RIGHT; }
key_select() { key KEYCODE_DPAD_CENTER; }
key_back() { key KEYCODE_BACK; }
key_menu() { key KEYCODE_MENU; }
# The Home key goes to whichever app owns Home (Projectivy, the stock launcher): use glass_home
# to return to Glass.
key_home() { key KEYCODE_HOME; }
# Long press on Select (23) opens the edit menu.
key_select_long() { adb_ shell input keyevent --longpress KEYCODE_DPAD_CENTER; }

# repeat <n> <function> : e.g. repeat 3 key_right
repeat() { local n=$1 i; shift; for ((i = 0; i < n; i++)); do "$@"; sleep 0.3; done; }

# D-pad only (19 up, 20 down, 21 left, 22 right). Never Select, so nothing gets launched.
NAV_PATTERN="22 22 22 22 22 21 21 21 21 21"

# ~4 presses/s: one `input` call per key.
nav_slow() {
    adb_ shell "for r in 1 2 3; do for k in $NAV_PATTERN; do input keyevent \$k; sleep 0.25; done; done"
}

# Back-to-back presses in one `input` call: closest adb gets to a held D-pad.
nav_burst() {
    adb_ shell "for r in 1 2 3; do input keyevent $NAV_PATTERN; done"
}

# Down then up through the grid.
nav_grid() {
    adb_ shell "for r in 1 2 3; do for k in 20 20 20 20 19 19 19 19; do input keyevent \$k; sleep 0.25; done; done"
}

# Start Glass with optional extras, e.g. `start_glass --es open settings`.
start_glass() { adb_ shell am start -W -n "$GL_ACTIVITY" "$@" >/dev/null; }

# --- capture -------------------------------------------------------------------------------------

# Via a file on the device: `exec-out screencap` truncates 4K captures over network adb.
screencap_to() {
    adb_ shell screencap -p /sdcard/gl-screen.png
    adb_ pull -q /sdcard/gl-screen.png "$1" >/dev/null
    adb_ shell rm -f /sdcard/gl-screen.png
}

# uiautomator dump of the current screen into <file>. A screen that never goes idle (edit-mode
# wiggle) makes uiautomator give up: retry once, then warn and skip that dump.
uidump_to() {
    local out
    out=$(adb_ shell uiautomator dump /sdcard/gl-window.xml 2>&1)
    if [[ "$out" != *"dumped to"* ]]; then
        sleep 1
        out=$(adb_ shell uiautomator dump /sdcard/gl-window.xml 2>&1)
    fi
    if [[ "$out" != *"dumped to"* ]]; then
        echo "warning: no layout dump for $(basename "$1") (${out//$'\r'/})" >&2
        return 0
    fi
    adb_ pull -q /sdcard/gl-window.xml "$1" >/dev/null
    adb_ shell rm -f /sdcard/gl-window.xml
}

# Home intent aimed at Glass itself: works even while another launcher (or an accessibility
# service such as Projectivy's) owns the Home key. On Glass it scrolls to the top and focuses the dock.
glass_home() { start_glass -a android.intent.action.MAIN -c android.intent.category.HOME; }

# Stop unless Glass is the app in front, so keys never drive another launcher.
gl_require_glass_front() {
    local top
    top=$(gl_top_package)
    [[ "$top" == "$GL_PKG" ]] || gl_die "Glass is not in front (${top:-unknown} is). Stopping."
}

# gfxinfo summary + per-frame stats for <pkg> into <out>/<name>-gfxinfo.txt / -framestats.txt
collect_frames() {
    local pkg=$1 out=$2 name=$3
    adb_ shell dumpsys gfxinfo "$pkg" > "$out/$name-gfxinfo.txt"
    adb_ shell dumpsys gfxinfo "$pkg" framestats > "$out/$name-framestats.txt"
}

reset_frames() { adb_ shell dumpsys gfxinfo "$1" reset >/dev/null; }

collect_mem() { adb_ shell dumpsys meminfo "$1" > "$2/$3-meminfo.txt"; }

# CPU of <pkg> sampled over $IDLE_SECONDS (default 10) while nothing moves. Falls back to dumpsys cpuinfo where toybox
# `top` lacks -p/-o (older Fire OS).
collect_idle_cpu() {
    local pkg=$1 out=$2 name=$3 pid secs=${IDLE_SECONDS:-10}
    pid=$(adb_ shell pidof "$pkg" | tr -d '\r')
    if [[ -n "$pid" ]] && adb_ shell top -b -n 2 -d $((secs / 2)) -p "$pid" -o PID,%CPU,RES,NAME > "$out/$name-idle-cpu.txt" 2>/dev/null; then
        return
    fi
    sleep "$secs"
    adb_ shell dumpsys cpuinfo | grep -E "$pkg|TOTAL" > "$out/$name-idle-cpu.txt" || true
}

# One-line summary from a gfxinfo dump.
summarize_frames() {
    grep -E "Total frames rendered|Janky frames|50th percentile|90th percentile|99th percentile|High input latency" "$1" | sed 's/^ *//' | head -6
}
