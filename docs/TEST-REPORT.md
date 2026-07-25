# Test Report

Date: 2026-07-25

No full acceptance tests have passed yet. This file intentionally does not claim
a working Panix desktop until the APK is built, installed, and exercised
on-device with the bundled rootfs and embedded X11 runtime.

## Current Checks

| Check | Result | Evidence |
| --- | --- | --- |
| GitHub repo exists | Pass | `https://github.com/Decentricity/Panix` created and pushed. |
| Termux app provenance recorded | Pass | `UPSTREAMS.md` records upstream SHAs and retrieval timestamp. |
| Termux:X11 source in normal clone | Pass | `third_party/termux-x11` imported as a Git subtree. |
| Panix app id configured | Pass | `aapt dump badging` reports package `io.github.decentricity.panix`. |
| Panix offered as Home app | Pending device install | Manifest contains `CATEGORY_HOME` and `CATEGORY_DEFAULT`. |
| Embedded X11 desktop | Partial | `PANIX_INCLUDE_X11_MODULE=1` packages the vendored Termux:X11 `lorie` module and `PanixX11Bridge` starts `com.termux.x11.CmdEntryPoint` before XFCE; CI confirms `libXlorie.so` is packaged, but the surface is still the same-APK Termux:X11 activity, not a Panix Home embedded view. |
| Bundled Debian rootfs | Pass for packaging | GitHub Actions builds and packages `debian-trixie-arm64-rootfs.tar.zst` and its checksum into the X11-enabled CI APK. |

## Build Checks

