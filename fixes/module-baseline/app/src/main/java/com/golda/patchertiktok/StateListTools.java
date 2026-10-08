package com.golda.patchertiktok;

import android.content.res.ColorStateList;

import java.lang.reflect.Method;

/**
 * Renk durum listelerini (ColorStateList) yeniden eşlemek için ortak yardımcı.
 *
 * {@code getColors()}/{@code getStates()} Android SDK'da gizli API'dir; yansıma ile
 * erişilir. Erişim engellenirse (Android 9+ kısıtları) özellik sessizce kapsam dışı
 * kalır ve **orijinal liste korunur** — uygulama asla çökmez.
 */
final class StateListTools {
    interface Mapper {
        int map(int color);
    }

    private static final Method GET_COLORS = hidden("getColors");
    private static final Method GET_STATES = hidden("getStates");
    private static volatile boolean unavailable;

    private StateListTools() { }

    private static Method hidden(String name) {
        try {
            Method method = ColorStateList.class.getDeclaredMethod(name);
            method.setAccessible(true);
            return method;
        } catch (Throwable error) {
            return null;
        }
    }

    static ColorStateList map(ColorStateList list, Mapper mapper) {
        if (list == null || unavailable || GET_COLORS == null || GET_STATES == null) return list;
        int[] colors;
        int[][] states;
        try {
            colors = (int[]) GET_COLORS.invoke(list);
            states = (int[][]) GET_STATES.invoke(list);
        } catch (Throwable blocked) {
            unavailable = true;
            RuntimeLog.log("state lists: hidden API unavailable, skipping");
            return list;
        }
        int[] mapped = new int[colors.length];
        boolean changed = false;
        for (int i = 0; i < colors.length; i++) {
            mapped[i] = mapper.map(colors[i]);
            if (mapped[i] != colors[i]) changed = true;
        }
        if (!changed) return list;
        try {
            return new ColorStateList(states, mapped);
        } catch (Throwable error) {
            return list;
        }
    }
}
