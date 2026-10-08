package com.golda.patchertiktok;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Map;

import org.junit.Test;

/**
 * TT+ — profil bannerı (kapak) ve yeni profil düzeni anahtarlarının A/B tablosuna
 * doğru şekilde girdiğini doğrular.
 *
 * Banner anahtarları (profile_bg_*) ile düzen anahtarları (profile_left_align*) artık
 * ayrı ayarlara bağlı: kullanıcı banner'ı tek başına açabilir.
 */
public class AbOverridesProfileTest {

    @Test
    public void bannerToggleUnlocksBannerKeys() {
        Prefs.set(Prefs.BANNER, true);
        Prefs.set(Prefs.NEW_PROFILE, false);

        AbOverrides.collect();
        Map<String, Object> values = AbOverrides.values();

        assertEquals(1, values.get("profile_bg_in_allow_list"));
        assertEquals(3, values.get("profile_bg_enable_consumption_group"));
        assertEquals(Boolean.TRUE, values.get("profile_bg_status_bar_immersive_fix"));
        assertFalse("banner açıkken düzen anahtarları gelmemeli",
                values.containsKey("profile_left_align"));
    }

    @Test
    public void layoutToggleDoesNotTouchBannerKeys() {
        Prefs.set(Prefs.BANNER, false);
        Prefs.set(Prefs.NEW_PROFILE, true);

        AbOverrides.collect();
        Map<String, Object> values = AbOverrides.values();

        assertFalse(values.containsKey("profile_bg_in_allow_list"));
        assertFalse(values.containsKey("profile_bg_enable_consumption_group"));
        assertEquals(1, values.get("profile_left_align"));
        assertEquals(11, values.get("profile_left_align_ab_group"));
    }

    @Test
    public void collectIsIdempotentAndCombinesBothToggles() {
        Prefs.set(Prefs.BANNER, true);
        Prefs.set(Prefs.NEW_PROFILE, true);

        AbOverrides.collect();
        int first = AbOverrides.values().size();
        AbOverrides.collect();

        assertEquals("collect() her çağrıda tabloyu yeniden kurmalı", first, AbOverrides.values().size());
        assertEquals(1, AbOverrides.values().get("profile_bg_in_allow_list"));
        assertEquals(1, AbOverrides.values().get("profile_left_align"));
    }

    @Test
    public void disablingBothRemovesProfileKeys() {
        Prefs.set(Prefs.BANNER, false);
        Prefs.set(Prefs.NEW_PROFILE, false);

        AbOverrides.collect();
        Map<String, Object> values = AbOverrides.values();

        assertFalse(values.containsKey("profile_bg_in_allow_list"));
        assertFalse(values.containsKey("profile_left_align"));
    }
}
