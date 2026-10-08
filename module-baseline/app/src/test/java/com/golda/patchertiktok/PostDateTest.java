package com.golda.patchertiktok;

import static org.junit.Assert.assertEquals;

import java.util.TimeZone;

import org.junit.Test;

/** Akışta gönderi tarihi: biçim ve güvenli ekleme kuralı. */
public class PostDateTest {
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    @Test
    public void stampUsesDayMonthYear() {
        // 2023-11-14 22:13:20 UTC
        assertEquals("14.11.2023", PostDate.stamp(1_700_000_000L, UTC));
    }

    @Test
    public void textGetsTheSuffixOnce() {
        // Saat diliminden bağımsız: beklenen damga aynı kuralla üretilir.
        // (Sabit "14.11.2023" yazmak Türkiye saatinde 15.11.2023 verirdi.)
        CharSequence result = PostDate.stamped("İlk kar", 1_700_000_000L);
        assertEquals("İlk kar · " + PostDate.stamp(1_700_000_000L, TimeZone.getDefault()),
                result.toString());
        org.junit.Assert.assertTrue("onek korunmali", result.toString().startsWith("İlk kar · "));
    }

    @Test
    public void missingValuesAreLeftAlone() {
        assertEquals("aciklama", PostDate.stamped("aciklama", 0L).toString());
        assertEquals(null, PostDate.stamped(null, 1L));
    }
}
