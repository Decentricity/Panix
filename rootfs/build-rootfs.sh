#!/usr/bin/env sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
REPO_ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
OUT_DIR="${OUT_DIR:-$REPO_ROOT/build/rootfs}"
MANIFEST_DIR="$REPO_ROOT/rootfs/manifests"
ROOTFS_NAME="debian-trixie-arm64-rootfs.tar.zst"
ROOTFS_OUT="$OUT_DIR/$ROOTFS_NAME"
ROOTFS_TREE="$OUT_DIR/debian-trixie-arm64-rootfs"
ROOTFS_TAR="$OUT_DIR/debian-trixie-arm64-rootfs.tar"
ROOTFS_TMP="$ROOTFS_OUT.tmp"
APT_SNAPSHOT="${APT_SNAPSHOT:-}"

PACKAGES="bash,coreutils,apt,ca-certificates,sudo,curl,wget,git,nano,less,procps,psmisc,iproute2,python3,build-essential,dbus,dbus-x11,xfce4,xfce4-terminal,thunar,xterm,fonts-dejavu,adwaita-icon-theme"

command -v mmdebstrap >/dev/null 2>&1 || {
    echo "mmdebstrap is required to build the Debian rootfs." >&2
    echo "Install it in a Debian build environment, then rerun rootfs/build-rootfs.sh." >&2
    exit 1
}

command -v zstd >/dev/null 2>&1 || {
    echo "zstd is required to compress the Debian rootfs." >&2
    exit 1
}

fail() {
    echo "rootfs/build-rootfs.sh: $*" >&2
    exit 1
}

mkdir -p "$OUT_DIR" "$MANIFEST_DIR"

PRIMARY_SOURCE="deb http://deb.debian.org/debian trixie main"
UPDATES_SOURCE="deb http://deb.debian.org/debian trixie-updates main"
SECURITY_SOURCE="deb http://security.debian.org/debian-security trixie-security main"

if [ -n "$APT_SNAPSHOT" ]; then
    PRIMARY_SOURCE="deb $APT_SNAPSHOT trixie main"
    UPDATES_SOURCE=
    SECURITY_SOURCE=
fi

KEYRING_ARG=
if [ -r /usr/share/keyrings/debian-archive-keyring.gpg ]; then
    KEYRING_ARG=--keyring=/usr/share/keyrings/debian-archive-keyring.gpg
fi

cleanup() {
    rm -rf "$ROOTFS_TREE" "$ROOTFS_TAR" "$ROOTFS_TMP"
}
trap cleanup EXIT INT TERM
cleanup

# shellcheck disable=SC2086
if [ -n "$UPDATES_SOURCE" ]; then
    mmdebstrap \
        $KEYRING_ARG \
        --architectures=arm64 \
        --variant=important \
        --include="$PACKAGES" \
        --components=main \
        --aptopt='Acquire::Languages "none"' \
        trixie \
        "$ROOTFS_TREE" \
        "$PRIMARY_SOURCE" \
        "$UPDATES_SOURCE" \
        "$SECURITY_SOURCE"
else
    mmdebstrap \
        $KEYRING_ARG \
        --architectures=arm64 \
        --variant=important \
        --include="$PACKAGES" \
        --components=main \
        --aptopt='Acquire::Languages "none"' \
        trixie \
        "$ROOTFS_TREE" \
        "$PRIMARY_SOURCE"
fi

"$SCRIPT_DIR/configure-rootfs.sh" "$ROOTFS_TREE"
mkdir -p "$ROOTFS_TREE/dev" "$ROOTFS_TREE/proc" "$ROOTFS_TREE/sys"

tar --numeric-owner --hard-dereference --exclude='./dev/*' -C "$ROOTFS_TREE" -cf "$ROOTFS_TAR" .
zstd -19 -T0 -f "$ROOTFS_TAR" -o "$ROOTFS_TMP"

archive_size=$(wc -c < "$ROOTFS_TMP" | tr -d ' ')
[ "$archive_size" -gt 52428800 ] || fail "rootfs archive is unexpectedly small: $archive_size bytes"

mv "$ROOTFS_TMP" "$ROOTFS_OUT"
rm -rf "$ROOTFS_TREE" "$ROOTFS_TAR"
trap - EXIT INT TERM

(cd "$OUT_DIR" && sha256sum "$ROOTFS_NAME") > "$MANIFEST_DIR/$ROOTFS_NAME.sha256"
cat > "$MANIFEST_DIR/debian-trixie-arm64-rootfs.provenance" <<EOF
name=$ROOTFS_NAME
debian_release=13
debian_codename=trixie
architecture=arm64
build_date=$(date -u +%Y-%m-%dT%H:%M:%SZ)
primary_source=$PRIMARY_SOURCE
updates_source=$UPDATES_SOURCE
security_source=$SECURITY_SOURCE
packages=$PACKAGES
android_extractable=true
archive_excludes=./dev/*
archive_hardlinks=dereferenced
sha256=$(cut -d ' ' -f 1 "$MANIFEST_DIR/$ROOTFS_NAME.sha256")
size_bytes=$archive_size
EOF

printf 'Built %s\n' "$ROOTFS_OUT"
