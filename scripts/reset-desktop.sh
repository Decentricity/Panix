#!/usr/bin/env sh
set -eu

PANIX_FILES_DIR="${PANIX_FILES_DIR:-/data/data/io.github.decentricity.panix/files}"
PANIX_ROOTFS_DIR="${PANIX_ROOTFS_DIR:-$PANIX_FILES_DIR/debian}"
PANIX_STAGING_DIR="${PANIX_STAGING_DIR:-$PANIX_FILES_DIR/debian.staging}"
PANIX_EXPORT_DIR="${PANIX_EXPORT_DIR:-$PANIX_FILES_DIR/export}"

"$(dirname "$0")/stop-desktop.sh"
mkdir -p "$PANIX_EXPORT_DIR"
rm -rf "$PANIX_STAGING_DIR" "$PANIX_ROOTFS_DIR"
rm -f "$PANIX_FILES_DIR/panix-state/firstboot.state"
printf 'Panix Debian environment reset; export directory preserved at %s\n' "$PANIX_EXPORT_DIR"
