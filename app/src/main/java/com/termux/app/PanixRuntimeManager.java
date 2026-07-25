package com.termux.app;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.StatFs;
import android.system.Os;

import com.termux.shared.termux.TermuxConstants;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;

final class PanixRuntimeManager {

    static final String STATE_NOT_INSTALLED = "NOT_INSTALLED";
    static final String STATE_VERIFYING_ASSET = "VERIFYING_ASSET";
    static final String STATE_EXTRACTING = "EXTRACTING";
    static final String STATE_CONFIGURING = "CONFIGURING";
    static final String STATE_READY = "READY";
    static final String STATE_STARTING_DESKTOP = "STARTING_DESKTOP";
    static final String STATE_RUNNING = "RUNNING";
    static final String STATE_STOPPING = "STOPPING";
    static final String STATE_FAILED = "FAILED";

    private static final String ROOTFS_NAME = "debian-trixie-arm64-rootfs.tar.zst";
    private static final String ROOTFS_SHA_NAME = ROOTFS_NAME + ".sha256";
    private static final String VERSION_MARKER = ".panix-rootfs";
    private static final long MIN_FREE_BYTES_BEFORE_EXTRACTION = 1536L * 1024L * 1024L;
    private static final Object LOCK = new Object();

    private static boolean sWorkerRunning;
    private static Process sDesktopProcess;

    private PanixRuntimeManager() {}

    static void startAsync(Context context) {
        Context appContext = context.getApplicationContext();
        synchronized (LOCK) {
            if (sWorkerRunning) {
                return;
            }
            sWorkerRunning = true;
        }

        Thread worker = new Thread(() -> {
            try {
                ensureInstalled(appContext);
                startDesktop(appContext);
            } catch (Exception e) {
                setState(appContext, STATE_FAILED, e.getMessage() == null ? e.toString() : e.getMessage());
                appendLog(appContext, "runtime", stackTrace(e));
            } finally {
                synchronized (LOCK) {
                    sWorkerRunning = false;
                }
            }
        }, "panix-runtime-start");
        worker.start();
    }

    static void restartDesktopAsync(Context context) {
        Context appContext = context.getApplicationContext();
        synchronized (LOCK) {
            if (sWorkerRunning) {
                return;
            }
            sWorkerRunning = true;
        }

        Thread worker = new Thread(() -> {
            try {
                stopDesktop(appContext);
                startDesktop(appContext);
            } catch (Exception e) {
                setState(appContext, STATE_FAILED, e.getMessage() == null ? e.toString() : e.getMessage());
                appendLog(appContext, "runtime", stackTrace(e));
            } finally {
                synchronized (LOCK) {
                    sWorkerRunning = false;
                }
            }
        }, "panix-runtime-restart");
        worker.start();
    }

    static void stopDesktop(Context context) {
        Context appContext = context.getApplicationContext();
        setState(appContext, STATE_STOPPING, "Stopping Panix desktop supervisor.");
        synchronized (LOCK) {
            if (sDesktopProcess != null) {
                sDesktopProcess.destroy();
                sDesktopProcess = null;
            }
        }
        deleteFile(lockFile(appContext));
        setState(appContext, isRootfsInstalled(appContext) ? STATE_READY : STATE_NOT_INSTALLED, "Panix desktop is stopped.");
    }

    static void resetDebianAsync(Context context) {
        Context appContext = context.getApplicationContext();
        synchronized (LOCK) {
            if (sWorkerRunning) {
                return;
            }
            sWorkerRunning = true;
        }

        Thread worker = new Thread(() -> {
            try {
                stopDesktop(appContext);
                setState(appContext, STATE_CONFIGURING, "Resetting Debian rootfs.");
                deleteRecursively(stagingDir(appContext));
                deleteRecursively(rootfsDir(appContext));
                setState(appContext, STATE_NOT_INSTALLED, "Debian reset complete. The export directory was preserved.");
            } catch (Exception e) {
                setState(appContext, STATE_FAILED, e.getMessage() == null ? e.toString() : e.getMessage());
                appendLog(appContext, "runtime", stackTrace(e));
            } finally {
                synchronized (LOCK) {
                    sWorkerRunning = false;
                }
            }
        }, "panix-runtime-reset");
        worker.start();
    }

