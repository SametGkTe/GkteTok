package com.golda.patchertiktok;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Version-independent lookup of obfuscated TikTok classes by string fingerprints.
 * Results are cached per TikTok build, so the dex scan runs once after each update.
 */
final class Discovery {
    private static final String FILE = "tiktokpatchxposed_index";
    private static final String NONE = "-";
    private static SharedPreferences cache;
    private static String build;
    private static ApplicationInfo info;
    private static File scratch;
    private static DexFinder finder;
    private static final Set<String> LOGGED = new HashSet<>();

    private Discovery() { }

    static synchronized void init(Context context) {
        if (cache != null) return;
        info = context.getApplicationInfo();
        scratch = context.getCacheDir();
        long version;
        try {
            android.content.pm.PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            version = android.os.Build.VERSION.SDK_INT >= 28 ? packageInfo.getLongVersionCode() : packageInfo.versionCode;
        } catch (Exception error) {
            version = 0;
        }
        build = version + ":" + new File(info.sourceDir).lastModified();
        LOGGED.clear();
        cache = context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
        if (!build.equals(cache.getString("build", null))) {
            cache.edit().clear().putString("build", build).apply();
        }
        RuntimeLog.log("discovery ready: " + build);
    }

    /** Classes whose code loads {@code literal}, in dex order. */
    static synchronized List<String> classesUsing(String literal) {
        if (cache == null) { logNotReady(); return Collections.emptyList(); }
        String key = "c:" + literal;
        String saved = cache.getString(key, null);
        if (saved == null) {
            long start = System.currentTimeMillis();
            List<String> found = new ArrayList<>(finder().classesUsing(literal));
            saved = found.isEmpty() ? NONE : String.join(",", found);
            cache.edit().putString(key, saved).apply();
            RuntimeLog.log("indexed " + key + " in " + (System.currentTimeMillis() - start) + " ms: " + saved);
        } else {
            remember(key, saved);
        }
        return NONE.equals(saved) ? Collections.emptyList() : Arrays.asList(saved.split(","));
    }

    /** "class#method" entries for methods whose code loads {@code literal}. */
    static synchronized List<String> methodsUsing(String literal) {
        if (cache == null) { logNotReady(); return Collections.emptyList(); }
        String key = "m:" + literal;
        String saved = cache.getString(key, null);
        if (saved == null) {
            long start = System.currentTimeMillis();
            List<String> found = new ArrayList<>();
            for (DexFinder.Hit hit : finder().methodsUsing(literal)) found.add(hit.className + "#" + hit.methodName);
            saved = found.isEmpty() ? NONE : String.join(",", found);
            cache.edit().putString(key, saved).apply();
            RuntimeLog.log("indexed " + key + " in " + (System.currentTimeMillis() - start) + " ms: " + saved);
        } else {
            remember(key, saved);
        }
        return NONE.equals(saved) ? Collections.emptyList() : Arrays.asList(saved.split(","));
    }

    /** "class#method" entries for the calls made from {@code className#methodName}. */
    static synchronized List<String> invokedBy(String className, String methodName) {
        if (cache == null) { logNotReady(); return Collections.emptyList(); }
        String key = "i:" + className + "#" + methodName;
        String saved = cache.getString(key, null);
        if (saved == null) {
            long start = System.currentTimeMillis();
            List<String> found = new ArrayList<>(new LinkedHashSet<>(finder().invokedBy(className, methodName)));
            saved = found.isEmpty() ? NONE : String.join(",", found);
            cache.edit().putString(key, saved).apply();
            RuntimeLog.log("indexed " + key + " in " + (System.currentTimeMillis() - start) + " ms: " + saved);
        } else {
            remember(key, saved);
        }
        return NONE.equals(saved) ? Collections.emptyList() : Arrays.asList(saved.split(","));
    }

    static Class<?> firstClass(ClassLoader loader, String literal) {
        for (String name : classesUsing(literal)) {
            try {
                return Class.forName(name, false, loader);
            } catch (Throwable ignored) { }
        }
        return null;
    }

    private static DexFinder finder() {
        if (finder == null) {
            List<String> paths = new ArrayList<>();
            paths.add(info.sourceDir);
            if (info.splitSourceDirs != null) {
                for (String split : info.splitSourceDirs) {
                    // Configuration splits (abi, density, language) carry no code.
                    if (!split.contains("split_config.")) paths.add(split);
                }
            }
            finder = new DexFinder(paths, scratch);
        }
        return finder;
    }

    private static boolean notReadyLogged;

    private static void logNotReady() {
        if (!notReadyLogged) {
            notReadyLogged = true;
            RuntimeLog.log("discovery not ready - init cagrilmadi");
        }
    }

    /** One diagnostic line per key and run, so a warm cache is still visible in the log. */
    private static void remember(String key, String value) {
        if (LOGGED.add(key)) RuntimeLog.log("discovery " + key + " = " + value);
    }

    /** Releases the mapped dex files once startup lookups are done. */
    static synchronized void release() {
        finder = null;
        // Mapped scratch dex are unlinked at once; a nudge helps the collector unmap them.
        System.gc();
    }
}
