package com.golda.patchertiktok;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** Süre uyarısı: aralık katı hesabı. */
public class TimeLimitTest {

    @Test
    public void countsFullIntervalsOnly() {
        assertEquals(0, TimeLimit.dueCount(0L, 30));
        assertEquals(0, TimeLimit.dueCount(29L * 60_000L, 30));
        assertEquals(1, TimeLimit.dueCount(30L * 60_000L, 30));
        assertEquals(1, TimeLimit.dueCount(59L * 60_000L, 30));
        assertEquals(2, TimeLimit.dueCount(61L * 60_000L, 30));
    }

    @Test
    public void differentIntervals() {
        assertEquals(4, TimeLimit.dueCount(60L * 60_000L, 15));
        assertEquals(1, TimeLimit.dueCount(60L * 60_000L, 60));
        assertEquals(0, TimeLimit.dueCount(60L * 60_000L, 120));
    }

    @Test
    public void invalidIntervalIsHarmless() {
        assertEquals(0, TimeLimit.dueCount(10L * 60_000L, 0));
        assertEquals(0, TimeLimit.dueCount(10L * 60_000L, -5));
    }
}
