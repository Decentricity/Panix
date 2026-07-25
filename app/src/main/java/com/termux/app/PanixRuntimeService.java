package com.termux.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import com.termux.R;

public final class PanixRuntimeService extends Service {

    static final String ACTION_START = "io.github.decentricity.panix.runtime.START";
    static final String ACTION_RESTART_DESKTOP = "io.github.decentricity.panix.runtime.RESTART_DESKTOP";
    static final String ACTION_STOP_DESKTOP = "io.github.decentricity.panix.runtime.STOP_DESKTOP";
    static final String ACTION_RESET_DEBIAN = "io.github.decentricity.panix.runtime.RESET_DEBIAN";

    private static final String CHANNEL_ID = "panix_runtime";
    private static final String CHANNEL_NAME = "Panix Runtime";
    private static final int NOTIFICATION_ID = 4242;

    @Override
    public void onCreate() {
        super.onCreate();
        setupNotificationChannel();
        startForeground(NOTIFICATION_ID, buildNotification());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? ACTION_START : intent.getAction();
        if (ACTION_STOP_DESKTOP.equals(action)) {
            PanixRuntimeManager.stopDesktop(this);
            stopForeground(true);
            stopSelf(startId);
            return START_NOT_STICKY;
        } else if (ACTION_RESET_DEBIAN.equals(action)) {
            PanixRuntimeManager.resetDebianAsync(this);
        } else if (ACTION_RESTART_DESKTOP.equals(action)) {
            PanixRuntimeManager.restartDesktopAsync(this);
        } else {
            PanixRuntimeManager.startAsync(this);
        }

        startForeground(NOTIFICATION_ID, buildNotification());
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    static void requestStart(Context context) {
        startWithAction(context, ACTION_START);
    }

    static void requestRestartDesktop(Context context) {
        startWithAction(context, ACTION_RESTART_DESKTOP);
    }

    static void requestStopDesktop(Context context) {
        startWithAction(context, ACTION_STOP_DESKTOP);
    }

    static void requestResetDebian(Context context) {
        startWithAction(context, ACTION_RESET_DEBIAN);
    }

    private static void startWithAction(Context context, String action) {
        Intent intent = new Intent(context, PanixRuntimeService.class).setAction(action);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    private Notification buildNotification() {
        Intent contentIntent = new Intent(this, PanixHomeActivity.class);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, contentIntent, flags);
        PanixRuntimeManager.RuntimeStatus status = PanixRuntimeManager.getStatus(this);

        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
            ? new Notification.Builder(this, CHANNEL_ID)
            : new Notification.Builder(this);
        return builder
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Panix desktop runtime")
            .setContentText(status.state + ": " + status.detail)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build();
    }

    private void setupNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }
}
