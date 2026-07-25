#!/usr/bin/env sh
set -eu

PANIX_FILES_DIR="${PANIX_FILES_DIR:-/data/data/io.github.decentricity.panix/files}"
PANIX_PREFIX_DIR="${PANIX_PREFIX_DIR:-$PANIX_FILES_DIR/usr}"
PANIX_ROOTFS_DIR="${PANIX_ROOTFS_DIR:-$PANIX_FILES_DIR/debian}"
PANIX_TMP_DIR="${PANIX_TMP_DIR:-$PANIX_FILES_DIR/tmp}"
PANIX_LOG_DIR="${PANIX_LOG_DIR:-$PANIX_FILES_DIR/logs}"
PANIX_LOCK_DIR="${PANIX_LOCK_DIR:-$PANIX_FILES_DIR/run}"
DISPLAY="${DISPLAY:-:1}"

mkdir -p "$PANIX_TMP_DIR" "$PANIX_LOG_DIR" "$PANIX_LOCK_DIR" "$PANIX_FILES_DIR/export" "$PANIX_ROOTFS_DIR/home/panix/Downloads"

LOCK_FILE="$PANIX_LOCK_DIR/desktop.lock"
if [ -e "$LOCK_FILE" ] && kill -0 "$(cat "$LOCK_FILE")" 2>/dev/null; then
    echo "Panix desktop already running with supervisor pid $(cat "$LOCK_FILE")"
    exit 0
fi

echo $$ > "$LOCK_FILE"
trap 'rm -f "$LOCK_FILE"' EXIT INT TERM

export DISPLAY
export PATH="$PANIX_PREFIX_DIR/bin:/system/bin"
export PREFIX="$PANIX_PREFIX_DIR"
export LD_LIBRARY_PATH="$PANIX_PREFIX_DIR/lib"
export PROOT_LOADER="$PANIX_PREFIX_DIR/libexec/proot/loader"
export PROOT_TMP_DIR="$PANIX_TMP_DIR"
export HOME=/home/panix
export USER=panix
export LOGNAME=panix
export SHELL=/bin/bash
export LANG=C.UTF-8
export TMPDIR=/tmp
export XDG_RUNTIME_DIR=/tmp/panix-runtime

exec "$PANIX_PREFIX_DIR/bin/proot" \
    --rootfs="$PANIX_ROOTFS_DIR" \
    --link2symlink \
    --kill-on-exit \
    --sysvipc \
    --ashmem-memfd \
    --change-id=1000:1000 \
    --bind=/dev \
    --bind=/proc \
    --bind=/sys \
    --bind="$PANIX_TMP_DIR:/tmp" \
    --bind="$PANIX_FILES_DIR/export:/home/panix/Downloads" \
    --cwd=/home/panix \
    /usr/bin/env -i \
    HOME="$HOME" USER="$USER" LOGNAME="$LOGNAME" SHELL="$SHELL" \
    DISPLAY="$DISPLAY" LANG="$LANG" TMPDIR="$TMPDIR" XDG_RUNTIME_DIR="$XDG_RUNTIME_DIR" \
    PATH=/usr/local/bin:/usr/bin:/bin:/usr/local/sbin:/usr/sbin:/sbin \
    /bin/bash -lc 'mkdir -p "$XDG_RUNTIME_DIR" /home/panix/Downloads && chmod 700 "$XDG_RUNTIME_DIR" && dbus-launch --exit-with-session startxfce4' \
    >> "$PANIX_LOG_DIR/desktop.log" 2>&1
