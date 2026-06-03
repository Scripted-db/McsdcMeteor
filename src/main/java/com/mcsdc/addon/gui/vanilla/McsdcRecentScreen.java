package com.mcsdc.addon.gui.vanilla;

import com.mcsdc.addon.system.McsdcSystem;
import com.mcsdc.addon.system.ServerStorage;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;

import java.util.ArrayList;
import java.util.List;

public class McsdcRecentScreen extends McsdcParentScreen {
    private McsdcServerListWidget serverList;
    private List<ServerStorage> servers = new ArrayList<>();
    private ButtonWidget joinBtn;
    private ButtonWidget addBtn;
    private ButtonWidget infoBtn;
    private ButtonWidget removeBtn;

    public McsdcRecentScreen(Screen parent) {
        super(Text.literal("Recent Servers"), parent);
    }

    @Override
    protected void init() {
        servers = new ArrayList<>(McsdcSystem.get().getRecentServers().reversed());

        int top = 36;
        int bottom = height - 32;
        serverList = new McsdcServerListWidget(16, top, width - 32, bottom - top);
        addDrawableChild(serverList);

        addDrawableChild(ButtonWidget.builder(Text.literal("Clear all"), b -> {
            McsdcSystem.get().clearRecentServers();
            servers.clear();
            serverList.setServers(List.of());
        }).dimensions(16, height - 28, 72, 20).build());

        joinBtn = addDrawableChild(ButtonWidget.builder(Text.literal("Join"), b -> ServerListActions.join(serverList))
            .dimensions(width / 2 - 120, height - 28, 56, 20).build());
        addBtn = addDrawableChild(ButtonWidget.builder(Text.literal("Add"), b -> ServerListActions.add(serverList))
            .dimensions(width / 2 - 60, height - 28, 56, 20).build());
        infoBtn = addDrawableChild(ButtonWidget.builder(Text.literal("Info"), b -> ServerListActions.info(client, serverList))
            .dimensions(width / 2, height - 28, 56, 20).build());
        removeBtn = addDrawableChild(ButtonWidget.builder(Text.literal("Remove"), b -> removeSelected())
            .dimensions(width / 2 + 60, height - 28, 64, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Back"), b -> close())
            .dimensions(width - 60, height - 28, 44, 20).build());

        serverList.setOnSelectionChanged(this::updateButtons);
        serverList.setServers(servers);
        updateButtons();
    }

    private void removeSelected() {
        ServerStorage s = serverList.getSelectedServer();
        if (s == null) return;
        McsdcSystem.get().removeRecentServer(s);
        servers.remove(s);
        serverList.setServers(servers);
    }

    private void updateButtons() {
        if (joinBtn == null) return;
        ServerListActions.setActive(serverList.getSelectedServer() != null, joinBtn, addBtn, infoBtn, removeBtn);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 12, Colors.WHITE);
        if (servers.isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, "Recently joined servers will appear here.", width / 2, height / 2, Colors.GRAY);
        }
    }
}
