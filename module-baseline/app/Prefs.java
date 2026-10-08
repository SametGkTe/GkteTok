package com.golda.patchertiktok;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Settings stored inside TikTok's own data directory. Hooks read cached values,
 * so a toggle applies immediately where the hooked code is consulted again.
 */
final class Prefs {
    static final String FILE = "tiktokpatchxposed";

    static final String ADS = "block_ads";
    static final String LIVE = "hide_live";
    static final String ACQUAINTANCES = "hide_acquaintances";
    static final String SEEKBAR = "seekbar";
    static final String NO_WATERMARK = "no_watermark";
    static final String DOWNLOAD_ANY = "download_any";
    static final String SCREENSHOTS = "allow_screenshots";
    static final String COMMENT_REPOSTS = "comment_reposts";
    static final String NEW_PROFILE = "new_profile";
    static final String EXTRA_FEATURES = "extra_features";
    static final String CLEAN_LINKS = "clean_links";
    static final String REGION = "region";
    static final String AUTO_STREAK = "auto_streak";

    // ── TT+ : görünüm ───────────────────────────────────────────────────────
    /** Saf siyah (#000000) tema — koyu gri zeminleri siyaha çevirir. */
    static final String AMOLED = "ttplus_amoled";
    /** Ek olarak koyu kart tonlarını da karart. */
    static final String AMOLED_CARDS = "ttplus_amoled_cards";

    /**
     * TT+ : profil bannerı (kapak görseli).
     * TikTok bu özelliği sunucu tarafında kademeli açıyor; pek çok hesapta düzenleme
     * arayüzü hiç görünmüyor. Bu anahtar, hesabı yerelde "izinli + deney grubunda"
     * göstererek arayüzü açar.
     */
    static final String BANNER = "ttplus_banner";

    /**
     * GkteTok : açılış ekranı.
     * TikTok açılırken kısa süreliğine markalı bir GkteTok katmanı gösterir.
     */
    static final String SPLASH = "ttplus_gkte_splash";

    /** Açılış ekranındaki logo dosyasının yolu ("" = modüle gömülü GkTeLogo.png kullanılır). */
    static final String LOGO_PATH = "ttplus_logo_path";

    // ── GkteTok : görünüm ve kişiselleştirme anahtarları ─────────────────────
    /** Vurgu (tema) rengi palet kimliği: "" kapalı, teal/purple/green/gold/blue. */
    static final String ACCENT = "ttplus_accent";
    /** Gece zamanlayıcısı (AMOLED belirli saatlerde kendiliğinden açılır). */
    static final String NIGHT = "ttplus_night";
    /** Kendi yazı tipi (.ttf) kullanılsın mı. */
    static final String FONT = "ttplus_font";
    /** Seçilen .ttf dosyasının yolu. */
    static final String FONT_PATH = "ttplus_font_path";
    /** Yumuşak siyah: saf siyah yerine koyu gri zeminler. */
    static final String SOFT_BLACK = "ttplus_soft_black";
    /** Yazı boyutu ölçeği: yüzde (100 = varsayılan, 0 = kapalı). */
    static final String FONT_SCALE = "ttplus_font_scale";
    /** Açılış ekranı süresi (ms). */
    static final String SPLASH_MS = "ttplus_splash_ms";
    /** Açılış animasyonu: fade / zoom / slide / none. */
    static final String SPLASH_ANIM = "ttplus_splash_anim";
    /** Kullanım süresi uyarısı. */
    static final String TIME_LIMIT = "ttplus_time_limit";
    static final String TIME_LIMIT_MIN = "ttplus_time_limit_min";
    /** Günlük özet bildirimi. */
    static final String SUMMARY = "ttplus_summary";
    /** Güvenli mod: hata veren kanca kendini kapatır. */
    static final String SAFE_MODE = "ttplus_safe_mode";
    /** Açılış hızlandırma (deneysel). */
    static final String FAST_OPEN = "ttplus_fast_open";
    /** Yanlışlıkla beğeni koruması. */
    static final String LIKE_GUARD = "ttplus_like_guard";
    /** İndirme dosya adına tarih ekle. */
    static final String DOWNLOAD_NAME = "ttplus_download_name";
    /** Temiz akış: reklam + canlı + fotoğraf + hikâye tek anahtarla. */
    static final String CLEAN_FEED = "ttplus_clean_feed";
    /** Fotoğraf gönderilerini ve hikâyeleri akıştan gizle. */
    static final String PHOTO = "ttplus_hide_photo";
    static final String STORY = "ttplus_hide_story";
    /** Arayüz opaklığı (yüzde; 0 = kapalı) — OLED yanmasına karşı. */
    static final String OPACITY = "ttplus_opacity";
    /** İndirme seçenekleri: sadece ses, kalite, klasör. */
    static final String DL_AUDIO = "ttplus_dl_audio";
    static final String DL_QUALITY = "ttplus_dl_quality";
    static final String DL_FOLDER = "ttplus_dl_folder";
    /** Akışta gönderi tarihi. */
    static final String POST_DATE = "ttplus_post_date";
    /** Mesaj (DM) kelime filtresi. */
    static final String DM_FILTER = "ttplus_dm_filter";
    static final String DM_WORDS = "ttplus_dm_words";
    /** Yorumları "En yeni" ile aç. */
    static final String COMMENT_SORT = "ttplus_comment_sort";
    /** Acil gizleme (Kill Switch): mesajlar gizlenir, beğeni sayısı 0 görünür. */
    static final String KILL_SWITCH = "ttplus_kill_switch";
    /** Ayar profilleri (yedeğin aynısı, 3 yuva). */
    static final String PROFILE_1 = "ttplus_profile_1";
    static final String PROFILE_2 = "ttplus_profile_2";
    static final String PROFILE_3 = "ttplus_profile_3";

