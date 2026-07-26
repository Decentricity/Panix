# Test Report

Date: 2026-07-26

Current status: the local `0.1.0-alpha.2` line now boots the bundled Debian
13/XFCE desktop as the Android Home screen on the attached ARM64 phone. This is
real device evidence, not just APK inspection. The final `v0.1.0` tag/release is
still not published because the full acceptance suite has not been completed.

## Current Device Evidence

Device:

- `ab6b77a8`, model `CPH2499`, ARM64 Android target attached over adb.

Current test build:

- APK:
  `build/device-test/rebuild-x11-extra-key/Panix-arm64-v8a-alpha.2-extra-key-test-signed.apk`
- Version: `0.1.0-alpha.2`
- SHA-256:
  `dd77f38a73f513d33a14b707282da4763444ad35969804cd822c7196a8df8020`
- Test signing certificate SHA-256:
  `ddea70a805747e040fe393a8c8024b04fb9a0ed2be532c5b37bcdcbbcb0c9872`
- `apksigner verify --verbose --print-certs`: v2 and v3 signatures verified.

APK inspection:

- Package: `io.github.decentricity.panix`
- Launcher/Home activity: `com.termux.x11.PanixHomeActivity`
- Android Home category: present
- Fallback native Home activity: disabled in APK inspection
- Standalone Termux:X11 launcher: absent
- Native code: `arm64-v8a`
- Bundled rootfs: `assets/debian-trixie-arm64-rootfs.tar.zst`
- Bundled PRoot: `assets/termux-proot-aarch64.tar.zst`
- Embedded X11 library: `lib/arm64-v8a/libXlorie.so`
- `libXlorie.so` zip method: `Stored`, not deflated
- VNC/RDP files: absent from APK listing

Fresh first boot on device:

- Install command: `adb install --no-incremental`
- Home command:
  `cmd package set-home-activity io.github.decentricity.panix/com.termux.x11.PanixHomeActivity`
- Runtime state: `RUNNING`
- Screenshot:
  `docs/images/panix-extra-key-firstboot-xfce.png`
- Evidence folder:
  `build/device-test/rebuild-x11-extra-key/fresh-firstboot/`

Runtime log sequence:

```text
INSTALLING_PROOT: Installing bundled PRoot runtime.
VERIFYING_ASSET: Preparing bundled Debian rootfs asset.
EXTRACTING: Extracting Debian rootfs.
CONFIGURING: Configuring Debian rootfs.
READY: Debian rootfs is installed.
STARTING_X11: Starting embedded Termux:X11 server.
STARTING_DESKTOP: Starting Debian XFCE through bundled PRoot.
RUNNING: Panix desktop supervisor is running.
```

X11 log:

```text
Command: [/system/bin/app_process, -Xnoimage-dex2oat, /, --nice-name=panix-x11, com.termux.x11.CmdEntryPoint, :1]
TMPDIR=/data/user/0/io.github.decentricity.panix/files/debian/tmp
XKB_CONFIG_ROOT=/data/user/0/io.github.decentricity.panix/files/debian/usr/share/X11/xkb
Embedded Termux:X11 server is running.
```

Observed Panix process path:

```text
io.github.decentricity.panix
panix-x11
proot
xfce4-session
dbus-launch
dbus-daemon
xfce4-panel
```

Termux and VNC dependency evidence:

- Existing `com.termux`, `com.termux.api`, `com.termux.window`, and
  `com.iiordanov.freebVNC` packages on the phone were preserved.
- No `com.termux.x11` package was installed.
- Panix started `panix-x11` from its own APK through
  `CLASSPATH=context.getPackageCodePath()`.
- Panix started Debian through its bundled `proot` and bundled rootfs under
  `io.github.decentricity.panix` private app storage.
- The observed Panix process tree contains no VNC server/viewer process, and
  the APK listing contains no VNC/RDP payload files.

## Fixed Device Failures

