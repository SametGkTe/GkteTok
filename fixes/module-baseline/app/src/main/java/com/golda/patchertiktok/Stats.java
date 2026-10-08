package com.golda.patchertiktok;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import java.util.Map;
import java.util.TreeMap;

/**
 * GkteTok — sayaçlar.
 *
 * Hepsi cihazda tutulur, hiçbir veri dışarı gitmez.
 *   • stat_ads   → engellenen reklam (başlatma reklamı + akışta atlananlar)
 *   • stat_opens → TikTok kaç kez açıldı
 *   • stat_since → ilk kurulumun zamanı (kullanım günü bundan hesaplanır)
 *   • stat_days  → günlük kovalar: "gun:sayı" çiftleri (son 7 gün grafiği)
 *   • stat_mile  → ulaşılan son kilometre taşı (kutlama bir kez yapılır)
 */
final class Stats {
    static final String ADS = "stat_ads";
    static final String OPENS = "stat_opens";
    static final String SINCE = "stat_since";
    private static final String DAYS = "stat_days";
    private static final String MILE = "stat_mile";
    private static final String SUMMARY = "stat_summary";

    /** Kutlama eşikleri. */
    private static final int[] MILESTONES = {50, 100, 250, 500, 1000, 2500, 5000, 10_000, 25_000, 50_000, 100_000};

    /** Tembel kurulur: sınıf yüklenirken android çağrısı yapılmaz (JVM testleri). */
    private static volatile Handler handler;
    private static volatile Context context;

    private Stats() { }

    static void attach(Context applicationContext) {
        context = applicationContext;
    }

    // ---- yazma ------------------------------------------------------------------------------

    static synchronized void countOpen() {
        SharedPreferences preferences = Prefs.raw();
        if (preferences == null) return;
        int opens = preferences.getInt(OPENS, 0) + 1;
        SharedPreferences.Editor edit = preferences.edit().putInt(OPENS, opens);
        if (preferences.getLong(SINCE, 0L) == 0L) edit.putLong(SINCE, System.currentTimeMillis());
        edit.apply();
        RuntimeLog.log("stats: open #" + opens);
    }

    static synchronized void countAdsBlocked() {
        SharedPreferences preferences = Prefs.raw();
        if (preferences == null) return;
        int total = preferences.getInt(ADS, 0) + 1;
        long today = day();
        Map<Long, Integer> days = parseDays(preferences.getString(DAYS, ""));
        Integer current = days.get(today);
        days.put(today, current == null ? 1 : current + 1);
        prune(days, today);
        SharedPreferences.Editor edit = preferences.edit()
                .putInt(ADS, total)
                .putString(DAYS, encodeDays(days));
        int milestone = crossed(preferences.getInt(MILE, 0), total);
        if (milestone > 0) {
            edit.putInt(MILE, milestone);
            celebrate(milestone);
        }
        edit.apply();
    }

    static synchronized void reset() {
        SharedPreferences preferences = Prefs.raw();
        if (preferences == null) return;
        preferences.edit().putInt(ADS, 0).putInt(OPENS, 0).remove(SINCE)
                .remove(DAYS).remove(MILE).remove(SUMMARY).apply();
    }

    // ---- okuma ------------------------------------------------------------------------------

    static int ads() {
        SharedPreferences preferences = Prefs.raw();
        return preferences == null ? 0 : preferences.getInt(ADS, 0);
    }

    static int opens() {
        SharedPreferences preferences = Prefs.raw();
        return preferences == null ? 0 : preferences.getInt(OPENS, 0);
    }

    static long since() {
        SharedPreferences preferences = Prefs.raw();
        return preferences == null ? 0L : preferences.getLong(SINCE, 0L);
    }

    /** İlk kurulum günü 1 sayılır. */
    static int days() {
        long since = since();
        if (since == 0L) return 0;
        return (int) ((System.currentTimeMillis() - since) / 86_400_000L) + 1;
    }

