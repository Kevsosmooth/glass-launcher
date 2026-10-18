#!/usr/bin/env bash
# Make Glass Launcher the Home screen on an Android TV / Google TV by disabling the stock launcher
# (reversible). Dry run unless --apply. Fire TV cannot replace Home: the script points to
# tools/home-helper.sh and exits 0.
#
# Usage: DEVICE=<ip:port> tools/make-default.sh [-d <ip:port>] [--apply | --undo] [--remove-projectivy]
#   (no flag)            dry run: print what --apply would do, change nothing
#   --apply              install check, then `pm disable-user --user 0` on the stock launcher(s); offers
#                        to uninstall Projectivy Launcher if present (asks y/N)
#   --remove-projectivy  with --apply: uninstall Projectivy without asking. Cannot be undone: its
#                        settings are lost, reinstall it from the Play Store to get it back
#   --undo               re-enable the stock launcher(s) right away (undoing is always safe)
set -euo pipefail
source "$(dirname "$0")/lib.sh"
gl_parse_device "$@"; set -- ${GL_ARGS[@]+"${GL_ARGS[@]}"}; gl_init

STOCK_LAUNCHERS="com.google.android.tvlauncher com.google.android.apps.tv.launcherx"
PROJECTIVY=com.spocky.projengmenu
MODE=dry
REMOVE_PROJECTIVY=0
for arg in "$@"; do
    case "$arg" in
        --apply) [[ $MODE == undo ]] && gl_die "--apply and --undo together"; MODE=apply ;;
        --undo) [[ $MODE == apply ]] && gl_die "--apply and --undo together"; MODE=undo ;;
        --remove-projectivy) REMOVE_PROJECTIVY=1 ;;
        *) gl_die "unknown option $arg (see the header of $0)" ;;
    esac
done

installed_packages=$(adb_ shell pm list packages | tr -d '\r')
is_installed() { grep -qx "package:$1" <<<"$installed_packages"; }

is_fire_tv() {
    [[ "$(adb_ shell getprop ro.product.manufacturer | tr -d '\r')" == Amazon ]] || is_installed com.amazon.tv.launcher
}

if is_fire_tv; then
    echo "This is a Fire TV. Fire OS does not let apps replace the Home screen, and its launcher"
    echo "cannot be disabled without root. To make the Home button open Glass, turn on its Home button"
    echo "helper: tools/home-helper.sh --apply (or Glass Settings > Home button helper)."
    exit 0
fi
gl_has_leanback \
    || gl_die "$DEVICE is not an Android TV / Google TV (no leanback feature); nothing to do"

present=()
for pkg in $STOCK_LAUNCHERS; do is_installed "$pkg" && present+=("$pkg"); done

run() {
    if [[ $MODE == dry ]]; then echo "dry run, would run: pm $*"; else adb_ shell pm "$@"; fi
}

confirm() {
    [[ $REMOVE_PROJECTIVY == 1 ]] && return 0
    [[ -t 0 ]] || return 1
    read -r -p "$1 [y/N] " answer
    [[ "$answer" == y || "$answer" == Y ]]
}

if [[ $MODE == undo ]]; then
    MODE=apply   # `run` should really execute
    for pkg in "${present[@]}"; do run enable "$pkg"; echo "enabled $pkg"; done
    echo "Stock launcher back. Press Home and pick it; Glass Launcher stays installed."
    exit 0
fi

is_installed "$GL_PKG" || gl_die "$GL_PKG is not installed on $DEVICE: tools/install.sh first; refusing to disable the stock launcher"
[[ ${#present[@]} -gt 0 ]] || gl_die "no stock launcher package found (${STOCK_LAUNCHERS}); nothing to disable"

for pkg in "${present[@]}"; do
    run disable-user --user 0 "$pkg"
    [[ $MODE == apply ]] && echo "disabled $pkg"
done

if is_installed "$PROJECTIVY"; then
    if [[ $MODE == dry ]]; then
        echo "dry run, would ask: uninstall Projectivy Launcher ($PROJECTIVY)?"
    elif confirm "Uninstall Projectivy Launcher ($PROJECTIVY)? This cannot be undone."; then
        adb_ uninstall "$PROJECTIVY"
    else
        echo "kept $PROJECTIVY"
    fi
fi

if [[ $MODE == dry ]]; then
    echo "dry run: nothing changed. Re-run with --apply to do it; --undo reverses it."
else
    echo "Done. Press Home on the remote; Glass Launcher is now the only launcher. Undo: $0 --undo"
fi
