#!/usr/bin/env bash
# Build Glass Launcher and copy the APKs to out/.
# Usage: tools/build.sh [debug|release|bench|sync]
#   debug    app-debug.apk
#   release  app-release.apk (R8-minified; what you measure and ship)
#   bench    release app + the macrobenchmark test APK
#   sync     only copy the sources to BUILD_HOST (used by bench.sh)
# Environment:
#   BUILD_HOST    ssh host to build on (rsync + gradle over ssh); empty = build in this checkout
#   BUILD_DIR     checkout path on BUILD_HOST, relative to its $HOME (default projects/glass-launcher)
#   BUILD_PREFIX  command placed in front of gradlew (e.g. a memory cap or lock wrapper)
#   OUT_DIR       where the APKs land (default <repo>/out)
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/.." && pwd)
VARIANT=${1:-debug}
BUILD_HOST=${BUILD_HOST:-}
BUILD_DIR=${BUILD_DIR:-projects/glass-launcher}
BUILD_PREFIX=${BUILD_PREFIX:-}
OUT_DIR=${OUT_DIR:-$ROOT/out}

case "$VARIANT" in
    debug) TASKS=":app:assembleDebug"; APK_DIRS="app/build/outputs/apk/debug" ;;
    release) TASKS=":app:assembleRelease"; APK_DIRS="app/build/outputs/apk/release" ;;
    bench) TASKS=":app:assembleRelease :benchmark:assemble"; APK_DIRS="app/build/outputs/apk/release benchmark/build/outputs/apk" ;;
    sync) [[ -n "${BUILD_HOST:-}" ]] || { echo "sync needs BUILD_HOST" >&2; exit 2; }; TASKS=""; APK_DIRS="" ;;
    *) echo "usage: build.sh [debug|release|bench|sync]" >&2; exit 2 ;;
esac

mkdir -p "$OUT_DIR"

if [[ -z "$BUILD_HOST" ]]; then
    cd "$ROOT"
    # shellcheck disable=SC2086  # BUILD_PREFIX and TASKS are intentionally word-split
    $BUILD_PREFIX ./gradlew --no-daemon -q $TASKS
    # shellcheck disable=SC2086
    find $APK_DIRS -name '*.apk' -exec cp -v {} "$OUT_DIR/" \;
    exit 0
fi

# rsync --delete empties BUILD_DIR first: refuse anything that is not a dedicated checkout folder.
case "$BUILD_DIR" in
    ''|.|..|/*|~*|*..*) echo "BUILD_DIR must be a folder under the build host's home, e.g. projects/glass-launcher (got '$BUILD_DIR')" >&2; exit 2 ;;
esac
[[ "$(basename "$BUILD_DIR")" == glass-launcher* ]] \
    || { echo "BUILD_DIR must end in a folder named glass-launcher* (got '$BUILD_DIR')" >&2; exit 2; }

rsync -a --delete \
    --exclude .git --exclude .worktrees --exclude build --exclude .gradle --exclude .kotlin \
    --exclude shots --exclude out --exclude reports --exclude local.properties \
    "$ROOT/" "$BUILD_HOST:$BUILD_DIR/"
[[ "$VARIANT" == sync ]] && exit 0

REMOTE_DIR=$(printf %q "$BUILD_DIR")
# sdk.dir is resolved on the build host; ANDROID_HOME there wins if you export it in the host's shell profile.
ssh "$BUILD_HOST" "cd $REMOTE_DIR && { [ -n \"\${ANDROID_HOME:-}\" ] || echo \"sdk.dir=\$HOME/Android/Sdk\" > local.properties; } && $BUILD_PREFIX ./gradlew --no-daemon -q $TASKS"
ssh "$BUILD_HOST" "cd $REMOTE_DIR && find $APK_DIRS -name '*.apk'" \
    | rsync -a --no-relative --files-from=- "$BUILD_HOST:$BUILD_DIR/" "$OUT_DIR/"
ls -1 "$OUT_DIR"/*.apk
