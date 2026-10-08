package com.golda.patchertiktok;

import android.app.Activity;
import android.content.res.Resources;
import android.os.SystemClock;
import android.view.MotionEvent;

/**
 * GkteTok — yanlışlıkla beğeni koruması.
 *
 * TikTok'ta videonun ortasına **çift dokunmak** beğeni atar; kaydırırken sık sık
 * kazara beğeni oluşur. Bu koruma, çift dokunuşun ikinci basışını yutar; beğeni
 * oluşmaz. **Kalp düğmesi etkilenmez** — orası ekranın kenarında olduğu için
 * koruma alanı dışında kalır.
 *
 * Neden `Activity.dispatchTouchEvent`? Dokunuşun uygulamaya girdiği ilk durak
 * orasıdır (Android'in sabit API'si), TikTok'un obfuscate sınıflarına bağlı
 * kalmayız. Tek parmak, orta alan ve kısa süre koşulları birlikte aranır;
 * yakınlaştırma (iki parmak) ve kenar düğmeleri korunur.
 */
final class LikeGuard {
    private static final long DOUBLE_TAP_MS = 320L;
    private static final float SLOP_DP = 60f;
    /** Koruma yalnızca videonun bulunduğu orta bölgede geçerli (ekran oranı). */
    private static final float X_MIN = 0.08f, X_MAX = 0.92f, Y_MIN = 0.16f, Y_MAX = 0.84f;

    private static volatile long lastUp;
    private static volatile float lastX, lastY;
    private static volatile boolean swallowing;

    private LikeGuard() { }

    /** İki basış arası süre ve mesafe çift dokunuş sayılır mı? (saf fonksiyon) */
    static boolean isDoubleTap(long now, long previousUp, float x, float y, float previousX, float previousY, float slop) {
        if (previousUp <= 0L || now - previousUp > DOUBLE_TAP_MS) return false;
        float dx = x - previousX;
        float dy = y - previousY;
        return dx * dx + dy * dy <= slop * slop;
    }

    /** Dokunuş videonun orta bölgesinde mi? (saf fonksiyon) */
    static boolean inVideoArea(float x, float y, int width, int height) {
        if (width <= 0 || height <= 0) return false;
        float fx = x / width;
        float fy = y / height;
        return fx >= X_MIN && fx <= X_MAX && fy >= Y_MIN && fy <= Y_MAX;
    }

    static void install(ClassLoader loader) {
        Hooks.hookAll(Activity.class, "dispatchTouchEvent", new Hooks.Hook() {
            @Override protected void before(Hooks.Call call) {
                if (!Prefs.on(Prefs.LIKE_GUARD)) {
                    swallowing = false;
                    return;
                }
                if (call.thisObject instanceof ModSettingsActivity) return;
                if (!(call.args.length >= 1 && call.args[0] instanceof MotionEvent)) return;
                MotionEvent event = (MotionEvent) call.args[0];
                if (event.getPointerCount() > 1) {
                    swallowing = false;
                    return;
                }
                int action = event.getActionMasked();
                long now = SystemClock.uptimeMillis();
                if (action == MotionEvent.ACTION_DOWN) {
                    if (swallowing) {
                        call.setResult(true);
                        return;
                    }
                    Resources system = Resources.getSystem();
                    float slop = SLOP_DP * system.getDisplayMetrics().density;
                    if (isDoubleTap(now, lastUp, event.getX(), event.getY(), lastX, lastY, slop)
                            && inVideoArea(event.getX(), event.getY(),
                            system.getDisplayMetrics().widthPixels, system.getDisplayMetrics().heightPixels)) {
                        swallowing = true;
                        call.setResult(true);
                        RuntimeLog.log("like guard: double tap blocked");
                    }
                } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                    if (swallowing) {
                        swallowing = false;
                        lastUp = 0L;
                        call.setResult(true);
                    } else {
                        lastUp = now;
                        lastX = event.getX();
                        lastY = event.getY();
                    }
                } else if (swallowing) {
                    call.setResult(true);
                }
            }
        });
        RuntimeLog.log("like guard: watching touches");
    }
}
