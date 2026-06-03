package com.mcsdc.addon.gui.vanilla;

import com.mcsdc.addon.gui.LoginScreen;
import com.mcsdc.addon.system.McsdcSystem;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.gui.WindowScreen;
import net.minecraft.client.gui.screen.Screen;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class LoginBridgeScreen extends WindowScreen {
    private final Screen parent;

    public LoginBridgeScreen(Screen parent) {
        super(GuiThemes.get(), "MCSDC");
        this.parent = parent;
    }

    @Override
    public void initWidgets() {
        mc.setScreen(new LoginScreen(this));
    }

    @Override
    public void reload() {
        if (!McsdcSystem.get().getToken().isEmpty()) {
            mc.setScreen(new McsdcHubScreen(parent));
        }
    }
}
