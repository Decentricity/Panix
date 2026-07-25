# Panix

Turn an Android phone into a Debian graphical workstation and home launcher.

[Download Panix for ARM64 Android](https://github.com/Decentricity/Panix/releases/latest/download/Panix-arm64-v8a.apk)

Current status: source integration is in progress. No acceptance-tested v0.1.0
release APK has been published yet, so the download link is expected to work
only after the first GitHub Release is created.

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

Exact download size, installed size, extracted rootfs size, APK SHA-256, and
signing certificate fingerprint will be added after the first release artifact
is built and signed.

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
its command entry point before XFCE; the visual surface is still opened through a
same-APK Termux:X11 activity rather than being hosted directly inside Panix Home,
so the desktop is not acceptance-ready yet.

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