    static RuntimeStatus getStatus(Context context) {
        String state = readFile(stateFile(context)).trim();
        String detail = readFile(detailFile(context)).trim();
        if (state.isEmpty()) {
            state = isRootfsInstalled(context) ? STATE_READY : STATE_NOT_INSTALLED;
        }
        if (detail.isEmpty()) {
            detail = defaultDetailForState(state);
        }
        return new RuntimeStatus(state, detail, sWorkerRunning);
    }

    static String readRecentLogs(Context context) {
        StringBuilder result = new StringBuilder();
        appendFileTail(result, new File(logDir(context), "runtime.log"), 12000);
        appendFileTail(result, new File(logDir(context), "firstboot.log"), 20000);
        appendFileTail(result, new File(logDir(context), "desktop.log"), 12000);
        if (result.length() == 0) {
            return "No Panix logs have been written yet.";
        }
        return result.toString();
    }

    private static void ensureInstalled(Context context) throws Exception {
        if (isRootfsInstalled(context)) {
            setState(context, STATE_READY, "Debian rootfs is installed.");
            return;
        }

        setState(context, STATE_VERIFYING_ASSET, "Preparing bundled Debian rootfs asset.");
        ensureDirectories(context);
        TermuxInstaller.setupBootstrapForPanixRuntime(context);
        File asset = copyRequiredAsset(context, ROOTFS_NAME);
        File checksumFile = copyRequiredAsset(context, ROOTFS_SHA_NAME);
        String expectedSha = readExpectedSha(checksumFile);
        String actualSha = sha256(asset);
        if (!expectedSha.equals(actualSha)) {
            throw new IOException("Bundled rootfs checksum mismatch: expected " + expectedSha + ", actual " + actualSha);
        }

        long availableBytes = new StatFs(filesDir(context).getAbsolutePath()).getAvailableBytes();
        long requiredBytes = Math.max(MIN_FREE_BYTES_BEFORE_EXTRACTION, asset.length() * 3L);
        if (availableBytes < requiredBytes) {
            throw new IOException("Not enough free storage for Debian extraction. Available=" +
                formatBytes(availableBytes) + ", required=" + formatBytes(requiredBytes));
        }

        setState(context, STATE_EXTRACTING, "Extracting Debian rootfs.");
        File staging = stagingDir(context);
        deleteRecursively(staging);
        mkdirs(staging);
        runShell(context,
            quote(new File(TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH, "zstd").getAbsolutePath()) +
                " -dc " + quote(asset.getAbsolutePath()) + " | " +
                quote(new File(TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH, "tar").getAbsolutePath()) +
                " -C " + quote(staging.getAbsolutePath()) + " -xf -",
            new File(logDir(context), "firstboot.log"));

        setState(context, STATE_CONFIGURING, "Configuring Debian rootfs.");
        configureRootfs(context, staging, expectedSha);

        File rootfs = rootfsDir(context);
        if (rootfs.exists()) {
            throw new IOException("Refusing to replace existing rootfs: " + rootfs.getAbsolutePath());
        }
        if (!staging.renameTo(rootfs)) {
            throw new IOException("Failed to move staging rootfs into place.");
        }

        setState(context, STATE_READY, "Debian rootfs is installed.");
    }

    private static void startDesktop(Context context) throws Exception {
        if (!isRootfsInstalled(context)) {
            setState(context, STATE_NOT_INSTALLED, "Debian rootfs is not installed.");
            return;
        }

        setState(context, STATE_STARTING_DESKTOP, "Desktop launch is waiting for embedded Termux:X11 and bundled PRoot integration.");
        throw new IllegalStateException("Debian rootfs is ready, but embedded Termux:X11 and bundled PRoot launch are not wired yet.");
    }

