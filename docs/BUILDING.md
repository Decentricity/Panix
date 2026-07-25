# Building Panix

The intended top-level build command is:

```sh
./scripts/build-panix.sh
```

For CI builds without the private release key:

```sh
PANIX_USE_EXTERNAL_NATIVE_BUILD=1 PANIX_SIGN_RELEASE=0 ./scripts/build-panix.sh
```

The script currently verifies:

- Java.
- Android SDK platform `android-36`.
- Termux `aapt2` override.
- Termux-native `zipalign` and `apksigner`.
- Private signing properties at
  `/data/data/com.termux/files/home/.signing/panix-release.properties`.
- The bundled Debian rootfs asset and checksum.
- The bundled rootfs checksum is copied into APK assets beside the rootfs so
  Android first boot can verify the asset.
- A pinned Termux PRoot payload built from verified `proot`,
  `libandroid-shmem`, and `libtalloc` package files.
- The bundled PRoot payload checksum is copied into APK assets beside the
  payload so Android first boot can verify it.
- The expected release APK filename, `Panix-arm64-v8a.apk`.

Current local blocker:

- The Debian rootfs asset is built and bundled by GitHub Actions. Local phone
  release builds still need the rootfs asset under `build/rootfs/` or
  `app/src/main/assets/` before `./scripts/build-panix.sh` can package it. The
  PRoot payload is small enough for `./scripts/build-panix.sh` to build locally
  when missing.
- Official SDK/NDK host tools are Linux x86_64, so Panix's on-phone build path
  generates ARM64 JNI libraries with Termux `clang`/`clang++` and packages them
  from `jniLibs`. Conventional CI hosts can opt back into upstream `ndk-build`
  with `PANIX_USE_EXTERNAL_NATIVE_BUILD=1`.

Release signing must use a dedicated Panix keystore outside the repository.
Do not commit keystores, passwords, or signing properties.

Current Panix release keystore:

- Path: `/data/data/com.termux/files/home/.signing/panix-release.jks`
- Certificate SHA-256: `5F:33:3B:9B:D8:8C:24:17:4C:FA:CE:14:73:BA:36:17:77:A3:E6:6F:06:F6:73:1E:D8:8F:E0:17:70:C7:2A:42`

GitHub Actions workflow:

- `.github/workflows/build.yml` installs SDK 36 and NDK 29.
- It builds the Debian rootfs on `ubuntu-latest`.
- It builds the pinned Termux PRoot payload.
- It builds an unsigned ARM64 CI APK with `PANIX_SIGN_RELEASE=0`.
- It uploads the APK, SHA-256 file, rootfs manifests, and build logs.
