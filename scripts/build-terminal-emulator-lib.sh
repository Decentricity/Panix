#!/usr/bin/env sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
REPO_ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
JNI_DIR="$REPO_ROOT/terminal-emulator/src/main/jni"
OUT_DIR="$REPO_ROOT/terminal-emulator/src/main/jniLibs/arm64-v8a"
JAVA_HOME="${JAVA_HOME:-/data/data/com.termux/files/usr/lib/jvm/java-21-openjdk}"
CC="${CC:-clang}"

mkdir -p "$OUT_DIR"

(
    cd "$JNI_DIR"
    "$CC" \
        -shared \
        -fPIC \
        -D_GNU_SOURCE \
        -std=c11 \
        -Wall \
        -Wextra \
        -Werror \
        -Os \
        -fno-stack-protector \
        -Wl,--gc-sections \
        -I"$JAVA_HOME/include" \
        -I"$JAVA_HOME/include/linux" \
        termux.c \
        -o "$OUT_DIR/libtermux.so"
)

printf 'Built %s\n' "$OUT_DIR/libtermux.so"
