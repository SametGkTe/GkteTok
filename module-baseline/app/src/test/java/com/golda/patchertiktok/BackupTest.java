package com.golda.patchertiktok;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Ayar yedeği: yaz → oku turu ve bozuk/eksik satırlara dayanıklılık.
 */
public class BackupTest {

    @Test
    public void encodeCarriesEveryKindOfSetting() {
        Prefs.set(Prefs.ADS, true);
        Prefs.set(Prefs.BANNER, true);
        Prefs.set(Prefs.NIGHT, true);
        Prefs.setNightHours(23, 6);
        Prefs.setRegion("de");
        Prefs.setAccent("purple");
        Prefs.setFontPath("/data/test/gkte-font.ttf");

        String text = Backup.encode();
        assertTrue(text.startsWith(Backup.HEADER));
        assertTrue(text.contains("block_ads=true"));
        assertTrue(text.contains("ttplus_night=true"));
        assertTrue(text.contains("ttplus_night_from=23"));
        assertTrue(text.contains("ttplus_night_to=6"));
        assertTrue(text.contains("region=de"));
        assertTrue(text.contains("ttplus_accent=purple"));
        assertTrue(text.contains("ttplus_font_path=/data/test/gkte-font.ttf"));
    }

    @Test
    public void roundTripRestoresValues() {
        Prefs.set(Prefs.ADS, true);
        Prefs.set(Prefs.NIGHT, false);
        Prefs.setNightHours(22, 7);
        Prefs.setRegion("de");
        Prefs.setAccent("gold");
        String saved = Backup.encode();

        Prefs.set(Prefs.ADS, false);
        Prefs.set(Prefs.NIGHT, true);
        Prefs.setNightHours(1, 2);
        Prefs.setRegion("us");
        Prefs.setAccent("");

        int applied = Backup.apply(saved);
        assertTrue("en az 5 ayar uygulanmalı", applied >= 5);
        assertTrue(Prefs.on(Prefs.ADS));
        assertFalse(Prefs.on(Prefs.NIGHT));
        assertEquals(22, Prefs.nightFrom());
        assertEquals(7, Prefs.nightTo());
        assertEquals("de", Prefs.region());
        assertEquals("gold", Prefs.accent());
    }

    @Test
    public void unknownAndMalformedLinesAreIgnored() {
        String text = Backup.HEADER + "\n"
                + "\n"
                + "# yorum satiri\n"
                + "bilinmeyen_anahtar=true\n"
                + "bozuk satir\n"
                + "=deger\n"
                + "block_ads=1\n"
                + "hide_live=0\n"
                + "ttplus_night_from=abc\n";
        int applied = Backup.apply(text);
        assertEquals(2, applied);
        assertTrue(Prefs.on(Prefs.ADS));
        assertFalse(Prefs.on(Prefs.LIVE));
    }

    @Test
    public void nullTextIsHarmless() {
        assertEquals(0, Backup.apply(null));
    }
}
