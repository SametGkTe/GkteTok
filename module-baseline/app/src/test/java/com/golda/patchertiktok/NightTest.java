package com.golda.patchertiktok;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Gece penceresi mantığı — özellikle gece yarısını aşan aralıklar (22 → 07).
 */
public class NightTest {

    @Test
    public void windowAcrossMidnight() {
        assertTrue(Night.inWindow(22, 22, 7));
        assertTrue(Night.inWindow(23, 22, 7));
        assertTrue(Night.inWindow(0, 22, 7));
        assertTrue(Night.inWindow(6, 22, 7));
        assertFalse("bitiş saati pencerenin dışında", Night.inWindow(7, 22, 7));
        assertFalse(Night.inWindow(12, 22, 7));
        assertFalse(Night.inWindow(21, 22, 7));
    }

    @Test
    public void plainDayWindow() {
        assertTrue(Night.inWindow(8, 8, 17));
        assertTrue(Night.inWindow(16, 8, 17));
        assertFalse(Night.inWindow(17, 8, 17));
        assertFalse(Night.inWindow(7, 8, 17));
    }

    @Test
    public void equalHoursMeanAllDay() {
        assertTrue(Night.inWindow(0, 3, 3));
        assertTrue(Night.inWindow(15, 3, 3));
        assertTrue(Night.inWindow(23, 0, 0));
    }
}
