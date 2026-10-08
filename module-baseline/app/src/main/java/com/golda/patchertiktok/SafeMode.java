package com.golda.patchertiktok;

import java.lang.reflect.Member;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * GkteTok — güvenli mod.
 *
 * Bir kanca (hook) art arda hata verirse o kanca **kendini kapatır**: kurulu kalır ama
 * geri çağrıları artık çalışmaz. Böylece tek bir özellikteki hata TikTok'u rahatsız
 * etmeye devam etmez. Kapatılanlar "Sağlık" ekranında görünür ve sıfırlanabilir.
 *
 * Neden kancayı sökmek yerine sessizleştirmek? libxposed/LSPatch sürümleri arasında
 * sökme (unhook) desteği değişken; sessizleştirme her sürümde çalışır ve geri alınabilir.
 */
final class SafeMode {
    /** Bu sayıya ulaşan kanca kapatılır. */
    static final int LIMIT = 8;

    private static final Map<Member, Integer> ERRORS = new ConcurrentHashMap<>();
    private static final Set<Member> OFF = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private SafeMode() { }

    static boolean blocked(Member member) {
        return member != null && !OFF.isEmpty() && OFF.contains(member);
    }

    static void report(Member member, Throwable error) {
        if (member == null || !Prefs.on(Prefs.SAFE_MODE)) return;
        int count = ERRORS.merge(member, 1, Integer::sum);
        if (count == LIMIT && OFF.add(member)) {
            String label = member.getDeclaringClass().getSimpleName() + "#" + member.getName();
            RuntimeLog.log("safe mode: " + label + " turned off after " + count + " errors");
        }
    }

    static int offCount() {
        return OFF.size();
    }

    static void reset() {
        OFF.clear();
        ERRORS.clear();
    }
}
