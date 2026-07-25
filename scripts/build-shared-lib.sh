#!/usr/bin/env sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
REPO_ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
CPP_DIR="$REPO_ROOT/termux-shared/src/main/cpp"
OUT_DIR="$REPO_ROOT/termux-shared/src/main/jniLibs/arm64-v8a"
JAVA_HOME="${JAVA_HOME:-/data/data/com.termux/files/usr/lib/jvm/java-21-openjdk}"
CXX="${CXX:-clang++}"

mkdir -p "$OUT_DIR"

(
    cd "$CPP_DIR"
    "$CXX" \
        -shared \
        -fPIC \
        -std=c++17 \
        -Wall \
        -Wextra \
        -Os \
        -fno-stack-protector \
        -Wl,--gc-sections \
        -I"$JAVA_HOME/include" \
        -I"$JAVA_HOME/include/linux" \
        local-socket.cpp \
        -llog \
        -o "$OUT_DIR/liblocal-socket.so"
)

printf 'Built %s\n' "$OUT_DIR/liblocal-socket.so"
