package com.golda.patchertiktok;

import android.content.Context;

import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * GkteTok — sağlık durumu ve tanı raporu.
 *
 * İki şeyi toplar:
 *   • Kurulum durumu: hangi özellik kuruldu (✅), hangisi hata verdi (❌) ve neden.
 *     {@link Module#run} her görevden sonra buraya yazar.
 *   • Günlük izleri: modülün yazdığı son satırlar ({@link RuntimeLog} buraya da gönderir).
 *
 * Böylece ayarlardaki "Sağlık" ekranı tek bakışta neyin çalıştığını gösterir ve
 * "Tanı raporu oluştur" düğmesi bu bilgiyi tek dosyaya yazar.
 */
final class Health {
    /** Modül sürümü — {@link Module} başlangıçta bir kez yazar. */
    private static volatile String version = "";
    private static volatile String packageName = "";

    private static final Map<String, String> STATUS = new LinkedHashMap<>();
    private static final ArrayDeque<String> LINES = new ArrayDeque<>();
    private static final int MAX_LINES = 120;

    private Health() { }

    static synchronized void record(String name, boolean ok, String detail) {
        STATUS.put(name, ok ? "ok" : "fail" + (detail == null || detail.isEmpty() ? "" : ": " + detail));
    }

    static void startup(String versionName, String packageNameValue) {
        version = versionName;
        packageName = packageNameValue;
    }

    /** {@link RuntimeLog} her satırı buraya da verir; en eskiler düşer. */
    static synchronized void note(String line) {
        if (line == null) return;
        LINES.addLast(line);
        while (LINES.size() > MAX_LINES) LINES.removeFirst();
    }

    static synchronized Map<String, String> statuses() {
        return new LinkedHashMap<>(STATUS);
    }

    static synchronized List<String> lines() {
        return new ArrayList<>(LINES);
    }

    static synchronized void clearLines() {
        LINES.clear();
    }

    /** Ayarlar ekranının gösterdiği "kuruldu / hata" simgesi. */
    static boolean ok(String status) {
        return status != null && status.startsWith("ok");
    }

    /** Hata durumunda nedeni döndürür; yoksa boş metin. */
    static String detail(String status) {
        if (ok(status) || status == null) return "";
        if (!status.startsWith("fail")) return status;
        String rest = status.substring("fail".length());
        return rest.startsWith(":") ? rest.substring(1).trim() : rest.trim();
    }

    /** Tek dosyaya yazılan tanı raporu. */
    static String report() {
        StringBuilder out = new StringBuilder();
        out.append("GkteTok tani raporu\n");
        out.append("===================\n");
        out.append("Tarih    : ").append(new SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()).format(new Date())).append('\n');
        out.append("Surum    : ").append(version).append('\n');
        out.append("Paket    : ").append(packageName).append('\n');
        out.append("Reklam   : ").append(Stats.ads()).append(" engellendi, bugun ").append(Stats.todayAds()).append('\n');
        out.append("Acilis   : ").append(Stats.opens()).append(" kez, ").append(Stats.days()).append(" gun\n");
        out.append('\n').append("KURULUM DURUMU\n");
        for (Map.Entry<String, String> entry : statuses().entrySet()) {
            out.append(ok(entry.getValue()) ? "  [OK]   " : "  [HATA] ").append(entry.getKey());
            if (!ok(entry.getValue())) out.append(" -> ").append(detail(entry.getValue()));
            out.append('\n');
        }
        out.append('\n').append("GUNLUK IZLERI\n");
        for (String line : lines()) out.append("  ").append(line).append('\n');
        return out.toString();
    }
}
