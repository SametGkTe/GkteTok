package com.golda.patchertiktok;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Çökme özeti: sınıf adı, mesaj, yığın izi ve neden zinciri tek satırda görünmeli. */
public class CrashGuardTest {

    @Test
    public void messageAndClassAreIncluded() {
        String text = CrashGuard.describe(new IllegalStateException("ayar bozuk"));
        assertTrue(text.startsWith("java.lang.IllegalStateException: ayar bozuk"));
        assertTrue(text.contains("CrashGuardTest"));
    }

    @Test
    public void causeChainIsAppended() {
        Throwable root = new NullPointerException("bos deger");
        String text = CrashGuard.describe(new RuntimeException("ust", root));
        assertTrue(text.contains("java.lang.RuntimeException: ust"));
        assertTrue(text.contains("nedeni: java.lang.NullPointerException: bos deger"));
    }

    @Test
    public void messageMayBeMissing() {
        String text = CrashGuard.describe(new Throwable());
        assertEquals("java.lang.Throwable", text.split(" \\| ")[0]);
    }

    @Test
    public void nullIsSafe() {
        assertEquals("-", CrashGuard.describe(null));
    }
}
