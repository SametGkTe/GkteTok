package com.golda.patchertiktok;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.widget.Toast;

/**
 * GkteTok — kullanım süresi uyarısı.
 *
 * TikTok ön plandayken geçen süre sayılır; seçilen aralığın her katında bir kez
 * kısa bir uyarı gösterilir (ör. 30, 60, 90. dakikada). Hepsi cihazda; hiçbir veri
 * dışarı gitmez.
 */
final class TimeLimit {
    /** Tembel kurulur: sınıf yüklenirken değil, ilk bildirimde. */
    private static Handler handler;
    private static final long TICK_MS = 60_000L;

    private static volatile Application application;
    private static long sessionStart;
    private static long accumulated;
    private static int fired;

    private static final Runnable TICK = new Runnable() {
        @Override public void run() {
            evaluate();
            main().postDelayed(this, TICK_MS);
        }
    };

    private TimeLimit() { }

    /** Tembel ana iş parçacığı: sınıf yüklenirken android çağrısı yapılmaz (JVM testleri). */
    private static Handler main() {
        Handler current = handler;
        if (current == null) {
            current = new Handler(Looper.getMainLooper());
            handler = current;
        }
        return current;
    }

    /** Aralık katı sayısı: 30 dk ayarında 61 dk → 2. Saf fonksiyon (bkz. TimeLimitTest). */
    static int dueCount(long totalMillis, int minutes) {
        if (minutes <= 0) return 0;
        return (int) (totalMillis / (minutes * 60_000L));
    }

    static void install(Application app) {
        application = app;
        app.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override public void onActivityResumed(android.app.Activity activity) {
                if (sessionStart == 0L) {
                    sessionStart = SystemClock.elapsedRealtime();
                    main().removeCallbacks(TICK);
                    main().postDelayed(TICK, TICK_MS);
                }
            }

            @Override public void onActivityPaused(android.app.Activity activity) {
                if (sessionStart != 0L) {
                    accumulated += SystemClock.elapsedRealtime() - sessionStart;
                    sessionStart = 0L;
                    main().removeCallbacks(TICK);
                }
            }

            @Override public void onActivityCreated(android.app.Activity activity, android.os.Bundle state) { }
            @Override public void onActivityStarted(android.app.Activity activity) { }
            @Override public void onActivityStopped(android.app.Activity activity) { }
            @Override public void onActivitySaveInstanceState(android.app.Activity activity, android.os.Bundle state) { }
            @Override public void onActivityDestroyed(android.app.Activity activity) { }
        });
        RuntimeLog.log("time limit: watching");
    }

    private static long total() {
        return accumulated + (sessionStart == 0L ? 0L : SystemClock.elapsedRealtime() - sessionStart);
    }

    private static void evaluate() {
        try {
            if (!Prefs.on(Prefs.TIME_LIMIT)) return;
            int minutes = Prefs.timeLimitMin();
            int due = dueCount(total(), minutes);
            if (due <= fired) return;
            fired = due;
            Application app = application;
            if (app == null) return;
            String text = String.format(I18n.get(I18n.S.TIME_LIMIT_ALERT), minutes * due);
            Toast.makeText(app, text, Toast.LENGTH_LONG).show();
            RuntimeLog.log("time limit: minute " + (minutes * due));
        } catch (Throwable error) {
            RuntimeLog.log("time limit failed: " + error);
        }
    }
}
