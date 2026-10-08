package com.golda.patchertiktok;

import android.content.Context;
import android.os.Build;

/**
 * GkteTok — Material You (sistem) rengi.
 *
 * Android 12+ telefonlarda duvar kağıdından türetilen sistem vurgu renkleri
 * (`android.R.color.system_accent1_500` / `system_accent2_500`) okunur ve vurgu
 * paleti olarak kullanılır. Böylece TikTok arayüzü telefonun temasıyla uyumlu olur.
 *
 * Renk bir kez okunur ve önbelleğe alınır (her renk çağrısında disk erişimi olmasın).
 * Android sürümü eskiyse ya da renkler okunamazsa `null` döner; o durumda vurgu rengi
 * turkuaz paletine düşer ve durum günlüğe yazılır.
 */
final class DynamicColor {
    private static volatile int[] cached;
    private static volatile boolean asked;

    private DynamicColor() { }

    /** {ana, ikincil} ya da okunamadıysa null. */
    static int[] accent(Context context) {
        if (asked) return cached;
        asked = true;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null;
        try {
            Context app = context == null ? null : context.getApplicationContext();
            if (app == null) return null;
            int primary = app.getColor(android.R.color.system_accent1_500);
            int secondary = app.getColor(android.R.color.system_accent2_500);
            cached = new int[]{primary, secondary};
            RuntimeLog.log(String.format("accent theme: system color=#%06X", primary & 0xFFFFFF));
        } catch (Throwable error) {
            RuntimeLog.log("accent theme: system color unavailable: " + error.getClass().getSimpleName());
            cached = null;
        }
        return cached;
    }

    /** Uygulama bağlamı yoksa Android'in kendi kaynaklarından okumayı dener. */
    static int[] accent() {
        return accent(currentContext());
    }

    private static Context currentContext() {
        try {
            return (Context) Class.forName("android.app.ActivityThread")
                    .getMethod("currentApplication").invoke(null);
        } catch (Throwable error) {
            return null;
        }
    }
}