| Check | Result | Evidence |
| --- | --- | --- |
| Shell script syntax | Pass | `sh -n scripts/*.sh rootfs/*.sh` completed. |
| Native bootstrap JNI build | Pass | `scripts/build-bootstrap-lib.sh` built `libtermux-bootstrap.so`. |
| Terminal emulator JNI build | Pass | `scripts/build-terminal-emulator-lib.sh` built `libtermux.so`. |
| Shared local-socket JNI build | Pass with warnings | `scripts/build-shared-lib.sh` built `liblocal-socket.so`; warnings are not fatal in the phone helper. |
| Debug APK assemble | Pass | `./gradlew :app:assembleDebug` produced `Panix-debug-arm64-v8a.apk` (36 MB). |
| Release APK assemble | Pass via CI | `Build Panix` run `30145730437` assembled the X11-enabled ARM64 APK with Debian rootfs and PRoot assets bundled. |
| Release signing flow | Pass for alpha APK | The CI artifact was zipaligned and signed locally with the Panix release keystore; `apksigner verify --verbose --print-certs` reports v2/v3 signatures with certificate SHA-256 `5f333b9bd88c24174cface1473ba361777a3e66f06f6731ed88fe01770c72a42`. |
| Signed release APK size | Recorded for alpha APK | The signed APK is 254,069,497 bytes; the unsigned CI artifact is 253,971,502 bytes before zipalign/signing. |
| Release APK SHA-256 | Recorded for alpha APK | `527bc8c5afd90c8d83786943b726178b4fae5a2d242264d5f222192ec3188fa8`. |
| APK metadata | Pass | `aapt dump badging` reports package `io.github.decentricity.panix`, label `Panix`, min SDK 26, target SDK 28, native code `arm64-v8a`, and launcher `com.termux.app.PanixHomeActivity`. |
| HOME manifest entry | Pass | `aapt dump xmltree` shows `android.intent.category.HOME` and `android.intent.category.DEFAULT`. |
| Panix runtime service packaged | Pass | `aapt dump xmltree` shows `com.termux.app.PanixRuntimeService` registered and not exported. |
| Android runtime first-boot state machine | Compile-pass only | `PanixRuntimeManager` implements `NOT_INSTALLED`, `VERIFYING_ASSET`, `INSTALLING_PROOT`, `EXTRACTING`, `CONFIGURING`, `READY`, `STARTING_X11`, `STARTING_DESKTOP`, `RUNNING`, `STOPPING`, and `FAILED`; on-device extraction has not been run. |
| Rootfs checksum packaged as asset | CI path implemented | `scripts/build-panix.sh` and `.github/workflows/build.yml` copy `debian-trixie-arm64-rootfs.tar.zst.sha256` into APK assets beside the rootfs. |
| PRoot payload builder | Pass | `./scripts/build-proot-payload.sh` downloaded pinned Termux `proot`, `libandroid-shmem`, and `libtalloc` packages, verified SHA-256s, and produced deterministic payload SHA-256 `42f6e757d2cd5e7bf2ad49007b7f64cbe54f59f327e29bffe2aa7d77b48d6000` (111 KB). |
| PRoot checksum packaged as asset | CI path implemented | `scripts/build-panix.sh` and `.github/workflows/build.yml` copy `termux-proot-aarch64.tar.zst.sha256` into APK assets beside the PRoot payload. |
| PRoot payload relocation smoke test | Pass | Extracting the payload under `build/proot-smoke.*/files/usr` and running `proot --version` with `LD_LIBRARY_PATH`, `PROOT_LOADER`, and `PROOT_TMP_DIR` overrides reported `5.1.107.86`. |
| Runtime PRoot launch path | Compile-pass only | `:app:compileDebugJavaWithJavac` passed with the Termux-native `aapt2` override after adding bundled PRoot extraction, X11 startup state, and XFCE supervisor launch path. |
| Default no-X11 local compile | Pass | `ANDROID_HOME=/data/data/com.termux/files/home/android-tooling/android-sdk ./gradlew --no-daemon -Pandroid.aapt2FromMavenOverride=/data/data/com.termux/files/usr/bin/aapt2 :app:compileDebugJavaWithJavac` completed on the phone. |
| X11-enabled local phone compile | Blocked by host tools | `PANIX_INCLUDE_X11_MODULE=1 :lorie:compileDebugAidl` reaches the vendored `:lorie` module but cannot run the official Linux x86_64 SDK `aidl` binary under Termux on Android. |
| X11 native source submodules | CI path implemented | Root `.gitmodules` maps Termux:X11 native gitlinks to their upstream URLs, `.github/workflows/build.yml` checks them out recursively, and `scripts/build-panix.sh` preflights representative native source files when `PANIX_INCLUDE_X11_MODULE=1`. |
| Direct device install | Blocked | `pm install` cannot read APKs from Termux private storage or shared FUSE paths; adb has no attached device. |
| Debug APK with PRoot assets | Pass | `:app:assembleDebug` produced `Panix-debug-arm64-v8a.apk` with SHA-256 `956ffbe3116449693d88f3fa8d0dccb898cded65d1504a4a3b653e608b3dd258`; `zipinfo` shows `assets/termux-proot-aarch64.tar.zst` and `.sha256`. |
| Top-level build script | Blocked locally as intended | `PANIX_SIGN_RELEASE=0 ./scripts/build-panix.sh` builds/copies the PRoot payload and then stops with `missing bundled Debian rootfs asset` until `debian-trixie-arm64-rootfs.tar.zst` is built and copied into `build/rootfs/` or `app/src/main/assets/`. |
| GitHub Actions workflow | Pass for X11-enabled unsigned CI artifact | The `Build Panix` workflow run `30145730437` builds the Debian rootfs, checks out Termux:X11 native submodules, assembles `Panix-arm64-v8a.apk`, verifies its checksum, and uploads the APK/checksum/manifests/logs artifact on `master`. |
| GitHub Actions unit tests | Pass | The `Unit tests` workflow installs SDK platform `android-36` and runs `./gradlew test` on `master`. |
| GitHub Actions wrapper validation | Pass | The `Validate Gradle Wrapper` workflow runs on `master`. |
| GitHub alpha prerelease | Pass | `panix-v0.1.0-alpha.1` is published at `https://github.com/Decentricity/Panix/releases/tag/panix-v0.1.0-alpha.1` with the signed APK, checksum, signing verification report, and rootfs/PRoot provenance assets. |
| Signed alpha APK in shared storage | Pass | `/storage/emulated/0/Download/Panix/Panix-arm64-v8a.apk` matches the recorded SHA-256. |
| Local phone unit tests | Fails in Robolectric harness | `./gradlew test` fails in existing `FileReceiverActivityTest` cleanup with `ShadowActivityThread.reset: ActivityThread not set`; this is not a Panix runtime assertion failure. |

## Required Acceptance Tests

The full v0.1.0 acceptance list from the product objective remains open.
