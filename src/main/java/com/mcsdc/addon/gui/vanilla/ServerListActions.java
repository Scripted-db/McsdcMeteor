package com.mcsdc.addon.gui.vanilla;

import com.mcsdc.addon.ServerListHelper;
import com.mcsdc.addon.gui.ServerInfoScreen;
import com.mcsdc.addon.system.ServerStorage;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ButtonWidget;
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

    public static void info(MinecraftClient client, McsdcServerListWidget list) {
        ServerStorage server = selected(list);
        if (server != null) client.setScreen(new ServerInfoScreen(server.ip()));
    }

    public static void setActive(boolean active, ButtonWidget... buttons) {
        for (ButtonWidget button : buttons) {
            if (button != null) button.active = active;
        }
    }
}
