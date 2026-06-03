package com.mcsdc.addon;

import com.mcsdc.addon.mixin.MultiplayerScreenAccessor;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;

public final class MultiplayerScreenUtils {
    private MultiplayerScreenUtils() {}

    public static void reload(MultiplayerScreen multiplayerScreen) {
        MultiplayerScreenAccessor accessor = (MultiplayerScreenAccessor) multiplayerScreen;
        accessor.getServerListWidget().setServers(multiplayerScreen.getServerList());
    }
}
