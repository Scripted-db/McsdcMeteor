package com.mcsdc.addon.gui.vanilla;

import com.mcsdc.addon.ServerListHelper;
import com.mcsdc.addon.system.McsdcSystem;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;

public class McsdcHubScreen extends McsdcParentScreen {
    public McsdcHubScreen(Screen parent) {
        super(Component.literal("MCSDC"), parent);
    }

    @Override
    protected void init() {
        if (McsdcSystem.get().getToken().isEmpty()) {
            minecraft.setScreen(new LoginBridgeScreen(parent));
            return;
        }

        int cx = width / 2;
        int y = height / 4 + 48;
        int bw = 200;
        int gap = 24;

        addMenuButton("Find Servers", y, bw, b -> minecraft.setScreen(new McsdcBrowseScreen(this)));
        y += gap;

        addMenuButton("Friends", y, bw, b -> minecraft.setScreen(new McsdcFriendsScreen(this)));
        y += gap;

        addMenuButton("Recent Servers", y, bw, b -> minecraft.setScreen(new McsdcRecentScreen(this)));
        y += gap;

        addMenuButton("Find Player", y, bw, b -> minecraft.setScreen(new McsdcFindPlayerScreen(this)));
        y += gap;

        addMenuButton("Ticket ID", y, bw, b -> minecraft.setScreen(new McsdcTicketScreen(this)));
        y += gap;

        addMenuButton("Clear MCSDC Servers", y, bw, b -> ServerListHelper.removeMcsdcServers());

        addRenderableWidget(Button.builder(Component.literal("Logout"), b -> {
            McsdcSystem.get().setToken("");
            McsdcSystem.get().setUsername("");
            McsdcSystem.get().setLevel(-1);
            minecraft.setScreen(new LoginBridgeScreen(parent));
        }).bounds(cx - bw / 2 - 52, height - 52, 98, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
            .bounds(cx - bw / 2 + 54, height - 52, 98, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);
        context.centeredText(font, title, width / 2, 20, CommonColors.WHITE);
        context.centeredText(font, "Logged in as: " + McsdcSystem.get().getUsername(), width / 2, 36, CommonColors.LIGHT_GRAY);
    }

    private void addMenuButton(String label, int y, int width, Button.OnPress action) {
        addRenderableWidget(Button.builder(Component.literal(label), action)
            .bounds(this.width / 2 - width / 2, y, width, 20).build());
    }
}