    /** Germany keeps the behaviour of earlier releases; users can pick another country or turn it off. */
    static final String DEFAULT_REGION = "de";

    /** Keys whose hooks only take effect when TikTok starts. */
    static final String[] RESTART_KEYS = {COMMENT_REPOSTS, NEW_PROFILE, EXTRA_FEATURES, REGION, ADS, BANNER};

    private static final Map<String, Boolean> DEFAULTS = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> FLAGS = new ConcurrentHashMap<>();
    private static final CopyOnWriteArrayList<Runnable> LISTENERS = new CopyOnWriteArrayList<>();
    private static volatile SharedPreferences preferences;
    private static volatile String region = DEFAULT_REGION;
    private static volatile String accent = "";
    private static volatile String logoPath = "";
    private static volatile String fontPath = "";
    private static volatile int nightFrom = 22;
    private static volatile int nightTo = 7;
    private static volatile int fontScale = 100;
    private static volatile int splashMs = 1100;
    private static volatile int timeLimitMin = 30;
    private static volatile String splashAnim = "fade";
    private static volatile int opacity;
    private static volatile String startSnapshot;

    static {
        DEFAULTS.put(ADS, true);
        DEFAULTS.put(LIVE, true);
        DEFAULTS.put(ACQUAINTANCES, true);
        DEFAULTS.put(SEEKBAR, true);
        DEFAULTS.put(NO_WATERMARK, true);
        DEFAULTS.put(DOWNLOAD_ANY, true);
        DEFAULTS.put(SCREENSHOTS, true);
        DEFAULTS.put(COMMENT_REPOSTS, true);
        DEFAULTS.put(NEW_PROFILE, true);
        DEFAULTS.put(EXTRA_FEATURES, true);
        DEFAULTS.put(CLEAN_LINKS, true);
        // Automated messages are opt-in only.
        DEFAULTS.put(AUTO_STREAK, false);
        // TT+ görünüm özellikleri varsayılan olarak kapalı (kullanıcı bilinçli açar).
        DEFAULTS.put(AMOLED, false);
        DEFAULTS.put(AMOLED_CARDS, false);
        // TT+ banner: kullanıcı istediği için açık; kapatmak isterse ayarlardan kapatır.
        DEFAULTS.put(BANNER, true);
        // GkteTok açılış ekranı: kullanıcı istediği için açık.
        DEFAULTS.put(SPLASH, true);
        // Gece zamanlayıcısı ve kendi yazı tipi: kullanıcı açıkça açar.
        DEFAULTS.put(NIGHT, false);
        DEFAULTS.put(FONT, false);
        DEFAULTS.put(SOFT_BLACK, false);
        DEFAULTS.put(TIME_LIMIT, false);
        DEFAULTS.put(SUMMARY, true);
        DEFAULTS.put(SAFE_MODE, true);
        DEFAULTS.put(FAST_OPEN, false);
        DEFAULTS.put(LIKE_GUARD, false);
        DEFAULTS.put(DOWNLOAD_NAME, false);
        DEFAULTS.put(KILL_SWITCH, false);
        DEFAULTS.put(CLEAN_FEED, false);
        DEFAULTS.put(PHOTO, false);
        DEFAULTS.put(STORY, false);
        DEFAULTS.put(DL_AUDIO, false);
        DEFAULTS.put(POST_DATE, false);
        DEFAULTS.put(DM_FILTER, false);
        DEFAULTS.put(COMMENT_SORT, false);
    }

