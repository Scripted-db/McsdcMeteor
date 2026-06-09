package com.mcsdc.addon.gui.vanilla;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mcsdc.addon.Api;
import com.mcsdc.addon.system.McsdcSystem;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class McsdcFriendsScreen extends McsdcParentScreen {
    private boolean locationsTab;
    private final TabData list = new TabData();
    private final TabData locations = new TabData();
    @Nullable private String busy;
    private EditBox nameField;
    private McsdcFriendListWidget listWidget;
    private String status = "";
    private Button actionBtn;

    public McsdcFriendsScreen(Screen parent) {
        super(Component.literal("Friends"), parent);
    }

    @Override
    protected void init() {
        int top = 60;
        int listH = height - top - 80;

        addRenderableWidget(Button.builder(Component.literal("List"), b -> switchTab(false))
            .bounds(16, 28, 80, 20).build());
        addRenderableWidget(Button.builder(Component.literal("On a server"), b -> switchTab(true))
            .bounds(100, 28, 100, 20).build());

        listWidget = new McsdcFriendListWidget(16, top, width - 32, listH);
        addRenderableWidget(listWidget);

        if (!locationsTab) {
            nameField = new EditBox(font, 16, height - 52, width - 140, 20, Component.literal("username"));
            nameField.setMaxLength(32);
            addRenderableWidget(nameField);
            addRenderableWidget(Button.builder(Component.literal("Add"), b -> addFriend())
                .bounds(width - 116, height - 52, 50, 20).build());
        }

        actionBtn = addRenderableWidget(Button.builder(Component.literal(locationsTab ? "Join" : "Remove"), b -> runAction())
            .bounds(16, height - 28, 80, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Back"), b -> onClose())
            .bounds(width - 60, height - 28, 44, 20).build());

        loadTab();
        updateActionBtn();
    }

    private TabData activeTab() {
        return locationsTab ? locations : list;
    }

    private void switchTab(boolean showLocations) {
        locationsTab = showLocations;
        activeTab().invalidate();
        rebuildUi();
    }

    private void rebuildUi() {
        clearWidgets();
        init();
    }

    private void loadTab() {
        TabData tab = activeTab();
        if (!tab.loaded && !tab.loading) {
            status = "Loading...";
            tab.loading = true;
            String path = locationsTab ? "/my/friends/locations" : "/my/friends";
            CompletableFuture.supplyAsync(() -> Api.requestGet(path))
                .thenAccept(r -> minecraft.execute(() -> {
                    if (r.ok()) tab.ok(Api.unwrapArray(r.body()));
                    else tab.fail(r.error());
                    status = tab.error.isEmpty() ? "" : tab.error;
                    populateList();
                    updateActionBtn();
                }))
                .exceptionally(e -> {
                    minecraft.execute(() -> {
                        tab.fail(e.getMessage());
                        status = tab.error;
                        populateList();
                    });
                    return null;
                });
        } else if (tab.loaded) {
            populateList();
        }
    }

    private void populateList() {
        TabData tab = activeTab();
        if (!tab.error.isEmpty()) status = tab.error;
        if (tab.items.isEmpty() && tab.loaded) {
            status = locationsTab ? "No friends on a server right now." : "No friends yet.";
            listWidget.setRows(List.of());
            return;
        }
        List<McsdcFriendListWidget.Row> rows = new ArrayList<>();
        for (JsonElement el : tab.items) {
            if (!el.isJsonObject()) continue;
            JsonObject obj = el.getAsJsonObject();
            if (locationsTab) {
                rows.add(new McsdcFriendListWidget.Row(
                    Api.jsonString(obj, "name"),
                    Api.jsonString(obj, "server"),
                    ""
                ));
            } else {
                String stage = Api.jsonString(obj, "stage");
                rows.add(new McsdcFriendListWidget.Row(
                    Api.jsonString(obj, "name"),
                    Api.jsonString(obj, "role", "user"),
                    stage.isEmpty() ? "—" : stage
                ));
            }
        }
        listWidget.setRows(rows);
    }

    private void addFriend() {
        String name = nameField.getValue().trim();
        if (name.isEmpty()) return;
        if (name.equals(McsdcSystem.get().getUsername())) {
            status = "cant add yourself lol";
            return;
        }
        busy = "add";
        JsonObject body = new JsonObject();
        body.addProperty("name", name);
        CompletableFuture.supplyAsync(() -> Api.requestPost("/my/friends", body))
            .thenAccept(r -> minecraft.execute(() -> {
                busy = null;
                if (!r.ok()) status = r.error();
                else {
                    nameField.setValue("");
                    list.invalidate();
                    rebuildUi();
                }
            }));
    }

    private void runAction() {
        McsdcFriendListWidget.Row row = listWidget.getSelectedRow();
        if (row == null) return;
        if (locationsTab) {
            if (canJoin(row.col2())) join(row.col2());
        } else {
            removeFriend(row.name());
        }
    }

    private void removeFriend(String name) {
        busy = name;
        JsonObject body = new JsonObject();
        body.addProperty("name", name);
        CompletableFuture.supplyAsync(() -> Api.requestPost("/my/friends/deny", body))
            .thenAccept(r -> minecraft.execute(() -> {
                busy = null;
                if (!r.ok()) status = r.error();
                else {
                    list.invalidate();
                    rebuildUi();
                }
            }));
    }

    private void join(String address) {
        if (!canJoin(address)) return;
        if (minecraft.level != null) minecraft.level.disconnect(Component.literal(""));
        VanillaScreens.connectTo(minecraft, address);
    }

    private void updateActionBtn() {
        if (actionBtn == null) return;
        McsdcFriendListWidget.Row row = listWidget != null ? listWidget.getSelectedRow() : null;
        if (locationsTab) {
            actionBtn.setMessage(Component.literal("Join"));
            actionBtn.active = row != null && canJoin(row.col2());
        } else {
            actionBtn.setMessage(Component.literal("Remove"));
            actionBtn.active = row != null && busy == null;
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);
        context.centeredText(font, title, width / 2, 12, CommonColors.WHITE);
        if (locationsTab) {
            context.text(font, "Name", 20, 52, CommonColors.GRAY, true);
            context.text(font, "Server", 136, 52, CommonColors.GRAY, true);
        } else {
            context.text(font, "Name", 20, 52, CommonColors.GRAY, true);
            context.text(font, "Role", 136, 52, CommonColors.GRAY, true);
            context.text(font, "Stage", 236, 52, CommonColors.GRAY, true);
        }
        if (!status.isEmpty()) {
            context.centeredText(font, status, width / 2, height - 68, CommonColors.YELLOW);
        }
        updateActionBtn();
    }

    private static boolean canJoin(String server) {
        return !server.isBlank() && !server.equalsIgnoreCase("singleplayer");
    }

    private static final class TabData {
        boolean loading;
        boolean loaded;
        String error = "";
        JsonArray items = new JsonArray();

        void invalidate() {
            loading = false;
            loaded = false;
            error = "";
            items = new JsonArray();
        }

        void ok(JsonArray data) {
            loading = false;
            loaded = true;
            error = "";
            items = data;
        }

        void fail(@Nullable String message) {
            loading = false;
            loaded = true;
            error = message != null && !message.isBlank() ? message : "request failed";
            items = new JsonArray();
        }
    }
}
