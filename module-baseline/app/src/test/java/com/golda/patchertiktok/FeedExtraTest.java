package com.golda.patchertiktok;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Akış ekstraları: fotoğraf/hikâye türleri ve yorum adayı sayımı. */
public class FeedExtraTest {

    @Test
    public void photoTypesAreRecognised() {
        assertTrue(FeedFilter.isPhotoType(68));
        assertTrue(FeedFilter.isPhotoType(150));
        assertFalse(FeedFilter.isPhotoType(0));
        assertFalse(FeedFilter.isPhotoType(101));
    }

    @Test
    public void storyTypesArePositiveNumbersOnly() {
        assertTrue(FeedFilter.isStoryType(1));
        assertTrue(FeedFilter.isStoryType(7));
        assertFalse(FeedFilter.isStoryType(0));
        assertFalse(FeedFilter.isStoryType(-1));
    }
}
