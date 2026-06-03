package com.mcsdc.addon.gui.vanilla;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;

public final class VanillaScreens {
    private VanillaScreens() {}

    public static void connectTo(String address) {
        connectTo(MinecraftClient.getInstance(), address);
    }

    public static void connectTo(MinecraftClient client, String address) {
        ConnectScreen.connect(
            new MultiplayerScreen(new TitleScreen()),
            client,
            ServerAddress.parse(address),
            new ServerInfo("", address, ServerInfo.ServerType.OTHER),
            false,
            null
        );
    }
}