    private Prefs() { }

    static synchronized void load(Context context) {
        if (preferences != null || context == null) return;
        SharedPreferences loaded = context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
        for (String key : DEFAULTS.keySet()) FLAGS.put(key, loaded.getBoolean(key, DEFAULTS.get(key)));
        region = Regions.normalize(loaded.getString(REGION, DEFAULT_REGION));
        accent = AccentTheme.normalize(loaded.getString(ACCENT, ""));
        logoPath = orEmpty(loaded.getString(LOGO_PATH, ""));
        fontPath = orEmpty(loaded.getString(FONT_PATH, ""));
        nightFrom = clampHour(loaded.getInt("ttplus_night_from", 22));
        nightTo = clampHour(loaded.getInt("ttplus_night_to", 7));
        fontScale = clampInt(loaded.getInt(FONT_SCALE, 100), 85, 130);
        splashMs = clampInt(loaded.getInt(SPLASH_MS, 1100), 400, 5000);
        timeLimitMin = clampInt(loaded.getInt(TIME_LIMIT_MIN, 30), 5, 240);
        splashAnim = normalizeAnim(loaded.getString(SPLASH_ANIM, "fade"));
        opacity = clampInt(loaded.getInt(OPACITY, 0), 0, 100);
        preferences = loaded;
        startSnapshot = snapshot();
    }

    /** True when a setting that is applied only at startup differs from the running process. */
    static boolean restartPending() {
        return startSnapshot != null && !startSnapshot.equals(snapshot());
    }

    static boolean loaded() { return preferences != null; }

    static boolean on(String key) {
        Boolean value = FLAGS.get(key);
        if (value != null) return value;
        Boolean fallback = DEFAULTS.get(key);
        return fallback != null && fallback;
    }

    static void set(String key, boolean value) {
        FLAGS.put(key, value);
        SharedPreferences current = preferences;
        if (current != null) current.edit().putBoolean(key, value).apply();
        notifyChanged();
    }

    /** Selected spoof region as lower-case ISO code, or empty when disabled. */
    static String region() { return region; }

    static String accent() { return accent; }

    static void setAccent(String id) {
        accent = AccentTheme.normalize(id);
        SharedPreferences current = preferences;
        if (current != null) current.edit().putString(ACCENT, accent).apply();
        notifyChanged();
    }

    static String logoPath() { return logoPath; }

    static void setLogoPath(String path) {
        logoPath = orEmpty(path);
        SharedPreferences current = preferences;
        if (current != null) current.edit().putString(LOGO_PATH, logoPath).apply();
    }

    static String fontPath() { return fontPath; }

    static void setFontPath(String path) {
        fontPath = orEmpty(path);
        SharedPreferences current = preferences;
        if (current != null) current.edit().putString(FONT_PATH, fontPath).apply();
        notifyChanged();
    }

