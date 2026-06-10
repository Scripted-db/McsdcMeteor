package com.mcsdc.addon.gui.vanilla;

import com.mcsdc.addon.ServerListHelper;
import com.mcsdc.addon.gui.ServerInfoScreen;
import com.mcsdc.addon.system.ServerStorage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import org.jetbrains.annotations.Nullable;

public final class ServerListActions {
    private ServerListActions() {}

    @Nullable
    public static ServerStorage selected(McsdcServerListWidget list) {
        return list.getSelectedServer();
    }

    public static void join(McsdcServerListWidget list) {
        ServerStorage server = selected(list);
        if (server != null) VanillaScreens.connectTo(server.ip());
    }

    public static void add(McsdcServerListWidget list) {
        ServerStorage server = selected(list);
        if (server != null) ServerListHelper.addMcsdcServer(server.ip());
    }

    public static void info(Minecraft minecraft, McsdcServerListWidget list) {
        ServerStorage server = selected(list);
        if (server != null) minecraft.setScreen(new ServerInfoScreen(server.ip()));
    }

    public static void setActive(boolean active, Button... buttons) {
        for (Button button : buttons) {
            if (button != null) button.active = active;
        }
    }
}
