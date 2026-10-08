package com.golda.patchertiktok;

import android.content.SharedPreferences;

/**
 * GkteTok — ayar yedeği.
 *
 * Ayarlar tek bir düz metin dosyasına yazılır ve aynı dosyadan geri yüklenir
 * (`anahtar=deger` satırları). Dosya biçimi bilerek basit: bozuk veya eksik
 * satırlar sessizce atlanır, tanınmayan anahtarlar yok sayılır, böylece ileride
 * yeni ayar eklemek eski yedekleri bozmaz.
 */
final class Backup {
    static final String HEADER = "GkteTok ayar dosyasi v1";

    /** Dosyaya yazılan anahtar sırası (okunması kolay olsun diye gruplanmış). */
    private static final String[] FLAGS = {
            Prefs.ADS, Prefs.LIVE, Prefs.ACQUAINTANCES, Prefs.SEEKBAR,
            Prefs.COMMENT_REPOSTS, Prefs.NEW_PROFILE, Prefs.EXTRA_FEATURES,
            Prefs.NO_WATERMARK, Prefs.DOWNLOAD_ANY, Prefs.SCREENSHOTS, Prefs.CLEAN_LINKS,
            Prefs.AUTO_STREAK,
            Prefs.AMOLED, Prefs.AMOLED_CARDS, Prefs.BANNER, Prefs.SPLASH,
            Prefs.NIGHT, Prefs.FONT,
            Prefs.SOFT_BLACK, Prefs.TIME_LIMIT, Prefs.SUMMARY, Prefs.SAFE_MODE,
            Prefs.FAST_OPEN, Prefs.LIKE_GUARD, Prefs.DOWNLOAD_NAME, Prefs.KILL_SWITCH,
            Prefs.CLEAN_FEED, Prefs.PHOTO, Prefs.STORY, Prefs.DL_AUDIO,
            Prefs.POST_DATE, Prefs.DM_FILTER, Prefs.COMMENT_SORT,
    };

    private static final String NIGHT_FROM = "ttplus_night_from";
    private static final String NIGHT_TO = "ttplus_night_to";
    private static final String FONT_PATH = "ttplus_font_path";
    private static final String[] NUMBERS = {
            Prefs.FONT_SCALE, Prefs.SPLASH_MS, Prefs.TIME_LIMIT_MIN, Prefs.OPACITY,
    };

    private Backup() { }

    static String encode() {
        StringBuilder out = new StringBuilder(HEADER).append('\n');
        for (String key : FLAGS) out.append(key).append('=').append(Prefs.on(key)).append('\n');
        for (String key : NUMBERS) out.append(key).append('=').append(number(Prefs.raw(), key)).append('\n');
        out.append(NIGHT_FROM).append('=').append(Prefs.nightFrom()).append('\n');
        out.append(NIGHT_TO).append('=').append(Prefs.nightTo()).append('\n');
        out.append(Prefs.REGION).append('=').append(Prefs.region()).append('\n');
        out.append(Prefs.ACCENT).append('=').append(Prefs.accent()).append('\n');
        out.append(FONT_PATH).append('=').append(Prefs.fontPath()).append('\n');
        out.append(Prefs.SPLASH_ANIM).append('=').append(Prefs.splashAnim()).append('\n');
        out.append(Prefs.LOGO_PATH).append('=').append(Prefs.logoPath()).append('\n');
        out.append(Prefs.DL_QUALITY).append('=').append(preferences(Prefs.DL_QUALITY)).append('\n');
        out.append(Prefs.DL_FOLDER).append('=').append(preferences(Prefs.DL_FOLDER)).append('\n');
        out.append(Prefs.DM_WORDS).append('=').append(preferences(Prefs.DM_WORDS)).append('\n');
        return out.toString();
    }

    private static String preferences(String key) {
        return Prefs.rawString(key);
    }


    private static String placeholder() {
        return "";
    }

