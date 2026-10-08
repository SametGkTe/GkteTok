package com.golda.patchertiktok;

/**
 * GkteTok — gece zamanlayıcısı.
 *
 * Belirlenen saat aralığında AMOLED kendiliğinden devreye girer. Saf saat mantığı
 * ayrı tutulur (bkz. NightTest) çünkü gece yarısını aşan aralıklar (22 → 07) kolayca
 * yanlış yazılır.
 */
final class Night {
    private static volatile long checkedMinute = -1;
    private static volatile boolean checkedValue;

    private Night() { }

    /**
     * {@code hour} bu aralıkta mı? {@code from == to} "gün boyu" sayılır.
     * Aralık gece yarısını aşabilir: 22 → 7 demek 22,23,0,…,6 saatleri.
     */
    static boolean inWindow(int hour, int from, int to) {
        if (from == to) return true;
        if (from < to) return hour >= from && hour < to;
        return hour >= from || hour < to;
    }

    /** Şu an gece penceresi içinde miyiz? Sonuç dakika başına bir kez hesaplanır. */
    static boolean activeNow() {
        if (!Prefs.on(Prefs.NIGHT)) return false;
        long minute = System.currentTimeMillis() / 60_000L;
        if (minute != checkedMinute) {
            checkedMinute = minute;
            checkedValue = inWindow(currentHour(), Prefs.nightFrom(), Prefs.nightTo());
        }
        return checkedValue;
    }

    private static int currentHour() {
        return java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
    }
}
