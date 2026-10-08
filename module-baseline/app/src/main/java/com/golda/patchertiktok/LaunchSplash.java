package com.golda.patchertiktok;

import android.app.Activity;
import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * GkteTok — açılış ekranı.
 *
 * TikTok açıldığında, süreç başına bir kez, kısa süreliğine markalı bir katman gösterir
 * ve yumuşakça kaybolur. Tasarım gereği "güvenli": her adım try/catch içinde, katman
 * dokunmaları engellemez (üzerinden aşağıya geçer) ve bir hata olursa TikTok normal
 * şekilde açılmaya devam eder.
 *
 * Kurallar:
 *   • Yalnızca ana süreçte kurulur ve yalnızca ayar açıkken gösterilir.
 *   • Yalnızca süreç başladıktan sonraki ilk saniyelerde açılan ilk Activity'de.
 *   • O Activity hızlıca kapanırsa (TikTok'un kendi açılış ekranı böyle davranabiliyor)
 *     bir sonraki açılan ekranda tekrar denenir — çift gösterim olmaz.
 */
final class LaunchSplash {
    /** TikTok bu süre içinde açılmazsa açılış ekranı hiç gösterilmez (geç açılmalarda karışmasın). */
    private static final long OPEN_WINDOW_MS = 6000;
    /** Pencere çizilsin diye küçük bir gecikme. */
    private static final long DELAY_MS = 120;
    /** Varsayılan kalma süresi (ayarlardan değiştirilebilir). */
    private static final long DEFAULT_HOLD_MS = 1100;
    /** Kaybolma animasyonu. */
    private static final long FADE_MS = 380;
    private static final int BACKGROUND_TOP = 0xFF101216;
    private static final int BACKGROUND_BOTTOM = 0xFF05060A;
    private static final int TITLE_COLOR = 0xFFFFFFFF;
    private static final int SUBTITLE_COLOR = 0xFF8A8F98;

    private static volatile boolean shown;
    private static volatile boolean busy;
    private static volatile long startedAt;

    private LaunchSplash() { }

    static void install(Application app) {
        startedAt = SystemClock.uptimeMillis();
        app.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override public void onActivityResumed(Activity activity) {
                if (shown || busy) return;
                if (!Prefs.on(Prefs.SPLASH)) return;
                if (SystemClock.uptimeMillis() - startedAt > OPEN_WINDOW_MS) return;
                View decor = activity.getWindow() == null ? null : activity.getWindow().getDecorView();
                if (decor == null) return;
                busy = true;
                decor.postDelayed(() -> {
                    busy = false;
                    // TikTok'un kendi açılış ekranı bu arada kapanmış olabilir; o zaman bekle.
                    if (activity.isFinishing() || activity.isDestroyed()) return;
                    shown = true;
                    show(activity, decor);
                }, DELAY_MS);
            }

            @Override public void onActivityCreated(Activity activity, Bundle state) { }
            @Override public void onActivityStarted(Activity activity) { }
            @Override public void onActivityPaused(Activity activity) { }
            @Override public void onActivityStopped(Activity activity) { }
            @Override public void onActivitySaveInstanceState(Activity activity, Bundle state) { }
            @Override public void onActivityDestroyed(Activity activity) { }
        });
        RuntimeLog.log("gkte splash: watching first screen");
    }

    private static void show(Activity activity, View decor) {
        try {
            if (!(decor instanceof ViewGroup)) return;
            ViewGroup parent = (ViewGroup) decor;

            LinearLayout column = new LinearLayout(activity);
            column.setOrientation(LinearLayout.VERTICAL);
            column.setGravity(Gravity.CENTER);

            // GkTeLogo.png varsa yazının üstünde gösterilir; yoksa ekran yalnızca yazıyla açılır.
            Bitmap logo = Logo.load(activity);
            if (logo != null) {
                ImageView mark = new ImageView(activity);
                mark.setImageBitmap(logo);
                mark.setAdjustViewBounds(true);
                mark.setScaleType(ImageView.ScaleType.FIT_CENTER);
                LinearLayout.LayoutParams markParams =
                        new LinearLayout.LayoutParams(dp(activity, 240), dp(activity, 96));
                markParams.bottomMargin = dp(activity, 18);
                column.addView(mark, markParams);
            }

            TextView title = new TextView(activity);
            title.setText("GkteTok");
            title.setTextColor(TITLE_COLOR);
            title.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
            title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 34f);
            title.setLetterSpacing(0.04f);
            title.setGravity(Gravity.CENTER);
            column.addView(title);

            TextView subtitle = new TextView(activity);
            subtitle.setText("GkTe Tool");
            subtitle.setTextColor(SUBTITLE_COLOR);
            subtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
            subtitle.setLetterSpacing(0.22f);
            subtitle.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(-2, -2);
            subtitleParams.topMargin = dp(activity, 8);
            column.addView(subtitle, subtitleParams);

            FrameLayout overlay = new FrameLayout(activity);
            overlay.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{BACKGROUND_TOP, BACKGROUND_BOTTOM}));
            overlay.setClickable(false);
            overlay.setFocusable(false);
            overlay.addView(column, new FrameLayout.LayoutParams(-1, -1));

            parent.addView(overlay, new ViewGroup.LayoutParams(-1, -1));
            animateIn(overlay, activity);
            long hold = Prefs.splashMs() > 0 ? Prefs.splashMs() : DEFAULT_HOLD_MS;
            overlay.postDelayed(() -> overlay.animate().alpha(0f).setDuration(FADE_MS)
                    .withEndAction(() -> dismiss(parent, overlay)).start(), hold);
            RuntimeLog.log("gkte splash: shown");
        } catch (Throwable error) {
            RuntimeLog.log("gkte splash failed: " + error);
        }
    }

    /** Giriş animasyonu: fade / zoom / slide / none (ayarlardan seçilir). */
    private static void animateIn(View overlay, Activity activity) {
        String anim = Prefs.splashAnim();
        if ("none".equals(anim)) {
            overlay.setAlpha(1f);
            return;
        }
        overlay.setAlpha(0f);
        if ("zoom".equals(anim)) {
            overlay.setScaleX(1.06f);
            overlay.setScaleY(1.06f);
            overlay.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(260).start();
        } else if ("slide".equals(anim)) {
            overlay.setTranslationY(dp(activity, 40));
            overlay.animate().alpha(1f).translationY(0f).setDuration(260).start();
        } else {
            overlay.animate().alpha(1f).setDuration(220).start();
        }
    }

    private static void dismiss(ViewGroup parent, View overlay) {
        try {
            parent.removeView(overlay);
        } catch (Throwable ignored) { }
    }

    private static int dp(Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
