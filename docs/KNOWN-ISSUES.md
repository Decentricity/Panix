# Known Issues

- No v0.1.0 release APK has passed the acceptance test suite yet.
- The Panix Home activity currently provides a recovery shell and Android app
  escape routes; it does not yet embed the X11 surface.
- `third_party/termux-x11` is vendored but still contains separate-package
  assumptions for `com.termux.x11`.
- Termux:X11 native startup still contains hard-coded `/data/data/com.termux`
  paths that must be replaced or parameterized.
- The Debian 13 Trixie ARM64 rootfs asset is not yet built, pinned, or bundled.
- `scripts/start-desktop.sh` is a bootstrap scaffold and is not yet called by an
  Android runtime manager.
- The phone build path cannot execute official Android NDK Linux x86_64 host
  binaries. The base app works around this for current JNI libraries by building
  ARM64 `.so` files with Termux `clang`, but embedded Termux:X11 native builds
  still need a CI or cross-build path.
- On-phone Gradle builds emit non-fatal `llvm-strip` warnings because the NDK
  strip binary is Linux x86_64. Packaged libraries are currently unstripped.
