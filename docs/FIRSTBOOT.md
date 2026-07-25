# First Boot

Panix first boot must be transactional.

Target states:

- `NOT_INSTALLED`
- `VERIFYING_ASSET`
- `EXTRACTING`
- `CONFIGURING`
- `READY`
- `FAILED`

The shell scaffold in `scripts/firstboot.sh` records these states in
`$PANIX_FILES_DIR/panix-state/firstboot.state`. The production Android runtime
still needs to wrap this behavior in a foreground-service-aware Java/Kotlin
state machine with progress events for the native UI.

Required invariant:

An existing healthy rootfs must never be destroyed because a new extraction
failed. Extraction must happen into a staging directory and move into place only
after verification and health checks pass.
