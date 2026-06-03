package com.mcsdc.addon.gui.vanilla;

import com.mcsdc.addon.ServerListHelper;
import com.mcsdc.addon.system.McsdcSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;

public class McsdcHubScreen extends McsdcParentScreen {
    public McsdcHubScreen(Screen parent) {
        super(Text.literal("MCSDC"), parent);
    }

    @Override
    protected void init() {
        if (McsdcSystem.get().getToken().isEmpty()) {
            client.setScreen(new LoginBridgeScreen(parent));
            return;
        }

        int cx = width / 2;
        int y = height / 4 + 48;
        int bw = 200;
        int gap = 24;

        addMenuButton("Find Servers", y, bw, b -> client.setScreen(new McsdcBrowseScreen(this)));
        y += gap;

        addMenuButton("Friends", y, bw, b -> client.setScreen(new McsdcFriendsScreen(this)));
        y += gap;

        addMenuButton("Recent Servers", y, bw, b -> client.setScreen(new McsdcRecentScreen(this)));
        y += gap;

        addMenuButton("Find Player", y, bw, b -> client.setScreen(new McsdcFindPlayerScreen(this)));
        y += gap;

        addMenuButton("Ticket ID", y, bw, b -> client.setScreen(new McsdcTicketScreen(this)));
        y += gap;

        addMenuButton("Clear MCSDC Servers", y, bw, b -> ServerListHelper.removeMcsdcServers());

        addDrawableChild(ButtonWidget.builder(Text.literal("Logout"), b -> {
            McsdcSystem.get().setToken("");
            McsdcSystem.get().setUsername("");
            McsdcSystem.get().setLevel(-1);
            client.setScreen(new LoginBridgeScreen(parent));
        }).dimensions(cx - bw / 2 - 52, height - 52, 98, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> close())
            .dimensions(cx - bw / 2 + 54, height - 52, 98, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 20, Colors.WHITE);
        context.drawCenteredTextWithShadow(textRenderer, "Logged in as: " + McsdcSystem.get().getUsername(), width / 2, 36, Colors.LIGHT_GRAY);
    }

    private void addMenuButton(String label, int y, int width, ButtonWidget.PressAction action) {
        addDrawableChild(ButtonWidget.builder(Text.literal(label), action)
            .dimensions(this.width / 2 - width / 2, y, width, 20).build());
    }
}
