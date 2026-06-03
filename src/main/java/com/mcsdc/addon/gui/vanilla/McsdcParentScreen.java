package com.mcsdc.addon.gui.vanilla;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public abstract class McsdcParentScreen extends Screen {
    protected final Screen parent;

    protected McsdcParentScreen(Text title, Screen parent) {
        super(title);
        this.parent = parent;
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }
}