    static int todayAds() {
        SharedPreferences preferences = Prefs.raw();
        if (preferences == null) return 0;
        Integer value = parseDays(preferences.getString(DAYS, "")).get(day());
        return value == null ? 0 : value;
    }

    /** Son {@code count} günün sayıları, en eskiden bugüne. */
    static int[] lastDays(int count) {
        SharedPreferences preferences = Prefs.raw();
        Map<Long, Integer> days = preferences == null
                ? new TreeMap<Long, Integer>()
                : parseDays(preferences.getString(DAYS, ""));
        long today = day();
        int[] out = new int[count];
        for (int i = 0; i < count; i++) {
            Integer value = days.get(today - (count - 1 - i));
            out[i] = value == null ? 0 : value;
        }
        return out;
    }

    // ---- saf yardımcılar (birim testlerle doğrulanır) ----------------------------------------

    /** Bugünün gün numarası (UTC gün). */
    static long day() {
        return System.currentTimeMillis() / 86_400_000L;
    }

    static Map<Long, Integer> parseDays(String text) {
        Map<Long, Integer> days = new TreeMap<>();
        if (text == null || text.isEmpty()) return days;
        for (String pair : text.split(",")) {
            int split = pair.indexOf(':');
            if (split <= 0) continue;
            try {
                days.put(Long.parseLong(pair.substring(0, split).trim()),
                        Integer.parseInt(pair.substring(split + 1).trim()));
            } catch (NumberFormatException ignored) {
                // Bozuk çift atlanır.
            }
        }
        return days;
    }

    static String encodeDays(Map<Long, Integer> days) {
        StringBuilder out = new StringBuilder();
        for (Map.Entry<Long, Integer> entry : days.entrySet()) {
            if (out.length() > 0) out.append(',');
            out.append(entry.getKey()).append(':').append(entry.getValue());
        }
        return out.toString();
    }

    /** 30 günden eski kovalar silinir. */
    static void prune(Map<Long, Integer> days, long today) {
        days.entrySet().removeIf(entry -> entry.getKey() < today - 30);
    }

    /** {@code total} sayaç, {@code previous} eşiğini aştıysa yeni eşiği döndürür (yoksa 0). */
    static int crossed(int previous, int total) {
        for (int index = MILESTONES.length - 1; index >= 0; index--) {
            int milestone = MILESTONES[index];
            if (previous < milestone && total >= milestone) return milestone;
        }
        return 0;
    }

    /** Sıradaki hedef; hepsi bittiyse son eşik döner. */
    static int nextMilestone(int ads) {
        for (int milestone : MILESTONES) {
            if (ads < milestone) return milestone;
        }
        return MILESTONES[MILESTONES.length - 1];
    }

    static synchronized void clearSummaryMark() {
        SharedPreferences preferences = Prefs.raw();
        if (preferences != null) preferences.edit().remove(SUMMARY).apply();
    }

    /** Günlük özet en son hangi gün gösterildi (0 = hiç). */
    static long summaryDay() {
        SharedPreferences preferences = Prefs.raw();
        return preferences == null ? 0L : preferences.getLong(SUMMARY, 0L);
    }

    static synchronized void markSummary(long day) {
        SharedPreferences preferences = Prefs.raw();
        if (preferences != null) preferences.edit().putLong(SUMMARY, day).apply();
    }

    private static void celebrate(int milestone) {
        Context app = context;
        if (app == null) return;
        String text = String.format(I18n.get(I18n.S.MILESTONE), milestone);
        Handler main = handler;
        if (main == null) {
            main = new Handler(Looper.getMainLooper());
            handler = main;
        }
        final Context target = app;
        main.post(() -> {
            try {
                Toast.makeText(target, text, Toast.LENGTH_LONG).show();
            } catch (Throwable ignored) { }
        });
        RuntimeLog.log("stats: milestone " + milestone);
    }
}
