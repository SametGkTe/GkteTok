package com.golda.patchertiktok;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * GkteTok — açılış logosu (GkTeLogo.png).
 *
 * Logo iki yoldan gelebilir:
 *   1) Kullanıcı ayarlardan bir PNG seçer (en güvenilir yol; dosya uygulama verisine kopyalanır)
 *   2) Modülün içine gömülür: module-baseline/app/src/main/assets/GkTeLogo.png
 *      → LSPatch modül APK'sını `assets/lspatch/modules/<paket>.apk` olarak yamalı uygulamaya
 *        gömer; çalışma anında bu APK'yı önbelleğe çıkarır. İki yeri de tarıyoruz.
 *
 * Hiçbiri yoksa açılış ekranı yalnızca yazıyla devam eder — hata çıkarmaz.
 * Sonuç süreç başına bir kez yüklenir (bkz. `tried`).
 */
final class Logo {
    private static final String ENTRY = "assets/GkTeLogo.png";
    private static final String FILE_NAME = "GkTeLogo.png";
    private static final int MAX_ENTRY_BYTES = 4 * 1024 * 1024;
    private static final int MAX_SIDE = 640;

    private static volatile Bitmap cached;
    private static volatile boolean tried;

    private Logo() { }

    static Bitmap load(Context context) {
        Bitmap current = cached;
        if (current != null) return current;
        if (tried) return null;
        tried = true;

        Bitmap picked = fromPickedFile();
        if (picked != null) {
            cached = picked;
            RuntimeLog.log("gkte splash: logo loaded (picked)");
            return picked;
        }
        Bitmap embedded = fromModuleApk(context);
        if (embedded == null) embedded = fromAssets(context);
        if (embedded == null) embedded = fromClassLoader();
        if (embedded != null) {
            cached = embedded;
            RuntimeLog.log("gkte splash: logo loaded (bundled)");
            return embedded;
        }
        RuntimeLog.log("gkte splash: logo not found");
        return null;
    }

    // ---- 1) kullanıcının seçtiği dosya ------------------------------------------------------

    private static Bitmap fromPickedFile() {
        String path = Prefs.logoPath();
        if (path.isEmpty()) return null;
        return decodeFile(new File(path));
    }

    // ---- 2) önbelleğe çıkarılmış modül APK'sı ----------------------------------------------

    private static Bitmap fromModuleApk(Context context) {
        File[] roots = {context.getCacheDir(), context.getFilesDir(), context.getDir("lspatch", Context.MODE_PRIVATE)};
        for (File root : roots) {
            File lspatch = new File(root, "lspatch");
            Bitmap found = scan(lspatch, 0);
            if (found != null) return found;
            found = scan(root, 0);
            if (found != null) return found;
        }
        return null;
    }

    private static Bitmap scan(File dir, int depth) {
        if (dir == null || depth > 4) return null;
        File[] children = dir.listFiles();
        if (children == null) return null;
        for (File child : children) {
            if (child.isDirectory()) {
                Bitmap nested = scan(child, depth + 1);
                if (nested != null) return nested;
            } else if (child.getName().endsWith(".apk") && child.length() < 64L * 1024 * 1024) {
                Bitmap found = fromZip(child);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static Bitmap fromZip(File apk) {
        ZipFile zip = null;
        try {
            zip = new ZipFile(apk);
            ZipEntry entry = zip.getEntry(ENTRY);
            if (entry == null || entry.getSize() > MAX_ENTRY_BYTES) return null;
            try (InputStream in = zip.getInputStream(entry)) {
                return decode(read(in));
            }
        } catch (Throwable error) {
            return null;
        } finally {
            if (zip != null) try {
                zip.close();
            } catch (Throwable ignored) { }
        }
    }

    // ---- 3) yamalı APK'nın içindeki gömülü modül -------------------------------------------

    private static Bitmap fromAssets(Context context) {
        AssetManager assets = context.getAssets();
        try {
            String[] modules = assets.list("lspatch/modules");
            if (modules == null) return null;
            for (String module : modules) {
                if (!module.endsWith(".apk")) continue;
                File temp = new File(context.getCacheDir(), "gkte-logo-src.apk");
                try (InputStream in = assets.open("lspatch/modules/" + module);
                     FileOutputStream out = new FileOutputStream(temp)) {
                    copy(in, out);
                }
                Bitmap found = fromZip(temp);
                // noinspection ResultOfMethodCallIgnored
                temp.delete();
                if (found != null) return found;
            }
        } catch (Throwable ignored) { }
        return null;
    }

    // ---- 4) sınıf yolundaki kaynak ---------------------------------------------------------

    private static Bitmap fromClassLoader() {
        try (InputStream in = Logo.class.getClassLoader().getResourceAsStream(ENTRY)) {
            return in == null ? null : decode(read(in));
        } catch (Throwable error) {
            return null;
        }
    }

    // ---- ortak -----------------------------------------------------------------------------

    private static Bitmap decodeFile(File file) {
        try {
            if (!file.isFile() || file.length() > MAX_ENTRY_BYTES) return null;
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(file.getAbsolutePath(), bounds);
            return BitmapFactory.decodeFile(file.getAbsolutePath(), options(bounds));
        } catch (Throwable error) {
            return null;
        }
    }

    private static Bitmap decode(byte[] data) {
        if (data == null || data.length == 0) return null;
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(data, 0, data.length, bounds);
        return BitmapFactory.decodeByteArray(data, 0, data.length, options(bounds));
    }

    /** Bellek için ikinin kuvveti örnekleme; en uzun kenarı {@link #MAX_SIDE} ile sınırlar. */
    private static BitmapFactory.Options options(BitmapFactory.Options bounds) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        int longest = Math.max(bounds.outWidth, bounds.outHeight);
        int sample = 1;
        while (longest / sample > MAX_SIDE) sample *= 2;
        options.inSampleSize = sample;
        return options;
    }

    private static byte[] read(InputStream in) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        int total = 0;
        while ((read = in.read(buffer)) > 0) {
            total += read;
            if (total > MAX_ENTRY_BYTES) return null;
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }

    private static void copy(InputStream in, FileOutputStream out) throws Exception {
        byte[] buffer = new byte[8192];
        int read;
        int total = 0;
        while ((read = in.read(buffer)) > 0) {
            total += read;
            if (total > 64 * 1024 * 1024) throw new IllegalStateException("module apk too big");
            out.write(buffer, 0, read);
        }
    }
}
