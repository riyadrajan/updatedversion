# Technical Guide Part 2: Android Application

## ANDROID APP ARCHITECTURE

### Overview
The Android app is the user interface for the Study Focus Tracker. It allows users to start/stop sessions, view real-time metrics, and analyze historical data.

**Package:** com.example.studytrackerbasictest
**Language:** Java
**Min SDK:** 24 (Android 7.0)
**Target SDK:** 34 (Android 14)

---

## ACTIVITIES (6 files)

### MainActivity.java (Main container - 150 lines)
**Purpose:** Main container with bottom navigation

**What it does:**
- Hosts 3 fragments: Home, Analytics, Settings
- Manages bottom navigation bar
- Handles fragment transactions
- Checks Firebase authentication

**Key components:**
- BottomNavigationView: Tab navigation
- FragmentContainerView: Fragment host
- Firebase auth check on startup

**Navigation flow:**
```
MainActivity
  ├─ HomeFragment (default)
  ├─ AnalyticsFragment
  └─ SettingsFragment
```

---

### LoginActivity.java (Authentication - 180 lines)
**Purpose:** User login screen

**What it does:**
- Email/password input fields
- Firebase authentication
- Remember me checkbox
- Navigate to MainActivity on success

**Firebase auth:**
```java
FirebaseAuth.getInstance()
    .signInWithEmailAndPassword(email, password)
    .addOnSuccessListener(authResult -> {
        // Save user, go to MainActivity
    });
```

**Error handling:**
- Invalid credentials → Toast message
- Network error → Retry prompt
- Empty fields → Validation error

---

### SignupActivity.java (Registration - 200 lines)
**Purpose:** New user registration

**What it does:**
- Email, password, confirm password fields
- Input validation (email format, password strength)
- Create Firebase user
- Create Firestore user document

**Validation:**
- Email: Must contain @
- Password: Min 6 characters
- Confirm: Must match password

**Firestore user document:**
```
users/{userId}:
  - email: string
  - username: string
  - createdAt: timestamp
  - totalSessions: 0
  - totalStudyTime: 0
```

---

### LiveSessionActivity.java (Live session - 270 lines)
**Purpose:** Real-time session monitoring

**What it does:**
- Displays live focus score (0-100)
- Shows current activity type
- Distraction warnings
- Pause/Resume/End buttons
- Polls server every 2 seconds for updates

**UI Components:**
- PieChart: Focus ring (visual focus score)
- TextView: Focus score number
- TextView: Session time
- CardView: Distraction warning (shows/hides)
- Buttons: Pause, End

**Polling logic:**
```java
handler.postDelayed(new Runnable() {
    @Override
    public void run() {
        GET /session/stats
        updateUI(focusScore, isDistracted)
        handler.postDelayed(this, 2000);  // Repeat every 2s
    }
}, 2000);
```

**Distraction warning:**
- Shows red card when distracted
- Auto-dismisses after 7 seconds
- Plays notification sound

---

### StatsActivity.java (Session history - 220 lines)
**Purpose:** View past sessions

**What it does:**
- Lists all user sessions
- Shows date, duration, focus score
- Click to view details
- Loads from Firestore

**RecyclerView adapter:**
```java
sessionsList.setAdapter(new SessionAdapter(sessions));
```

**Firestore query:**
```java
db.collection("sessionServer")
    .whereEqualTo("username", username)
    .orderBy("startedAt", DESCENDING)
    .limit(50)
    .get();
```

---

### AchievementsActivity.java (Gamification - 180 lines)
**Purpose:** Display achievements and streaks

**What it does:**
- Shows earned achievements
- Displays current streak
- Motivational badges
- Progress bars

**Achievements:**
- First Session
- 10 Sessions
- 50 Sessions
- 7-Day Streak
- 30-Day Streak
- 90% Focus Score
- 100 Hours Total

---

## FRAGMENTS (4 files)

### HomeFragment.java (Main screen - 320 lines)
**Purpose:** Start/stop sessions, view status

