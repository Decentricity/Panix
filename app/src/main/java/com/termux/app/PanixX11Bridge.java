package com.termux.app;

import android.content.Context;
import android.content.Intent;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

final class PanixX11Bridge {

    static final String DISPLAY = ":1";

    private static final long START_GRACE_MS = 1800L;
    private static final Object LOCK = new Object();

    private static Process sX11Process;

    private PanixX11Bridge() {}

    static boolean isAvailable(Context context) {
        try {
            Class.forName("com.termux.x11.CmdEntryPoint", false, context.getClassLoader());
            Class.forName("com.termux.x11.MainActivity", false, context.getClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    static void startServer(Context context, File tmpDir, File logFile) throws Exception {
        if (!isAvailable(context)) {
            throw new IOException("Embedded Termux:X11 module is not packaged in this build.");
        }

        synchronized (LOCK) {
            if (sX11Process != null) {
                try {
                    int exitCode = sX11Process.exitValue();
                    appendLog(logFile, "Previous X11 server exited with code " + exitCode);
                    sX11Process = null;
                } catch (IllegalThreadStateException stillRunning) {
                    return;
                }
            }
        }

        mkdirs(tmpDir);
        mkdirs(logFile.getParentFile());
        File appProcess = new File("/system/bin/app_process");
        if (!appProcess.exists()) {
            throw new IOException("Android app_process is missing; embedded X11 cannot start.");
        }

        List<String> command = new ArrayList<>();
        command.add(appProcess.getAbsolutePath());
        command.add("-Xnoimage-dex2oat");
        command.add("/");
        command.add("--nice-name=panix-x11");
        command.add("com.termux.x11.CmdEntryPoint");
        command.add(DISPLAY);

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);
        builder.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile));
        builder.environment().put("CLASSPATH", context.getPackageCodePath());
        builder.environment().put("TMPDIR", tmpDir.getAbsolutePath());

        appendLog(logFile, "Starting embedded Termux:X11 server on display " + DISPLAY);
        Process process = builder.start();
        synchronized (LOCK) {
            sX11Process = process;
        }

        Thread.sleep(START_GRACE_MS);
        try {
            int exitCode = process.exitValue();
            synchronized (LOCK) {
                if (sX11Process == process) {
                    sX11Process = null;
                }
            }
            throw new IOException("Embedded X11 server exited during startup with code " + exitCode + ".");
        } catch (IllegalThreadStateException stillRunning) {
            appendLog(logFile, "Embedded Termux:X11 server is running.");
        }
    }

    static void stopServer() {
        Process processToStop = null;
        synchronized (LOCK) {
            if (sX11Process != null) {
                processToStop = sX11Process;
                sX11Process = null;
            }
        }
        if (processToStop == null) {
            return;
        }
        processToStop.destroy();
        try {
            if (!processToStop.waitFor(2, TimeUnit.SECONDS)) {
                processToStop.destroyForcibly();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            processToStop.destroyForcibly();
        }
    }

    static void openSurface(Context context) {
        Intent intent = new Intent();
        intent.setClassName(context.getPackageName(), "com.termux.x11.MainActivity");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    private static void appendLog(File file, String text) {
        try {
            mkdirs(file.getParentFile());
            try (OutputStreamWriter writer = new OutputStreamWriter(new java.io.FileOutputStream(file, true), StandardCharsets.UTF_8)) {
                writer.write(text);
                writer.write('\n');
            }
        } catch (IOException ignored) {
        }
    }

    private static void mkdirs(File dir) throws IOException {
        if (dir == null) {
            return;
        }
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Failed to create directory: " + dir.getAbsolutePath());
        }
    }
}
