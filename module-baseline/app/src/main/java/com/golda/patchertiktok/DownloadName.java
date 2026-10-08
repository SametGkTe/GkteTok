package com.golda.patchertiktok;

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * GkteTok — indirme dosya adı düzeni.
 *
 * TikTok indirmeleri `com.ss.android.socialbase.downloader` kütüphanesinden geçer ve
 * dosya adı orada belirlenir. Adı belirleyen metot bulunup **başına tarih** eklenir:
 *
 *     video.mp4  →  2026-10-04_video.mp4
 *
 * Aynı gün içinde indirilenler tarih sırasına girer. Metot bulunamazsa özellik
 * sessizce devre dışı kalır (günlüğe yazılır).
 */
final class DownloadName {
    private static final String INFO = "com.ss.android.socialbase.downloader.model.DownloadInfo";
    private static final String BUILDER = "com.ss.android.socialbase.downloader.model.DownloadTask$Builder";

    private DownloadName() { }

    /** Dosya adına tarih öneki ekler; zaten varsa dokunmaz. (saf fonksiyon) */
    static String prefix(String name, String stamp) {
        if (name == null || name.isEmpty()) return name;
        if (stamp == null || stamp.isEmpty()) return name;
        return name.startsWith(stamp) ? name : stamp + "_" + name;
    }

    static void install(ClassLoader loader) {
        int hooks = 0;
        Class<?> info = Hooks.findClass(INFO, loader);
        if (info != null) hooks += hook(info, "setName");
        Class<?> builder = Hooks.findClass(BUILDER, loader);
        if (builder != null) hooks += hook(builder, "name");
        RuntimeLog.log("download name: hooks=" + hooks);
    }

    private static int hook(Class<?> type, String method) {
        int count = 0;
        for (Method candidate : type.getDeclaredMethods()) {
            if (!candidate.getName().equals(method)) continue;
            Class<?>[] parameters = candidate.getParameterTypes();
            if (parameters.length != 1 || parameters[0] != String.class) continue;
            Hooks.hook(candidate, new Hooks.Hook() {
                @Override protected void before(Hooks.Call call) {
                    if (!Prefs.on(Prefs.DOWNLOAD_NAME)) return;
                    if (call.args.length == 0 || !(call.args[0] instanceof String)) return;
                    String stamp = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
                    call.args[0] = prefix((String) call.args[0], stamp);
                }
            });
            count++;
        }
        return count;
    }
}
