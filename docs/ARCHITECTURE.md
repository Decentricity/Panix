# Panix Architecture

Panix is a Termux-derived Android application that is being turned into a
single-APK Debian graphical workstation and Home launcher.

Current repository state:

- Termux app history is preserved as the base repository.
- Termux:X11 source is vendored under `third_party/termux-x11` using a Git subtree.
- The Android package id is `io.github.decentricity.panix`.
- Java/Kotlin package namespaces remain `com.termux` for now to reduce refactor risk.
- A Panix Home activity is present and offers recovery actions.
- `PanixRuntimeService` is registered as Panix's foreground runtime boundary.
- `PanixRuntimeManager` persists first-boot/session state and can copy, verify,
  extract, configure, reset, and log the bundled Debian rootfs transaction.

Target runtime path:

```text
Debian GUI application
  -> X11 protocol
  -> local Unix socket shared through Panix tmp
  -> embedded Termux:X11 server
  -> Android native Surface in PanixHomeActivity
```

Major remaining implementation boundaries:

- Build Termux:X11 `lorie` as an embedded Panix module rather than a separate APK.
- Replace `com.termux.x11` package assumptions in loader, broadcasts, and native code.
- Replace `/data/data/com.termux` native path assumptions with Panix paths.
- Bundle PRoot and its Termux shared-library dependencies into Panix builds.
- Connect `PanixRuntimeManager.startDesktop()` to embedded X11, PRoot, D-Bus,
  and XFCE process supervision.
- Bundle and verify `debian-trixie-arm64-rootfs.tar.zst` as a release APK asset.
- Run XFCE as user `panix` through PRoot with shared `/tmp` for the X11 socket.
