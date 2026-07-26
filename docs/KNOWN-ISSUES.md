# Known Issues

- No final `v0.1.0` release APK has passed the complete acceptance suite yet.
- The main on-device boot gate now passes for the local `0.1.0-alpha.2` test
  build: Panix installs on the attached ARM64 phone, can be selected as Home,
  verifies/extracts the bundled Debian rootfs, starts embedded X11, and reaches
  XFCE with runtime state `RUNNING`.
- Remaining release blockers are evidence gaps, not the earlier black-screen
  boot failure: XFCE Terminal, `/etc/os-release`, `apt update`, small package
  install, direct touch/keyboard checks, repeated Home behavior, Android app
  round trip, in-app Restart Desktop, in-app Reset Debian, and recovery after
  killing/relaunching Panix still need device proof.
- Termux must not be removed from the test phone. Panix independence should be
  proven from APK contents, code paths, packages, and processes. Current
  evidence shows Panix uses its own package, bundled PRoot/rootfs, and embedded
  X11; it does not require the installed Termux app, a separate Termux:X11 APK,
  or VNC.
- `third_party/termux-x11` is vendored as an optional module and still contains
  upstream `com.termux.x11` namespace assumptions. The current Panix build works
  by embedding those classes inside the Panix APK and starting
  `CmdEntryPoint` with `CLASSPATH` pointed at Panix's own `base.apk`.
- Some native Termux:X11 code still has upstream path assumptions. Current
  runtime overrides provide the working `TMPDIR` and `XKB_CONFIG_ROOT`, but this
  area should remain part of release regression testing.
- On-phone Gradle builds cannot execute official Android SDK/NDK Linux x86_64
  host binaries. Use a Linux host or CI for X11-enabled release builds.
- Host lint currently reports existing PendingIntent mutability warnings in
  `TermuxService`. They are not blocking the current target SDK, but they should
  be addressed before raising target SDK.
- Gradle with JDK 21 emits Java 8 source/target deprecation warnings. The build
  still completes with the current toolchain.
