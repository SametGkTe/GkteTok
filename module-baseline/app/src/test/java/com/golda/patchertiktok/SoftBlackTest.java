package com.golda.patchertiktok;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** Yumuşak siyah: saf siyah yerine koyu gri hedef; diğer kurallar AMOLED ile aynı. */
public class SoftBlackTest {

    @Test
    public void softBlackReplacesPureBlack() {
        assertEquals(0xFF121212, Amoled.remap(0xFF0A0A0A, false, true));
        assertEquals(0xFF121212, Amoled.remap(0xFF000000, false, true));
        assertEquals(0xFF000000, Amoled.remap(0xFF0A0A0A, false, false));
    }

    @Test
    public void cardsUseSlightlyLighterGrey() {
        assertEquals(0xFF1C1C1C, Amoled.remap(0xFF202020, true, true));
        assertEquals(0xFF1C1C1C, Amoled.remap(0xFF0A0A0A, true, true));
    }

    @Test
    public void coloursAndLightSurfacesAreUntouched() {
        assertEquals(0xFFFE2C55, Amoled.remap(0xFFFE2C55, false, true));
        assertEquals(0xFFF5F5F5, Amoled.remap(0xFFF5F5F5, false, true));
        assertEquals(0x80000000, Amoled.remap(0x80000000, false, true));
    }

    @Test
    public void twoArgumentOverloadKeepsPureBlack() {
        assertEquals(0xFF000000, Amoled.remap(0xFF0A0A0A, false));
    }
}
