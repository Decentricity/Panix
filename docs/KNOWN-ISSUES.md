# Known Issues

- No v0.1.0 release APK has passed the acceptance test suite yet.
- The Panix Home activity currently provides a recovery shell and Android app
  escape routes; it does not yet embed the X11 surface.
- `PanixRuntimeService` and `PanixRuntimeManager` provide first-boot state,
  rootfs verification/extraction, reset, logs, and foreground-service plumbing,
  and a bundled PRoot/XFCE supervisor path, but desktop launch still cannot pass
  acceptance until embedded X11 is wired.
- `third_party/termux-x11` is vendored but still contains separate-package
  assumptions for `com.termux.x11`.
- Termux:X11 native startup still contains hard-coded `/data/data/com.termux`
  paths that must be replaced or parameterized.
- The Debian 13 Trixie ARM64 rootfs is built and bundled by CI, but there is no
  signed release artifact yet and the rootfs has not passed on-device first-boot
  extraction testing.
- PRoot is bundled from pinned Termux package payloads, but it has not passed an
  on-device Panix first-boot launch test yet.
- `scripts/start-desktop.sh` mirrors the Java launch path for debugging; the
  Android runtime manager starts PRoot directly.
- The phone build path cannot execute official Android NDK Linux x86_64 host
  binaries. The base app works around this for current JNI libraries by building
  ARM64 `.so` files with Termux `clang`, but embedded Termux:X11 native builds
  still need a CI or cross-build path.
- On-phone Gradle builds emit non-fatal `llvm-strip` warnings because the NDK
  strip binary is Linux x86_64. Packaged libraries are currently unstripped.
