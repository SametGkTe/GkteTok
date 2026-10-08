package com.golda.patchertiktok;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Beğeni koruması kararları ve indirme dosya adı öneki. */
public class GuardAndNamesTest {

    @Test
    public void doubleTapNeedsTimeAndDistance() {
        assertTrue(LikeGuard.isDoubleTap(1000L, 900L, 100f, 200f, 100f, 200f, 60f));
        assertFalse("320 ms'den sonra çift dokunuş değil",
                LikeGuard.isDoubleTap(1300L, 900L, 100f, 200f, 100f, 200f, 60f));
        assertFalse("uzak konum çift dokunuş sayılmaz",
                LikeGuard.isDoubleTap(1000L, 900L, 300f, 200f, 100f, 200f, 60f));
        assertFalse("önceki dokunuş yoksa koruma devreye girmez",
                LikeGuard.isDoubleTap(1000L, 0L, 100f, 200f, 100f, 200f, 60f));
    }

    @Test
    public void videoAreaIsTheScreenCentre() {
        assertTrue(LikeGuard.inVideoArea(540f, 1200f, 1080, 2400));
        assertFalse("sol kenar düğmeleri korunur", LikeGuard.inVideoArea(40f, 1200f, 1080, 2400));
        assertFalse("üst çubuk korunur", LikeGuard.inVideoArea(540f, 100f, 1080, 2400));
        assertFalse("alt çubuk korunur", LikeGuard.inVideoArea(540f, 2300f, 1080, 2400));
        assertFalse(LikeGuard.inVideoArea(540f, 1200f, 0, 2400));
    }

    @Test
    public void downloadNameGetsDatePrefix() {
        assertEquals("2026-10-04_video.mp4", DownloadName.prefix("video.mp4", "2026-10-04"));
        assertEquals("2026-10-04_video.mp4",
                DownloadName.prefix("2026-10-04_video.mp4", "2026-10-04"));
        assertEquals("video.mp4", DownloadName.prefix("video.mp4", ""));
        assertEquals(null, DownloadName.prefix(null, "2026-10-04"));
    }
}
