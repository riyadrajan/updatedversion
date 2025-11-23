package com.example.studytrackerbasictest;

import android.app.ActivityManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Monitors app usage to detect when user switches to other apps
 * during an active study session (phone distraction detection)
 */
public class AppUsageMonitor {
    
    private static final String TAG = "AppUsageMonitor";
    private static final long CHECK_INTERVAL_MS = 2000; // Check every 2 seconds
    private static final long DISTRACTION_THRESHOLD_MS = 5000; // 5 seconds in other app = distracted
    
    private Context context;
    private Handler handler;
    private boolean isMonitoring = false;
    private String packageName;
    private AppUsageListener listener;
    
    private long lastCheckTime = 0;
    private long timeInOtherApps = 0;
    private boolean currentlyDistracted = false;
    
    public interface AppUsageListener {
        void onAppSwitchedAway(String appName);
        void onAppReturned();
        void onDistractionDetected(long durationMs);
        void onDistractionEnded(long durationMs);
    }
    
    public AppUsageMonitor(Context context, AppUsageListener listener) {
        this.context = context.getApplicationContext();
        this.packageName = context.getPackageName();
        this.listener = listener;
        this.handler = new Handler(Looper.getMainLooper());
    }
    
    /**
     * Start monitoring app usage
     */
    public void startMonitoring() {
        if (isMonitoring) {
            return;
        }
        
        isMonitoring = true;
        lastCheckTime = System.currentTimeMillis();
        timeInOtherApps = 0;
        currentlyDistracted = false;
        
        Log.d(TAG, "Started app usage monitoring");
        handler.post(monitoringRunnable);
    }
    
    /**
     * Stop monitoring app usage
     */
    public void stopMonitoring() {
        isMonitoring = false;
        handler.removeCallbacks(monitoringRunnable);
        Log.d(TAG, "Stopped app usage monitoring");
    }
    
    /**
     * Check if our app is currently in foreground
     */
    private boolean isAppInForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return isAppInForegroundLollipop();
        } else {
            return isAppInForegroundLegacy();
        }
    }
    
    /**
     * Check foreground app using UsageStatsManager (API 21+)
     */
    private boolean isAppInForegroundLollipop() {
        try {
            UsageStatsManager usageStatsManager = (UsageStatsManager) 
                context.getSystemService(Context.USAGE_STATS_SERVICE);
            
            if (usageStatsManager == null) {
                return true; // Assume in foreground if can't check
            }
            
            long currentTime = System.currentTimeMillis();
            long queryTime = currentTime - 1000; // Last 1 second
            
            List<UsageStats> stats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY, queryTime, currentTime
            );
            
            if (stats == null || stats.isEmpty()) {
                return true; // Assume in foreground if no stats
            }
            
            // Find most recently used app
            SortedMap<Long, UsageStats> sortedStats = new TreeMap<>();
            for (UsageStats usageStats : stats) {
                sortedStats.put(usageStats.getLastTimeUsed(), usageStats);
            }
            
            if (!sortedStats.isEmpty()) {
                String foregroundApp = sortedStats.get(sortedStats.lastKey()).getPackageName();
                return packageName.equals(foregroundApp);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error checking foreground app: " + e.getMessage());
        }
        
        return true; // Default to foreground if error
    }
    
    /**
     * Check foreground app using ActivityManager (Legacy, less reliable)
     */
    private boolean isAppInForegroundLegacy() {
        try {
            ActivityManager activityManager = (ActivityManager) 
                context.getSystemService(Context.ACTIVITY_SERVICE);
            
            if (activityManager == null) {
                return true;
            }
            
            List<ActivityManager.RunningAppProcessInfo> processes = 
                activityManager.getRunningAppProcesses();
            
            if (processes != null) {
                for (ActivityManager.RunningAppProcessInfo processInfo : processes) {
                    if (processInfo.importance == 
                        ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                        
                        for (String activeProcess : processInfo.pkgList) {
                            if (activeProcess.equals(packageName)) {
                                return true;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking foreground app (legacy): " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Get name of currently foreground app
     */
    private String getForegroundAppName() {
        try {
            UsageStatsManager usageStatsManager = (UsageStatsManager) 
                context.getSystemService(Context.USAGE_STATS_SERVICE);
            
            if (usageStatsManager == null) {
                return "Unknown";
            }
            
            long currentTime = System.currentTimeMillis();
            List<UsageStats> stats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY, currentTime - 1000, currentTime
            );
            
            if (stats != null && !stats.isEmpty()) {
                SortedMap<Long, UsageStats> sortedStats = new TreeMap<>();
                for (UsageStats usageStats : stats) {
                    sortedStats.put(usageStats.getLastTimeUsed(), usageStats);
                }
                
                if (!sortedStats.isEmpty()) {
                    return sortedStats.get(sortedStats.lastKey()).getPackageName();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting foreground app name: " + e.getMessage());
        }
        
        return "Unknown";
    }
    
    /**
     * Monitoring runnable - checks app state periodically
     */
    private final Runnable monitoringRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isMonitoring) {
                return;
            }
            
            long currentTime = System.currentTimeMillis();
            long elapsed = currentTime - lastCheckTime;
            lastCheckTime = currentTime;
            
            boolean inForeground = isAppInForeground();
            
            if (!inForeground) {
                // User switched to another app
                timeInOtherApps += elapsed;
                
                if (!currentlyDistracted && timeInOtherApps >= DISTRACTION_THRESHOLD_MS) {
                    // Sustained distraction detected
                    currentlyDistracted = true;
                    String appName = getForegroundAppName();
                    Log.d(TAG, "Distraction detected: User in " + appName + 
                          " for " + timeInOtherApps + "ms");
                    
                    if (listener != null) {
                        listener.onDistractionDetected(timeInOtherApps);
                    }
                }
                
                if (listener != null && timeInOtherApps < DISTRACTION_THRESHOLD_MS) {
                    String appName = getForegroundAppName();
                    listener.onAppSwitchedAway(appName);
                }
                
            } else {
                // User is back in our app
                if (currentlyDistracted) {
                    // Distraction ended
                    Log.d(TAG, "User returned to app after " + timeInOtherApps + "ms");
                    
                    if (listener != null) {
                        listener.onDistractionEnded(timeInOtherApps);
                    }
                    
                    currentlyDistracted = false;
                } else if (timeInOtherApps > 0 && timeInOtherApps < DISTRACTION_THRESHOLD_MS) {
                    // Brief switch, not counted as distraction
                    if (listener != null) {
                        listener.onAppReturned();
                    }
                }
                
                timeInOtherApps = 0;
            }
            
            // Schedule next check
            handler.postDelayed(this, CHECK_INTERVAL_MS);
        }
    };
    
    /**
     * Get total time spent in other apps during current monitoring session
     */
    public long getTotalDistractionTime() {
        return timeInOtherApps;
    }
    
    /**
     * Check if currently distracted
     */
    public boolean isCurrentlyDistracted() {
        return currentlyDistracted;
    }
}
