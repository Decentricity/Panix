#!/usr/bin/env sh
set -eu

PANIX_FILES_DIR="${PANIX_FILES_DIR:-/data/data/io.github.decentricity.panix/files}"
PANIX_ROOTFS_DIR="${PANIX_ROOTFS_DIR:-$PANIX_FILES_DIR/debian}"
PANIX_TMP_DIR="${PANIX_TMP_DIR:-$PANIX_FILES_DIR/tmp}"
PANIX_LOG_DIR="${PANIX_LOG_DIR:-$PANIX_FILES_DIR/logs}"
PANIX_LOCK_DIR="${PANIX_LOCK_DIR:-$PANIX_FILES_DIR/run}"
DISPLAY="${DISPLAY:-:1}"

mkdir -p "$PANIX_TMP_DIR" "$PANIX_LOG_DIR" "$PANIX_LOCK_DIR"

LOCK_FILE="$PANIX_LOCK_DIR/desktop.lock"
if [ -e "$LOCK_FILE" ] && kill -0 "$(cat "$LOCK_FILE")" 2>/dev/null; then
    echo "Panix desktop already running with supervisor pid $(cat "$LOCK_FILE")"
    exit 0
fi

echo $$ > "$LOCK_FILE"
trap 'rm -f "$LOCK_FILE"' EXIT INT TERM

export DISPLAY
export HOME=/home/panix
export USER=panix
export LOGNAME=panix
export SHELL=/bin/bash
export LANG=C.UTF-8
export TMPDIR=/tmp

exec proot \
    --rootfs="$PANIX_ROOTFS_DIR" \
    --link2symlink \
    --kill-on-exit \
    --bind=/dev \
    --bind=/proc \
    --bind=/sys \
    --bind="$PANIX_TMP_DIR:/tmp" \
    --cwd=/home/panix \
    /usr/bin/env -i \
    HOME="$HOME" USER="$USER" LOGNAME="$LOGNAME" SHELL="$SHELL" \
    DISPLAY="$DISPLAY" LANG="$LANG" TMPDIR="$TMPDIR" \
    /bin/bash -lc 'dbus-launch --exit-with-session startxfce4' \
    >> "$PANIX_LOG_DIR/desktop.log" 2>&1
