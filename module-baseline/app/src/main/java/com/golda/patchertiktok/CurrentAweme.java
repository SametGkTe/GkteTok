package com.golda.patchertiktok;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * GkteTok — "şu an oynatılan video" ortak erişimi.
 *
 * Akış paneli ({@code BaseListFragmentPanel}) ilk kare çizildiğinde hatırlanır; güncel
 * video, panelin kendi getter'ı ile alınır. İndirme seçenekleri (sadece ses / kalite)
 * ve akışta gösterilen tarih bu nesneye ihtiyaç duyar. Bulunamazsa `null` döner ve
 * ilgili özellik sessizce devre dışı kalır (günlüğe yazılır).
 */
final class CurrentAweme {
    private static final String PANEL = "com.ss.android.ugc.aweme.feed.panel.BaseListFragmentPanel";
    private static final String AWEME = "com.ss.android.ugc.aweme.feed.model.Aweme";

    private static volatile Object panel;
    private static volatile Method getter;

    private CurrentAweme() { }

    static void install(ClassLoader loader) {
        if (getter != null) return;
        Class<?> panelType = Hooks.findClass(PANEL, loader);
        if (panelType == null) return;
        Method found = null;
        for (Method method : panelType.getDeclaredMethods()) {
            if (method.getParameterTypes().length != 0) continue;
            if (!AWEME.equals(method.getReturnType().getName())) continue;
            if ("getCurrentAweme".equals(method.getName()) || Modifier.isPublic(method.getModifiers())) {
                found = method;
                break;
            }
        }
        if (found == null) return;
        found.setAccessible(true);
        getter = found;
        Hooks.hookAll(panelType, "onRenderFirstFrame", new Hooks.Hook() {
            @Override protected void after(Hooks.Call param) {
                panel = param.thisObject;
            }
        });
    }

    /** Şu an oynatılan video nesnesi; yoksa null. */
    static Object get() {
        try {
            Method method = getter;
            Object instance = panel;
            if (method == null || instance == null) return null;
            return method.invoke(instance);
        } catch (Throwable error) {
            return null;
        }
    }
}
