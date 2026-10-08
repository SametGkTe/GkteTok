package com.golda.patchertiktok;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

/**
 * GkteTok — günlük özet bildirimi.
 *
 * Günde bir kez (en fazla her 20 saatte bir) kısa bir bildirim gösterilir:
 * "Bugün 24 reklam engellendi". Bildirim TikTok sürecinden çıktığı için Android
 * onu TikTok'un bildirimi olarak gösterir; kullanıcı bunu bilerek açar.
 */
final class Summary {
    private static final String CHANNEL = "gktetok_ozet";
    private static final long MIN_GAP_MS = 20L * 3600_000L;
    private static final int ID = 1001;

    private Summary() { }

    static void install(Context context) {
        try {
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager == null) return;
            if (android.os.Build.VERSION.SDK_INT >= 26) {
                NotificationChannel channel = new NotificationChannel(CHANNEL, "GkteTok",
                        NotificationManager.IMPORTANCE_LOW);
                channel.setDescription("Gunluk ozet");
                manager.createNotificationChannel(channel);
            }
            RuntimeLog.log("summary: channel ready");
        } catch (Throwable error) {
            RuntimeLog.log("summary: channel failed: " + error);
        }
    }

    static void maybePost(Context context) {
        try {
            if (!Prefs.on(Prefs.SUMMARY)) return;
            SharedPreferences preferences = Prefs.raw();
            if (preferences == null) return;
            long last = preferences.getLong("stat_summary", 0L);
            if (System.currentTimeMillis() - last < MIN_GAP_MS) return;
            int today = Stats.todayAds();
            if (today <= 0) return;
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager == null || !manager.areNotificationsEnabled()) return;
            Notification.Builder builder = android.os.Build.VERSION.SDK_INT >= 26
                    ? new Notification.Builder(context, CHANNEL)
                    : new Notification.Builder(context);
            builder.setSmallIcon(android.R.drawable.stat_notify_more);
            builder.setContentTitle("GkteTok");
            builder.setContentText(String.format(I18n.get(I18n.S.SUMMARY_TEXT), today));
            builder.setAutoCancel(true);
            Intent launch = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
            if (launch != null) {
                builder.setContentIntent(PendingIntent.getActivity(context, 0, launch,
                        PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT));
            }
            manager.notify(ID, builder.build());
            preferences.edit().putLong("stat_summary", System.currentTimeMillis()).apply();
            RuntimeLog.log("summary: posted (" + today + ")");
        } catch (Throwable error) {
            RuntimeLog.log("summary failed: " + error);
        }
    }
}
