package com.golda.patchertiktok;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Yazı tipi dosyası imzası: geçerli .ttf/.otf tanınır, diğer dosyalar reddedilir. */
public class FontOverrideTest {

    @Test
    public void trueTypeAndOpenTypeAreAccepted() {
        assertTrue(FontOverride.isFontHeader(new byte[]{0x00, 0x01, 0x00, 0x00}));
        assertTrue(FontOverride.isFontHeader(new byte[]{'O', 'T', 'T', 'O'}));
        assertTrue(FontOverride.isFontHeader(new byte[]{'t', 'r', 'u', 'e'}));
        assertTrue(FontOverride.isFontHeader(new byte[]{'t', 't', 'c', 'f'}));
    }

    @Test
    public void otherFilesAreRejected() {
        assertFalse(FontOverride.isFontHeader(new byte[]{'P', 'K', 3, 4}));      // zip/apk
        assertFalse(FontOverride.isFontHeader(new byte[]{'w', 'O', 'F', 'F'})); // web font
        assertFalse(FontOverride.isFontHeader(new byte[]{-1, -40, -1, -31}));    // jpeg
        assertFalse(FontOverride.isFontHeader(new byte[]{0x00, 0x01}));
        assertFalse(FontOverride.isFontHeader(null));
    }

    @Test
    public void systemFamiliesAreReplacedCustomOnesAreNot() {
        assertTrue(FontOverride.systemFamily("sans-serif"));
        assertTrue(FontOverride.systemFamily("sans-serif-medium"));
        assertTrue(FontOverride.systemFamily(null));
        assertFalse(FontOverride.systemFamily("TikTokSans"));
        assertFalse(FontOverride.systemFamily("IconFont"));
    }
}
