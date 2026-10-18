#!/usr/bin/env bash
# Turn Glass's Home button helper (an accessibility service) on or off over adb, keeping any other
# accessibility services that are on. For TVs whose Accessibility screen does not list it, such as
# Fire TV. Dry run unless --apply.
#
# Usage: DEVICE=<ip:port> tools/home-helper.sh [-d <ip:port>] [--apply | --undo]
#   (no flag)  dry run: print the list --apply would write, change nothing
#   --apply    add the helper to enabled_accessibility_services and switch accessibility on
#   --undo     take the helper out again; other services stay as they were
set -euo pipefail
source "$(dirname "$0")/lib.sh"
gl_parse_device "$@"; set -- ${GL_ARGS[@]+"${GL_ARGS[@]}"}; gl_init

SERVICE="$GL_PKG/$GL_PKG.homebutton.HomeButtonService"
SHORT_SERVICE="$GL_PKG/.homebutton.HomeButtonService"
MODE=dry
for arg in "$@"; do
    case "$arg" in
        --apply) [[ $MODE == undo ]] && gl_die "--apply and --undo together"; MODE=apply ;;
        --undo) [[ $MODE == apply ]] && gl_die "--apply and --undo together"; MODE=undo ;;
        *) gl_die "unknown option $arg (see the header of $0)" ;;
    esac
done

read_services() {
    local v
    v=$(adb_ shell settings get secure enabled_accessibility_services | tr -d '\r')
    [[ "$v" == null ]] && v=""
    echo "$v"
}

current=$(read_services)
others=$(tr ':' '\n' <<<"$current" | grep -v -x -e "$SERVICE" -e "$SHORT_SERVICE" -e '' | paste -sd: - || true)
if [[ $MODE == undo ]]; then new=$others; else new=${others:+$others:}$SERVICE; fi

if [[ $MODE == dry ]]; then
    echo "now:                ${current:-<none>}"
    echo "dry run, would set: $new"
    echo "Nothing changed. Re-run with --apply to turn the helper on; --undo turns it off."
    exit 0
fi

if [[ $MODE == apply ]]; then
    installed=$(adb_ shell pm list packages "$GL_PKG" | tr -d '\r')
    grep -qx "package:$GL_PKG" <<<"$installed" || gl_die "$GL_PKG is not installed on $DEVICE: tools/install.sh first"
fi

# Quoted for the device shell: service names may contain $ (inner classes).
if [[ -n "$new" ]]; then
    adb_ shell settings put secure enabled_accessibility_services "'$new'"
    adb_ shell settings put secure accessibility_enabled 1
else
    adb_ shell settings delete secure enabled_accessibility_services >/dev/null
    adb_ shell settings put secure accessibility_enabled 0
fi

after=$(read_services)
if [[ $MODE == apply ]]; then
    tr ':' '\n' <<<"$after" | grep -qx -e "$SERVICE" -e "$SHORT_SERVICE" \
        || gl_die "the TV did not keep the setting (now: ${after:-<none>})"
    echo "Helper on. Press Home on the remote: Glass should open. Undo: $0 --undo"
else
    echo "Helper off. Home goes to the stock screen again (services now: ${after:-<none>})."
fi
