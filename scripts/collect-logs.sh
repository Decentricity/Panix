#!/usr/bin/env sh
set -eu

PANIX_FILES_DIR="${PANIX_FILES_DIR:-/data/data/io.github.decentricity.panix/files}"
OUT="${1:-$PWD/panix-logs-$(date +%Y%m%d-%H%M%S).tar.gz}"

tar -czf "$OUT" \
    -C "$PANIX_FILES_DIR" \
    logs panix-state run 2>/dev/null || {
        echo "No complete Panix log set found under $PANIX_FILES_DIR" >&2
        exit 1
    }

printf '%s\n' "$OUT"
