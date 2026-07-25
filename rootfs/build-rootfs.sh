#!/usr/bin/env sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
REPO_ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
OUT_DIR="${OUT_DIR:-$REPO_ROOT/build/rootfs}"
MANIFEST_DIR="$REPO_ROOT/rootfs/manifests"
ROOTFS_NAME="debian-trixie-arm64-rootfs.tar.zst"
ROOTFS_OUT="$OUT_DIR/$ROOTFS_NAME"
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

mkdir -p "$OUT_DIR" "$MANIFEST_DIR"

MIRROR="http://deb.debian.org/debian"
SECURITY_MIRROR="http://security.debian.org/debian-security"

if [ -n "$APT_SNAPSHOT" ]; then
    MIRROR="$APT_SNAPSHOT"
fi

mmdebstrap \
    --architectures=arm64 \
    --variant=important \
    --include="$PACKAGES" \
    --components=main \
    --aptopt='Acquire::Languages "none"' \
    trixie \
    - \
    "$MIRROR" \
    "$SECURITY_MIRROR" |
    zstd -19 -T0 > "$ROOTFS_OUT"

(cd "$OUT_DIR" && sha256sum "$ROOTFS_NAME") > "$MANIFEST_DIR/$ROOTFS_NAME.sha256"
cat > "$MANIFEST_DIR/debian-trixie-arm64-rootfs.provenance" <<EOF
name=$ROOTFS_NAME
debian_release=13
debian_codename=trixie
architecture=arm64
build_date=$(date -u +%Y-%m-%dT%H:%M:%SZ)
mirror=$MIRROR
security_mirror=$SECURITY_MIRROR
packages=$PACKAGES
sha256=$(cut -d ' ' -f 1 "$MANIFEST_DIR/$ROOTFS_NAME.sha256")
EOF

printf 'Built %s\n' "$ROOTFS_OUT"
