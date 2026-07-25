#!/usr/bin/env sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
REPO_ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)

ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-/data/data/com.termux/files/home/android-tooling/android-sdk}"
ANDROID_HOME="${ANDROID_HOME:-$ANDROID_SDK_ROOT}"
JAVA_HOME="${JAVA_HOME:-/data/data/com.termux/files/usr/lib/jvm/java-21-openjdk}"
GRADLE_BIN="${GRADLE_BIN:-$REPO_ROOT/gradlew}"
AAPT2_OVERRIDE="${AAPT2_OVERRIDE:-}"
if [ -z "$AAPT2_OVERRIDE" ] && [ -x /data/data/com.termux/files/usr/bin/aapt2 ]; then
    AAPT2_OVERRIDE=/data/data/com.termux/files/usr/bin/aapt2
fi
ZIPALIGN="${ZIPALIGN:-/data/data/com.termux/files/usr/bin/zipalign}"
APKSIGNER="${APKSIGNER:-/data/data/com.termux/files/usr/bin/apksigner}"
PANIX_KEYSTORE_PROPERTIES="${PANIX_KEYSTORE_PROPERTIES:-/data/data/com.termux/files/home/.signing/panix-release.properties}"
PANIX_SIGN_RELEASE="${PANIX_SIGN_RELEASE:-1}"
PANIX_USE_EXTERNAL_NATIVE_BUILD="${PANIX_USE_EXTERNAL_NATIVE_BUILD:-0}"
BUILD_LOG_DIR="$REPO_ROOT/build/panix-logs"
ROOTFS_NAME="debian-trixie-arm64-rootfs.tar.zst"
ROOTFS_ASSET="$REPO_ROOT/app/src/main/assets/debian-trixie-arm64-rootfs.tar.zst"
ROOTFS_SHA_FILE="$REPO_ROOT/rootfs/manifests/debian-trixie-arm64-rootfs.tar.zst.sha256"

mkdir -p "$BUILD_LOG_DIR"

fail() {
    printf 'build-panix: %s\n' "$*" >&2
    exit 1
}

require_file() {
    [ -e "$1" ] || fail "missing $2: $1"
}

require_exec() {
    [ -x "$1" ] || fail "missing executable $2: $1"
}

require_exec "$GRADLE_BIN" "Gradle"
require_exec "$JAVA_HOME/bin/java" "Java"
require_file "$ANDROID_SDK_ROOT/platforms/android-36/android.jar" "Android SDK platform android-36"
if [ -n "$AAPT2_OVERRIDE" ]; then
    require_exec "$AAPT2_OVERRIDE" "aapt2 override"
fi
if [ "$PANIX_SIGN_RELEASE" = 1 ]; then
    require_exec "$ZIPALIGN" "zipalign"
    require_exec "$APKSIGNER" "apksigner"
    require_file "$PANIX_KEYSTORE_PROPERTIES" "Panix signing properties"
fi

if [ ! -e "$ROOTFS_ASSET" ] && [ -e "$REPO_ROOT/build/rootfs/$ROOTFS_NAME" ]; then
    mkdir -p "$(dirname "$ROOTFS_ASSET")"
    cp "$REPO_ROOT/build/rootfs/$ROOTFS_NAME" "$ROOTFS_ASSET"
fi

require_file "$ROOTFS_ASSET" "bundled Debian rootfs asset"
require_file "$ROOTFS_SHA_FILE" "bundled Debian rootfs checksum"

if [ "$PANIX_USE_EXTERNAL_NATIVE_BUILD" != 1 ]; then
    "$SCRIPT_DIR/build-bootstrap-lib.sh"
    "$SCRIPT_DIR/build-terminal-emulator-lib.sh"
    "$SCRIPT_DIR/build-shared-lib.sh"
fi

expected_rootfs_sha=$(cut -d ' ' -f 1 "$ROOTFS_SHA_FILE")
actual_rootfs_sha=$(sha256sum "$ROOTFS_ASSET" | cut -d ' ' -f 1)
if [ "$expected_rootfs_sha" != "$actual_rootfs_sha" ]; then
    fail "rootfs checksum verification failed"
fi

export ANDROID_HOME
export ANDROID_SDK_ROOT
export JAVA_HOME
export GRADLE_USER_HOME="${GRADLE_USER_HOME:-$HOME/.gradle}"
export PATH="$JAVA_HOME/bin:$ANDROID_SDK_ROOT/platform-tools:$PATH"

cd "$REPO_ROOT"

GRADLE_ARGS="--no-daemon clean :app:assembleRelease"
if [ -n "$AAPT2_OVERRIDE" ]; then
    GRADLE_ARGS="$GRADLE_ARGS -Pandroid.aapt2FromMavenOverride=$AAPT2_OVERRIDE"
fi

# shellcheck disable=SC2086
"$GRADLE_BIN" $GRADLE_ARGS 2>&1 | tee "$BUILD_LOG_DIR/assembleRelease.log"

APK="$REPO_ROOT/app/build/outputs/apk/release/Panix-arm64-v8a.apk"
require_file "$APK" "release APK"

if [ "$PANIX_SIGN_RELEASE" = 1 ]; then
    set -a
    . "$PANIX_KEYSTORE_PROPERTIES"
    set +a

    : "${PANIX_KEYSTORE:?missing PANIX_KEYSTORE in signing properties}"
    : "${PANIX_KEY_ALIAS:?missing PANIX_KEY_ALIAS in signing properties}"
    : "${PANIX_KEYSTORE_PASSWORD:?missing PANIX_KEYSTORE_PASSWORD in signing properties}"
    : "${PANIX_KEY_PASSWORD:?missing PANIX_KEY_PASSWORD in signing properties}"

    require_file "$PANIX_KEYSTORE" "Panix release keystore"

    ALIGNED_APK="$REPO_ROOT/app/build/outputs/apk/release/Panix-arm64-v8a-aligned.apk"
    SIGNED_APK="$REPO_ROOT/app/build/outputs/apk/release/Panix-arm64-v8a-signed.apk"

    "$ZIPALIGN" -f -p 4 "$APK" "$ALIGNED_APK"
    "$APKSIGNER" sign \
        --ks "$PANIX_KEYSTORE" \
        --ks-key-alias "$PANIX_KEY_ALIAS" \
        --ks-pass env:PANIX_KEYSTORE_PASSWORD \
        --key-pass env:PANIX_KEY_PASSWORD \
        --out "$SIGNED_APK" \
        "$ALIGNED_APK"
    "$APKSIGNER" verify --verbose "$SIGNED_APK"
    mv "$SIGNED_APK" "$APK"
    rm -f "$ALIGNED_APK"
fi

(cd "$(dirname "$APK")" && sha256sum "$(basename "$APK")") > "$APK.sha256"
printf 'Built %s\n' "$APK"
printf 'Checksum %s\n' "$APK.sha256"
