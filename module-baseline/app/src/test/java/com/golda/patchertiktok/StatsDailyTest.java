package com.golda.patchertiktok;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.Test;

/** Günlük sayaç kovaları, kilometre taşları ve budama kuralı. */
public class StatsDailyTest {

    @Test
    public void daysRoundTrip() {
        Map<Long, Integer> days = Stats.parseDays("100:2,101:3,102:7");
        assertEquals(3, days.size());
        assertEquals(Integer.valueOf(2), days.get(100L));
        assertEquals("100:2,101:3,102:7", Stats.encodeDays(days));
        assertEquals(days, Stats.parseDays(Stats.encodeDays(days)));
    }

    @Test
    public void malformedPairsAreSkipped() {
        Map<Long, Integer> days = Stats.parseDays("bozuk,x:y,102:5,,103:");
        assertEquals(1, days.size());
        assertEquals(Integer.valueOf(5), days.get(102L));
        assertTrue(Stats.parseDays(null).isEmpty());
        assertTrue(Stats.parseDays("").isEmpty());
    }

    @Test
    public void pruningDropsOldBucketsOnly() {
        Map<Long, Integer> days = Stats.parseDays("10:5,40:3,46:1");
        Stats.prune(days, 45L);
        assertFalse("30 günden eski kova kalmalı değil", days.containsKey(10L));
        assertTrue(days.containsKey(40L));
        assertTrue(days.containsKey(46L));
    }

    @Test
    public void milestonesAreCrossedOnce() {
        assertEquals(0, Stats.crossed(0, 49));
        assertEquals(50, Stats.crossed(0, 50));
        assertEquals(50, Stats.crossed(0, 60));
        assertEquals(0, Stats.crossed(50, 99));
        assertEquals(250, Stats.crossed(50, 250));
        assertEquals(100_000, Stats.crossed(50_000, 120_000));
    }

    @Test
    public void nextGoalSkipsReachedOnes() {
        assertEquals(50, Stats.nextMilestone(0));
        assertEquals(100, Stats.nextMilestone(50));
        assertEquals(250, Stats.nextMilestone(120));
        assertEquals(100_000, Stats.nextMilestone(999_999));
    }
}
