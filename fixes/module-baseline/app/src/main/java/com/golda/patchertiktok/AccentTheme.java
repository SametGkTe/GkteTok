package com.golda.patchertiktok;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * GkteTok — vurgu (tema) rengi.
 *
 * TikTok'un marka renkleri sabittir: ana renk kırmızı (#FE2C55 ailesi), ikincil renk
 * turkuazdır (#25F4EE ailesi). Bu sınıf AMOLED ile aynı Android renk giriş noktalarını
 * kancalar ve **yalnızca bu iki aileyi** seçilen paletle değiştirir; başka hiçbir renge
 * dokunmaz (fotoğraflar, avatarlar, içerik renkleri korunur).
 *
 * Güvenlik kuralları:
 *   • Alfa ≥ 0xF0 olmalı → yarı saydam katmanlar korunur
 *   • Renk, referans tonlardan birine kanal başına ≤ 34 sapma ile yakın olmalı
 *   • Palet "kapalı" ise hiçbir şey değişmez
 *
 * Renk eşlemesi saf bir fonksiyondur (bkz. AccentThemeTest).
 */
final class AccentTheme {
    /** Kanal başına izin verilen sapma. */
    private static final int TOLERANCE = 34;
    private static final int ALPHA_MIN = 0xF0;

    /** Palet kimlikleri → {ana renk (kırmızı ailesi), ikincil renk (turkuaz ailesi)}. */
    /** Material You (duvar kağıdından gelen sistem rengi) palet kimliği. */
    static final String SYSTEM = "system";

    private static final String[] IDS = {"teal", "purple", "green", "gold", "blue"};
    private static final int[][] PALETTES = {
            {0xFF25F4EE, 0xFFFE2C55},   // Turkuaz (TikTok'un ikincil rengi ana renk olur)
            {0xFFA855F7, 0xFFF472B6},   // Mor
            {0xFF22C55E, 0xFF86EFAC},   // Yeşil
            {0xFFF59E0B, 0xFFFDE68A},   // Altın
            {0xFF3B82F6, 0xFF93C5FD},   // Mavi
    };

    /** TikTok'un kırmızı ailesi (kullanılan tonlar ve yakın varyantları). */
    private static final int[] RED_FAMILY = {0xFFFE2C55, 0xFFFF3B5C, 0xFFFF2C55, 0xFFFE2C54, 0xFFF0234B};
    /** TikTok'un turkuaz ailesi. */
    private static final int[] TEAL_FAMILY = {0xFF25F4EE, 0xFF20D5D2, 0xFF2BE8DC};

    private static final Map<String, Integer> INDEX;

    static {
        Map<String, Integer> index = new HashMap<>();
        for (int i = 0; i < IDS.length; i++) index.put(IDS[i], i);
        INDEX = Collections.unmodifiableMap(index);
    }

    private AccentTheme() { }

    /** Geçersiz kimlik boş dizeye (kapalı) döner. */
    static String normalize(String id) {
        if (SYSTEM.equals(id)) return SYSTEM;
        return id != null && INDEX.containsKey(id) ? id : "";
    }

    /** Ayarlardaki tüm seçenekler: 5 sabit palet + sistem rengi. */
    static String[] choices() {
        String[] all = new String[IDS.length + 1];
        System.arraycopy(IDS, 0, all, 0, IDS.length);
        all[IDS.length] = SYSTEM;
        return all;
    }

    static String[] ids() {
        return IDS.clone();
    }

    /** Paletin ana rengi; kapalıyken 0 döner. Sistem rengi okunamazsa turkuaza düşer. */
    static int primary(String id) {
        String normalized = normalize(id);
        if (normalized.isEmpty()) return 0;
        if (SYSTEM.equals(normalized)) {
            int[] system = DynamicColor.accent();
            return system != null ? system[0] : PALETTES[0][0];
        }
        return PALETTES[INDEX.get(normalized)][0];
    }

    /**
     * Saf renk eşlemesi — birim testlerle doğrulanır (bkz. AccentThemeTest).
     *
     * @param color   ARGB renk
     * @param palette palet kimliği ("" = kapalı)
     * @return değiştirilmiş ya da dokunulmamış renk
     */
    static int map(int color, String palette) {
        return map(color, palette, PALETTES[0][0], PALETTES[0][1]);
    }

    /**
     * Sistem rengi (Material You) seçildiğinde okunan renkler dışarıdan verilir; böylece
     * eşleme saf kalır ve birim testlerle doğrulanabilir (bkz. AccentThemeTest).
     */
    static int map(int color, String palette, int systemPrimary, int systemSecondary) {
        String normalized = normalize(palette);
        if (normalized.isEmpty()) return color;
        int primary;
        int secondary;
        if (SYSTEM.equals(normalized)) {
            primary = systemPrimary;
            secondary = systemSecondary;
        } else {
            int index = INDEX.get(normalized);
            primary = PALETTES[index][0];
            secondary = PALETTES[index][1];
        }
        int alpha = (color >>> 24) & 0xFF;
        if (alpha < ALPHA_MIN) return color;
        if (near(color, RED_FAMILY)) return withAlpha(primary, alpha);
        if (near(color, TEAL_FAMILY)) return withAlpha(secondary, alpha);
        return color;
    }

    private static boolean near(int color, int[] family) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        for (int reference : family) {
            int dr = Math.abs(r - ((reference >> 16) & 0xFF));
            int dg = Math.abs(g - ((reference >> 8) & 0xFF));
            int db = Math.abs(b - (reference & 0xFF));
            if (dr <= TOLERANCE && dg <= TOLERANCE && db <= TOLERANCE) return true;
        }
        return false;
    }

    private static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (alpha << 24);
    }

    // ---- kancalar ---------------------------------------------------------------------------

    static void install() {
        String palette = Prefs.accent();
        hook(Resources.class, "getColor", int.class);
        hook(Resources.class, "getColor", int.class, Resources.Theme.class);
        hook(Resources.class, "getColorStateList", int.class);
        hook(Resources.class, "getColorStateList", int.class, Resources.Theme.class);
        hook(TypedArray.class, "getColor", int.class, int.class);
        hook(TypedArray.class, "getColorStateList", int.class);
        RuntimeLog.log("accent theme: " + (palette.isEmpty() ? "off" : palette));
    }

    private static void hook(Class<?> owner, String name, Class<?>... parameters) {
        try {
            Method method = owner.getDeclaredMethod(name, parameters);
            Hooks.hook(method, new Hooks.Hook() {
                @Override protected void after(Hooks.Call call) {
                    String palette = Prefs.accent();
                    if (palette.isEmpty()) return;
                    int[] system = SYSTEM.equals(palette) ? DynamicColor.accent() : null;
                    int primary = system != null ? system[0] : PALETTES[0][0];
                    int secondary = system != null ? system[1] : PALETTES[0][1];
                    Object result = call.getResult();
                    if (result instanceof ColorStateList) {
                        call.setResult(StateListTools.map((ColorStateList) result,
                                color -> map(color, palette, primary, secondary)));
                    } else if (result instanceof Integer) {
                        call.setResult(map((Integer) result, palette, primary, secondary));
                    }
                }
            });
        } catch (Throwable error) {
            RuntimeLog.log("accent theme: cannot hook " + owner.getSimpleName() + "." + name + ": " + error);
        }
    }
}
