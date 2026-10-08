package com.golda.patchertiktok;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

/**
 * Vurgu rengi eşlemesi: yalnızca TikTok'un kırmızı/turkuaz aileleri değişir,
 * diğer renkler ve yarı saydam katmanlar korunur.
 */
public class AccentThemeTest {
    private static final int RED = 0xFFFE2C55;
    private static final int TEAL = 0xFF25F4EE;
    private static final int PURPLE_PRIMARY = 0xFFA855F7;
    private static final int PURPLE_SECONDARY = 0xFFF472B6;

    @Test
    public void redFamilyBecomesPalettePrimary() {
        assertEquals(PURPLE_PRIMARY, AccentTheme.map(RED, "purple"));
        assertEquals(PURPLE_PRIMARY, AccentTheme.map(0xFFFF3B5C, "purple"));
    }

    @Test
    public void tealFamilyBecomesPaletteSecondary() {
        assertEquals(PURPLE_SECONDARY, AccentTheme.map(TEAL, "purple"));
        assertEquals(PURPLE_SECONDARY, AccentTheme.map(0xFF20D5D2, "purple"));
    }

    @Test
    public void tealPaletteSwapsBrandColours() {
        assertEquals(TEAL, AccentTheme.map(RED, "teal"));
        assertEquals(RED, AccentTheme.map(TEAL, "teal"));
    }

    @Test
    public void unrelatedColoursAreUntouched() {
        int olive = 0xFF7A8B2A;
        assertEquals(olive, AccentTheme.map(olive, "green"));
        assertEquals(0xFFFFFFFF, AccentTheme.map(0xFFFFFFFF, "blue"));
        assertEquals(0xFF000000, AccentTheme.map(0xFF000000, "blue"));
    }

    @Test
    public void translucentLayersAreLeftAlone() {
        int translucentRed = 0x80FE2C55;
        assertEquals(translucentRed, AccentTheme.map(translucentRed, "gold"));
    }

    @Test
    public void disabledPaletteChangesNothing() {
        assertEquals(RED, AccentTheme.map(RED, ""));
        assertEquals(TEAL, AccentTheme.map(TEAL, null));
        assertEquals("", AccentTheme.normalize("olmayan-palet"));
        assertEquals("", AccentTheme.normalize(null));
        assertEquals(0, AccentTheme.primary(""));
    }

    @Test
    public void paletteIdsAreStable() {
        assertEquals("teal", AccentTheme.normalize("teal"));
        assertEquals(TEAL, AccentTheme.primary("teal"));
        assertNotEquals(0, AccentTheme.primary("blue"));
    }
}
