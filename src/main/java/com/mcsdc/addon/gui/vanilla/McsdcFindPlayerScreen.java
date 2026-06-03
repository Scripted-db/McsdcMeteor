package com.mcsdc.addon.gui.vanilla;

import com.google.gson.JsonObject;
import com.mcsdc.addon.Api;
import com.mcsdc.addon.ServerListHelper;
import com.mcsdc.addon.system.FindPlayerSearchBuilder;
import com.mcsdc.addon.system.ServerStorage;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class McsdcFindPlayerScreen extends McsdcParentScreen {
    private TextFieldWidget playerField;
    private McsdcServerListWidget serverList;
    private List<ServerStorage> results = new ArrayList<>();
    private String status = "";
    private ButtonWidget joinBtn;
    private ButtonWidget addBtn;
    private ButtonWidget infoBtn;
    private ButtonWidget addAllBtn;

    public McsdcFindPlayerScreen(Screen parent) {
        super(Text.literal("Find Player"), parent);
    }

    @Override
    protected void init() {
        playerField = new TextFieldWidget(textRenderer, width / 2 - 100, 28, 140, 20, Text.literal("name/uuid"));
        playerField.setMaxLength(64);
        playerField.setText("popbob");
        addDrawableChild(playerField);

        addDrawableChild(ButtonWidget.builder(Text.literal("Search"), b -> runSearch())
            .dimensions(width / 2 + 44, 28, 56, 20).build());

        int top = 64;
        int bottom = height - 32;
        serverList = new McsdcServerListWidget(16, top, width - 32, bottom - top);
        serverList.setOnSelectionChanged(this::updateButtons);
        addDrawableChild(serverList);

        joinBtn = addDrawableChild(ButtonWidget.builder(Text.literal("Join"), b -> ServerListActions.join(serverList))
            .dimensions(width / 2 - 120, height - 28, 56, 20).build());
        addBtn = addDrawableChild(ButtonWidget.builder(Text.literal("Add"), b -> ServerListActions.add(serverList))
            .dimensions(width / 2 - 60, height - 28, 56, 20).build());
        infoBtn = addDrawableChild(ButtonWidget.builder(Text.literal("Info"), b -> ServerListActions.info(client, serverList))
            .dimensions(width / 2, height - 28, 56, 20).build());
        addAllBtn = addDrawableChild(ButtonWidget.builder(Text.literal("Add all"), b -> addAll())
            .dimensions(width / 2 + 60, height - 28, 64, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Back"), b -> close())
            .dimensions(width - 60, height - 28, 44, 20).build());

        serverList.setOnSelectionChanged(this::updateButtons);
        updateButtons();
    }

    private void runSearch() {
        String query = playerField.getText().trim();
        if (query.isEmpty()) {
            status = "Enter a name or UUID.";
            return;
        }
        status = "Searching...";
        CompletableFuture.supplyAsync(() -> {
            JsonObject body = FindPlayerSearchBuilder.create(query);
            return Api.postJson("/search/player", body);
        }).thenAccept(response -> client.execute(() -> {
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
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 12, Colors.WHITE);
        if (!status.isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, status, width / 2, 52, Colors.YELLOW);
        }
    }
}
