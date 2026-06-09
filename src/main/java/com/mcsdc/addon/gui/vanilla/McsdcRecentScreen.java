package com.mcsdc.addon.gui.vanilla;

import com.mcsdc.addon.system.McsdcSystem;
import com.mcsdc.addon.system.ServerStorage;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;

import java.util.ArrayList;
import java.util.List;

public class McsdcRecentScreen extends McsdcParentScreen {
    private McsdcServerListWidget serverList;
    private List<ServerStorage> servers = new ArrayList<>();
    private Button joinBtn;
    private Button addBtn;
    private Button infoBtn;
    private Button removeBtn;

    public McsdcRecentScreen(Screen parent) {
        super(Component.literal("Recent Servers"), parent);
    }

    @Override
    protected void init() {
        servers = new ArrayList<>(McsdcSystem.get().getRecentServers().reversed());

        int top = 36;
        int bottom = height - 32;
        serverList = new McsdcServerListWidget(16, top, width - 32, bottom - top);
        addRenderableWidget(serverList);

        addRenderableWidget(Button.builder(Component.literal("Clear all"), b -> {
            McsdcSystem.get().clearRecentServers();
            servers.clear();
            serverList.setServers(List.of());
        }).bounds(16, height - 28, 72, 20).build());

        joinBtn = addRenderableWidget(Button.builder(Component.literal("Join"), b -> ServerListActions.join(serverList))
            .bounds(width / 2 - 120, height - 28, 56, 20).build());
        addBtn = addRenderableWidget(Button.builder(Component.literal("Add"), b -> ServerListActions.add(serverList))
            .bounds(width / 2 - 60, height - 28, 56, 20).build());
        infoBtn = addRenderableWidget(Button.builder(Component.literal("Info"), b -> ServerListActions.info(minecraft, serverList))
            .bounds(width / 2, height - 28, 56, 20).build());
        removeBtn = addRenderableWidget(Button.builder(Component.literal("Remove"), b -> removeSelected())
            .bounds(width / 2 + 60, height - 28, 64, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Back"), b -> onClose())
            .bounds(width - 60, height - 28, 44, 20).build());

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
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);
        context.centeredText(font, title, width / 2, 12, CommonColors.WHITE);
        if (servers.isEmpty()) {
            context.centeredText(font, "Recently joined servers will appear here.", width / 2, height / 2, CommonColors.GRAY);
        }
    }
}
