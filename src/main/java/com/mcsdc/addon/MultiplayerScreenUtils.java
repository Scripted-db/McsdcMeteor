package com.mcsdc.addon;

import com.mcsdc.addon.mixin.MultiplayerScreenAccessor;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;

public final class MultiplayerScreenUtils {
    private MultiplayerScreenUtils() {}

    public static void reload(JoinMultiplayerScreen multiplayerScreen) {
        MultiplayerScreenAccessor accessor = (MultiplayerScreenAccessor) multiplayerScreen;
        accessor.getServerSelectionList().updateOnlineServers(multiplayerScreen.getServers());
    }
}
