package com.termux.x11;

import android.app.AlertDialog;
import android.app.role.RoleManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.termux.app.PanixRuntimeManager;
import com.termux.app.PanixRuntimeService;
import com.termux.app.TermuxActivity;

import java.util.Collections;
import java.util.List;

public final class PanixHomeActivity extends MainActivity {

    private final Handler panixStatusHandler = new Handler(Looper.getMainLooper());
    private final Runnable panixStatusPoller = new Runnable() {
        @Override
        public void run() {
            updateStartupStatus();
            panixStatusHandler.postDelayed(this, 1000);
        }
    };

    private TextView startupStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startupStatus = findViewById(R.id.textView);
        rebrandStartupScreen();
        addPanixMenuButton();
        PanixRuntimeService.requestStart(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        panixStatusHandler.post(panixStatusPoller);
    }

    @Override
    public void onPause() {
        panixStatusHandler.removeCallbacks(panixStatusPoller);
        super.onPause();
    }

    @Override
    public void onUserLeaveHint() {
        // Panix is the HOME activity; pressing Home should return here, not
        // move the desktop into Termux:X11 picture-in-picture mode.
    }

    private void rebrandStartupScreen() {
        View preferences = findViewById(R.id.preferences_button);
        if (preferences instanceof Button) {
            Button button = (Button) preferences;
            button.setText("Display Settings");
            button.setOnClickListener(v -> openDisplaySettings());
        }

        View help = findViewById(R.id.help_button);
        if (help instanceof Button) {
            Button button = (Button) help;
            button.setText("Panix Menu");
            button.setOnClickListener(v -> showPanixMenu());
        }

        View exit = findViewById(R.id.exit_button);
        if (exit instanceof Button) {
            Button button = (Button) exit;
            button.setText("Stop Desktop");
            button.setOnClickListener(v -> PanixRuntimeService.requestStopDesktop(this));
        }

        updateStartupStatus();
    }

    private void addPanixMenuButton() {
        FrameLayout content = findViewById(android.R.id.content);
        Button button = new Button(this);
        button.setText("P");
        button.setTextColor(Color.WHITE);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setTextSize(16);
        button.setAllCaps(false);
        button.setGravity(Gravity.CENTER);
        button.setBackground(menuButtonBackground());
        button.setOnClickListener(v -> showPanixMenu());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            button.setTooltipText("Panix menu");
        }

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(dp(44), dp(44), Gravity.TOP | Gravity.END);
        params.setMargins(0, dp(12), dp(12), 0);
        content.addView(button, params);
    }

    private void showPanixMenu() {
        String[] items = new String[] {
            "Open Debian Terminal",
            "Open Panix Logs",
            "Restart Desktop",
            "Stop Desktop",
            "Reset Debian",
            "Toggle Soft Keyboard",
            "Android Apps",
            "Android Settings",
            "Display Settings",
            "Choose Home App"
        };

        new AlertDialog.Builder(this)
            .setTitle("Panix")
            .setItems(items, (dialog, which) -> {
                switch (which) {
                    case 0:
                        openPanixTerminal();
                        break;
                    case 1:
                        showPanixLogs();
                        break;
                    case 2:
                        PanixRuntimeService.requestRestartDesktop(this);
                        break;
                    case 3:
                        PanixRuntimeService.requestStopDesktop(this);
                        break;
                    case 4:
                        confirmResetDebian();
                        break;
                    case 5:
                        MainActivity.toggleKeyboardVisibility(this);
                        break;
                    case 6:
                        showAndroidApps();
                        break;
                    case 7:
                        startActivity(new Intent(Settings.ACTION_SETTINGS));
                        break;
                    case 8:
                        openDisplaySettings();
                        break;
                    case 9:
                        requestHomeRole();
                        break;
                    default:
                        break;
                }
            })
            .show();
    }

    private void openPanixTerminal() {
        Intent intent = new Intent(this, TermuxActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
    }

    private void openDisplaySettings() {
        Intent intent = new Intent(this, LoriePreferences.class);
        intent.setAction(Intent.ACTION_MAIN);
        startActivity(intent);
    }

    private void confirmResetDebian() {
        new AlertDialog.Builder(this)
            .setTitle("Reset Debian")
            .setMessage("Delete the installed Debian rootfs and keep the Panix export directory?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Reset", (dialog, which) -> PanixRuntimeService.requestResetDebian(this))
            .show();
    }

    private void showPanixLogs() {
        TextView logText = new TextView(this);
        logText.setText(PanixRuntimeManager.readRecentLogs(this));
        logText.setTextIsSelectable(true);
        logText.setTextSize(12);
        int padding = dp(16);
        logText.setPadding(padding, padding, padding, padding);

        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(logText);

        new AlertDialog.Builder(this)
            .setTitle("Panix Logs")
            .setView(scrollView)
            .setPositiveButton("Close", null)
            .show();
    }

    private void showAndroidApps() {
        PackageManager packageManager = getPackageManager();
        Intent launcherIntent = new Intent(Intent.ACTION_MAIN);
        launcherIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> apps = packageManager.queryIntentActivities(launcherIntent, 0);
        Collections.sort(apps, new ResolveInfo.DisplayNameComparator(packageManager));

        String[] labels = new String[apps.size()];
        for (int i = 0; i < apps.size(); i++) {
            ResolveInfo app = apps.get(i);
            labels[i] = app.loadLabel(packageManager).toString();
        }

        new AlertDialog.Builder(this)
            .setTitle("Android Apps")
            .setItems(labels, (dialog, which) -> launchAndroidApp(apps.get(which)))
            .show();
    }

    private void launchAndroidApp(ResolveInfo resolveInfo) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setComponent(new ComponentName(
            resolveInfo.activityInfo.packageName,
            resolveInfo.activityInfo.name));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void requestHomeRole() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            RoleManager roleManager = getSystemService(RoleManager.class);
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                startActivity(roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME));
                return;
            }
        }

        startActivity(new Intent(Settings.ACTION_HOME_SETTINGS));
    }

    private void updateStartupStatus() {
        if (startupStatus == null) {
            return;
        }
        PanixRuntimeManager.RuntimeStatus runtimeStatus = PanixRuntimeManager.getStatus(this);
        String worker = runtimeStatus.workerRunning ? " working" : "";
        startupStatus.setText("Panix: " + runtimeStatus.state + worker + "\n" + runtimeStatus.detail);
    }

    private GradientDrawable menuButtonBackground() {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(Color.rgb(12, 18, 24));
        drawable.setCornerRadius(dp(22));
        drawable.setStroke(dp(1), Color.rgb(74, 222, 128));
        return drawable;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
