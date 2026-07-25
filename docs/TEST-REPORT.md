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
| Embedded X11 desktop | Not implemented | X11 source is vendored but not embedded into Panix runtime. |
| Bundled Debian rootfs | Not implemented | Rootfs build scripts exist; no release asset is pinned. |

## Build Checks

| Check | Result | Evidence |
| --- | --- | --- |
| Shell script syntax | Pass | `sh -n scripts/*.sh rootfs/*.sh` completed. |
| Native bootstrap JNI build | Pass | `scripts/build-bootstrap-lib.sh` built `libtermux-bootstrap.so`. |
| Terminal emulator JNI build | Pass | `scripts/build-terminal-emulator-lib.sh` built `libtermux.so`. |
| Shared local-socket JNI build | Pass with warnings | `scripts/build-shared-lib.sh` built `liblocal-socket.so`; warnings are not fatal in the phone helper. |
| Debug APK assemble | Pass | `./gradlew :app:assembleDebug` produced `Panix-debug-arm64-v8a.apk` (36 MB). |
| Release APK assemble | Pass | `./gradlew :app:assembleRelease` produced `Panix-arm64-v8a.apk` (32 MB). |
| Release signing flow | Pass for development APK | `zipalign` and `apksigner` signed the current development `Panix-arm64-v8a.apk`; `apksigner verify` reports v2/v3 signatures with certificate SHA-256 `5f333b9bd88c24174cface1473ba361777a3e66f06f6731ed88fe01770c72a42`. |
| Release APK SHA-256 | Recorded for development APK | `ae5436e345bdd3bc895ca164f4ec50c7cade06214bfc8ac058341ea5eee43675`. |
| APK metadata | Pass | `aapt dump badging` reports package `io.github.decentricity.panix`, label `Panix`, min SDK 26, target SDK 28, native code `arm64-v8a`, and launcher `com.termux.app.PanixHomeActivity`. |
| HOME manifest entry | Pass | `aapt dump xmltree` shows `android.intent.category.HOME` and `android.intent.category.DEFAULT`. |
| Panix runtime service packaged | Pass | `aapt dump xmltree` shows `com.termux.app.PanixRuntimeService` registered and not exported. |
| Android runtime first-boot state machine | Compile-pass only | `PanixRuntimeManager` implements `NOT_INSTALLED`, `VERIFYING_ASSET`, `EXTRACTING`, `CONFIGURING`, `READY`, `STARTING_DESKTOP`, `RUNNING`, `STOPPING`, and `FAILED`; on-device extraction has not been run. |
| Rootfs checksum packaged as asset | CI path implemented | `scripts/build-panix.sh` and `.github/workflows/build.yml` copy `debian-trixie-arm64-rootfs.tar.zst.sha256` into APK assets beside the rootfs. |
| Direct device install | Blocked | `pm install` cannot read APKs from Termux private storage or shared FUSE paths; adb has no attached device. |
| Top-level build script | Blocked locally as intended | `./scripts/build-panix.sh` stops with `missing bundled Debian rootfs asset` until `debian-trixie-arm64-rootfs.tar.zst` is built and copied into `build/rootfs/` or `app/src/main/assets/`. |
| GitHub Actions workflow | Pass for unsigned CI artifact | The `Build Panix` workflow builds the Debian rootfs, assembles `Panix-arm64-v8a.apk`, verifies its checksum, and uploads the APK/checksum/manifests/logs artifact on `master`. |
| GitHub Actions unit tests | Pass | The `Unit tests` workflow installs SDK platform `android-36` and runs `./gradlew test` on `master`. |
| GitHub Actions wrapper validation | Pass | The `Validate Gradle Wrapper` workflow runs on `master`. |
| Local phone unit tests | Fails in Robolectric harness | `./gradlew test` fails in existing `FileReceiverActivityTest` cleanup with `ShadowActivityThread.reset: ActivityThread not set`; this is not a Panix runtime assertion failure. |

## Required Acceptance Tests

The full v0.1.0 acceptance list from the product objective remains open.
