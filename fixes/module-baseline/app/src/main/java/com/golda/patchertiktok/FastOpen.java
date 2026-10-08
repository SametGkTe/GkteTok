package com.golda.patchertiktok;

import java.lang.reflect.Method;
import java.util.Locale;

/**
 * GkteTok — açılış hızlandırma (deneysel).
 *
 * TikTok'un kendi açılış ekranı bir süre beklemek zorunda kalabilir. Bu özellik,
 * açılış sınıfında **süre döndüren** metotları bulup 0 yapar. Yaşam döngüsüne
 * dokunulmaz (aktivite bitirilmez, atlanmaz) — yalnızca süre değeri sıfırlanır,
 * bu yüzden TikTok'un açılış akışını bozmaz.
 *
 * Sınıf/metot adları sürüme göre değişebilir; bulunan her şey "Sağlık" ekranına ve
 * günlüğe yazılır. Hiçbiri bulunamazsa özellik sessizce devre dışı kalır.
 */
final class FastOpen {
    private static final String[] CANDIDATES = {
            "com.ss.android.ugc.aweme.splash.SplashActivity",
            "com.ss.android.ugc.aweme.splash.SplashActivityV2",
            "com.ss.android.ugc.aweme.splash.TiktokSplashActivity",
            "com.ss.android.ugc.aweme.splash.HotSplashActivity",
    };

    private static final String[] DURATION_METHODS = {
            "getSplashDuration", "getSplashDelay", "getShowDuration", "getSplashShowTime",
            "getDelayTime", "getMinimumShowTime", "splashDuration",
    };

    private FastOpen() { }

    static void install(ClassLoader loader) {
        int hooks = 0;
        String found = "";
        for (String name : CANDIDATES) {
            Class<?> type = Hooks.findClass(name, loader);
            if (type == null) continue;
            for (Method method : type.getDeclaredMethods()) {
                if (!isDuration(method)) continue;
                Hooks.hook(method, new Hooks.Hook() {
                    @Override protected void after(Hooks.Call call) {
                        if (!Prefs.on(Prefs.FAST_OPEN)) return;
                        Object result = call.getResult();
                        if (result instanceof Integer) call.setResult(0);
                        else if (result instanceof Long) call.setResult(0L);
                    }
                });
                hooks++;
                if (found.isEmpty()) found = name + "#" + method.getName();
            }
            break; // İlk bulunan açılış sınıfı yeterli.
        }
        RuntimeLog.log("fast open: hooks=" + hooks
                + (hooks == 0 ? " (acilis sinifi bulunamadi)" : " " + found));
    }

    private static boolean isDuration(Method method) {
        if (method.getParameterTypes().length != 0) return false;
        Class<?> type = method.getReturnType();
        if (type != int.class && type != long.class) return false;
        String name = method.getName().toLowerCase(Locale.ROOT);
        for (String candidate : DURATION_METHODS) {
            if (candidate.toLowerCase(Locale.ROOT).equals(name)) return true;
        }
        return false;
    }
}
