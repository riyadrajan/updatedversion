package com.example.studytrackerbasictest;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

public class SettingsFragment extends Fragment {

    private static final String PREFS_NAME = "AppPrefs";
    private static final String KEY_IP = "server_ip";

    EditText ipInput;
    Button saveBtn, logoutBtn;
    SwitchCompat switchDistractionAlerts, switchBreakReminders, switchAchievements;

    public SettingsFragment() {}

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ipInput = view.findViewById(R.id.ipInput);
        saveBtn = view.findViewById(R.id.saveBtn);
        logoutBtn = view.findViewById(R.id.logoutBtn);
        switchDistractionAlerts = view.findViewById(R.id.switchDistractionAlerts);
        switchBreakReminders = view.findViewById(R.id.switchBreakReminders);
        switchAchievements = view.findViewById(R.id.switchAchievements);

        SharedPreferences prefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        ipInput.setText(prefs.getString(KEY_IP, ""));
        
        // Load notification preferences
        switchDistractionAlerts.setChecked(prefs.getBoolean("distraction_alerts", true));
        switchBreakReminders.setChecked(prefs.getBoolean("break_reminders", true));
        switchAchievements.setChecked(prefs.getBoolean("achievement_notifications", true));

        saveBtn.setOnClickListener(v -> {
            String ip = ipInput.getText().toString().trim();
            if (TextUtils.isEmpty(ip)) {
                Toast.makeText(requireContext(), "Enter a valid IP", Toast.LENGTH_SHORT).show();
                return;
            }
            prefs.edit().putString(KEY_IP, ip).apply();
            Toast.makeText(requireContext(), "IP saved", Toast.LENGTH_SHORT).show();
        });

        // Save notification preferences when changed
        switchDistractionAlerts.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("distraction_alerts", isChecked).apply();
            Toast.makeText(requireContext(), "Distraction alerts " + (isChecked ? "enabled" : "disabled"), Toast.LENGTH_SHORT).show();
        });

        switchBreakReminders.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("break_reminders", isChecked).apply();
            Toast.makeText(requireContext(), "Break reminders " + (isChecked ? "enabled" : "disabled"), Toast.LENGTH_SHORT).show();
        });

        switchAchievements.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("achievement_notifications", isChecked).apply();
            Toast.makeText(requireContext(), "Achievement notifications " + (isChecked ? "enabled" : "disabled"), Toast.LENGTH_SHORT).show();
        });

        logoutBtn.setOnClickListener(v -> {
            // Clear stored login state if needed
            prefs.edit().remove("logged_in_user").apply();

            Intent i = new Intent(requireContext(), LoginActivity.class);
            startActivity(i);
            requireActivity().finish();
        });
    }
}
