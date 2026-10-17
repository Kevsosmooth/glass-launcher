#!/usr/bin/env bash
# Offline tests for lib.sh and make-default.sh using tools/fixtures/fake-adb. No device needed.
# Usage: tools/test_scripts.sh
set -uo pipefail
HERE=$(cd "$(dirname "$0")" && pwd)
STATE=$(mktemp -d)
trap 'rm -rf "$STATE"' EXIT
export ADB="$HERE/fixtures/fake-adb" DEVICE=fake:5555 FAKE_STATE="$STATE" FAKE_LOG="$STATE/log"
export GL_LOCAL_CONF=/nonexistent   # use the built-in defaults, not this machine's tools/local.conf
FAILS=0; RUNS=0

# check <name> <expected exit> <expected output substring or ""> -- <command...>
check() {
    local name=$1 want=$2 needle=$3; shift 4
    local out rc
    : > "$FAKE_LOG"; rm -f "$FAKE_STATE/top"
    out=$("$@" 2>&1); rc=$?
    RUNS=$((RUNS + 1))
    if [[ $rc -ne $want ]] || { [[ -n "$needle" ]] && [[ "$out" != *"$needle"* ]]; }; then
        FAILS=$((FAILS + 1)); echo "FAIL $name (exit $rc, want $want, needle '$needle')"; echo "$out" | sed 's/^/    /'
    else
        echo "ok   $name"
    fi
}
idle() { ( source "$HERE/lib.sh"; gl_ensure_idle; echo idle-ok ); }
logged() { grep -q -- "$1" "$FAKE_LOG"; }

echo 1 > "$STATE/awake"
check "idle on stock launcher" 0 idle-ok -- idle
FAKE_TOP=org.videolan.vlc check "refuses a player app" 1 "in use" -- idle
FAKE_TOP=com.netflix.ninja check "refuses Netflix" 1 "in use" -- idle
FAKE_TOP=com.example.someapp check "refuses unknown app" 1 "not on Home" -- idle
FAKE_TOP=com.example.someapp GL_ALLOW_ANY_TOP=1 check "override allows unknown app" 0 idle-ok -- idle
FAKE_TOP=com.kevin.glasslauncher check "idle on Glass" 0 idle-ok -- idle
echo 0 > "$STATE/awake"
check "asleep and cannot wake" 1 "did not wake" -- idle
FAKE_WAKES=1 check "wakes a sleeping screen" 0 idle-ok -- idle
echo 1 > "$STATE/awake"

check "no device given" 1 "no device" -- env -u DEVICE bash -c "source '$HERE/lib.sh'; gl_parse_device; gl_init"
printf 'ADB=${ADB:-/conf/adb}\nDEVICE=${DEVICE:-conf:5555}\n' > "$STATE/local.conf"
check "local.conf fills unset ADB and DEVICE" 0 "/conf/adb conf:5555" -- \
    env -u ADB -u DEVICE GL_LOCAL_CONF="$STATE/local.conf" bash -c "source '$HERE/lib.sh'; echo \$ADB \$DEVICE"
check "environment beats local.conf" 0 "/env/adb" -- \
    env ADB=/env/adb GL_LOCAL_CONF="$STATE/local.conf" bash -c "source '$HERE/lib.sh'; echo \$ADB"
FAKE_DUMP_BUSY=1 check "busy screen skips the layout dump" 0 "no layout dump for x.xml" -- \
    bash -c "source '$HERE/lib.sh'; uidump_to '$STATE/x.xml'; [[ ! -e '$STATE/x.xml' ]]"
check "-d overrides" 0 "other:1" -- bash -c "source '$HERE/lib.sh'; gl_parse_device -d other:1 x; gl_init; echo \$DEVICE \${GL_ARGS[*]}"

# build.sh / install.sh against a throwaway checkout with a fake gradlew
FAKE_REPO="$STATE/repo"
mkdir -p "$FAKE_REPO/tools" "$FAKE_REPO/app/build/outputs/apk/release"
cp "$HERE/build.sh" "$HERE/install.sh" "$HERE/lib.sh" "$FAKE_REPO/tools/"
printf '#!/usr/bin/env bash\necho "gradle $*" >> "%s"\ntouch app/build/outputs/apk/release/app-release.apk\n' "$FAKE_LOG" > "$FAKE_REPO/gradlew"
chmod +x "$FAKE_REPO/gradlew"
check "build.sh rejects bad variant" 2 usage -- "$FAKE_REPO/tools/build.sh" nonsense
check "build.sh release (local)" 0 app-release.apk -- "$FAKE_REPO/tools/build.sh" release
[[ -f "$FAKE_REPO/out/app-release.apk" ]] || { FAILS=$((FAILS + 1)); echo "FAIL APK not copied to out/"; }
check "install.sh installs" 0 "" -- "$FAKE_REPO/tools/install.sh" release
logged "install -r $FAKE_REPO/out/app-release.apk" || { FAILS=$((FAILS + 1)); echo "FAIL install -r not called"; }
check "install.sh needs a build" 1 "run tools/build.sh" -- "$FAKE_REPO/tools/install.sh" debug

