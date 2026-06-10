package com.mcsdc.addon.gui.vanilla;

import com.google.gson.JsonObject;
import com.mcsdc.addon.Api;
import com.mcsdc.addon.ServerListHelper;
import com.mcsdc.addon.system.FindPlayerSearchBuilder;
import com.mcsdc.addon.system.ServerStorage;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class McsdcFindPlayerScreen extends McsdcParentScreen {
    private EditBox playerField;
    private McsdcServerListWidget serverList;
    private List<ServerStorage> results = new ArrayList<>();
    private String status = "";
    private Button joinBtn;
    private Button addBtn;
    private Button infoBtn;
    private Button addAllBtn;

    public McsdcFindPlayerScreen(Screen parent) {
        super(Component.literal("Find Player"), parent);
    }

    @Override
    protected void init() {
        playerField = new EditBox(font, width / 2 - 100, 28, 140, 20, Component.literal("name/uuid"));
        playerField.setMaxLength(64);
        playerField.setValue("popbob");
        addRenderableWidget(playerField);

        addRenderableWidget(Button.builder(Component.literal("Search"), b -> runSearch())
            .bounds(width / 2 + 44, 28, 56, 20).build());

        int top = 64;
        int bottom = height - 32;
        serverList = new McsdcServerListWidget(16, top, width - 32, bottom - top);
        serverList.setOnSelectionChanged(this::updateButtons);
        addRenderableWidget(serverList);

        joinBtn = addRenderableWidget(Button.builder(Component.literal("Join"), b -> ServerListActions.join(serverList))
            .bounds(width / 2 - 120, height - 28, 56, 20).build());
        addBtn = addRenderableWidget(Button.builder(Component.literal("Add"), b -> ServerListActions.add(serverList))
            .bounds(width / 2 - 60, height - 28, 56, 20).build());
        infoBtn = addRenderableWidget(Button.builder(Component.literal("Info"), b -> ServerListActions.info(minecraft, serverList))
            .bounds(width / 2, height - 28, 56, 20).build());
        addAllBtn = addRenderableWidget(Button.builder(Component.literal("Add all"), b -> addAll())
            .bounds(width / 2 + 60, height - 28, 64, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Back"), b -> onClose())
            .bounds(width - 60, height - 28, 44, 20).build());

        serverList.setOnSelectionChanged(this::updateButtons);
        updateButtons();
    }

    private void runSearch() {
        String query = playerField.getValue().trim();
        if (query.isEmpty()) {
            status = "Enter a name or UUID.";
            return;
        }
        status = "Searching...";
        CompletableFuture.supplyAsync(() -> {
            JsonObject body = FindPlayerSearchBuilder.create(query);
            return Api.postJson("/search/player", body);
        }).thenAccept(response -> minecraft.execute(() -> {
            ServerSearchResults.ParseResult parsed = ServerSearchResults.parse(response);
            if (!parsed.ok()) {
                status = parsed.error();
                return;
            }
            results = new ArrayList<>(parsed.serversOrEmpty());
            status = ServerSearchResults.statusFor(results);
            serverList.setServers(results);
            updateButtons();
        }));
    }

    private void addAll() {
        if (results.isEmpty()) return;
        ServerListHelper.addAllMcsdcServers(results.stream().map(ServerStorage::ip).toList());
        status = "Added all servers.";
    }

    private void updateButtons() {
        if (joinBtn == null) return;
        boolean sel = serverList.getSelectedServer() != null;
        ServerListActions.setActive(sel, joinBtn, addBtn, infoBtn);
        if (addAllBtn != null) addAllBtn.active = !results.isEmpty();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);
        context.centeredText(font, title, width / 2, 12, CommonColors.WHITE);
        if (!status.isEmpty()) {
            context.centeredText(font, status, width / 2, 52, CommonColors.YELLOW);
        }
    }
}
