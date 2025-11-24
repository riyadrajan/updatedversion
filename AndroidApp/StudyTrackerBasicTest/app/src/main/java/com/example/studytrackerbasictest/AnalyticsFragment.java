package com.example.studytrackerbasictest;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.example.studytrackerbasictest.databases.SessionDatabase;
import com.google.android.material.tabs.TabLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Collections;
import java.util.Comparator;

public class AnalyticsFragment extends Fragment {

    private TabLayout timePeriodTabs;
    private TextView avgFocusScore, streakDays, totalSessions, totalHours;
    // Charts removed (views commented out in layout)
    private ListView recentSessionsList;
    private ArrayAdapter<String> recentAdapter;
    
    private String username;
    private String currentPeriod = "today";
    private static final String PREFS_NAME = "AppPrefs";

    @Override
    public View onCreateView(LayoutInflater infl, ViewGroup parent, Bundle savedInstanceState) {
        View v = infl.inflate(R.layout.fragment_analytics, parent, false);

        initViews(v);
        setupTabs();

        // Get username
        username = getActivity().getIntent().getStringExtra("username");
        if (username == null || username.isEmpty()) {
            SharedPreferences prefs = requireActivity()
                    .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            username = prefs.getString("logged_in_user", null);
        }

        loadStats("today");
        return v;
    }

    private void initViews(View v) {
        timePeriodTabs = v.findViewById(R.id.timePeriodTabs);
        avgFocusScore = v.findViewById(R.id.avgFocusScore);
        streakDays = v.findViewById(R.id.streakDays);
        totalSessions = v.findViewById(R.id.totalSessions);
        totalHours = v.findViewById(R.id.totalHours);
        // chart views removed from layout; do not initialize them
        recentSessionsList = v.findViewById(R.id.recentSessionsList);
        // initialize adapter to show sessions as "Session N — YYYY-MM-DD — Focus X%"
        recentAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, new ArrayList<>());
        recentSessionsList.setAdapter(recentAdapter);
    }

    private void setupTabs() {
        timePeriodTabs.addTab(timePeriodTabs.newTab().setText("Today"));
        timePeriodTabs.addTab(timePeriodTabs.newTab().setText("Week"));
        timePeriodTabs.addTab(timePeriodTabs.newTab().setText("Month"));

        timePeriodTabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0: loadStats("today"); break;
                    case 1: loadStats("week"); break;
                    case 2: loadStats("month"); break;
                }
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (username != null && !username.isEmpty()) {
            loadStats(currentPeriod);   // refresh when returning here
        }
    }

    // Charts removed: setupCharts is intentionally omitted because chart views are commented out in layout

    private void loadStats(String period) {
        currentPeriod = period;
        SessionDatabase db = new SessionDatabase();
        
        db.getSessionsForUser(username, sessions -> {
            List<Map<String, Object>> filteredSessions = filterSessionsByPeriod(sessions, period);
            
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    calculateAndDisplayStats(filteredSessions);
                    updateRecentSessions(sessions);
                });
            }
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
                totalFocus += ((Number) fs).doubleValue();
                focusCount++;
            }
        }

        int hours = totalMins / 60;
        double avgFocus = focusCount > 0 ? totalFocus / focusCount : 0;
        int streak = 0; // Simplified

        avgFocusScore.setText(String.format(Locale.getDefault(), "%.0f%%", avgFocus));
        streakDays.setText(String.valueOf(streak));
        totalSessions.setText(String.valueOf(sessionCount));
        totalHours.setText(hours + "h");
    }

    // updatePeakFocusChart removed

    // updateDistractionBreakdown removed

    private void updateRecentSessions(List<Map<String, Object>> sessions) {
        // sessions are expected ordered by date desc from the DB
        recentAdapter.clear();
        for (Map<String, Object> session : sessions) {
            String name = session.get("name") != null ? (String) session.get("name") : "Session";
            String date = session.get("date") != null ? (String) session.get("date") : "";
            Object fs = session.get("focusScore");
            String fsStr = fs != null ? String.format(Locale.getDefault(), "%.0f%%", ((Number) fs).doubleValue()) : "N/A";
            String item = String.format(Locale.getDefault(), "%s — %s — Focus %s", name, date, fsStr);
            recentAdapter.add(item);
        }
        recentAdapter.notifyDataSetChanged();
    }
}