    private static int number(SharedPreferences preferences, String key) {
        if (preferences == null) return 0;
        return preferences.getInt(key, 0);
    }

    /**
     * Metni uygular ve değiştirilen ayar sayısını döndürür.
     * Aynı satır iki kez geçerse son değer kazanır.
     */
    static int apply(String text) {
        if (text == null) return 0;
        int applied = 0;
        for (String raw : text.split("\n")) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#") || line.startsWith(HEADER)) continue;
            int split = line.indexOf('=');
            if (split <= 0) continue;
            String key = line.substring(0, split).trim();
            String value = line.substring(split + 1).trim();
            if (Prefs.REGION.equals(key)) {
                Prefs.setRegion(value);
                applied++;
            } else if (Prefs.ACCENT.equals(key)) {
                Prefs.setAccent(AccentTheme.normalize(value));
                applied++;
            } else if (NIGHT_FROM.equals(key) || NIGHT_TO.equals(key)) {
                Integer hour = hour(value);
                if (hour == null) continue;
                Prefs.setNightHours(NIGHT_FROM.equals(key) ? hour : Prefs.nightFrom(),
                        NIGHT_TO.equals(key) ? hour : Prefs.nightTo());
                applied++;
            } else if (FONT_PATH.equals(key)) {
                Prefs.setFontPath(value);
                applied++;
            } else if (Prefs.FONT_SCALE.equals(key)) {
                Prefs.setFontScale(intValue(value, 100));
                applied++;
            } else if (Prefs.SPLASH_MS.equals(key)) {
                Prefs.setSplashMs(intValue(value, 1100));
                applied++;
            } else if (Prefs.TIME_LIMIT_MIN.equals(key)) {
                Prefs.setTimeLimitMin(intValue(value, 30));
                applied++;
            } else if (Prefs.SPLASH_ANIM.equals(key)) {
                Prefs.setSplashAnim(value);
                applied++;
            } else if (Prefs.LOGO_PATH.equals(key)) {
                Prefs.setLogoPath(value);
                applied++;
            } else if (Prefs.OPACITY.equals(key)) {
                Prefs.setOpacity(intValue(value, 0));
                applied++;
            } else if (Prefs.DL_QUALITY.equals(key) || Prefs.DL_FOLDER.equals(key) || Prefs.DM_WORDS.equals(key)) {
                Prefs.setRawString(key, value);
                applied++;
            } else if (isFlag(key)) {
                Boolean flag = bool(value);
                if (flag == null) continue;
                Prefs.set(key, flag);
                applied++;
            }
        }
        return applied;
    }

    private static int intValue(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException invalid) {
            return fallback;
        }
    }

    /** Tüm ayarları varsayılana döndürür. */
    static void factoryReset() {
        for (String key : Prefs.flags()) Prefs.set(key, Prefs.defaultValue(key));
        Prefs.setRegion(Prefs.DEFAULT_REGION);
        Prefs.setAccent("");
        Prefs.setFontPath("");
        Prefs.setLogoPath("");
        Prefs.setNightHours(22, 7);
        Prefs.setFontScale(100);
        Prefs.setSplashMs(1100);
        Prefs.setTimeLimitMin(30);
        Prefs.setSplashAnim("fade");
        Prefs.setOpacity(0);
        Prefs.setRawString(Prefs.DL_QUALITY, "");
        Prefs.setRawString(Prefs.DL_FOLDER, "");
        Prefs.setRawString(Prefs.DM_WORDS, "");
    }

    private static boolean isFlag(String key) {
        for (String flag : FLAGS) if (flag.equals(key)) return true;
        return false;
    }

    private static Boolean bool(String value) {
        if ("true".equalsIgnoreCase(value) || "1".equals(value)) return Boolean.TRUE;
        if ("false".equalsIgnoreCase(value) || "0".equals(value)) return Boolean.FALSE;
        return null;
    }

    private static Integer hour(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException invalid) {
            return null;
        }
    }
}
