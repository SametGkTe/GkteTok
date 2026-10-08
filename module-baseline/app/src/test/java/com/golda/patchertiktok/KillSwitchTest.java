package com.golda.patchertiktok;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Kill Switch: hangi ekran gizlenir, perde rengi ne olur, hangi anahtar beğeni sayacıdır. */
public class KillSwitchTest {

    @Test
    public void messageScreensArePrivate() {
        assertTrue(KillSwitch.isPrivateScreen("com.ss.android.ugc.aweme.im.sdk.chat.ui.ChatRoomActivity"));
        assertTrue(KillSwitch.isPrivateScreen("com.ss.android.ugc.aweme.im.sdk.session.ui.SessionListFragment"));
        assertTrue(KillSwitch.isPrivateScreen("InboxFragment"));
        assertTrue(KillSwitch.isPrivateScreen("ConversationListActivity"));
        assertTrue(KillSwitch.isPrivateScreen("InstantMessageDetail"));
    }

    @Test
    public void otherScreensStayVisible() {
        assertFalse(KillSwitch.isPrivateScreen("com.ss.android.ugc.aweme.main.MainActivity"));
        assertFalse(KillSwitch.isPrivateScreen("com.ss.android.ugc.aweme.profile.ui.UserProfileActivity"));
        assertFalse(KillSwitch.isPrivateScreen("com.ss.android.ugc.aweme.discover.ui.DiscoverFragment"));
        assertFalse(KillSwitch.isPrivateScreen(null));
    }

    @Test
    public void ownSettingsScreenIsNeverCovered() {
        assertFalse(KillSwitch.isPrivateScreen("com.golda.patchertiktok.ModSettingsActivity"));
        assertFalse(KillSwitch.isPrivateScreen("com.golda.patchertiktok.KillSwitch$Toggle"));
    }

    @Test
    public void likeKeysAreRecognised() {
        assertTrue(KillSwitch.isLikeKey("favoriting_count"));
        assertTrue(KillSwitch.isLikeKey("favoritingCount"));
        assertTrue(KillSwitch.isLikeKey("FAVORITING_COUNT"));
        assertFalse(KillSwitch.isLikeKey("follower_count"));
        assertFalse(KillSwitch.isLikeKey("aweme_count"));
        assertFalse(KillSwitch.isLikeKey(null));
    }
}
