package com.example.studytrackerbasictest;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BreakReminderReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        NotificationHelper.showBreakReminder(context);
    }
}
