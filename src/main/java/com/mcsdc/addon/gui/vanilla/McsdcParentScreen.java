package com.mcsdc.addon.gui.vanilla;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public abstract class McsdcParentScreen extends Screen {
    protected final Screen parent;

    protected McsdcParentScreen(Component title, Screen parent) {
        super(title);
        this.parent = parent;
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
