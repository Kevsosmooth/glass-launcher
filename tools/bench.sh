#!/usr/bin/env bash
# Run the macrobenchmarks (frame timing, startup) on a build host with the TV attached over adb.
# Never run while someone is watching: the benchmarks launch Glass and drive it.
#
# Usage: DEVICE=<ip:port> tools/bench.sh [-d <ip:port>]
# Environment: BUILD_HOST (ssh host that has the SDK and adb; empty = run here), BUILD_DIR,
#              BUILD_PREFIX (see build.sh), ADB (adb on the build host, default `adb`).
set -euo pipefail
TOOLS=$(cd "$(dirname "$0")" && pwd)
ROOT=$(cd "$TOOLS/.." && pwd)
source "$TOOLS/lib.sh"
gl_parse_device "$@"
[[ -n "$DEVICE" ]] || gl_die "no device: set DEVICE=<ip:port> or pass -d <ip:port>"

BUILD_HOST=${BUILD_HOST:-}
BUILD_DIR=${BUILD_DIR:-projects/glass-launcher}
BUILD_PREFIX=${BUILD_PREFIX:-}
TASK=":benchmark:connectedBenchmarkReleaseAndroidTest"
OUT_DIR=${OUT_DIR:-$ROOT/out}

if [[ -z "$BUILD_HOST" ]]; then
    cd "$ROOT"
    # shellcheck disable=SC2086  # BUILD_PREFIX is intentionally word-split
    ANDROID_SERIAL=$DEVICE $BUILD_PREFIX ./gradlew --no-daemon "$TASK"
    exit 0
fi

REMOTE_DIR=$(printf %q "$BUILD_DIR")
"$TOOLS/build.sh" sync
ssh "$BUILD_HOST" "adb connect $(printf %q "$DEVICE") >/dev/null; cd $REMOTE_DIR && ANDROID_SERIAL=$(printf %q "$DEVICE") $BUILD_PREFIX ./gradlew --no-daemon $TASK"
mkdir -p "$OUT_DIR/benchmark"
rsync -a "$BUILD_HOST:$BUILD_DIR/benchmark/build/outputs/connected_android_test_additional_output/" "$OUT_DIR/benchmark/" 2>/dev/null \
    || echo "no benchmark JSON found under benchmark/build/outputs; see the gradle report on $BUILD_HOST"
echo "results: $OUT_DIR/benchmark"