    private static void configureRootfs(Context context, File rootfs, String rootfsSha) throws Exception {
        File tmp = new File(rootfs, "tmp");
        File home = new File(rootfs, "home/panix");
        File sudoersDir = new File(rootfs, "etc/sudoers.d");
        mkdirs(tmp);
        mkdirs(home);
        mkdirs(sudoersDir);
        Os.chmod(tmp.getAbsolutePath(), 01777);

        ensureLine(new File(rootfs, "etc/group"), "panix:", "panix:x:1000:\n");
        ensureLine(new File(rootfs, "etc/passwd"), "panix:", "panix:x:1000:1000:Panix User:/home/panix:/bin/bash\n");
        writeFile(new File(sudoersDir, "panix"), "panix ALL=(ALL) NOPASSWD:ALL\n");
        Os.chmod(new File(sudoersDir, "panix").getAbsolutePath(), 0440);
        writeFile(new File(rootfs, "etc/resolv.conf"), "nameserver 1.1.1.1\nnameserver 8.8.8.8\n");

        File osRelease = new File(rootfs, "etc/os-release");
        String osReleaseText = readFile(osRelease);
        if (!osReleaseText.contains("trixie") && !osReleaseText.contains("13")) {
            throw new IOException("Rootfs health check failed: /etc/os-release does not identify Debian 13/Trixie.");
        }
        if (!new File(rootfs, "bin/bash").exists()) {
            throw new IOException("Rootfs health check failed: /bin/bash is missing.");
        }

        writeFile(new File(rootfs, VERSION_MARKER),
            "version=0.1.0-dev\nrootfs_sha256=" + rootfsSha + "\npackage=io.github.decentricity.panix\n");
        appendLog(context, "firstboot", "Configured Debian rootfs at " + rootfs.getAbsolutePath());
    }

    private static void ensureDirectories(Context context) throws IOException {
        mkdirs(assetDir(context));
        mkdirs(logDir(context));
        mkdirs(runDir(context));
        mkdirs(stateDir(context));
        mkdirs(exportDir(context));
        mkdirs(tmpDir(context));
    }

