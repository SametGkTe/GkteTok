package com.golda.patchertiktok;

import android.graphics.Typeface;
import android.widget.TextView;

import java.io.File;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * GkteTok — kendi yazı tipin.
 *
 * Kullanıcının seçtiği .ttf dosyası TikTok'un veri klasörüne kopyalanır ve
 * {@link TextView#setTypeface} kancasında **sistem varsayılanı olan** yazı tipleri
 * yerine bu yazı tipi kurulur.
 *
 * Neden aile kontrolü var?
 *   TikTok ikonları özel ikon yazı tipleriyle çiziyor (ör. "IconFont"). Onları da
 *   değiştirseydik ikonlar bozulurdu. Bu yüzden yalnızca ailesi boş, "sans-serif",
 *   "roboto" gibi **sistem** aileleri olan yazı tipleri değiştirilir; marka/ikon
 *   yazı tiplerine dokunulmaz.
 *
 * Kalınlık (bold/italic) korunur: seçilen yazı tipinden istenen stille türetilir ve
 * stil başına önbelleğe alınır.
 */
final class FontOverride {
    private static final Map<Integer, Typeface> STYLED = new ConcurrentHashMap<>();
    private static volatile String loadedPath;
    private static volatile Typeface loaded;

    private FontOverride() { }

    static void install() {
        try {
            // TikTok bazı yazıları Typeface.create("sans-serif", stil) ile kuruyor;
            // bu yol da kancalanmazsa yazı tipi ekranın yarısında uygulanır.
            Hooks.findAndHook(Typeface.class, "create", String.class, int.class, new Hooks.Hook() {
                @Override protected void after(Hooks.Call call) {
                    if (!Prefs.on(Prefs.FONT)) return;
                    if (call.args.length < 2 || !(call.args[0] instanceof String)) return;
                    if (!systemFamily((String) call.args[0])) return;
                    Typeface base = custom(Prefs.fontPath());
                    if (base == null) return;
                    int style = call.args[1] instanceof Integer
                            ? ((Integer) call.args[1]) & (Typeface.BOLD | Typeface.ITALIC) : Typeface.NORMAL;
                    call.setResult(styled(base, style));
                }
            });
            Hooks.hookAll(TextView.class, "setTypeface", new Hooks.Hook() {
                @Override protected void before(Hooks.Call call) {
                    if (!Prefs.on(Prefs.FONT)) return;
                    Typeface base = custom(Prefs.fontPath());
                    if (base == null) return;
                    if (call.args.length == 0 || !(call.args[0] instanceof Typeface)) return;
                    if (!replaceable((Typeface) call.args[0])) return;
                    int style = call.args.length > 1 && call.args[1] instanceof Integer
                            ? ((Integer) call.args[1]) & (Typeface.BOLD | Typeface.ITALIC)
                            : Typeface.NORMAL;
                    call.args[0] = styled(base, style);
                }
            });
            RuntimeLog.log("font: hooks installed" + (Prefs.fontPath().isEmpty() ? "" : " (" + name(Prefs.fontPath()) + ")"));
        } catch (Throwable error) {
            RuntimeLog.log("font: cannot install: " + error);
        }
    }

    /** Sistem ailesi mi (marka/ikon yazı tipleri değil). */
    static boolean systemFamily(String family) {
        if (family == null || family.isEmpty()) return true;
        String value = family.toLowerCase(Locale.ROOT);
        return value.contains("sans-serif") || value.contains("system-ui")
                || value.equals("roboto") || value.equals("default") || value.equals("normal");
    }

    /**
     * Seçilen dosyayı yüklemeyi dener. Başarısızsa false döner ve eski yazı tipi korunur.
     * Bu, "yazı tipi ekledim ama olmadı" durumunu sessizce geçiştirmemek için var.
     */
    static boolean accept(java.io.File file) {
        if (file == null || !file.isFile() || file.length() < 256) {
            RuntimeLog.log("font: dosya bos ya da cok kucuk");
            return false;
        }
        if (!isFontHeader(header(file))) {
            RuntimeLog.log("font: bu dosya yazi tipi degil (" + file.getName() + ")");
            return false;
        }
        try {
            Typeface created = Typeface.createFromFile(file);
            if (created == null) {
                RuntimeLog.log("font: okunamadi (" + file.getName() + ")");
                return false;
            }
            synchronized (FontOverride.class) {
                loaded = created;
                loadedPath = file.getAbsolutePath();
                STYLED.clear();
            }
            RuntimeLog.log("font: loaded " + file.getName() + " (" + file.length() + " bayt)");
            return true;
        } catch (Throwable error) {
            RuntimeLog.log("font: yuklenemedi (" + file.getName() + "): " + error);
            return false;
        }
    }

    private static byte[] header(java.io.File file) {
        try (java.io.FileInputStream in = new java.io.FileInputStream(file)) {
            byte[] head = new byte[4];
            return in.read(head) == 4 ? head : null;
        } catch (Throwable error) {
            return null;
        }
    }

    /** Saf kural: ilk dört bayt bir yazı tipi imzası mı? (bkz. FontOverrideTest) */
    static boolean isFontHeader(byte[] head) {
        if (head == null || head.length < 4) return false;
        int magic = ((head[0] & 0xFF) << 24) | ((head[1] & 0xFF) << 16) | ((head[2] & 0xFF) << 8) | (head[3] & 0xFF);
        return magic == 0x00010000            // TrueType
                || magic == 0x4F54544F         // "OTTO" — OpenType/CFF
                || magic == 0x74727565         // "true" — Apple TrueType
                || magic == 0x74746366;        // "ttcf" — koleksiyon
    }

    /** Yalnızca sistem aileleri değiştirilir; ikon/marka yazı tipleri korunur. */
    static boolean replaceable(Typeface original) {
        if (original == null) return true;
        String family = family(original);
        if (family == null) {
            // Aile adı okunamadı (gizli API engeli): yalnızca bilinen sistem nesnelerine dokun.
            return original == Typeface.DEFAULT || original == Typeface.DEFAULT_BOLD
                    || original == Typeface.SANS_SERIF || original == Typeface.SERIF
                    || original == Typeface.MONOSPACE;
        }
        return systemFamily(family);
    }

    /**
     * Aile adı SDK'da açık değildir (getFamilyName/getFamily gizli API); yansıma ile okunur.
     * Okunamazsa null döner ve karar muhafazakâr kalır — ikon yazı tiplerini bozmamak önceliğimiz.
     */
    private static String family(Typeface typeface) {
        for (String name : new String[]{"getFamilyName", "getFamily"}) {
            try {
                java.lang.reflect.Method method = Typeface.class.getDeclaredMethod(name);
                method.setAccessible(true);
                Object value = method.invoke(typeface);
                if (value instanceof String) return (String) value;
            } catch (Throwable ignored) {
                // Sıradaki adı dene.
            }
        }
        if (!familyLogged) {
            familyLogged = true;
            RuntimeLog.log("font: family lookup unavailable, only system typefaces are replaced");
        }
        return null;
    }

    private static volatile boolean familyLogged;

    private static Typeface custom(String path) {
        if (path == null || path.isEmpty()) return null;
        Typeface current = loaded;
        if (current != null && path.equals(loadedPath)) return current;
        synchronized (FontOverride.class) {
            if (path.equals(loadedPath) && loaded != null) return loaded;
            Typeface created = null;
            try {
                File file = new File(path);
                if (file.isFile()) created = Typeface.createFromFile(file);
            } catch (Throwable error) {
                RuntimeLog.log("font: cannot read " + name(path) + ": " + error);
            }
            loadedPath = path;
            loaded = created;
            STYLED.clear();
            return created;
        }
    }

    private static Typeface styled(Typeface base, int style) {
        Typeface cached = STYLED.get(style);
        if (cached != null) return cached;
        Typeface created = Typeface.create(base, style);
        STYLED.put(style, created);
        return created;
    }

    private static String name(String path) {
        int slash = path.lastIndexOf('/');
        return slash < 0 ? path : path.substring(slash + 1);
    }
}
