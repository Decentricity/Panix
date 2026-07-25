# Panix

Turn an Android phone into a Debian graphical workstation and home launcher.

[Download Panix alpha for ARM64 Android](https://github.com/Decentricity/Panix/releases/download/panix-v0.1.0-alpha.1/Panix-arm64-v8a.apk)

Current status: source integration is in progress. An alpha APK has been built
and signed, but no acceptance-tested final v0.1.0 release has passed on-device
first-boot testing yet.

For the remaining execution checklist and workstation handoff, see
[`AGENTS.md`](AGENTS.md).

## What Panix Is

Panix is an experimental, independent Termux-derived Android app. The goal is a
single APK that bundles a Debian 13 Trixie ARM64 rootfs, embeds Termux:X11, and
can be selected as the Android Home app so XFCE appears as the phone home screen.

Panix is not affiliated with, endorsed by, or released by the Termux project.

## Requirements

- Android 8.0 or newer.
- ARM64 device.
- Sideloading enabled by the user.
- Enough free storage for the APK, compressed rootfs, and extracted Debian tree.

Current alpha artifact details:

- Signed APK size: 254,069,497 bytes.
- Bundled Debian rootfs size: 219,263,824 bytes compressed.
- Bundled PRoot payload size: 113,366 bytes compressed.
- APK SHA-256:
  `527bc8c5afd90c8d83786943b726178b4fae5a2d242264d5f222192ec3188fa8`.
- Signing certificate SHA-256:
  `5F:33:3B:9B:D8:8C:24:17:4C:FA:CE:14:73:BA:36:17:77:A3:E6:6F:06:F6:73:1E:D8:8F:E0:17:70:C7:2A:42`.

## Install

1. Install `Panix-arm64-v8a.apk`.
2. Open Panix from the app icon.
3. Use Android's Home app chooser when prompted, or tap `Choose Home App`.
4. During the finished first-boot flow, Panix will verify and extract the
   bundled Debian rootfs, start embedded X11, then start XFCE.

The current development build opens a native Panix Home shell with a foreground
runtime service. It can start the transactional first-boot path, install the
bundled PRoot runtime, verify/extract the bundled rootfs, and attempt the XFCE
supervisor. X11-enabled CI builds package Termux:X11 into the same APK and start
its command entry point before XFCE. Current source routes launcher/Home intents
to a Panix-named X11 home activity that subclasses the vendored Termux:X11
surface and overlays the Panix emergency menu. CI builds now package that X11
Home surface, but it still needs on-device first-boot testing before it can be
treated as acceptance-ready.

## Recovery

The Panix Home shell includes:

- Start Runtime.
- Restart Desktop.
- Stop Desktop.
- Reset Debian.
- Open X11 Surface.
- Open Panix Logs.
- Open Panix Terminal.
- Open Android Apps.
- Open Android Settings.
- Choose Home App.

These controls are present so a broken launcher build does not trap the user.

## Debian And APT

The release target is Debian 13 Trixie with ordinary Debian APT sources for
Trixie, Trixie updates, and Debian security. Normal commands such as `apt
update`, `apt install git`, `python3`, and `gcc` must work inside the Debian
environment after v0.1.0 acceptance tests pass.

The rootfs will run under unprivileged PRoot, not root, a VM, or a container
with its own kernel. PRoot has compatibility limits around privileged operations,
kernel features, daemons, and filesystem semantics.

## Build

```sh
./scripts/build-panix.sh
```

See `docs/BUILDING.md` for current toolchain requirements and blockers.

## Documentation

- `AGENTS.md`
- `UPSTREAMS.md`
- `THIRD_PARTY_NOTICES.md`
- `docs/ARCHITECTURE.md`
- `docs/BUILDING.md`
- `docs/FIRSTBOOT.md`
- `docs/KNOWN-ISSUES.md`
- `docs/TEST-REPORT.md`

## License

Panix is GPL-compatible and retains upstream Termux and Termux:X11 notices.
Complete corresponding source must be available for every release APK.
