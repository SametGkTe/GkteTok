package com.golda.patchertiktok;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Materyal You (sistem rengi) paleti: verilen renklerle eşleme saf çalışır. */
public class AccentSystemTest {
    private static final int SYSTEM_PRIMARY = 0xFF3366CC;
    private static final int SYSTEM_SECONDARY = 0xFF99BBDD;

    @Test
    public void redFamilyTakesSystemPrimary() {
        assertEquals(0xFF3366CC, AccentTheme.map(0xFFFE2C55, AccentTheme.SYSTEM, SYSTEM_PRIMARY, SYSTEM_SECONDARY));
        assertEquals(0xFF3366CC, AccentTheme.map(0xFFFF3B5C, AccentTheme.SYSTEM, SYSTEM_PRIMARY, SYSTEM_SECONDARY));
    }

    @Test
    public void tealFamilyTakesSystemSecondary() {
        assertEquals(0xFF99BBDD, AccentTheme.map(0xFF25F4EE, AccentTheme.SYSTEM, SYSTEM_PRIMARY, SYSTEM_SECONDARY));
    }

    @Test
    public void otherColoursAreUntouched() {
        assertEquals(0xFF808080, AccentTheme.map(0xFF808080, AccentTheme.SYSTEM, SYSTEM_PRIMARY, SYSTEM_SECONDARY));
        assertEquals(0x80FE2C55, AccentTheme.map(0x80FE2C55, AccentTheme.SYSTEM, SYSTEM_PRIMARY, SYSTEM_SECONDARY));
    }

    @Test
    public void systemIsAValidChoiceAndKnownId() {
        assertEquals(AccentTheme.SYSTEM, AccentTheme.normalize(AccentTheme.SYSTEM));
        boolean found = false;
        for (String id : AccentTheme.choices()) {
            if (AccentTheme.SYSTEM.equals(id)) found = true;
        }
        assertTrue(found);
    }
}
