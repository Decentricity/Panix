#!/usr/bin/env sh
set -eu

PANIX_FILES_DIR="${PANIX_FILES_DIR:-/data/data/io.github.decentricity.panix/files}"
PANIX_ROOTFS_ASSET="${PANIX_ROOTFS_ASSET:-$PANIX_FILES_DIR/assets/debian-trixie-arm64-rootfs.tar.zst}"
PANIX_ROOTFS_SHA256="${PANIX_ROOTFS_SHA256:-$PANIX_FILES_DIR/assets/debian-trixie-arm64-rootfs.tar.zst.sha256}"
PANIX_ROOTFS_DIR="${PANIX_ROOTFS_DIR:-$PANIX_FILES_DIR/debian}"
PANIX_STAGING_DIR="${PANIX_STAGING_DIR:-$PANIX_FILES_DIR/debian.staging}"
PANIX_STATE_DIR="${PANIX_STATE_DIR:-$PANIX_FILES_DIR/panix-state}"
PANIX_STATE_FILE="$PANIX_STATE_DIR/firstboot.state"

state() {
    mkdir -p "$PANIX_STATE_DIR"
    printf '%s\n' "$1" > "$PANIX_STATE_FILE"
    printf '%s\n' "$1"
}

[ -d "$PANIX_ROOTFS_DIR" ] && {
    state READY
    exit 0
}

state VERIFYING_ASSET
(cd "$(dirname "$PANIX_ROOTFS_ASSET")" && sha256sum -c "$PANIX_ROOTFS_SHA256")

state EXTRACTING
rm -rf "$PANIX_STAGING_DIR"
mkdir -p "$PANIX_STAGING_DIR"
zstd -dc "$PANIX_ROOTFS_ASSET" | tar -C "$PANIX_STAGING_DIR" -xf -

state CONFIGURING
mkdir -p "$PANIX_STAGING_DIR/tmp" "$PANIX_STAGING_DIR/home/panix"
chmod 1777 "$PANIX_STAGING_DIR/tmp"

mv "$PANIX_STAGING_DIR" "$PANIX_ROOTFS_DIR"
state READY
