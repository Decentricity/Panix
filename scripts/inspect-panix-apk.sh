#!/usr/bin/env sh
set -eu

APK="${1:-}"
OUT_DIR="${2:-}"
ROOTFS_PROVENANCE="${3:-}"
AAPT="${AAPT:-aapt}"

fail() {
    printf 'inspect-panix-apk: %s\n' "$*" >&2
    exit 1
}

require_file() {
    [ -f "$1" ] || fail "missing $2: $1"
}

require_exec() {
    command -v "$1" >/dev/null 2>&1 || fail "missing executable: $1"
}

contains() {
    grep -Fq "$2" "$1"
}

not_contains_regex() {
    ! grep -Eiq "$2" "$1"
}

[ -n "$APK" ] || fail "usage: scripts/inspect-panix-apk.sh <apk> <out-dir> [rootfs-provenance]"
[ -n "$OUT_DIR" ] || fail "usage: scripts/inspect-panix-apk.sh <apk> <out-dir> [rootfs-provenance]"
require_file "$APK" "APK"
require_exec "$AAPT"
require_exec unzip
require_exec sha256sum

mkdir -p "$OUT_DIR"

"$AAPT" dump badging "$APK" > "$OUT_DIR/aapt-badging.txt"
"$AAPT" dump xmltree "$APK" AndroidManifest.xml > "$OUT_DIR/androidmanifest-xmltree.txt"
unzip -l "$APK" > "$OUT_DIR/apk-contents.txt"
sha256sum "$APK" > "$OUT_DIR/Panix-arm64-v8a.apk.sha256"

contains "$OUT_DIR/aapt-badging.txt" "package: name='io.github.decentricity.panix'" ||
    fail "APK package id is not io.github.decentricity.panix"
contains "$OUT_DIR/aapt-badging.txt" "application-label:'Panix'" ||
    fail "APK application label is not Panix"
contains "$OUT_DIR/aapt-badging.txt" "sdkVersion:'26'" ||
    fail "APK min SDK is not 26"
contains "$OUT_DIR/aapt-badging.txt" "targetSdkVersion:'28'" ||
    fail "APK target SDK is not 28"
contains "$OUT_DIR/aapt-badging.txt" "native-code: 'arm64-v8a'" ||
    fail "APK is not restricted to arm64-v8a native code"
contains "$OUT_DIR/aapt-badging.txt" "launchable-activity: name='com.termux.x11.PanixHomeActivity'" ||
    fail "APK launcher is not the X11-backed Panix HOME activity"

not_contains_regex "$OUT_DIR/aapt-badging.txt" "launchable-activity: name='com\\.termux\\.x11\\.MainActivity'" ||
    fail "Termux:X11 MainActivity is still exposed as a launcher"
contains "$OUT_DIR/androidmanifest-xmltree.txt" "android.intent.category.HOME" ||
    fail "APK manifest does not contain CATEGORY_HOME"
contains "$OUT_DIR/androidmanifest-xmltree.txt" "android.intent.category.DEFAULT" ||
    fail "APK manifest does not contain CATEGORY_DEFAULT"

contains "$OUT_DIR/apk-contents.txt" "assets/debian-trixie-arm64-rootfs.tar.zst" ||
    fail "APK does not contain bundled Debian rootfs"
contains "$OUT_DIR/apk-contents.txt" "assets/debian-trixie-arm64-rootfs.tar.zst.sha256" ||
    fail "APK does not contain bundled Debian rootfs checksum"
contains "$OUT_DIR/apk-contents.txt" "assets/termux-proot-aarch64.tar.zst" ||
    fail "APK does not contain bundled PRoot payload"
contains "$OUT_DIR/apk-contents.txt" "assets/termux-proot-aarch64.tar.zst.sha256" ||
    fail "APK does not contain bundled PRoot payload checksum"
contains "$OUT_DIR/apk-contents.txt" "lib/arm64-v8a/libXlorie.so" ||
    fail "APK does not contain embedded Termux:X11 native library"
contains "$OUT_DIR/apk-contents.txt" "lib/arm64-v8a/libtermux.so" ||
    fail "APK does not contain Termux terminal native library"
contains "$OUT_DIR/apk-contents.txt" "lib/arm64-v8a/libtermux-bootstrap.so" ||
    fail "APK does not contain Panix bootstrap native library"
contains "$OUT_DIR/apk-contents.txt" "lib/arm64-v8a/liblocal-socket.so" ||
    fail "APK does not contain local socket native library"
not_contains_regex "$OUT_DIR/apk-contents.txt" "vnc|tigervnc|x11vnc|novnc|xrdp" ||
    fail "APK contents contain VNC/RDP-related files"

if [ -n "$ROOTFS_PROVENANCE" ] && [ -f "$ROOTFS_PROVENANCE" ]; then
    cp "$ROOTFS_PROVENANCE" "$OUT_DIR/debian-trixie-arm64-rootfs.provenance"
    not_contains_regex "$ROOTFS_PROVENANCE" "vnc|tigervnc|x11vnc|novnc|xrdp" ||
        fail "rootfs package provenance contains VNC/RDP-related packages"
fi

{
    printf 'apk=%s\n' "$APK"
    printf 'sha256=%s\n' "$(cut -d ' ' -f 1 "$OUT_DIR/Panix-arm64-v8a.apk.sha256")"
    printf 'package=io.github.decentricity.panix\n'
    printf 'launcher=com.termux.x11.PanixHomeActivity\n'
    printf 'home_category=present\n'
    printf 'native_code=arm64-v8a\n'
    printf 'bundled_rootfs=assets/debian-trixie-arm64-rootfs.tar.zst\n'
    printf 'bundled_proot=assets/termux-proot-aarch64.tar.zst\n'
    printf 'embedded_x11=lib/arm64-v8a/libXlorie.so\n'
    printf 'vnc_files=absent_in_apk_listing\n'
} > "$OUT_DIR/summary.properties"

printf 'Inspection written to %s\n' "$OUT_DIR"
cat "$OUT_DIR/summary.properties"