    static int fontScale() { return fontScale; }

    static void setFontScale(int percent) {
        fontScale = clampInt(percent, 85, 130);
        SharedPreferences current = preferences;
        if (current != null) current.edit().putInt(FONT_SCALE, fontScale).apply();
        notifyChanged();
    }

    static int splashMs() { return splashMs; }

    static void setSplashMs(int millis) {
        splashMs = clampInt(millis, 400, 5000);
        SharedPreferences current = preferences;
        if (current != null) current.edit().putInt(SPLASH_MS, splashMs).apply();
    }

    static String splashAnim() { return splashAnim; }

    static void setSplashAnim(String anim) {
        splashAnim = normalizeAnim(anim);
        SharedPreferences current = preferences;
        if (current != null) current.edit().putString(SPLASH_ANIM, splashAnim).apply();
    }

    /** Arayüz opaklığı yüzdesi (0 = kapalı, 100 = tamamen opak). */
    static int opacity() { return opacity; }

    static void setOpacity(int percent) {
        opacity = clampInt(percent, 0, 100);
        SharedPreferences current = preferences;
        if (current != null) current.edit().putInt(OPACITY, opacity).apply();
        notifyChanged();
    }

    static int timeLimitMin() { return timeLimitMin; }

    static void setTimeLimitMin(int minutes) {
        timeLimitMin = clampInt(minutes, 5, 240);
        SharedPreferences current = preferences;
        if (current != null) current.edit().putInt(TIME_LIMIT_MIN, timeLimitMin).apply();
        notifyChanged();
    }

    /** Profiller ve yedek gibi serbest metin alanları. */
    static String rawString(String key) {
        SharedPreferences current = preferences;
        return current == null ? "" : current.getString(key, "");
    }

    static void setRawString(String key, String value) {
        SharedPreferences current = preferences;
        if (current != null) current.edit().putString(key, value == null ? "" : value).apply();
        notifyChanged();
    }

    /** Yedek ve fabrika ayarları için: bir anahtarın varsayılan değeri. */
    static boolean defaultValue(String key) {
        Boolean fallback = DEFAULTS.get(key);
        return fallback != null && fallback;
    }

    /** Kayıtlı ve varsayılan tüm mantıksal anahtarlar. */
    static String[] flags() { return DEFAULTS.keySet().toArray(new String[0]); }

    static int nightFrom() { return nightFrom; }

    static int nightTo() { return nightTo; }

    static void setNightHours(int from, int to) {
        nightFrom = clampHour(from);
        nightTo = clampHour(to);
        SharedPreferences current = preferences;
        if (current != null) {
            current.edit().putInt("ttplus_night_from", nightFrom).putInt("ttplus_night_to", nightTo).apply();
        }
        notifyChanged();
    }

    /** Sayaçlar ve yedek gibi alt sınıfların ham SharedPreferences erişimi. */
    static SharedPreferences raw() { return preferences; }

    private static String orEmpty(String value) { return value == null ? "" : value; }

    static int clampHour(int hour) { return ((hour % 24) + 24) % 24; }

    static int clampInt(int value, int min, int max) { return value < min ? min : (value > max ? max : value); }

    static String normalizeAnim(String anim) {
        if ("zoom".equals(anim) || "slide".equals(anim) || "none".equals(anim)) return anim;
        return "fade";
    }

    static void setRegion(String iso) {
        region = Regions.normalize(iso);
        SharedPreferences current = preferences;
        if (current != null) current.edit().putString(REGION, region).apply();
        notifyChanged();
    }

    static String snapshot() {
        StringBuilder value = new StringBuilder(region);
        for (String key : RESTART_KEYS) value.append(on(key) ? '1' : '0');
        return value.toString();
    }

    static void listen(Runnable listener) { LISTENERS.addIfAbsent(listener); }

    static void unlisten(Runnable listener) { LISTENERS.remove(listener); }

    private static void notifyChanged() {
        for (Runnable listener : LISTENERS) listener.run();
    }
}
