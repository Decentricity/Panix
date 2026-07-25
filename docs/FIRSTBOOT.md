# First Boot

Panix first boot must be transactional.

Target states:

- `NOT_INSTALLED`
- `VERIFYING_ASSET`
- `INSTALLING_PROOT`
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
- Copies, verifies, and extracts the pinned `termux-proot-aarch64.tar.zst`
  payload into Panix's private prefix.
- Copies `debian-trixie-arm64-rootfs.tar.zst` and its `.sha256` file from APK
  assets into Panix private storage.
- Verifies the rootfs SHA-256 before extraction.
- Checks free private storage before extraction.
- Extracts into `debian.staging`.
- Ensures `/home/panix`, `tmp`, `passwd`, `group`, sudoers, resolver config,
  and a Panix rootfs version marker exist.
- Moves the staging rootfs into `debian` only after health checks pass.
- Leaves an existing healthy rootfs in place if a new extraction fails.
- Starts the XFCE supervisor through bundled PRoot with `PROOT_LOADER`,
  `PROOT_TMP_DIR`, and `LD_LIBRARY_PATH` pointed at the Panix private prefix.

Current missing first-boot behavior:

- The release APK still needs a fully wired embedded Termux:X11 surface.
- Desktop launch cannot pass acceptance until an embedded X server is started
  and its surface is hosted by Panix Home.

Required invariant:

An existing healthy rootfs must never be destroyed because a new extraction
failed. Extraction must happen into a staging directory and move into place only
after verification and health checks pass.
