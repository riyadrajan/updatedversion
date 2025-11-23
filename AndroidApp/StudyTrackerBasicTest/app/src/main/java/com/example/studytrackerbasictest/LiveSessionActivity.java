package com.example.studytrackerbasictest;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;

import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class LiveSessionActivity extends AppCompatActivity {

    private PieChart focusRing;
    private TextView focusScoreText, focusedStreakText, todayTotalText, sessionTimeText;
    private Button pauseBtn, endBtn;
    private CardView distractionWarning;
    private TextView warningText;

    private OkHttpClient client;
    private String BASE_URL;
    private Handler handler = new Handler();
    private Handler warningHandler = new Handler();
    
    private boolean isPaused = false;
    private long pausedAtMs = 0;
    private long totalPausedMs = 0;
    private long sessionStartTime = 0;

    private static final int POLL_INTERVAL_MS = 2000; // Poll every 2 seconds
    private static final int WARNING_DISMISS_MS = 7000; // 7 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Full screen immersive mode
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );
        
        setContentView(R.layout.activity_live_session);

        initViews();
        setupServer();
        setupChart();
        setupButtons();
        checkUsageStatsPermission();
        
        sessionStartTime = System.currentTimeMillis();
        startPolling();
    }
    
    private void checkUsageStatsPermission() {
        if (!UsageStatsHelper.hasUsageStatsPermission(this)) {
            // Show dialog explaining why we need this permission
            new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Phone Distraction Detection")
                .setMessage("To detect when you switch to other apps (like Instagram, TikTok, etc.) during your study session, we need Usage Access permission.\n\nThis helps track phone distractions accurately.")
                .setPositiveButton("Grant Permission", (dialog, which) -> {
                    UsageStatsHelper.requestUsageStatsPermission(this);
                })
                .setNegativeButton("Skip", (dialog, which) -> {
                    Toast.makeText(this, "Phone distraction detection disabled", Toast.LENGTH_SHORT).show();
                })
                .setCancelable(false)
                .show();
        }
    }

    private void initViews() {
        focusRing = findViewById(R.id.focusRing);
        focusScoreText = findViewById(R.id.focusScoreText);
        focusedStreakText = findViewById(R.id.focusedStreakText);
        todayTotalText = findViewById(R.id.todayTotalText);
        sessionTimeText = findViewById(R.id.sessionTimeText);
        pauseBtn = findViewById(R.id.pauseBtn);
        endBtn = findViewById(R.id.endBtn);
        distractionWarning = findViewById(R.id.distractionWarning);
        warningText = findViewById(R.id.warningText);
    }

    private void setupServer() {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        String savedIp = prefs.getString("server_ip", "");
        BASE_URL = savedIp.isEmpty() ? "http://10.0.2.2:3000" : "http://" + savedIp + ":3000";
        client = new OkHttpClient();
    }

    private void setupChart() {
        focusRing.setUsePercentValues(false);
        focusRing.getDescription().setEnabled(false);
        focusRing.setDrawHoleEnabled(true);
        focusRing.setHoleRadius(75f);
        focusRing.setTransparentCircleRadius(0f);
        focusRing.setDrawEntryLabels(false);
        focusRing.getLegend().setEnabled(false);
        focusRing.setRotationEnabled(false);
        focusRing.setHighlightPerTapEnabled(false);
        focusRing.setDrawCenterText(false);
    }

    private void setupButtons() {
        pauseBtn.setOnClickListener(v -> togglePause());
        endBtn.setOnClickListener(v -> endSession());
    }

    private void togglePause() {
        isPaused = !isPaused;
        if (isPaused) {
            pausedAtMs = System.currentTimeMillis();
            pauseBtn.setText("Resume");
            pauseBtn.setBackgroundTintList(getResources().getColorStateList(R.color.green_primary));
            stopPolling();
        } else {
            totalPausedMs += (System.currentTimeMillis() - pausedAtMs);
            pauseBtn.setText("Pause");
            pauseBtn.setBackgroundTintList(getResources().getColorStateList(R.color.deep_sea_blue));
            startPolling();
        }
    }

    private void endSession() {
        stopPolling();
        finish();
    }

    private void startPolling() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isPaused) {
                    fetchSessionStats();
                    handler.postDelayed(this, POLL_INTERVAL_MS);
                }
            }
        }, 0);
    }

    private void stopPolling() {
        handler.removeCallbacksAndMessages(null);
    }

    private void fetchSessionStats() {
        Request request = new Request.Builder()
                .url(BASE_URL + "/session/stats")
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> 
                    Toast.makeText(LiveSessionActivity.this, "Connection error", Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) return;

                try {
                    JSONObject json = new JSONObject(response.body().string());
                    String status = json.optString("status");
                    
                    if ("ok".equals(status)) {
                        double focusScore = json.optDouble("currentFocusScore", 100.0);
                        long focusedMs = json.optLong("focusedMs", 0);
                        long elapsedMs = json.optLong("elapsedMs", 0);
                        boolean isDistracted = json.optBoolean("isDistracted", false);

                        runOnUiThread(() -> {
                            updateUI(focusScore, focusedMs, elapsedMs);
                            if (isDistracted) {
                                showDistractionWarning();
                            }
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void updateUI(double focusScore, long focusedMs, long elapsedMs) {
        // Update focus score text
        focusScoreText.setText(String.format(Locale.getDefault(), "%.0f", focusScore));

        // Update focus ring
        updateFocusRing((float) focusScore);

        // Update focused streak
        int focusedMins = (int) (focusedMs / 60000);
        focusedStreakText.setText(String.format(Locale.getDefault(), 
            "Current Focused Streak: %d min", focusedMins));

        // Update session time
        long actualElapsed = System.currentTimeMillis() - sessionStartTime - totalPausedMs;
        int sessionMins = (int) (actualElapsed / 60000);
        int sessionSecs = (int) ((actualElapsed / 1000) % 60);
        sessionTimeText.setText(String.format(Locale.getDefault(), 
            "Session Time: %02d:%02d", sessionMins, sessionSecs));

        // Update today's total (placeholder - would need to fetch from database)
        todayTotalText.setText("Today's Total: 0h 0m");
    }

    private void updateFocusRing(float focusScore) {
        List<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(focusScore, "Focused"));
        entries.add(new PieEntry(100 - focusScore, "Distracted"));

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(Color.parseColor("#4CAF50"), Color.parseColor("#FF5252"));
        dataSet.setDrawValues(false);
        dataSet.setSliceSpace(2f);

        PieData data = new PieData(dataSet);
        focusRing.setData(data);
        focusRing.invalidate();
    }

    private void showDistractionWarning() {
        distractionWarning.setVisibility(View.VISIBLE);
        warningText.setText("⚠️ FOCUS!\nYou're looking away from your work.\nGet back to studying!");

        // Auto-dismiss after 7 seconds
        warningHandler.removeCallbacksAndMessages(null);
        warningHandler.postDelayed(() -> {
            distractionWarning.setVisibility(View.GONE);
        }, WARNING_DISMISS_MS);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopPolling();
        warningHandler.removeCallbacksAndMessages(null);
    }
}