**What it does:**
- Start Session button
- Stop Session button
- Current session timer
- Server status indicator
- Quick stats (today's total, streak)

**Start session flow:**
```java
1. Click "Start Session"
2. POST /start → Flask server
3. POST /session/start → Firestore
4. Navigate to LiveSessionActivity
5. Start SessionForegroundService
```

**Server communication:**
```java
OkHttpClient client = new OkHttpClient();
Request request = new Request.Builder()
    .url(BASE_URL + "/start")
    .post(RequestBody.create(json, JSON))
    .build();
client.newCall(request).enqueue(callback);
```

**WebSocket (potential):**
Currently polls, but has WebSocket code for future real-time updates.

---

### AnalyticsFragment.java (Graphs - 290 lines)
**Purpose:** Visual analytics and trends

**What it does:**
- Peak focus by hour (BarChart)
- Distraction breakdown (PieChart)
- Weekly trends (LineChart)
- Average focus score
- Total study time

**Charts:**
1. **Peak Focus Bar Chart:**
   - X-axis: Hour of day (0-23)
   - Y-axis: Focus score (0-100)
   - Shows best study times

2. **Distraction Pie Chart:**
   - Phone usage: %
   - Looking away: %
   - Left desk: %
   - Other: %

3. **Weekly Trend Line:**
   - X-axis: Day of week
   - Y-axis: Average focus score
   - Shows improvement over time

**Data loading:**
```java
db.collection("sessionServer")
    .whereEqualTo("username", username)
    .whereGreaterThan("startedAt", sevenDaysAgo)
    .get()
    .addOnSuccessListener(docs -> {
        // Process and display
    });
```

---

### CountdownFragment.java (Timer - 250 lines)
**Purpose:** Pomodoro-style countdown timer

**What it does:**
- Set study duration (25, 45, 60, 90 min)
- Countdown timer
- Break reminders
- Auto-start detection when timer starts

**Timer logic:**
```java
CountDownTimer timer = new CountDownTimer(durationMs, 1000) {
    @Override
    public void onTick(long millisUntilFinished) {
        updateTimerDisplay(millisUntilFinished);
    }
    
    @Override
    public void onFinish() {
        showBreakReminder();
        stopSession();
    }
};
```

**Break reminders:**
- 25 min → 5 min break
- 45 min → 10 min break
- 60 min → 15 min break

---

### SettingsFragment.java (Configuration - 96 lines)
**Purpose:** App settings and preferences

**What it does:**
- Server IP configuration
- Notification preferences
- Logout button

**Settings stored in SharedPreferences:**
```java
SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
prefs.edit()
    .putString("server_ip", "192.168.1.100")
    .putBoolean("distraction_alerts", true)
    .putBoolean("break_reminders", true)
    .apply();
```

**IP configuration:**
- User enters server IP (e.g., 192.168.1.100)
- Saved to SharedPreferences
- Used by all activities for API calls

---

## HELPER CLASSES (7 files)

### NotificationHelper.java (Notifications - 210 lines)
**Purpose:** Create and show notifications

**What it does:**
- Session start/stop notifications
- Distraction alerts
- Break reminders
- Achievement unlocked

**Notification channels:**
```java
CHANNEL_SESSION = "session_notifications"
CHANNEL_DISTRACTION = "distraction_alerts"
CHANNEL_ACHIEVEMENT = "achievements"
```

**Show distraction alert:**
```java
NotificationHelper.showWarning(
    context,
    "Distraction Detected",
    "Phone usage detected. Stay focused!"
);
```

**Importance levels:**
- Session: LOW (silent)
- Distraction: HIGH (sound + vibrate)
- Achievement: DEFAULT (sound)

---

### AppUsageMonitor.java (Phone monitoring - 150 lines)
**Purpose:** Detect phone app switches during study

**What it does:**
- Monitors app usage using UsageStatsManager
- Detects when user switches to other apps
- Triggers notification after 5 seconds
- Logs distraction to Firestore

**How it works:**
```java
UsageStatsManager usm = getSystemService(USAGE_STATS_SERVICE);
List<UsageStats> stats = usm.queryUsageStats(
    INTERVAL_DAILY,
    startTime,
    endTime
);

// Check if user switched apps
for (UsageStats stat : stats) {
    if (stat.getLastTimeUsed() > sessionStartTime) {
        if (!stat.getPackageName().equals(ourPackage)) {
            // User switched to another app
            onDistractionDetected();
        }
    }
}
```

**Monitored apps:**
- Social media (Instagram, TikTok, Facebook)
- Messaging (WhatsApp, Messenger)
- Games
- Any app except Study Tracker

---

### UsageStatsHelper.java (Permission - 35 lines)
**Purpose:** Request Usage Stats permission

**What it does:**
- Checks if permission granted
- Opens system settings to grant permission
- Required for AppUsageMonitor

**Permission check:**
```java
public static boolean hasUsageStatsPermission(Context context) {
    AppOpsManager appOps = (AppOpsManager) 
        context.getSystemService(Context.APP_OPS_SERVICE);
    int mode = appOps.checkOpNoThrow(
        OPSTR_GET_USAGE_STATS,
        android.os.Process.myUid(),
        context.getPackageName()
    );
    return mode == AppOpsManager.MODE_ALLOWED;
}
```

**Request permission:**
```java
Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
startActivity(intent);
```

---

### IPHelper.java (Network - 35 lines)
**Purpose:** Get device IP address

**What it does:**
- Finds device's local IP on WiFi network
- Used for server connection
- Filters out loopback and IPv6

**Get IP:**
```java
public static String getIPAddress() {
    for (NetworkInterface intf : NetworkInterface.getNetworkInterfaces()) {
        for (InetAddress addr : intf.getInetAddresses()) {
            if (!addr.isLoopbackAddress() && addr instanceof Inet4Address) {
                return addr.getHostAddress();  // e.g., "192.168.1.50"
            }
        }
    }
    return "0.0.0.0";
}
```

---

### AchievementManager.java (Gamification - 120 lines)
**Purpose:** Track and unlock achievements

**What it does:**
- Checks session milestones
- Unlocks achievements
- Shows notifications
- Saves to SharedPreferences

**Achievements:**
```java
FIRST_SESSION: 1 session
TEN_SESSIONS: 10 sessions
FIFTY_SESSIONS: 50 sessions
SEVEN_DAY_STREAK: 7 consecutive days
THIRTY_DAY_STREAK: 30 consecutive days
HIGH_FOCUS: 90%+ focus score
HUNDRED_HOURS: 100 hours total
```

**Check and unlock:**
```java
if (totalSessions == 10 && !isUnlocked("TEN_SESSIONS")) {
    unlockAchievement("TEN_SESSIONS");
    showNotification("Achievement Unlocked: 10 Sessions!");
}
```

---

### BreakReminderReceiver.java (Broadcast - 50 lines)
**Purpose:** Handle break reminder alarms

**What it does:**
- Receives AlarmManager broadcasts
- Shows break reminder notification
- Triggered after study duration

**Register alarm:**
```java
AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
Intent intent = new Intent(this, BreakReminderReceiver.class);
PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
```

---

## DATABASE CLASSES (2 files)

### SessionDatabase.java (Local storage - 80 lines)
**Purpose:** SQLite database for offline session storage

**What it does:**
- Stores sessions locally (backup)
- Syncs with Firestore when online
- Provides offline access

**Schema:**
```sql
CREATE TABLE sessions (
    id INTEGER PRIMARY KEY,
    session_id TEXT,
    username TEXT,
    start_time INTEGER,
    end_time INTEGER,
    duration INTEGER,
    focus_score INTEGER,
    synced INTEGER DEFAULT 0
);
```

**CRUD operations:**
```java
insertSession(session)
getSessionById(id)
getAllSessions()
updateSession(session)
deleteSession(id)
```

---

### UserDatabase.java (User data - 60 lines)
**Purpose:** SQLite database for user profile

**What it does:**
- Stores user preferences
- Caches user data
- Tracks local statistics

**Schema:**
```sql
CREATE TABLE users (
    id INTEGER PRIMARY KEY,
    user_id TEXT,
    username TEXT,
    email TEXT,
    total_sessions INTEGER,
    total_study_time INTEGER,
    current_streak INTEGER
);
```

---

## SERVICES (2 files)

### SessionForegroundService.java (Background - 110 lines)
**Purpose:** Keep session running in background

**What it does:**
- Shows persistent notification
- Updates timer every second
- Prevents Android from killing app
- Monitors phone usage in background

**Foreground notification:**
```java
Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
    .setContentTitle("Study Session Active")
    .setContentText("Elapsed: " + formatTime(seconds))
    .setSmallIcon(R.drawable.ic_timer)
    .setOngoing(true)  // Can't be swiped away
    .build();

startForeground(NOTIFICATION_ID, notification);
```

**Timer update:**
```java
timer = new Timer();
timer.scheduleAtFixedRate(new TimerTask() {
    @Override
    public void run() {
        seconds++;
        updateNotification(formatTime(seconds));
    }
}, 0, 1000);  // Every 1 second
```

**Actions:**
- START: Start timer and foreground service
- STOP: Stop timer and remove notification
- PAUSE: Pause timer
- RESUME: Resume timer

---

## RESOURCES (XML FILES)

### AndroidManifest.xml (App configuration)
**Permissions:**
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.PACKAGE_USAGE_STATS" />
```

**Activities declared:**
- MainActivity (launcher)
- LoginActivity
- SignupActivity
- LiveSessionActivity
- StatsActivity
- AchievementsActivity

**Services:**
- SessionForegroundService

**Receivers:**
- BreakReminderReceiver

---

### Layout Files (11 files)

**activity_main.xml:**
- FrameLayout: Fragment container
- BottomNavigationView: Navigation tabs

**fragment_home.xml:**
- CardView: Server status
- Button: Start/Stop session
- TextView: Current session time
- RecyclerView: Recent sessions

**fragment_analytics.xml:**
- BarChart: Peak focus by hour
- PieChart: Distraction breakdown
- LineChart: Weekly trends
- TextViews: Statistics

**activity_live_session.xml:**
- PieChart: Focus ring (circular progress)
- TextView: Focus score (large number)
- TextView: Session time
- CardView: Distraction warning (red, animated)
- Buttons: Pause, End

---

### Drawable Files (15 files)

**Icons:**
- outline_home_24.xml: Home tab icon
- outline_analytics_24.xml: Analytics tab icon
- outline_display_settings_24.xml: Settings tab icon
- baseline_person_24.xml: User icon
- baseline_lock_24.xml: Password icon

**Buttons:**
- round_button_green.xml: Start button (green, rounded)
- round_button_red.xml: Stop button (red, rounded)
- custom_edittext.xml: Input field styling

---

### Values Files (5 files)

**colors.xml:**
```xml
<color name="primary">#6200EE</color>
<color name="primary_dark">#3700B3</color>
<color name="accent">#03DAC5</color>
<color name="focused">#4CAF50</color>
<color name="distracted">#F44336</color>
```

**strings.xml:**
```xml
<string name="app_name">Study Focus Tracker</string>
<string name="start_session">Start Session</string>
<string name="stop_session">Stop Session</string>
<string name="focus_score">Focus Score</string>
```

**themes.xml:**
- Material Design theme
- Light/dark mode support
- Custom color scheme

---

## BUILD CONFIGURATION

### build.gradle.kts (App level)
**Dependencies:**
```kotlin
dependencies {
    // Firebase
    implementation("com.google.firebase:firebase-auth:22.1.1")
    implementation("com.google.firebase:firebase-firestore:24.7.1")
    
    // Networking
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    
    // Charts
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    
    // Material Design
    implementation("com.google.android.material:material:1.9.0")
}
```

**Build config:**
```kotlin
android {
    compileSdk = 34
    defaultConfig {
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
}
```

---

## KEY ANDROID CONCEPTS

### Activity Lifecycle
```
onCreate() → onStart() → onResume() → [RUNNING]
onPause() → onStop() → onDestroy()
```

**What we do in each:**
- onCreate(): Initialize UI, load data
- onResume(): Start polling, resume timers
- onPause(): Stop polling, pause timers
- onDestroy(): Cleanup, cancel requests

### Fragment Lifecycle
```
onAttach() → onCreate() → onCreateView() → onViewCreated()
→ onStart() → onResume() → [RUNNING]
```

**What we do:**
- onCreateView(): Inflate layout
- onViewCreated(): Initialize views, set listeners
- onResume(): Refresh data

### Foreground Service
- Required for background tasks (Android 8+)
- Must show persistent notification
- Can't be killed by system
- Used for: Session timer, phone monitoring

### SharedPreferences
- Key-value storage
- Persists across app restarts
- Used for: Settings, user preferences
- Fast, synchronous access

### Firebase Firestore
- NoSQL cloud database
- Real-time sync
- Offline support
- Used for: Sessions, user data

---

See TECH_GUIDE_PART3.md for integration details.
