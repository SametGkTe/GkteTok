package com.golda.patchertiktok;

import android.app.Application;
import android.content.res.Resources;
import android.util.DisplayMetrics;

/**
 * GkteTok — yazı boyutu ölçeği.
 *
 * Tüm arayüz yazılarını büyütmek/küçültmek için tek tek TextView'ları değil,
 * sürecin **yoğunluk ölçeğini** (scaledDensity) ayarlıyoruz: sp → px dönüşümü
 * her yerde bu değerden geçtiği için XML'den gelen yazılar da ölçeklenir.
 *
 * İki incelik:
 *   • Katlanarak büyümeyi önlemek için uygulanan katsayı saklanır; yeni değer
 *     eskisine bölünerek taban yoğunluk yeniden kurulur (`scaled = base * factor`).
 *   • Yapılandırma değişiminde Android yoğunluğu baştan kurar; o an katsayı
 *     sıfırlanıp yeniden uygulanır.
 */
final class FontScale {
    private static volatile Resources resources;
    private static volatile float applied = 1f;

    private FontScale() { }

    /** Yüzde → katsayı (100 = varsayılan). */
    static float factor(int percent) {
        return percent <= 0 ? 1f : percent / 100f;
    }

    /**
     * Yeni yoğunluğu hesaplar: önce uygulanmış katsayı geri alınır, sonra yenisi uygulanır.
     * Saf fonksiyon — birim testlerle doğrulanır (bkz. FontScaleTest).
     */
    static float target(float currentScaled, float alreadyApplied, float factor) {
        if (alreadyApplied <= 0f) alreadyApplied = 1f;
        return (currentScaled / alreadyApplied) * factor;
    }

    static void install(Application app) {
        resources = app.getResources();
        apply();
        try {
            Hooks.hookAll(Resources.class, "updateConfiguration", new Hooks.Hook() {
                @Override protected void after(Hooks.Call call) {
                    if (call.thisObject != resources) return;
                    // Android yoğunluğu yeniden kurdu: katsayı sıfırdan uygulanır.
                    applied = 1f;
                    apply();
                }
            });
            Prefs.listen(FontScale::apply);
            RuntimeLog.log("font scale: " + Prefs.fontScale() + "%");
        } catch (Throwable error) {
            RuntimeLog.log("font scale: cannot watch configuration: " + error);
        }
    }

    static void apply() {
        Resources target = resources;
        if (target == null) return;
        float factor = factor(Prefs.fontScale());
        float already = applied;
        if (Math.abs(factor - already) < 0.001f) return;
        try {
            DisplayMetrics metrics = target.getDisplayMetrics();
            if (metrics == null) return;
            metrics.scaledDensity = target(metrics.scaledDensity, already, factor);
            applied = factor;
            RuntimeLog.log("font scale applied: " + Math.round(factor * 100) + "%");
        } catch (Throwable error) {
            RuntimeLog.log("font scale failed: " + error);
        }
    }
}
