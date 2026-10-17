#!/usr/bin/env bash
# Launcher baseline: RAM, warm start (Settings -> launcher), frames while navigating, idle CPU.
# Sends D-pad keys and opens Settings: run only with the owner's OK while the TV is idle.
# Usage: DEVICE=<ip:port> tools/baseline-launcher.sh [-d <ip:port>] <out-dir> <name> <package>/<activity>
#   stock:      com.google.android.tvlauncher/.MainActivity
#   projectivy: com.spocky.projengmenu/.ui.home.MainActivity
set -euo pipefail
source "$(dirname "$0")/lib.sh"
gl_parse_device "$@"; set -- ${GL_ARGS[@]+"${GL_ARGS[@]}"}; gl_init
OUT=${1:?usage: baseline-launcher.sh [-d <ip:port>] <out-dir> <name> <component>}
NAME=${2:?name}; COMP=${3:?component}; P=${COMP%%/*}
mkdir -p "$OUT"
START="am start -W -n $COMP"

gl_ensure_idle
adb_ shell "$START" >/dev/null; sleep 4
collect_mem "$P" "$OUT" "$NAME-home"
collect_idle_cpu "$P" "$OUT" "$NAME"
screencap_to "$OUT/$NAME-home.png"

for i in 1 2 3; do
    adb_ shell am start -a android.settings.SETTINGS >/dev/null; sleep 3
    adb_ shell "$START" > "$OUT/$NAME-warm-start-$i.txt"; sleep 3
done

reset_frames "$P"; nav_slow;  collect_frames "$P" "$OUT" "$NAME-slow"
reset_frames "$P"; nav_burst; sleep 2; collect_frames "$P" "$OUT" "$NAME-burst"

echo "== $NAME"; grep -h "TOTAL PSS" "$OUT/$NAME-home-meminfo.txt"
grep -h TotalTime "$OUT/$NAME"-warm-start-*.txt
for f in slow burst; do echo "-- $f"; summarize_frames "$OUT/$NAME-$f-gfxinfo.txt"; done
tail -1 "$OUT/$NAME-idle-cpu.txt"
