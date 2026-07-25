# First Boot

Panix first boot must be transactional.

Target states:

- `NOT_INSTALLED`
- `VERIFYING_ASSET`
- `EXTRACTING`
- `CONFIGURING`
- `READY`
- `STARTING_DESKTOP`
- `RUNNING`
- `STOPPING`
- `FAILED`

`PanixRuntimeManager` now persists these states under
`$PANIX_FILES_DIR/panix-state/`, and `PanixRuntimeService` runs the state
machine from a foreground service. The Home activity starts the service,
polls status, and exposes runtime controls.

Current implemented first-boot behavior:

- Installs the embedded Termux bootstrap into Panix's private `files/usr` path
  when needed so bundled `bash`, `tar`, and `zstd` are available.
- Copies `debian-trixie-arm64-rootfs.tar.zst` and its `.sha256` file from APK
  assets into Panix private storage.
- Verifies the rootfs SHA-256 before extraction.
- Checks free private storage before extraction.
- Extracts into `debian.staging`.
- Ensures `/home/panix`, `tmp`, `passwd`, `group`, sudoers, resolver config,
  and a Panix rootfs version marker exist.
- Moves the staging rootfs into `debian` only after health checks pass.
- Leaves an existing healthy rootfs in place if a new extraction fails.

Current missing first-boot behavior:

- The release APK still needs a fully wired embedded Termux:X11 surface.
- PRoot and its required Termux shared libraries still need to be bundled into
  the APK or bootstrap path.
- Desktop launch and supervision are deliberately reported as failed after the
  rootfs reaches `READY` until X11 and PRoot integration are complete.

Required invariant:

An existing healthy rootfs must never be destroyed because a new extraction
failed. Extraction must happen into a staging directory and move into place only
after verification and health checks pass.
