package com.golda.patchertiktok;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import java.lang.reflect.Method;
import java.util.Locale;

/**
 * GkteTok — mesaj (DM) kelime filtresi.
 *
 * Mesaj ekranında görünen bir metin, senin listendeki bir kelimeyi taşıyorsa
 * yerine "···" yazılır; yani mesaj **gizlenir ama silinmez**. Filtre yalnızca
 * mesaj ekranlarında çalışır (kural {@link KillSwitch#isPrivateScreen} ile aynı).
 *
 * Kapatmak için ayarlardaki anahtarı kapatmak yeterlidir; kelime listesi virgülle
 * ayrılır ve büyük/küçük harf farkı gözetilmez.
 */
final class DmFilter {
    private static final String MASK = "···";

    private DmFilter() { }

    static void install() {
        int hooks = 0;
        for (Method method : TextView.class.getDeclaredMethods()) {
            if (!"setText".equals(method.getName())) continue;
            Class<?>[] parameters = method.getParameterTypes();
            if (parameters.length != 1 || parameters[0] != CharSequence.class) continue;
            Hooks.hook(method, new Hooks.Hook() {
                @Override protected void before(Hooks.Call param) {
                    if (!Prefs.on(Prefs.DM_FILTER) || param.args.length == 0) return;
                    Object text = param.args[0];
                    if (!(text instanceof CharSequence) || ((CharSequence) text).length() == 0) return;
                    if (!KillSwitch.privateNow()) return;
                    if (!matches(text.toString(), Prefs.rawString(Prefs.DM_WORDS))) return;
                    param.args[0] = MASK;
                    RuntimeLog.log("dm filter: message masked");
                }
            });
            hooks++;
        }
        RuntimeLog.log("dm filter: hooks=" + hooks + " words=" + wordCount(Prefs.rawString(Prefs.DM_WORDS)));
    }

    /** Metin, listedeki kelimelerden birini taşıyor mu? (saf fonksiyon; bkz. DmFilterTest) */
    static boolean matches(String text, String words) {
        if (text == null || words == null || words.isEmpty()) return false;
        String lower = text.toLowerCase(locale());
        for (String word : words.split("[,\\n;]")) {
            String candidate = word.trim().toLowerCase(locale());
            if (candidate.isEmpty()) continue;
            if (lower.contains(candidate)) return true;
        }
        return false;
    }

    static int wordCount(String words) {
        if (words == null || words.isEmpty()) return 0;
        int count = 0;
        for (String word : words.split("[,\\n;]")) {
            if (!word.trim().isEmpty()) count++;
        }
        return count;
    }

    /** Türkçe "İ/ı" eşleşmesi için cihaz yereliyle küçültme yapılır. */
    private static Locale locale() {
        return Locale.getDefault();
    }
}
