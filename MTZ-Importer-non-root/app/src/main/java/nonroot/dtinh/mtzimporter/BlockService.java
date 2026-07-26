package nonroot.dtinh.mtzimporter;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

public final class BlockService extends Service {
    public static final String ACTION_CHECK_THEME_UPDATE =
            "miui.intent.action.CHECK_THEME_UPDATE";
    public static final String ACTION_CHECK_TIME_UP =
            "miui.intent.action.CHECK_TIME_UP";
    public static final String ACTION_CLEAR_THEME_RUNTIME_DATA =
            "miui.intent.action.CLEAR_THEME_RUNTIME_DATA";
    public static final String ACTION_TRIAL_START_ACTIVITY =
            "com.android.thememanager.action.TRIAL_START_ACTIVITY";

    private static final String CHANNEL_ID = "mtz_tool_protection";
    private static final String TAG = "MTZToolBlock";
    private static final int NOTIFICATION_ID = 1100;
    private static volatile boolean running;

    private BroadcastReceiver blocker;
    private boolean registered;
    private long blockedCount;

    @Override
    public void onCreate() {
        super.onCreate();
        if (!ProtectionState.isEnabled(this)) {
            stopSelf();
            return;
        }
        startAsForeground();
        registerBlocker();
        cancelTrialJobIfPossible();
        running = true;
    }

    private void startAsForeground() {
        NotificationManager manager =
                (NotificationManager) getSystemService(
                        Context.NOTIFICATION_SERVICE
                );
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Bảo vệ theme",
                    NotificationManager.IMPORTANCE_MIN
            );
            channel.setDescription(
                    "Chặn Xiaomi kiểm tra và reset theme đã nhập"
            );
            channel.setShowBadge(false);
            manager.createNotificationChannel(channel);
        }

        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);
        Notification notification = builder
                .setSmallIcon(android.R.drawable.stat_notify_sync_noanim)
                .setContentTitle("MTZ Tool")
                .setContentText("Bảo vệ theme đang hoạt động")
                .setOngoing(true)
                .setShowWhen(false)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
        startForeground(NOTIFICATION_ID, notification);
    }

    private void registerBlocker() {
        if (registered) {
            return;
        }

        blocker = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent == null
                        ? null
                        : intent.getAction();
                if (ACTION_CHECK_THEME_UPDATE.equals(action)
                        || ACTION_CHECK_TIME_UP.equals(action)
                        || ACTION_CLEAR_THEME_RUNTIME_DATA.equals(action)
                        || ACTION_TRIAL_START_ACTIVITY.equals(action)) {
                    try {
                        setResultCode(Activity.RESULT_CANCELED);
                        setResultData(null);
                        abortBroadcast();
                        blockedCount++;
                        Log.i(
                                TAG,
                                "blocked " + action
                                        + " count=" + blockedCount
                        );
                    } catch (RuntimeException error) {
                        Log.e(TAG, "cannot abort " + action, error);
                    }
                    cancelTrialJobIfPossible();
                } else if (Intent.ACTION_SCREEN_ON.equals(action)) {
                    cancelTrialJobIfPossible();
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_CHECK_THEME_UPDATE);
        filter.addAction(ACTION_CHECK_TIME_UP);
        filter.addAction(ACTION_CLEAR_THEME_RUNTIME_DATA);
        filter.addAction(ACTION_TRIAL_START_ACTIVITY);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_TIME_TICK);
        filter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);

        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(
                    blocker,
                    filter,
                    Context.RECEIVER_EXPORTED
            );
        } else {
            registerReceiver(blocker, filter);
        }
        registered = true;
        Log.i(TAG, "priority-1000 theme blocker registered");
    }

    private void cancelTrialJobIfPossible() {
        if (!RootShell.isPermissionGranted()) {
            return;
        }
        new Thread(() -> {
            try {
                RootShell.run(
                        "cmd jobscheduler cancel -u 0 "
                                + "com.android.thememanager 64\n"
                );
            } catch (Throwable ignored) {
            }
        }, "mtz-trial-job").start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!ProtectionState.isEnabled(this)) {
            stopSelf();
            return START_NOT_STICKY;
        }
        if (!registered) {
            registerBlocker();
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (registered && blocker != null) {
            try {
                unregisterReceiver(blocker);
            } catch (IllegalArgumentException ignored) {
            }
        }
        registered = false;
        running = false;
        if (Build.VERSION.SDK_INT >= 24) {
            stopForeground(STOP_FOREGROUND_REMOVE);
        } else {
            stopForeground(true);
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    static boolean isRunning() {
        return running;
    }
}
