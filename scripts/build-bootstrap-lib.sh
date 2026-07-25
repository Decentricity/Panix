#!/usr/bin/env sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
REPO_ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
CPP_DIR="$REPO_ROOT/app/src/main/cpp"
OUT_DIR="$REPO_ROOT/app/src/main/jniLibs/arm64-v8a"
BOOTSTRAP_ZIP="$CPP_DIR/bootstrap-aarch64.zip"
BOOTSTRAP_VERSION="2026.02.12-r1%2Bapt.android-7"
BOOTSTRAP_SHA256="ea2aeba8819e517db711f8c32369e89e7c52cee73e07930ff91185e1ab93f4f3"
BOOTSTRAP_URL="https://github.com/termux/termux-packages/releases/download/bootstrap-$BOOTSTRAP_VERSION/bootstrap-aarch64.zip"
JAVA_HOME="${JAVA_HOME:-/data/data/com.termux/files/usr/lib/jvm/java-21-openjdk}"
CC="${CC:-clang}"

need_download=1
if [ -f "$BOOTSTRAP_ZIP" ]; then
    current_sha=$(sha256sum "$BOOTSTRAP_ZIP" | cut -d ' ' -f 1)
    [ "$current_sha" = "$BOOTSTRAP_SHA256" ] && need_download=0
fi

if [ "$need_download" = 1 ]; then
    mkdir -p "$CPP_DIR"
    curl -L --fail --output "$BOOTSTRAP_ZIP" "$BOOTSTRAP_URL"
fi

printf '%s  %s\n' "$BOOTSTRAP_SHA256" "$BOOTSTRAP_ZIP" | sha256sum -c -

mkdir -p "$OUT_DIR"

(
    cd "$CPP_DIR"
    "$CC" \
        -shared \
        -fPIC \
        -std=c11 \
        -Wall \
        -Wextra \
        -Werror \
        -Os \
        -fno-stack-protector \
        -Wl,--gc-sections \
        -I"$JAVA_HOME/include" \
        -I"$JAVA_HOME/include/linux" \
        termux-bootstrap.c \
        termux-bootstrap-zip.S \
        -o "$OUT_DIR/libtermux-bootstrap.so"
)

printf 'Built %s\n' "$OUT_DIR/libtermux-bootstrap.so"
