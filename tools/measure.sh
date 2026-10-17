#!/usr/bin/env bash
# Measure the RELEASE build against the spec budgets: idle RAM and CPU, then frame stats while the
# D-pad sweeps the dock and the grid. Writes reports/<date>-<name>/ with the raw dumps and summary.md.
# Sends D-pad keys (never Select) on the TV: run only while nobody is watching.
#
# Usage: DEVICE=<ip:port> tools/measure.sh [-d <ip:port>] [name]      (default name: glass)
# Environment: REPORT_DIR (default reports), IDLE_SECONDS (default 10), SETTLE (default 3),
#              SHORT_PAUSE (seconds between sweeps, default 1),
#              ALLOW_DEBUG=1 to measure a debug build anyway (numbers will not match the budgets).
# Install the release build first: tools/build.sh release && tools/install.sh release
set -euo pipefail
TOOLS=$(cd "$(dirname "$0")" && pwd)
source "$TOOLS/lib.sh"
gl_parse_device "$@"; set -- ${GL_ARGS[@]+"${GL_ARGS[@]}"}; gl_init

NAME=${1:-glass}
OUT=${REPORT_DIR:-reports}/$(date +%Y-%m-%d)-$NAME
SETTLE=${SETTLE:-3}
IDLE_SECONDS=${IDLE_SECONDS:-10}
SHORT=${SHORT_PAUSE:-1}
mkdir -p "$OUT"

gl_ensure_idle
if gl_is_debug_build; then
    [[ "${ALLOW_DEBUG:-0}" == 1 ]] || gl_die "$GL_PKG is a debug build; budgets are for release: tools/build.sh release && tools/install.sh release"
fi

# Back to the top of Glass's home. Not the Home key: another launcher may own it.
home() { glass_home; sleep "$1"; gl_require_glass_front; }

# Fresh start so the stats cover this run only, then let startup work finish.
adb_ shell am force-stop "$GL_PKG"
start_glass
sleep "$SETTLE"
home "$SETTLE"

echo "idle ${IDLE_SECONDS}s"
collect_mem "$GL_PKG" "$OUT" "$NAME"
collect_idle_cpu "$GL_PKG" "$OUT" "$NAME"

echo "dock sweep (slow)"
reset_frames "$GL_PKG"; nav_slow; collect_frames "$GL_PKG" "$OUT" "$NAME-dock-slow"

echo "dock sweep (burst)"
home "$SHORT"
reset_frames "$GL_PKG"; nav_burst; sleep "$SHORT"; collect_frames "$GL_PKG" "$OUT" "$NAME-dock-burst"

echo "grid sweep"
home "$SHORT"
key_down; key_down; sleep "$SHORT"
reset_frames "$GL_PKG"; nav_grid; nav_burst; sleep "$SHORT"; collect_frames "$GL_PKG" "$OUT" "$NAME-grid"
home 0

python3 "$TOOLS/summarize_report.py" "$OUT" "$NAME"
