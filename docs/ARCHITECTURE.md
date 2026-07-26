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
- The build produces a pinned Termux PRoot payload from verified `.deb` files,
  and first boot installs it under Panix's private `files/usr` prefix.
- `PANIX_INCLUDE_X11_MODULE=1` includes the vendored Termux:X11 `lorie` Android
  library and its shell-loader stub in the Panix APK.
- X11-enabled builds enable `com.termux.x11.PanixHomeActivity` as the launcher
  and HOME activity, disable the fallback native dashboard HOME activity, and
  suppress Termux:X11's standalone launcher entry.
- `com.termux.x11.PanixHomeActivity` subclasses the vendored Termux:X11
  `MainActivity`, preserving its `LorieView` surface, input, resize, clipboard,
  and binder connection path while adding a Panix emergency menu overlay and
  runtime status on the startup screen.
- `PanixX11Bridge` starts `com.termux.x11.CmdEntryPoint` through Android
  `app_process` with `CLASSPATH` pointed at the Panix APK and `TMPDIR` pointed at
  Panix's private shared tmp directory.
- `PanixX11Bridge` sets `XKB_CONFIG_ROOT` to the bundled Debian rootfs XKB
  directory so the embedded X11 server does not depend on Termux paths.
- Release packaging stores `lib/arm64-v8a/libXlorie.so` uncompressed because
  the embedded X11 command entry point loads that library directly from the APK
  path.
- `com.termux.x11.PanixHomeActivity` applies phone-friendly X11 defaults:
  scaled resolution, `displayScale=200`, fullscreen, and visible extra-key bar.

Target runtime path:

```text
Debian GUI application
  -> X11 protocol
  -> local Unix socket shared through Panix tmp
  -> embedded Termux:X11 server
  -> Android native Surface in PanixHomeActivity
```

Device evidence:

- On 2026-07-26, the local `0.1.0-alpha.2` test build booted the bundled
  Debian/XFCE desktop as Android Home on a CPH2499 ARM64 phone. The runtime
  reached `RUNNING` with `io.github.decentricity.panix`, `panix-x11`, bundled
  `proot`, and `xfce4-session` processes. See `docs/TEST-REPORT.md`.

Major remaining implementation boundaries:

- Complete the remaining device acceptance evidence for terminal, Debian
  identity, APT, input, recovery, and repeated Home/session behavior.
- Replace `com.termux.x11` package assumptions in loader, broadcasts, and native
  code where they conflict with Panix package identity.
- Replace `/data/data/com.termux` native path assumptions with Panix paths where
  runtime environment overrides are not enough.
