package com.golda.patchertiktok;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** Arayüz opaklığı: koyu yüzeyler yarı saydam olur; 0/100 dokunmaz. */
public class OpacityTest {

    @Test
    public void percentBecomesAlpha() {
        assertEquals(0xCC000000, Amoled.remap(0xFF0A0A0A, false, false, 80));
        assertEquals(0xE6121212, Amoled.remap(0xFF101010, false, true, 90));
    }

    @Test
    public void opaqueValuesStayUntouched() {
        assertEquals(0xFF000000, Amoled.remap(0xFF0A0A0A, false, false, 0));
        assertEquals(0xFF000000, Amoled.remap(0xFF0A0A0A, false, false, 100));
        assertEquals(0xFF121212, Amoled.remap(0xFF0A0A0A, false, true, 100));
    }

    @Test
    public void applyOpacityOnlyInBetween() {
        assertEquals(0xB3121212, Amoled.applyOpacity(0xFF121212, 70));
        assertEquals(0xFF121212, Amoled.applyOpacity(0xFF121212, 0));
        assertEquals(0xFF121212, Amoled.applyOpacity(0xFF121212, 100));
    }

    @Test
    public void brightAndColouredSurfacesAreUntouched() {
        assertEquals(0xFFF5F5F5, Amoled.remap(0xFFF5F5F5, false, false, 80));
        assertEquals(0xFFFE2C55, Amoled.remap(0xFFFE2C55, false, false, 80));
    }
}