# screenshots.sh with canned dumps
SHOTS="$STATE/shots"
shots() { SETTLE=0 KEY_DELAY=0 "$HERE/screenshots.sh" "$@"; }
FAKE_DUMP="$HERE/fixtures/layout_pass.xml" check "screenshots.sh good layout" 0 "screenshots in" -- shots "$SHOTS"
[[ -f "$SHOTS/01-home.png" && -f "$SHOTS/10-wallpaper-graphite.xml" && -f "$SHOTS/13-hud.xml" ]] || { FAILS=$((FAILS + 1)); echo "FAIL screenshots missing"; }
logged "--es look Solid" || { FAILS=$((FAILS + 1)); echo "FAIL Solid look not requested"; }
logged "--ei fake_apps 200" || { FAILS=$((FAILS + 1)); echo "FAIL fake apps not requested"; }
logged "--longpress" || { FAILS=$((FAILS + 1)); echo "FAIL no long press"; }
tail -n 12 "$FAKE_LOG" | grep -q -- "--ez hud false" || { FAILS=$((FAILS + 1)); echo "FAIL prefs not reset at the end"; }
FAKE_DUMP="$HERE/fixtures/layout_overlap.xml" check "screenshots.sh bad layout fails" 1 "FAIL no tile overlaps" -- shots "$STATE/shots-bad"
FAKE_RELEASE=1 FAKE_DUMP="$HERE/fixtures/layout_pass.xml" check "screenshots.sh refuses release build" 1 "not a debug build" -- shots "$STATE/shots-rel"
FAKE_TOP=org.videolan.vlc FAKE_DUMP="$HERE/fixtures/layout_pass.xml" check "screenshots.sh stops while a player is in front" 1 "in use" -- shots "$STATE/shots-busy"
logged "keyevent" && { FAILS=$((FAILS + 1)); echo "FAIL keys sent while TV busy"; }

# measure.sh on a release build with canned gfxinfo
export FAKE_REPORT="$HERE/fixtures/report"
measure() { SETTLE=0 SHORT_PAUSE=0 IDLE_SECONDS=2 REPORT_DIR="$STATE/reports" "$HERE/measure.sh" "$@"; }
FAKE_RELEASE=1 check "measure.sh within budget" 0 "Idle RAM (PSS) | 60.0 MB" -- measure t1
[[ -f "$STATE/reports/$(date +%Y-%m-%d)-t1/summary.md" && -f "$STATE/reports/$(date +%Y-%m-%d)-t1/t1-grid-framestats.txt" ]] || { FAILS=$((FAILS + 1)); echo "FAIL report files missing"; }
logged "KEYCODE_HOME" && { FAILS=$((FAILS + 1)); echo "FAIL measure.sh pressed Home (another launcher may own it)"; }
FAKE_STUCK=1 FAKE_RELEASE=1 check "measure.sh stops when Glass is not in front" 1 "Glass is not in front" -- measure t4
logged "keyevent 22" && { FAILS=$((FAILS + 1)); echo "FAIL measure.sh swept another launcher"; }
check "measure.sh refuses debug build" 1 "debug build" -- measure t2
FAKE_TOP=com.netflix.ninja FAKE_RELEASE=1 check "measure.sh stops while Netflix is in front" 1 "in use" -- measure t3
check "bench.sh needs a device" 1 "no device" -- env -u DEVICE "$HERE/bench.sh"
check "build.sh sync needs BUILD_HOST" 2 "BUILD_HOST" -- "$HERE/build.sh" sync

echo 1 > "$STATE/awake"
if [[ -x "$HERE/make-default.sh" ]]; then
    PKGS="com.kevin.glasslauncher com.google.android.tvlauncher com.google.android.apps.tv.launcherx com.spocky.projengmenu"
    FAKE_PACKAGES="$PKGS" check "make-default dry run" 0 "dry run" -- "$HERE/make-default.sh"
    logged "pm disable-user" && { FAILS=$((FAILS + 1)); echo "FAIL dry run changed the device"; }
    logged "uninstall" && { FAILS=$((FAILS + 1)); echo "FAIL dry run uninstalled"; }
    FAKE_PACKAGES="$PKGS" check "make-default apply" 0 "disabled" -- "$HERE/make-default.sh" --apply --remove-projectivy
    logged "pm disable-user --user 0 com.google.android.tvlauncher" || { FAILS=$((FAILS + 1)); echo "FAIL apply did not disable stock launcher"; }
    logged "pm disable-user --user 0 com.google.android.apps.tv.launcherx" || { FAILS=$((FAILS + 1)); echo "FAIL apply skipped launcherx"; }
    logged "uninstall com.spocky.projengmenu" || { FAILS=$((FAILS + 1)); echo "FAIL --remove-projectivy did not uninstall Projectivy"; }
    FAKE_PACKAGES="$PKGS" check "apply keeps Projectivy unless asked" 0 "kept com.spocky.projengmenu" -- "$HERE/make-default.sh" --apply < /dev/null
    logged "uninstall" && { FAILS=$((FAILS + 1)); echo "FAIL uninstalled Projectivy without asking"; }
    FAKE_PACKAGES="com.google.android.tvlauncher" check "refuses without Glass" 1 "not installed" -- "$HERE/make-default.sh" --apply
    FAKE_PACKAGES="$PKGS" check "undo" 0 "enabled" -- "$HERE/make-default.sh" --undo
    logged "pm enable com.google.android.tvlauncher" || { FAILS=$((FAILS + 1)); echo "FAIL undo did not enable stock launcher"; }
    FAKE_FEATURES="" FAKE_MANUFACTURER=Amazon FAKE_PACKAGES="$PKGS" check "Fire TV explains and exits 0" 0 "Fire TV" -- "$HERE/make-default.sh" --apply
    logged "pm disable-user" && { FAILS=$((FAILS + 1)); echo "FAIL Fire TV was modified"; }
fi
echo "$((RUNS)) checks, $FAILS failed"
exit $((FAILS > 0))
