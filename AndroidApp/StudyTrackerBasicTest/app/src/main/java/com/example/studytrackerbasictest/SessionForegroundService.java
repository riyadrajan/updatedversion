package com.example.studytrackerbasictest;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.util.Locale;

public class SessionForegroundService extends Service implements AppUsageMonitor.AppUsageListener {

    private static final String TAG = "SessionService";
    private static final int NOTIFICATION_ID = NotificationHelper.NOTIFICATION_SESSION_ACTIVE;
    private int seconds = 0;
    private Handler handler = new Handler();
    private boolean isRunning = false;
    private AppUsageMonitor appUsageMonitor;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : null;

        if ("START".equals(action)) {
            startForeground(NOTIFICATION_ID, buildNotification("00:00"));
            startTimer();
            startAppMonitoring();
        } else if ("STOP".equals(action)) {
            stopTimer();
            stopAppMonitoring();
            stopForeground(true);
            stopSelf();
        } else if ("PAUSE".equals(action)) {
            stopTimer();
            stopAppMonitoring();
            updateNotification("Paused: " + formatTime(seconds));
        } else if ("RESUME".equals(action)) {
            startTimer();
            startAppMonitoring();
        }

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private Notification buildNotification(String time) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        );

        Intent stopIntent = new Intent(this, SessionForegroundService.class);
        stopIntent.setAction("STOP");
        PendingIntent stopPendingIntent = PendingIntent.getService(
                this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, NotificationHelper.CHANNEL_SESSION)
                .setContentTitle("Study Session Active")
                .setContentText("Running: " + time)
                .setSmallIcon(R.drawable.outline_home_24)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .addAction(R.drawable.round_button_red, "Stop", stopPendingIntent)
                .build();
    }

    private void startTimer() {
        isRunning = true;
        handler.post(timerRunnable);
    }

    private void stopTimer() {
        isRunning = false;
        handler.removeCallbacks(timerRunnable);
    }

    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (isRunning) {
                seconds++;
                updateNotification(formatTime(seconds));
                handler.postDelayed(this, 1000);
            }
        }
    };

    private void updateNotification(String time) {
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            Notification notification = buildNotification(time);
            manager.notify(NOTIFICATION_ID, notification);
        }
    }

    private String formatTime(int totalSeconds) {
        int minutes = totalSeconds / 60;
        int secs = totalSeconds % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, secs);
    }

    private void startAppMonitoring() {
        if (appUsageMonitor == null) {
            appUsageMonitor = new AppUsageMonitor(this, this);
        }
        appUsageMonitor.startMonitoring();
        Log.d(TAG, "App usage monitoring started");
    }

    private void stopAppMonitoring() {
        if (appUsageMonitor != null) {
            appUsageMonitor.stopMonitoring();
        }
        Log.d(TAG, "App usage monitoring stopped");
    }

    // AppUsageMonitor.AppUsageListener callbacks
    @Override
    public void onAppSwitchedAway(String appName) {
        Log.d(TAG, "User switched to: " + appName);
        // Brief switch - don't flag yet
    }

    @Override
    public void onAppReturned() {
        Log.d(TAG, "User returned to study app");
        // User came back quickly - no distraction
    }

    @Override
    public void onDistractionDetected(long durationMs) {
        Log.d(TAG, "Phone distraction detected: " + durationMs + "ms in other apps");
        
        // Show notification warning
        NotificationHelper.showWarning(
            this,
            "Phone Distraction Detected",
            "You've been using other apps. Stay focused!"
        );
        
        // TODO: Send to server/database
        // Mark as distracted in session data
    }

    @Override
    public void onDistractionEnded(long durationMs) {
        Log.d(TAG, "User returned after " + durationMs + "ms distraction");
        
        // TODO: Send to server/database
        // Record distraction duration
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopTimer();
        stopAppMonitoring();
    }
}
