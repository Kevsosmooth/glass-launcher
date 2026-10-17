#!/usr/bin/env bash
# Drive a fixed sequence of launcher states on a DEBUG build and save a screenshot (.png) and a
# uiautomator dump (.xml) for each, then run tools/check_layout.py on the plain home states.
# Sends D-pad keys and opens Glass on the TV: run only while nobody is watching.
#
# Usage: DEVICE=<ip:port> tools/screenshots.sh [-d <ip:port>] [outdir]     (default shots/<timestamp>)
# Environment:
#   WALLPAPERS             wallpaper ids to cycle (default: the built-in set)
#   EDIT_MOVE_KEYS         keys that enter move mode from the edit menu and move the tile (default: Select, Right)
#   EDIT_UNDO_KEYS         keys that move it back before Back saves the order (default: Left)
#   WALLPAPER_PICKER_KEYS  keys from Settings to the wallpaper picker (default: Down, Select)
#   SETTLE                 seconds to wait for animations before each capture (default 1.5)
# Resets look, wallpaper, fake apps and HUD on exit.
set -euo pipefail
TOOLS=$(cd "$(dirname "$0")" && pwd)
source "$TOOLS/lib.sh"
gl_parse_device "$@"; set -- ${GL_ARGS[@]+"${GL_ARGS[@]}"}; gl_init

OUT=${1:-shots/$(date +%Y%m%d-%H%M%S)}
WALLPAPERS=${WALLPAPERS:-"aurora dusk ocean forest ember graphite"}
EDIT_MOVE_KEYS=${EDIT_MOVE_KEYS:-"KEYCODE_DPAD_CENTER KEYCODE_DPAD_RIGHT"}
EDIT_UNDO_KEYS=${EDIT_UNDO_KEYS:-"KEYCODE_DPAD_LEFT"}
WALLPAPER_PICKER_KEYS=${WALLPAPER_PICKER_KEYS:-"KEYCODE_DPAD_DOWN KEYCODE_DPAD_CENTER"}
SETTLE=${SETTLE:-1.5}
KEY_DELAY=${KEY_DELAY:-0.4}
CHECKED=()   # dumps of plain home states, handed to check_layout.py at the end
mkdir -p "$OUT"

settle() { sleep "$SETTLE"; }
press() { local k; for k in "$@"; do key "$k"; sleep "$KEY_DELAY"; done; }

# Overlays close with Back (a launcher never exits on Back); Home then returns to the first dock tile.
back_to_home() { press KEYCODE_BACK KEYCODE_BACK; glass_home; settle; }

# capture <name> [check] : screenshot + dump; "check" queues the dump for the layout checker.
capture() {
    settle
    screencap_to "$OUT/$1.png"
    uidump_to "$OUT/$1.xml"
    [[ "${2:-}" == check ]] && CHECKED+=("$OUT/$1.xml")
    echo "captured $1"
}

reset_prefs() {
    start_glass --es look Glass --es wallpaper aurora --ei fake_apps 0 --ez hud false
    settle
}

# Look, HUD and fake-app extras exist in debug builds only.
require_debug_build() {
    gl_is_debug_build \
        || gl_die "$GL_PKG on $DEVICE is not a debug build (or not installed): tools/build.sh debug && tools/install.sh debug"
}

gl_ensure_idle
require_debug_build
# Put the launcher back on exit, unless someone started watching TV meanwhile: then send nothing.
cleanup() {
    in_list "$(gl_top_package)" "$GL_BUSY_PACKAGES" && { echo "TV in use; skipping cleanup" >&2; return; }
    reset_prefs
    back_to_home
}
trap cleanup EXIT

reset_prefs
back_to_home
capture 01-home check

press KEYCODE_DPAD_RIGHT KEYCODE_DPAD_RIGHT
capture 02-dock-tile-3 check

back_to_home
press KEYCODE_DPAD_DOWN KEYCODE_DPAD_DOWN
capture 03-grid-row-2

back_to_home
key_select_long
capture 04-edit-menu

# shellcheck disable=SC2086  # key list is intentionally word-split
press $EDIT_MOVE_KEYS
capture 05-move-mode
# shellcheck disable=SC2086
press $EDIT_UNDO_KEYS
back_to_home

start_glass --es open settings
capture 06-settings
# shellcheck disable=SC2086
press $WALLPAPER_PICKER_KEYS
capture 07-wallpaper-picker
press KEYCODE_BACK KEYCODE_BACK KEYCODE_BACK

start_glass --es open controls
capture 08-quick-controls
back_to_home

start_glass --es look Solid
back_to_home
capture 09-solid-look check
start_glass --es look Glass

for id in $WALLPAPERS; do
    start_glass --es wallpaper "$id"
    sleep "$SETTLE"
    capture "10-wallpaper-$id" check
done
start_glass --es wallpaper aurora

start_glass --ei fake_apps 200
back_to_home
capture 11-fake-apps-200 check
press KEYCODE_DPAD_DOWN KEYCODE_DPAD_DOWN KEYCODE_DPAD_DOWN KEYCODE_DPAD_DOWN KEYCODE_DPAD_DOWN
capture 12-fake-apps-scrolled
start_glass --ei fake_apps 0

start_glass --ez hud true
back_to_home
capture 13-hud check
start_glass --ez hud false

echo "== layout checks"
status=0
for dump in "${CHECKED[@]}"; do
    python3 "$TOOLS/check_layout.py" "$dump" || status=1
done
echo "screenshots in $OUT"
exit $status
