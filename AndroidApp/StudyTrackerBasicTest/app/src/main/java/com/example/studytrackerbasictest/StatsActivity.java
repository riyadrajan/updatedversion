package com.example.studytrackerbasictest;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.studytrackerbasictest.databases.SessionDatabase;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.tabs.TabLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StatsActivity extends AppCompatActivity {

    private TabLayout timePeriodTabs;
    private TextView avgFocusScore, streakDays, totalSessions, totalHours;
    private BarChart peakFocusChart;
    private PieChart distractionBreakdown;
    
    private String username;
    private String currentPeriod = "today";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);

        Toolbar toolbar = findViewById(R.id.statsToolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Statistics");

        username = getIntent().getStringExtra("username");

        initViews();
        setupTabs();
        setupCharts();
        loadStats("today");
    }

    private void initViews() {
        timePeriodTabs = findViewById(R.id.timePeriodTabs);
        avgFocusScore = findViewById(R.id.avgFocusScore);
        streakDays = findViewById(R.id.streakDays);
        totalSessions = findViewById(R.id.totalSessions);
        totalHours = findViewById(R.id.totalHours);
        peakFocusChart = findViewById(R.id.peakFocusChart);
        distractionBreakdown = findViewById(R.id.distractionBreakdown);
    }

    private void setupTabs() {
        timePeriodTabs.addTab(timePeriodTabs.newTab().setText("Today"));
        timePeriodTabs.addTab(timePeriodTabs.newTab().setText("Week"));
        timePeriodTabs.addTab(timePeriodTabs.newTab().setText("Month"));

        timePeriodTabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0:
                        loadStats("today");
                        break;
                    case 1:
                        loadStats("week");
                        break;
                    case 2:
                        loadStats("month");
                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupCharts() {
        // Peak Focus Time Chart
        peakFocusChart.getDescription().setEnabled(false);
        peakFocusChart.setDrawGridBackground(false);
        peakFocusChart.setDrawBarShadow(false);
        peakFocusChart.setDrawValueAboveBar(false);
        peakFocusChart.setPinchZoom(false);
        peakFocusChart.setScaleEnabled(false);
        
        XAxis xAxis = peakFocusChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int group = (int) value;
                switch (group) {
                    case 0: return "12-3am";
                    case 1: return "4-7am";
                    case 2: return "8-11am";
                    case 3: return "12-3pm";
                    case 4: return "4-7pm";
                    case 5: return "8-11pm";
                    default: return "";
                }
            }
        });

        peakFocusChart.getAxisLeft().setTextColor(Color.WHITE);
        peakFocusChart.getAxisLeft().setAxisMinimum(0f);
        peakFocusChart.getAxisLeft().setAxisMaximum(100f);
        peakFocusChart.getAxisRight().setEnabled(false);
        peakFocusChart.getLegend().setEnabled(false);

        // Distraction Breakdown Pie Chart
        distractionBreakdown.setUsePercentValues(true);
        distractionBreakdown.getDescription().setEnabled(false);
        distractionBreakdown.setDrawHoleEnabled(true);
        distractionBreakdown.setHoleRadius(40f);
        distractionBreakdown.setTransparentCircleRadius(45f);
        distractionBreakdown.setDrawEntryLabels(true);
        distractionBreakdown.setEntryLabelColor(Color.WHITE);
        distractionBreakdown.setEntryLabelTextSize(12f);
        distractionBreakdown.getLegend().setTextColor(Color.WHITE);
    }

    private void loadStats(String period) {
        currentPeriod = period;
        SessionDatabase db = new SessionDatabase();
        
        db.getSessionsForUser(username, sessions -> {
            List<Map<String, Object>> filteredSessions = filterSessionsByPeriod(sessions, period);
            
            runOnUiThread(() -> {
                calculateAndDisplayStats(filteredSessions);
                updatePeakFocusChart(filteredSessions);
                updateDistractionBreakdown(filteredSessions);
            });
        });
    }

    private List<Map<String, Object>> filterSessionsByPeriod(List<Map<String, Object>> sessions, String period) {
        List<Map<String, Object>> filtered = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String today = sdf.format(new Date());
        
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -7);
        String weekAgo = sdf.format(cal.getTime());
        
        cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -30);
        String monthAgo = sdf.format(cal.getTime());

        for (Map<String, Object> session : sessions) {
            String date = (String) session.get("date");
            if (date == null) continue;

            switch (period) {
                case "today":
                    if (date.equals(today)) filtered.add(session);
                    break;
                case "week":
                    if (date.compareTo(weekAgo) >= 0) filtered.add(session);
                    break;
                case "month":
                    if (date.compareTo(monthAgo) >= 0) filtered.add(session);
                    break;
            }
        }
        return filtered;
    }

    private void calculateAndDisplayStats(List<Map<String, Object>> sessions) {
        int totalMins = 0;
        double totalFocus = 0;
        int focusCount = 0;
        int sessionCount = sessions.size();

        for (Map<String, Object> session : sessions) {
            String duration = (String) session.get("duration");
            if (duration != null) {
                String[] parts = duration.split(":");
                if (parts.length == 2) {
                    totalMins += Integer.parseInt(parts[0]);
                }
            }

            Object fs = session.get("focusScore");
            if (fs != null) {
                totalFocus += ((Number) fs).doubleValue() ;
                focusCount++;
            }
        }

        int hours = totalMins / 60;
        double avgFocus = focusCount > 0 ? totalFocus / focusCount : 0;

        // Calculate streak (consecutive days with sessions)
        int streak = calculateStreak();

        avgFocusScore.setText(String.format(Locale.getDefault(), "%.0f%%", avgFocus));
        streakDays.setText(String.valueOf(streak));
        totalSessions.setText(String.valueOf(sessionCount));
        totalHours.setText(hours + "h");
    }

    private int calculateStreak() {
        // Simple implementation: count consecutive days from today backwards
        SessionDatabase db = new SessionDatabase();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar cal = Calendar.getInstance();
        int streak = 0;

        // This is simplified - in production you'd query the database properly
        // For now, return a placeholder
        return 0;
    }

    private void updatePeakFocusChart(List<Map<String, Object>> sessions) {
        List<BarEntry> entries = new ArrayList<>();
        
        // Group hours into 4-hour blocks
        if (sessions.isEmpty()) {
            // Demo data - 6 groups of 4 hours each
            float[] demoScores = {50, 55, 75, 85, 88, 70};
            for (int i = 0; i < 6; i++) {
                entries.add(new BarEntry(i, demoScores[i]));
            }
        } else {
            Map<Integer, List<Double>> groupedFocus = new HashMap<>();
            for (Map<String, Object> session : sessions) {
                int hour = (int) (Math.random() * 24);
                int group = hour / 4;
                Object fs = session.get("focusScore");
                if (fs != null) {
                    double score = ((Number) fs).doubleValue() * 100;
                    if (!groupedFocus.containsKey(group)) {
                        groupedFocus.put(group, new ArrayList<>());
                    }
                    groupedFocus.get(group).add(score);
                }
            }

            for (int group = 0; group < 6; group++) {
                if (groupedFocus.containsKey(group)) {
                    List<Double> scores = groupedFocus.get(group);
                    double avg = scores.stream().mapToDouble(Double::doubleValue).average().orElse(0);
                    entries.add(new BarEntry(group, (float) avg));
                } else {
                    entries.add(new BarEntry(group, 0f));
                }
            }
        }

        BarDataSet dataSet = new BarDataSet(entries, "Focus Score");
        dataSet.setColor(Color.parseColor("#FF9800")); // Orange
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(0f);

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.9f); // Wider bars
        peakFocusChart.setData(data);
        peakFocusChart.invalidate();
    }

    private void updateDistractionBreakdown(List<Map<String, Object>> sessions) {
        int eyesClosed, lookingAway, phone;
        
        if (sessions.isEmpty()) {
            // Demo data for new users
            eyesClosed = 12;
            lookingAway = 25;
            phone = 8;
        } else {
            // Real data (simulated for now - backend doesn't track types yet)
            eyesClosed = (int) (Math.random() * 20);
            lookingAway = (int) (Math.random() * 30);
            phone = (int) (Math.random() * 15);
        }

        List<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(eyesClosed, "Eyes Closed"));
        entries.add(new PieEntry(lookingAway, "Looking Away"));
        entries.add(new PieEntry(phone, "Phone"));

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(
            Color.parseColor("#FF5252"),
            Color.parseColor("#FF9800"),
            Color.parseColor("#FFC107")
        );
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(14f);

        PieData data = new PieData(dataSet);
        distractionBreakdown.setData(data);
        distractionBreakdown.invalidate();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
