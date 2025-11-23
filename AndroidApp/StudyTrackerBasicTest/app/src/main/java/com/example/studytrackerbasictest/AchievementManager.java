package com.example.studytrackerbasictest;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.studytrackerbasictest.databases.SessionDatabase;

public class AchievementManager {

    private static final String PREFS_NAME = "AchievementPrefs";

    public static void checkAndNotifyAchievements(Context context, String username) {
        if (username == null || username.isEmpty()) return;

        SessionDatabase db = new SessionDatabase();
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Get stats
        db.getSessionsForUser(username, sessions -> {
            int totalSessions = sessions.size();

            // Calculate total hours
            int totalMinutes = 0;
            for (var session : sessions) {
                String duration = (String) session.get("duration");
                if (duration != null) {
                    String[] parts = duration.split(":");
                    if (parts.length == 2) {
                        totalMinutes += Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
                    }
                }
            }
            int totalHours = totalMinutes / 60;

            // Check achievements
            checkFirstSession(context, prefs, totalSessions);
            check10Sessions(context, prefs, totalSessions);
            check50Sessions(context, prefs, totalSessions);
            check100Sessions(context, prefs, totalSessions);
            check10Hours(context, prefs, totalHours);
            check50Hours(context, prefs, totalHours);
            check100Hours(context, prefs, totalHours);
        });
    }

    private static void checkFirstSession(Context context, SharedPreferences prefs, int totalSessions) {
        if (totalSessions == 1 && !prefs.getBoolean("first_session", false)) {
            NotificationHelper.showAchievement(context,
                    "First Session!",
                    "You've started your learning journey!");
            prefs.edit().putBoolean("first_session", true).apply();
        }
    }

    private static void check10Sessions(Context context, SharedPreferences prefs, int totalSessions) {
        if (totalSessions >= 10 && !prefs.getBoolean("ten_sessions", false)) {
            NotificationHelper.showAchievement(context,
                    "10 Sessions!",
                    "You're building a great habit!");
            prefs.edit().putBoolean("ten_sessions", true).apply();
        }
    }

    private static void check50Sessions(Context context, SharedPreferences prefs, int totalSessions) {
        if (totalSessions >= 50 && !prefs.getBoolean("fifty_sessions", false)) {
            NotificationHelper.showAchievement(context,
                    "50 Sessions!",
                    "Consistency is key. Keep going!");
            prefs.edit().putBoolean("fifty_sessions", true).apply();
        }
    }

    private static void check100Sessions(Context context, SharedPreferences prefs, int totalSessions) {
        if (totalSessions >= 100 && !prefs.getBoolean("hundred_sessions", false)) {
            NotificationHelper.showAchievement(context,
                    "100 Sessions!",
                    "You're a study legend!");
            prefs.edit().putBoolean("hundred_sessions", true).apply();
        }
    }

    private static void check10Hours(Context context, SharedPreferences prefs, int totalHours) {
        if (totalHours >= 10 && !prefs.getBoolean("ten_hours", false)) {
            NotificationHelper.showAchievement(context,
                    "10 Hours!",
                    "That's dedication!");
            prefs.edit().putBoolean("ten_hours", true).apply();
        }
    }

    private static void check50Hours(Context context, SharedPreferences prefs, int totalHours) {
        if (totalHours >= 50 && !prefs.getBoolean("fifty_hours", false)) {
            NotificationHelper.showAchievement(context,
                    "50 Hours!",
                    "You're on fire!");
            prefs.edit().putBoolean("fifty_hours", true).apply();
        }
    }

    private static void check100Hours(Context context, SharedPreferences prefs, int totalHours) {
        if (totalHours >= 100 && !prefs.getBoolean("hundred_hours", false)) {
            NotificationHelper.showAchievement(context,
                    "100 Hours!",
                    "You're a study master!");
            prefs.edit().putBoolean("hundred_hours", true).apply();
        }
    }
}
