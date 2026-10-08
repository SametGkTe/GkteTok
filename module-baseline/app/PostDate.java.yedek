package com.golda.patchertiktok;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * GkteTok — akışta gönderi tarihi.
 *
 * Akışta bir videonun açıklaması çizilirken metnin sonuna yüklenme tarihi eklenir:
 *
 *     "İlk kar yağışı"  →  "İlk kar yağışı · 04.10.2026"
 *
 * Nasıl güvenli yapılıyor? Ekrandaki HER metne dokunmuyoruz: yalnızca metin, o an
 * oynatılan videonun **açıklamasıyla birebir aynıysa** ekleniyor. Böylece mesajlar,
 * yorumlar ve başka ekranlar etkilenmiyor; tarih iki kez eklenmiyor (eklenmiş metin
 * artık açıklamaya eşit olmadığı için).
 */
final class PostDate {
    private static final String PANEL = "com.ss.android.ugc.aweme.feed.panel.BaseListFragmentPanel";
    private static final String AWEME = "com.ss.android.ugc.aweme.feed.model.Aweme";

    private static volatile Object panel;
    private static volatile Method currentAweme;
    private static volatile Method getDesc;
    private static volatile Method getCreateTime;

    private PostDate() { }

    static void install(ClassLoader loader) {
        int hooks = hookText();
        boolean feed = discoverFeed(loader);
        RuntimeLog.log("post date: hooks=" + hooks + (feed ? ", feed=ready" : ", feed=missing"));
    }

    /** Akış panelini ve güncel video erişimini bulur. */
    static boolean discoverFeed(ClassLoader loader) {
        Class<?> panelType = Hooks.findClass(PANEL, loader);
        if (panelType == null) return false;
        Method getter = null;
        for (Method method : panelType.getDeclaredMethods()) {
            if (method.getParameterTypes().length == 0 && AWEME.equals(method.getReturnType().getName())) {
                if (Modifier.isPublic(method.getModifiers()) || "getCurrentAweme".equals(method.getName())) {
                    getter = method;
                    break;
                }
            }
        }
        if (getter == null) return false;
        getter.setAccessible(true);
        currentAweme = getter;
        Hooks.hookAll(panelType, "onRenderFirstFrame", new Hooks.Hook() {
            @Override protected void after(Hooks.Call param) {
                panel = param.thisObject;
            }
        });
        return true;
    }

    private static Object aweme() {
        try {
            Method getter = currentAweme;
            Object instance = panel;
            if (getter == null || instance == null) return null;
            return getter.invoke(instance);
        } catch (Throwable error) {
            return null;
        }
    }

    private static int hookText() {
        int hooks = 0;
        for (Method method : TextView.class.getDeclaredMethods()) {
            if (!"setText".equals(method.getName())) continue;
            Class<?>[] parameters = method.getParameterTypes();
            if (parameters.length != 1 || parameters[0] != CharSequence.class) continue;
            Hooks.hook(method, new Hooks.Hook() {
                @Override protected void before(Hooks.Call param) {
                    if (!Prefs.on(Prefs.POST_DATE) || param.args.length == 0) return;
                    Object text = param.args[0];
                    if (!(text instanceof CharSequence)) return;
                    Object current = aweme();
                    if (current == null) return;
                    String desc = descOf(current);
                    if (desc == null || !desc.contentEquals((CharSequence) text)) return;
                    param.args[0] = stamped((CharSequence) text, createdOf(current));
                }
            });
            hooks++;
        }
        return hooks;
    }

    private static String descOf(Object aweme) {
        try {
            Method method = getDesc;
            if (method == null) {
                method = aweme.getClass().getMethod("getDesc");
                getDesc = method;
            }
            Object value = method.invoke(aweme);
            return value instanceof String ? (String) value : null;
        } catch (Throwable error) {
            return null;
        }
    }

    private static long createdOf(Object aweme) {
        try {
            Method method = getCreateTime;
            if (method == null) {
                method = aweme.getClass().getMethod("getCreateTime");
                getCreateTime = method;
            }
            Object value = method.invoke(aweme);
            return value instanceof Number ? ((Number) value).longValue() : 0L;
        } catch (Throwable error) {
            return 0L;
        }
    }

    /** "açıklama" + " · 04.10.2026" (saf fonksiyon; bkz. PostDateTest). */
    static CharSequence stamped(CharSequence text, long createTimeSeconds) {
        if (text == null || createTimeSeconds <= 0L) return text;
        return text + " · " + stamp(createTimeSeconds, TimeZone.getDefault());
    }

    /** Yüklenme zamanını gg.AA.yyyy olarak biçimler. */
    static String stamp(long createTimeSeconds, TimeZone zone) {
        SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy", Locale.US);
        format.setTimeZone(zone);
        return format.format(new Date(createTimeSeconds * 1000L));
    }
}
