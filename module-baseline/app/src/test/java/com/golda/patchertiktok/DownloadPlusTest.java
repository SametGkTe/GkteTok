package com.golda.patchertiktok;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/** İndirme seçenekleri: klasör çevirisi, ses dosyası adı, en yüksek kalite seçimi. */
public class DownloadPlusTest {

    @Test
    public void folderReplacesTheTailOfThePath() {
        assertEquals("/storage/emulated/0/Movies/GkteTok",
                DownloadPlus.folderFor("/storage/emulated/0/DCIM/Camera", "Movies/GkteTok"));
        assertEquals("/storage/emulated/0/Download/GkteTok",
                DownloadPlus.folderFor("/storage/emulated/0/Download", "Download/GkteTok"));
        assertEquals("/sdcard/Download",
                DownloadPlus.folderFor("/sdcard/DCIM/Camera", "Download"));
    }

    @Test
    public void folderIsUntouchedWhenNothingIsChosen() {
        assertEquals("/storage/emulated/0/DCIM/Camera",
                DownloadPlus.folderFor("/storage/emulated/0/DCIM/Camera", ""));
        assertNull(DownloadPlus.folderFor(null, "Movies/GkteTok"));
        assertEquals("/weird/path", DownloadPlus.folderFor("/weird/path", "Movies/GkteTok"));
    }

    @Test
    public void audioNameBecomesM4a() {
        assertEquals("video.m4a", DownloadPlus.audioName("video.mp4"));
        assertEquals("video.m4a", DownloadPlus.audioName("video.MP4"));
        assertEquals("video.m4a", DownloadPlus.audioName("video.m4a"));
        assertEquals("video.mp3", DownloadPlus.audioName("video.mp3"));
        assertEquals("muzik.m4a", DownloadPlus.audioName("muzik"));
        assertNull(DownloadPlus.audioName(null));
    }

    @Test
    public void bestIndexPicksTheHighestBitrate() {
        assertEquals(2, DownloadPlus.bestIndex(new int[]{500, 900, 2500, 1200}));
        assertEquals(0, DownloadPlus.bestIndex(new int[]{700}));
        assertEquals(-1, DownloadPlus.bestIndex(new int[0]));
        assertEquals(-1, DownloadPlus.bestIndex(new int[]{0, -5}));
    }
}
