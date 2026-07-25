#!/usr/bin/env sh
set -eu

PANIX_FILES_DIR="${PANIX_FILES_DIR:-/data/data/io.github.decentricity.panix/files}"
PANIX_LOCK_DIR="${PANIX_LOCK_DIR:-$PANIX_FILES_DIR/run}"
LOCK_FILE="$PANIX_LOCK_DIR/desktop.lock"

[ -f "$LOCK_FILE" ] || exit 0
PID=$(cat "$LOCK_FILE")
kill "$PID" 2>/dev/null || true
rm -f "$LOCK_FILE"
