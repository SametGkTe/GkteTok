package com.golda.patchertiktok;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * GkteTok — indirme seçenekleri.
 *
 * Üç şeyi yönetir (hepsi TikTok'un indirme kütüphanesi `com.ss.android.socialbase.downloader`
 * üzerindeki kancalarla):
 *
 *   1. Klasör   : kayıt yolu seçtiğin klasöre çevrilir (ör. Movies/GkteTok).
 *   2. Sadece ses: indirme adresi, o an oynatılan videonun **müzik** adresiyle değiştirilir
 *                  ve dosya adı .m4a olur.
 *   3. Kalite   : "yüksek" seçiliyse en yüksek bit hızındaki video adresi kullanılır.
 *
 * 2 ve 3, o an oynatılan videoya ihtiyaç duyar; akışta değilsen (ör. profilden indirme)
 * kural uygulanmaz ve bu durum günlüğe yazılır. Adres/klasör değişikliği TikTok
 * tarafından reddedilirse indirme TikTok'un kendi ayarlarıyla devam eder.
 */
final class DownloadPlus {
    private static final String INFO = "com.ss.android.socialbase.downloader.model.DownloadInfo";
    private static final String BUILDER = "com.ss.android.socialbase.downloader.model.DownloadTask$Builder";

    private DownloadPlus() { }

    static void install(ClassLoader loader) {
        int hooks = 0;
        Class<?> info = Hooks.findClass(INFO, loader);
        if (info != null) {
            hooks += hookString(info, "setSavePath", SavePath);
            hooks += hookString(info, "setUrl", Url);
            hooks += hookString(info, "setName", Name);
        }
        Class<?> builder = Hooks.findClass(BUILDER, loader);
        if (builder != null) {
            hooks += hookString(builder, "savePath", SavePath);
            hooks += hookString(builder, "url", Url);
            hooks += hookString(builder, "name", Name);
        }
        RuntimeLog.log("download plus: hooks=" + hooks + " folder=" + folder() + " audio=" + Prefs.on(Prefs.DL_AUDIO)
                + " quality=" + quality());
    }

    static String folder() {
        String value = Prefs.rawString(Prefs.DL_FOLDER);
        return value == null ? "" : value.trim();
    }

    static String quality() {
        String value = Prefs.rawString(Prefs.DL_QUALITY);
        return "high".equals(value) ? "high" : "default";
    }

    // ---- saf yardımcılar (bkz. DownloadPlusTest) --------------------------------------------

    /**
     * Kayıt yolunu seçilen klasörle değiştirir.
     *
     * "/storage/emulated/0/DCIM/Camera" + "Movies/GkteTok"  →  "/storage/emulated/0/Movies/GkteTok"
     * "/sdcard/DCIM/Camera"              + "Download"        →  "/sdcard/Download"
     * Yol tanınmıyorsa ya da seçim boşsa yol olduğu gibi kalır.
     */
    static String folderFor(String savePath, String choice) {
        if (savePath == null || savePath.isEmpty() || choice == null || choice.isEmpty()) return savePath;
        String root = null;
        for (String candidate : ROOTS) {
            if (savePath.startsWith(candidate)) {
                root = candidate;
                break;
            }
        }
        if (root == null) {
            String clean = savePath.endsWith("/") ? savePath.substring(0, savePath.length() - 1) : savePath;
            int second = clean.lastIndexOf('/');
            if (second <= 0) return savePath;
            int first = clean.lastIndexOf('/', second - 1);
            if (first <= 0) return savePath;
            root = clean.substring(0, first);
        }
        String separator = root.endsWith("/") ? "" : "/";
        return root + separator + choice;
    }

    private static final String[] ROOTS = {"/storage/emulated/0", "/sdcard"};

    /** Sadece ses modunda dosya adı .m4a olur; başka adlar dokunulmaz. */
    static String audioName(String name) {
        if (name == null || name.isEmpty()) return name;
        String lower = name.toLowerCase(java.util.Locale.US);
        if (lower.endsWith(".mp4")) return name.substring(0, name.length() - 4) + ".m4a";
        if (lower.endsWith(".m4a") || lower.endsWith(".mp3")) return name;
        return name + ".m4a";
    }

