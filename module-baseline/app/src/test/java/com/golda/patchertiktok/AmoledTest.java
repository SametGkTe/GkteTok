package com.golda.patchertiktok;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * TT+ — AMOLED renk dönüşümünün birim testleri.
 * Gerçek TikTok renkleriyle sınanır (koyu tema yüzeyleri, vurgu rengi, açık tema).
 */
public class AmoledTest {

    // ── Koyu tema yüzeyleri siyaha dönmeli ──────────────────────────────────
    @Test
    public void turnsNearBlackSurfacesIntoPureBlack() {
        assertEquals(0xFF000000, Amoled.remap(0xFF121212, false)); // tipik koyu zemin
        assertEquals(0xFF000000, Amoled.remap(0xFF1E1E1E, false)); // kart
        assertEquals(0xFF000000, Amoled.remap(0xFF161823, false)); // TikTok koyu lacivert-siyah (fark 13)
        assertEquals(0xFF000000, Amoled.remap(0xFF242424, false)); // sınır değeri (dahil)
    }

    // ── Kart kademesi yalnızca açıkken kart tonları kararır ─────────────────
    @Test
    public void leavesCardShadesAloneUntilCardsToggleIsOn() {
        assertEquals(0xFF2A2A2A, Amoled.remap(0xFF2A2A2A, false));
        assertEquals(0xFF3C3C3C, Amoled.remap(0xFF3C3C3C, false));
        assertEquals(0xFF000000, Amoled.remap(0xFF2A2A2A, true));
        assertEquals(0xFF000000, Amoled.remap(0xFF3C3C3C, true)); // sınır değeri (dahil)
        assertEquals(0xFF3D3D3D, Amoled.remap(0xFF3D3D3D, true)); // sınırın hemen üstü korunur
    }

    // ── Marka renkleri, ikonlar ve açık tema korunmalı ──────────────────────
    @Test
    public void keepsAccentsAndLightTheme() {
        assertEquals(0xFFFE2C55, Amoled.remap(0xFFFE2C55, true)); // TikTok vurgu rengi
        assertEquals(0xFF25F4EE, Amoled.remap(0xFF25F4EE, true)); // TikTok camgöbeği
        assertEquals(0xFFF5F5F5, Amoled.remap(0xFFF5F5F5, true)); // açık tema zemini
        assertEquals(0xFFFFFFFF, Amoled.remap(0xFFFFFFFF, true)); // beyaz metin
        assertEquals(0xFF0099CC, Amoled.remap(0xFF0099CC, true)); // orta ton mavi korunur
    }

    // ── Yarı saydam ve tamamen saydam renkler korunmalı ─────────────────────
    @Test
    public void keepsTranslucentColors() {
        assertEquals(0x80121212, Amoled.remap(0x80121212, true)); // %50 siyah katman
        assertEquals(0x33000000, Amoled.remap(0x33000000, true)); // gölge
        assertEquals(0x00000000, Amoled.remap(0x00000000, true)); // tamamen saydam
    }

    // ── Koyu ama renkli tonlar korunmalı (nötr değil) ───────────────────────
    @Test
    public void keepsDarkTintedColors() {
        assertEquals(0xFF1A2332, Amoled.remap(0xFF1A2332, true)); // mavi ağırlıklı koyu (fark 24)
        assertEquals(0xFF2B1600, Amoled.remap(0xFF2B1600, true)); // kahve tonlu koyu
    }
}
