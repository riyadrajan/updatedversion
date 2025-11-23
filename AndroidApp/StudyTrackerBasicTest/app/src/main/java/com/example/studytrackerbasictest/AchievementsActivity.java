package com.example.studytrackerbasictest;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class AchievementsActivity extends AppCompatActivity {

    private TextView achievement1Title, achievement1Desc, achievement1Status;
    private TextView achievement2Title, achievement2Desc, achievement2Status;
    private TextView achievement3Title, achievement3Desc, achievement3Status;
    private TextView achievement4Title, achievement4Desc, achievement4Status;
    private ImageView achievement1Icon, achievement2Icon, achievement3Icon, achievement4Icon;

    private static final String PREFS_NAME = "AppPrefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_achievements);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Achievements");
        }

        // Initialize views
        achievement1Title = findViewById(R.id.achievement1Title);
        achievement1Desc = findViewById(R.id.achievement1Desc);
        achievement1Status = findViewById(R.id.achievement1Status);
        achievement1Icon = findViewById(R.id.achievement1Icon);

        achievement2Title = findViewById(R.id.achievement2Title);
        achievement2Desc = findViewById(R.id.achievement2Desc);
        achievement2Status = findViewById(R.id.achievement2Status);
        achievement2Icon = findViewById(R.id.achievement2Icon);

        achievement3Title = findViewById(R.id.achievement3Title);
        achievement3Desc = findViewById(R.id.achievement3Desc);
        achievement3Status = findViewById(R.id.achievement3Status);
        achievement3Icon = findViewById(R.id.achievement3Icon);

        achievement4Title = findViewById(R.id.achievement4Title);
        achievement4Desc = findViewById(R.id.achievement4Desc);
        achievement4Status = findViewById(R.id.achievement4Status);
        achievement4Icon = findViewById(R.id.achievement4Icon);

        loadAchievements();
    }

    private void loadAchievements() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Achievement 1: First Session
        boolean firstSession = prefs.getBoolean("achievement_first_session", false);
        achievement1Title.setText("First Steps");
        achievement1Desc.setText("Complete your first study session");
        achievement1Status.setText(firstSession ? "Unlocked" : "Locked");
        achievement1Status.setTextColor(firstSession ? 0xFF4CAF50 : 0xFF999999);

        // Achievement 2: 5 Sessions
        boolean fiveSessions = prefs.getBoolean("achievement_5_sessions", false);
        achievement2Title.setText("Getting Started");
        achievement2Desc.setText("Complete 5 study sessions");
        achievement2Status.setText(fiveSessions ? "Unlocked" : "Locked");
        achievement2Status.setTextColor(fiveSessions ? 0xFF4CAF50 : 0xFF999999);

        // Achievement 3: 10 Hours
        boolean tenHours = prefs.getBoolean("achievement_10_hours", false);
        achievement3Title.setText("Time Master");
        achievement3Desc.setText("Study for 10 total hours");
        achievement3Status.setText(tenHours ? "Unlocked" : "Locked");
        achievement3Status.setTextColor(tenHours ? 0xFF4CAF50 : 0xFF999999);

        // Achievement 4: 50 Hours
        boolean fiftyHours = prefs.getBoolean("achievement_50_hours", false);
        achievement4Title.setText("Study Legend");
        achievement4Desc.setText("Study for 50 total hours");
        achievement4Status.setText(fiftyHours ? "Unlocked" : "Locked");
        achievement4Status.setTextColor(fiftyHours ? 0xFF4CAF50 : 0xFF999999);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