| Failure | Cause | Fix |
| --- | --- | --- |
| Rootfs extraction failed with tar exit code 2 | Android could not extract special `/dev` nodes and some preserved ownership/mode metadata | Rebuilt the rootfs as Android-extractable and added runtime tar flags `--no-same-owner --no-same-permissions --delay-directory-restore`. |
| Reset/extraction cleanup failed under `/dev/fd` | Recursive delete followed symlinks | `deleteRecursively()` now uses `NOFOLLOW_LINKS`. |
| Sudoers rewrite failed with permission denied | Existing app-owned 0440 file could not be overwritten | Runtime file writer chmods existing app-owned read-only files before overwrite. |
| Embedded X11 exited with code 137 | `libXlorie.so` was deflated in the APK, but `CmdEntryPoint` loads it directly from the APK path | Release packaging keeps native libraries uncompressed and the APK inspector enforces `Stored`. |
| Embedded X11 exited during startup | XKB config root was not set for the embedded server | `PanixX11Bridge` sets `XKB_CONFIG_ROOT` to the bundled Debian rootfs XKB path. |
| XFCE desktop was tiny on the phone | X11 display defaults used an unscaled desktop | `PanixHomeActivity` now defaults to scaled resolution, `displayScale=200`, fullscreen, and keeps the extra-key bar visible. |
| Build script could print success after Gradle failed | Gradle output was piped through `tee`, so POSIX `sh` saw `tee` status | `scripts/build-panix.sh` now preserves Gradle failure status before printing `Built`. |

## Acceptance Matrix

| Requirement | Status | Evidence |
| --- | --- | --- |
| Build/sign current `0.1.0-alpha.2` line | Pass for local test-signed APK | Current APK above, signed and verified with v2/v3 signatures. |
| Install on attached ARM64 Android target | Pass | Installed on CPH2499 with `adb install --no-incremental`. |
| Panix opens from app icon | Not yet directly captured | Home launch is proven; launcher icon tap/monkey evidence still needed. |
| Android offers/selects Panix as Home app | Pass | `set-home-activity` succeeded and activity resumed from `android.intent.category.HOME`. |
| First boot requires no rootfs download | Pass | Runtime extracted `assets/debian-trixie-arm64-rootfs.tar.zst` from the APK. |
| Bundled rootfs verified/extracted | Pass | `VERIFYING_ASSET`, `EXTRACTING`, `CONFIGURING`, `READY`, then `RUNNING`. |
| Embedded X11 surface appears | Pass | Screenshot and `panix-x11` process/log evidence. |
| XFCE usable desktop appears | Pass for desktop appearance | Screenshot shows XFCE panel, desktop icons, Panix overlay, and extra-key bar. |
| Touch input works | Not yet captured | Needs deliberate tap evidence. |
| Soft keyboard / extra-key controls | Partial | Extra-key bar is visible by default; soft keyboard open action still needs direct evidence. |
| Physical keyboard | Not applicable yet | No physical keyboard evidence captured. |
| XFCE Terminal opens | Not yet captured | Required before final release. |
| `/etc/os-release` identifies Debian 13 Trixie | Not yet captured | Required before final release. |
| `apt update` works | Not yet captured | Required before final release. |
| Installing a small Debian package works | Not yet captured | Required before final release. |
| Repeated Home returns to same session without duplicate processes | Not yet captured | Required before final release. |
| Android app round trip returns to Panix | Not yet captured | Required before final release. |
| Killing/relaunching Panix recovers cleanly | Not yet captured | Required before final release. |
| Restart Desktop works | Not yet captured | Required before final release. |
| Reset Debian works | Partial | `pm clear` plus fresh first boot works; in-app Reset Debian still needs direct evidence. |
| No VNC server/viewer/TCP VNC dependency | Pass for APK/code/process evidence | APK has no VNC/RDP files and runtime process tree contains no VNC process. |
| No separate Termux:X11 APK required | Pass | No `com.termux.x11` package installed; `panix-x11` starts from Panix APK `CLASSPATH`. |
| Logs reveal actionable errors | Pass | Earlier device failures exposed tar, symlink, sudoers, X11 library, and XKB causes in logs. |
| Required screenshots/logs captured | Partial | XFCE/Home screenshots and logs captured; terminal/os-release and Android Settings/app-drawer screenshots still needed. |

## Release Decision

Do not tag or publish final `v0.1.0` yet. The main boot gate is now green, but
the terminal, Debian identity, APT, input, recovery, and session-resume checks
still need direct device evidence.
