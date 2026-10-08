package com.golda.patchertiktok;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * TT+ — AMOLED saf siyah (#000000) tema.
 *
 * YAKLAŞIM (neden böyle?)
 *   TikTok'un tema sınıfları obfuscate ve her sürümde değişiyor. Onları kancalamak her
 *   güncellemede kırılır. Bunun yerine, TikTok süreci içinde **Android'in kendi renk
 *   giriş noktalarını** kancalıyoruz ve "siyaha çok yakın" renkleri saf siyaha çeviriyoruz.
 *   Böylece özellik TikTok sürümünden bağımsız çalışır (dex taraması bile gerekmez).
 *
 * GÜVENLİK KURALLARI (yanlış renk bozulmasını önler)
 *   Bir renk yalnızca şu üç koşulu birlikte sağlıyorsa değiştirilir:
 *     1) Tamamen opak (alfa = 0xFF)      → yarı saydam katmanlara dokunulmaz
 *     2) Nötr gri (kanal farkı ≤ 18)     → marka renkleri, ikonlar korunur
 *     3) Yeterince koyu (max kanal ≤ eşik) → açık tema ve beyazlar korunur
 *   TikTok'un koyu temasındaki yüzeyler (#121212, #1E1E1E, #161823) bu kurala girer;
 *   vurgu rengi (#FE2C55) ve açık tema (#F5F5F5) girmez.
 *
 * İKİ KADEME
 *   • AMOLED          → zemin tonları (max kanal ≤ 0x24) siyah olur
 *   • AMOLED_CARDS    → ek olarak kart tonları  (max kanal ≤ 0x3C) da siyah olur
 *
 * Performans notu: Bu kancalar çok sık çağrılır (her renk çözümlemesi). Bu yüzden dönüşüm
 * saf bit işlemidir, ayar okuması şart olana kadar yapılmaz ve ColorStateList sonuçları
 * kimlik bazlı zayıf önbelleğe alınır.
 */
final class Amoled {

    /** Zemin tonları için üst parlaklık sınırı (dahil). */
    private static final int BASE_LIMIT = 0x24;     // #242424
    /** "Kartları da karart" açıkken geçerli üst sınır (dahil). */
    private static final int CARDS_LIMIT = 0x3C;    // #3C3C3C
    /** Nötr gri sayılmak için izin verilen kanal farkı. (#161823 için fark 13'tür.) */
    private static final int MAX_SPREAD = 18;
    private static final int BLACK = 0xFF000000;

    /** Yumuşak siyah hedefi (saf siyah yerine). */
    private static final int SOFT_BLACK = 0xFF121212;
    private static final int SOFT_CARD = 0xFF1C1C1C;

    private static final Map<ColorStateList, ColorStateList> CACHE = new WeakHashMap<>();

    static {
        // Toggle değişince ColorStateList önbelleği geçersiz olur.
        Prefs.listen(Amoled::clearCache);
    }

    private Amoled() { }

    /** AMOLED şu an etkin mi? (kullanıcı anahtarı açık ya da gece penceresi içinde) */
    static boolean on() {
        return Prefs.on(Prefs.AMOLED) || Prefs.on(Prefs.SOFT_BLACK)
                || Prefs.opacity() > 0 || Night.activeNow();
    }

    static void install() {
        hook(Resources.class, "getColor", int.class);
        hook(Resources.class, "getColor", int.class, Resources.Theme.class);
        hook(Resources.class, "getColorStateList", int.class);
        hook(Resources.class, "getColorStateList", int.class, Resources.Theme.class);
        hook(TypedArray.class, "getColor", int.class, int.class);
        hook(TypedArray.class, "getColorStateList", int.class);
        RuntimeLog.log("amoled: hooks installed" + (Prefs.opacity() > 0 ? ", opacity=" + Prefs.opacity() : ""));
    }

    private static void hook(Class<?> owner, String name, Class<?>... parameters) {
        try {
            Method method = owner.getDeclaredMethod(name, parameters);
            Hooks.hook(method, new Hooks.Hook() {
                @Override protected void after(Hooks.Call call) {
                    if (!Amoled.on()) return;
                    Object result = call.getResult();
                    if (result instanceof ColorStateList) {
                        call.setResult(remapStateList((ColorStateList) result));
                    } else if (result instanceof Integer) {
                        call.setResult(remap((Integer) result, Prefs.on(Prefs.AMOLED_CARDS),
                                Prefs.on(Prefs.SOFT_BLACK), Prefs.opacity()));
                    }
                }
            });
        } catch (Throwable error) {
            RuntimeLog.log("amoled: cannot hook " + owner.getSimpleName() + "." + name + ": " + error);
        }
    }

    /**
     * Saf renk kararı — birim testlerle doğrulanır (bkz. AmoledTest).
     *
     * @param color ARGB renk
     * @param cards kart tonları da karartılsın mı
     * @return siyah ya da değiştirilmemiş renk
     */
    static int remap(int color, boolean cards) {
        return remap(color, cards, false);
    }

    /**
     * Saf renk kararı — birim testlerle doğrulanır (bkz. AmoledTest).
     * Yumuşak siyah açıkken hedef saf siyah değil koyu gridir (OLED yanmasına karşı).
     */
    static int remap(int color, boolean cards, boolean softBlack) {
        return remap(color, cards, softBlack, 0);
    }

    /**
     * Saf renk kararı — arayüz opaklığı dahil.
     *
     * @param opacity yüzde: 0/100 = tamamen opak; arada kalan değer koyu yüzeyi yarı
     *                saydam yapar (TikTokYou'daki "anti-burn-in" etkisi: statik koyu
     *                alanların ekranda bıraktığı iz azalır).
     */
    static int remap(int color, boolean cards, boolean softBlack, int opacity) {
        if (((color >>> 24) & 0xFF) != 0xFF) return color;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int max = Math.max(r, Math.max(g, b));
        int min = Math.min(r, Math.min(g, b));
        if (max - min > MAX_SPREAD) return color;
        if (max > (cards ? CARDS_LIMIT : BASE_LIMIT)) return color;
        int target = softBlack ? (cards ? SOFT_CARD : SOFT_BLACK) : BLACK;
        return applyOpacity(target, opacity);
    }

    /** Yüzdeyi alfa kanalına çevirir; 0 ve 100 dokunulmamış renk döndürür. */
    static int applyOpacity(int color, int opacity) {
        if (opacity <= 0 || opacity >= 100) return color;
        int alpha = Math.round(255f * opacity / 100f);
        return (color & 0x00FFFFFF) | (alpha << 24);
    }

    /**
     * ColorStateList.getColors()/getStates() Android SDK'da **gizli** API'dir
     * (android.jar'da yok → doğrudan çağrı derleme hatası verir). Bu yüzden yansıma
     * ile erişiyoruz; Android 9+ kısıtları engellerse özellik sessizce devre dışı kalır
     * ve orijinal liste korunur (uygulama asla çökmez).
     */
    private static final Method GET_COLORS = hidden(ColorStateList.class, "getColors");
    private static final Method GET_STATES = hidden(ColorStateList.class, "getStates");
    private static volatile boolean stateListUnavailable;

    private static Method hidden(Class<?> owner, String name) {
        try {
            Method method = owner.getDeclaredMethod(name);
            method.setAccessible(true);
            return method;
        } catch (Throwable error) {
            return null;
        }
    }

    private static ColorStateList remapStateList(ColorStateList list) {
        if (list == null) return null;
        if (stateListUnavailable || GET_COLORS == null || GET_STATES == null) return list;
        boolean cards = Prefs.on(Prefs.AMOLED_CARDS);
        boolean soft = Prefs.on(Prefs.SOFT_BLACK);
        int opacity = Prefs.opacity();
        synchronized (CACHE) {
            ColorStateList cached = CACHE.get(list);
            if (cached != null) return cached;
        }
        int[] colors;
        int[][] states;
        try {
            colors = (int[]) GET_COLORS.invoke(list);
            states = (int[][]) GET_STATES.invoke(list);
        } catch (Throwable blocked) {
            // Gizli API erişimi engellendi → bir daha denemeyiz, durum listeleri kapsam dışı kalır.
            stateListUnavailable = true;
            RuntimeLog.log("amoled: ColorStateList gizli API erişilemedi, durum listeleri atlanıyor");
            return list;
        }
        if (colors == null || states == null) return list;
        int[] remapped = new int[colors.length];
        boolean changed = false;
        for (int index = 0; index < colors.length; index++) {
            remapped[index] = remap(colors[index], cards, soft, opacity);
            if (remapped[index] != colors[index]) changed = true;
        }
        if (!changed) return list;
        try {
            // Genel kurucu: ColorStateList(int[][] states, int[] colors)
            ColorStateList result = new ColorStateList(states, remapped);
            synchronized (CACHE) {
                CACHE.put(list, result);
            }
            return result;
        } catch (Throwable error) {
            // Durum dizileri uyuşmazsa orijinali bırak — uygulama asla çökmesin.
            return list;
        }
    }

    private static void clearCache() {
        synchronized (CACHE) {
            CACHE.clear();
        }
    }
}
