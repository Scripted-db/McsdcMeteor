package com.mcsdc.addon.gui;

import com.mcsdc.addon.gui.vanilla.McsdcHubScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public final class McsdcScreen {
    private McsdcScreen() {}

    public static void open(Screen parent) {
        Minecraft.getInstance().setScreen(new McsdcHubScreen(parent));
    }
}
