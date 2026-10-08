package com.golda.patchertiktok;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** Yazı boyutu ölçeği: katsayı hesabı ve katlanarak büyümeme kuralı. */
public class FontScaleTest {
    private static final float DELTA = 0.0001f;

    @Test
    public void percentBecomesFactor() {
        assertEquals(1f, FontScale.factor(100), DELTA);
        assertEquals(1f, FontScale.factor(0), DELTA);
        assertEquals(1.2f, FontScale.factor(120), DELTA);
        assertEquals(0.85f, FontScale.factor(85), DELTA);
    }

    @Test
    public void applyingFactorScalesFromBase() {
        assertEquals(2.4f, FontScale.target(2.0f, 1f, 1.2f), DELTA);
    }

    @Test
    public void changingFactorUndoesPreviousOne() {
        // 2.0 → %120 uygulanmış (2.4). Şimdi %85'e geçiyoruz: 2.4 / 1.2 * 0.85
        assertEquals(1.7f, FontScale.target(2.4f, 1.2f, 0.85f), DELTA);
    }

    @Test
    public void backToDefaultRestoresBase() {
        assertEquals(2.0f, FontScale.target(2.4f, 1.2f, 1f), DELTA);
    }
}
