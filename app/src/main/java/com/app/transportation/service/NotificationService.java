package com.app.transportation.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.app.transportation.core.AlarmReceiver;

public class NotificationService  extends Service {
        public static PendingIntent pendingIntent = null;

        private void serviceMessageStart() {

            Intent alarmIntent = new Intent(NotificationService.this, AlarmReceiver.class);
            pendingIntent = PendingIntent.getBroadcast(NotificationService.this, 0, alarmIntent, PendingIntent.FLAG_CANCEL_CURRENT);

            if (pendingIntent != null) {
                AlarmManager manager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                manager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 3 * 1000, 3000, pendingIntent);
            }
        }

        @Nullable
        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }

        public void onCreate() {
            super.onCreate();


            serviceMessageStart();
        }

        public void onDestroy() {
            super.onDestroy();
        }
    }