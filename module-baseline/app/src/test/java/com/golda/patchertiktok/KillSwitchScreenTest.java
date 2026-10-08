package com.golda.patchertiktok;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Kill Switch ekran sınıflandırması: sohbet odası (perde) ve mesaj metni (maske). */
public class KillSwitchScreenTest {

    @Test
    public void chatRoomsGetTheCover() {
        assertTrue(KillSwitch.isRoom("com.ss.android.ugc.aweme.im.sdk.chat.ui.ChatRoomActivity"));
        assertTrue(KillSwitch.isRoom("com.x.ChatDetailActivity"));
    }

    @Test
    public void inboxIsNotARoom() {
        assertFalse(KillSwitch.isRoom("com.ss.android.ugc.aweme.im.sdk.session.ui.SessionListFragment"));
        assertFalse(KillSwitch.isRoom("com.ss.android.ugc.aweme.main.MainActivity"));
        assertFalse(KillSwitch.isRoom(null));
        assertFalse(KillSwitch.isRoom("com.golda.patchertiktok.ModSettingsActivity"));
    }

    @Test
    public void messageViewIdsAreMasked() {
        assertTrue(KillSwitch.maskId("message_content"));
        assertTrue(KillSwitch.maskId("im_item_content"));
        assertTrue(KillSwitch.maskId("tv_last_msg"));
        assertTrue(KillSwitch.maskId("chat_preview"));
    }

    @Test
    public void nameAndUnrelatedIdsStayVisible() {
        assertFalse(KillSwitch.maskId("tv_nickname"));
        assertFalse(KillSwitch.maskId("avatar"));
        assertFalse(KillSwitch.maskId("title"));
        assertFalse(KillSwitch.maskId(""));
        assertFalse(KillSwitch.maskId(null));
    }
}
