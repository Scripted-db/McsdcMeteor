package com.mcsdc.addon.gui;

import com.mcsdc.addon.gui.vanilla.McsdcHubScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

public final class McsdcScreen {
    private McsdcScreen() {}

    public static void open(Screen parent) {
        MinecraftClient.getInstance().setScreen(new McsdcHubScreen(parent));
    }
}