    /** En yüksek değerli kalitenin sırası (boş/eksik liste → -1). */
    static int bestIndex(int[] bitrates) {
        if (bitrates == null || bitrates.length == 0) return -1;
        int best = -1;
        for (int index = 0; index < bitrates.length; index++) {
            if (bitrates[index] <= 0) continue;
            if (best < 0 || bitrates[index] > bitrates[best]) best = index;
        }
        return best;
    }

    // ---- kancalar ---------------------------------------------------------------------------

    private interface Rewrite {
        String apply(String value);
    }

    private static final Rewrite SavePath = value -> {
        String choice = folder();
        return choice.isEmpty() ? value : folderFor(value, choice);
    };

    private static final Rewrite Url = value -> {
        if (value == null || value.isEmpty()) return value;
        if (Prefs.on(Prefs.DL_AUDIO)) {
            String audio = audioUrl();
            if (audio != null) {
                RuntimeLog.log("download plus: audio url");
                return audio;
            }
            return value;
        }
        if ("high".equals(quality())) {
            String best = bestUrl(value);
            if (best != null) return best;
        }
        return value;
    };

    private static final Rewrite Name = value -> Prefs.on(Prefs.DL_AUDIO) ? audioName(value) : value;

    private static int hookString(Class<?> type, String method, Rewrite rewrite) {
        int count = 0;
        for (Method candidate : type.getDeclaredMethods()) {
            if (!candidate.getName().equals(method)) continue;
            Class<?>[] parameters = candidate.getParameterTypes();
            if (parameters.length != 1 || parameters[0] != String.class) continue;
            Hooks.hook(candidate, new Hooks.Hook() {
                @Override protected void before(Hooks.Call call) {
                    if (call.args.length == 0 || !(call.args[0] instanceof String)) return;
                    String before = (String) call.args[0];
                    try {
                        call.args[0] = rewrite.apply(before);
                    } catch (Throwable error) {
                        RuntimeLog.log("download plus: " + method + " failed: " + error.getClass().getSimpleName());
                    }
                }
            });
            count++;
        }
        return count;
    }

    /** Şu an oynatılan videonun müzik adresi. */
    private static String audioUrl() {
        Object aweme = CurrentAweme.get();
        if (aweme == null) {
            RuntimeLog.log("download plus: no current video for audio");
            return null;
        }
        Object music = Reflect.call(aweme, "getMusic");
        if (music == null) return null;
        Object play = Reflect.call(music, "getPlayUrl");
        return firstUrl(play);
    }

    /** Varsa en yüksek bit hızındaki adres, yoksa null (mevcut adres korunur). */
    private static String bestUrl(String current) {
        Object aweme = CurrentAweme.get();
        if (aweme == null) return null;
        Object video = Reflect.call(aweme, "getVideo");
        if (video == null) return null;
        Object rates = Reflect.call(video, "getBitRate");
        List<String> urls = new ArrayList<>();
        List<Integer> bitrates = new ArrayList<>();
        if (rates instanceof List<?>) {
            for (Object rate : (List<?>) rates) {
                Object play = Reflect.call(rate, "getPlayAddr");
                String url = firstUrl(play);
                Object bitrate = Reflect.call(rate, "getBitRate");
                if (url == null || !(bitrate instanceof Number)) continue;
                urls.add(url);
                bitrates.add(((Number) bitrate).intValue());
            }
        }
        if (urls.isEmpty()) return null;
        int[] values = new int[bitrates.size()];
        for (int index = 0; index < values.length; index++) values[index] = bitrates.get(index);
        int best = bestIndex(values);
        if (best < 0) return null;
        String chosen = urls.get(best);
        return chosen.equals(current) ? null : chosen;
    }

    private static String firstUrl(Object playAddr) {
        if (playAddr == null) return null;
        Object list = Reflect.call(playAddr, "getUrlList");
        if (list instanceof List<?>) {
            for (Object url : (List<?>) list) {
                if (url instanceof String && !((String) url).isEmpty()) return (String) url;
            }
        }
        Object uri = Reflect.call(playAddr, "getUri");
        return uri instanceof String && !((String) uri).isEmpty() ? (String) uri : null;
    }
}
