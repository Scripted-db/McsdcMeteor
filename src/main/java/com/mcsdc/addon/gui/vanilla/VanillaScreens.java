package com.mcsdc.addon.gui.vanilla;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.multiplayer.ServerData;

public final class VanillaScreens {
    private VanillaScreens() {}

    public static void connectTo(String address) {
        connectTo(Minecraft.getInstance(), address);
    }

    public static void connectTo(Minecraft client, String address) {
        ConnectScreen.startConnecting(
            new JoinMultiplayerScreen(new TitleScreen()),
            client,
            ServerAddress.parseString(address),
            new ServerData("", address, ServerData.Type.OTHER),
            false,
            null
        );
    }
}