    private static File copyRequiredAsset(Context context, String name) throws IOException {
        File output = new File(assetDir(context), name);
        AssetManager assets = context.getAssets();
        try (InputStream input = assets.open(name); FileOutputStream outputStream = new FileOutputStream(output)) {
            byte[] buffer = new byte[1024 * 1024];
            int read;
            while ((read = input.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
        } catch (IOException e) {
            throw new IOException("Missing required bundled asset " + name +
                ". Build Panix with ./scripts/build-panix.sh so the Debian rootfs is packaged.", e);
        }
        return output;
    }

    private static void runShell(Context context, String command, File logFile) throws Exception {
        mkdirs(logFile.getParentFile());
        String shell = new File(TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH, "bash").getAbsolutePath();
        ProcessBuilder builder = new ProcessBuilder(shell, "-lc", "set -e\n" + command);
        builder.directory(filesDir(context));
        builder.redirectErrorStream(true);
        builder.environment().put("HOME", TermuxConstants.TERMUX_HOME_DIR_PATH);
        builder.environment().put("PATH", TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH + ":/system/bin");
        Process process = builder.start();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (InputStream input = process.getInputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
        }
        int exitCode = process.waitFor();
        String text = output.toString("UTF-8");
        appendLog(logFile, "$ " + command + "\n" + text + "\nexit=" + exitCode + "\n");
        if (exitCode != 0) {
            throw new IOException("Command failed with exit code " + exitCode + ": " + command);
        }
    }

    private static String readExpectedSha(File checksumFile) throws IOException {
        String line = readFile(checksumFile).trim();
        if (line.length() < 64) {
            throw new IOException("Invalid rootfs checksum file: " + checksumFile.getAbsolutePath());
        }
        return line.substring(0, 64).toLowerCase(Locale.ROOT);
    }

    private static String sha256(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream input = new FileInputStream(file)) {
            byte[] buffer = new byte[1024 * 1024];
            int read;
            while ((read = input.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }
        StringBuilder result = new StringBuilder();
        for (byte b : digest.digest()) {
            result.append(String.format(Locale.ROOT, "%02x", b));
        }
        return result.toString();
    }

    private static void setState(Context context, String state, String detail) {
        try {
            mkdirs(stateDir(context));
            writeFile(stateFile(context), state + "\n");
            writeFile(detailFile(context), detail + "\n");
            appendLog(context, "runtime", state + ": " + detail);
        } catch (Exception ignored) {
        }
    }

    private static boolean isRootfsInstalled(Context context) {
        return new File(rootfsDir(context), VERSION_MARKER).exists();
    }

    private static String defaultDetailForState(String state) {
        if (STATE_NOT_INSTALLED.equals(state)) {
            return "Debian rootfs has not been installed yet.";
        }
        if (STATE_READY.equals(state)) {
            return "Debian rootfs is ready.";
        }
        if (STATE_FAILED.equals(state)) {
            return "Panix runtime failed. Open logs for details.";
        }
        return "Panix runtime state: " + state;
    }

    private static void ensureLine(File file, String prefix, String line) throws IOException {
        String existing = readFile(file);
        if (!existing.contains(prefix)) {
            appendLog(file, line);
        }
    }

    private static void writeFile(File file, String content) throws IOException {
        mkdirs(file.getParentFile());
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            writer.write(content);
        }
    }

    private static String readFile(File file) {
        if (file == null || !file.exists()) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line).append('\n');
            }
        } catch (IOException ignored) {
        }
        return result.toString();
    }

    private static void appendLog(Context context, String basename, String text) {
        appendLog(new File(logDir(context), basename + ".log"), text + "\n");
    }

    private static void appendLog(File file, String text) {
        try {
            mkdirs(file.getParentFile());
            try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file, true), StandardCharsets.UTF_8)) {
                writer.write(text);
            }
        } catch (IOException ignored) {
        }
    }

    private static void appendFileTail(StringBuilder builder, File file, int maxChars) {
        String content = readFile(file);
        if (content.isEmpty()) {
            return;
        }
        if (content.length() > maxChars) {
            content = content.substring(content.length() - maxChars);
        }
        builder.append("== ").append(file.getName()).append(" ==\n").append(content).append('\n');
    }

    private static void mkdirs(File dir) throws IOException {
        if (dir == null) {
            return;
        }
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Failed to create directory: " + dir.getAbsolutePath());
        }
    }

    private static void deleteRecursively(File file) throws IOException {
        if (file == null || !file.exists()) {
            return;
        }
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        if (!file.delete()) {
            throw new IOException("Failed to delete " + file.getAbsolutePath());
        }
    }

    private static void deleteFile(File file) {
        if (file != null && file.exists()) {
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        }
    }

    private static String quote(String value) {
        return "'" + value.replace("'", "'\\''") + "'";
    }

    private static String formatBytes(long bytes) {
        long mib = bytes / (1024L * 1024L);
        return mib + " MiB";
    }

    private static String stackTrace(Exception e) {
        StringBuilder result = new StringBuilder(e.toString()).append('\n');
        for (StackTraceElement element : e.getStackTrace()) {
            result.append("  at ").append(element).append('\n');
        }
        return result.toString();
    }

    private static File filesDir(Context context) {
        return context.getFilesDir();
    }

    private static File rootfsDir(Context context) {
        return new File(filesDir(context), "debian");
    }

    private static File stagingDir(Context context) {
        return new File(filesDir(context), "debian.staging");
    }

    private static File assetDir(Context context) {
        return new File(filesDir(context), "assets");
    }

    private static File logDir(Context context) {
        return new File(filesDir(context), "logs");
    }

    private static File runDir(Context context) {
        return new File(filesDir(context), "run");
    }

    private static File tmpDir(Context context) {
        return new File(filesDir(context), "tmp");
    }

    private static File exportDir(Context context) {
        return new File(filesDir(context), "export");
    }

    private static File stateDir(Context context) {
        return new File(filesDir(context), "panix-state");
    }

    private static File stateFile(Context context) {
        return new File(stateDir(context), "runtime.state");
    }

    private static File detailFile(Context context) {
        return new File(stateDir(context), "runtime.detail");
    }

    private static File lockFile(Context context) {
        return new File(runDir(context), "desktop.lock");
    }

    static final class RuntimeStatus {
        final String state;
        final String detail;
        final boolean workerRunning;

        RuntimeStatus(String state, String detail, boolean workerRunning) {
            this.state = state;
            this.detail = detail;
            this.workerRunning = workerRunning;
        }
    }
}
