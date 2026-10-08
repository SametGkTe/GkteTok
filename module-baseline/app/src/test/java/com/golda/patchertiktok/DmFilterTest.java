package com.golda.patchertiktok;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** DM kelime filtresi: eşleşme ve kelime sayımı. */
public class DmFilterTest {

    @Test
    public void wordsMatchInsideTheText() {
        assertTrue(DmFilter.matches("bu bir SPAM mesajı", "spam, reklam"));
        assertTrue(DmFilter.matches("indirim var", "indirim"));
        assertFalse(DmFilter.matches("selam nasılsın", "spam, reklam"));
    }

    @Test
    public void emptyInputsAreHarmless() {
        assertFalse(DmFilter.matches(null, "spam"));
        assertFalse(DmFilter.matches("metin", ""));
        assertFalse(DmFilter.matches("metin", null));
        assertFalse(DmFilter.matches("metin", " , ; "));
    }

    @Test
    public void separatorsIncludeCommaNewlineAndSemicolon() {
        assertTrue(DmFilter.matches("fırsat!", "spam\nfırsat"));
        assertTrue(DmFilter.matches("deneme", "a; deneme"));
    }

    @Test
    public void wordCountSkipsEmptyEntries() {
        assertEquals(2, DmFilter.wordCount("spam, reklam"));
        assertEquals(3, DmFilter.wordCount("a,\nb\n;c"));
        assertEquals(0, DmFilter.wordCount(""));
    }
}
