package com.mcsdc.addon.gui.vanilla;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mcsdc.addon.Api;
import com.mcsdc.addon.system.McsdcSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class McsdcFriendsScreen extends McsdcParentScreen {
    private boolean locationsTab;
    private final TabData list = new TabData();
    private final TabData locations = new TabData();
    @Nullable private String busy;
    private TextFieldWidget nameField;
    private McsdcFriendListWidget listWidget;
    private String status = "";
    private ButtonWidget actionBtn;

    public McsdcFriendsScreen(Screen parent) {
        super(Text.literal("Friends"), parent);
    }

    @Override
    protected void init() {
        int top = 60;
        int listH = height - top - 80;

        addDrawableChild(ButtonWidget.builder(Text.literal("List"), b -> switchTab(false))
            .dimensions(16, 28, 80, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("On a server"), b -> switchTab(true))
            .dimensions(100, 28, 100, 20).build());

        listWidget = new McsdcFriendListWidget(16, top, width - 32, listH);
        addDrawableChild(listWidget);

        if (!locationsTab) {
            nameField = new TextFieldWidget(textRenderer, 16, height - 52, width - 140, 20, Text.literal("username"));
            nameField.setMaxLength(32);
            addDrawableChild(nameField);
            addDrawableChild(ButtonWidget.builder(Text.literal("Add"), b -> addFriend())
                .dimensions(width - 116, height - 52, 50, 20).build());
        }

        actionBtn = addDrawableChild(ButtonWidget.builder(Text.literal(locationsTab ? "Join" : "Remove"), b -> runAction())
            .dimensions(16, height - 28, 80, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Back"), b -> close())
            .dimensions(width - 60, height - 28, 44, 20).build());

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
        clearChildren();
        init();
    }

    private void loadTab() {
        TabData tab = activeTab();
        if (!tab.loaded && !tab.loading) {
            status = "Loading...";
            tab.loading = true;
            String path = locationsTab ? "/my/friends/locations" : "/my/friends";
            CompletableFuture.supplyAsync(() -> Api.requestGet(path))
                .thenAccept(r -> client.execute(() -> {
                    if (r.ok()) tab.ok(Api.unwrapArray(r.body()));
                    else tab.fail(r.error());
                    status = tab.error.isEmpty() ? "" : tab.error;
                    populateList();
                    updateActionBtn();
                }))
                .exceptionally(e -> {
                    client.execute(() -> {
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
        String name = nameField.getText().trim();
        if (name.isEmpty()) return;
        if (name.equals(McsdcSystem.get().getUsername())) {
            status = "cant add yourself lol";
            return;
        }
        busy = "add";
        JsonObject body = new JsonObject();
        body.addProperty("name", name);
        CompletableFuture.supplyAsync(() -> Api.requestPost("/my/friends", body))
            .thenAccept(r -> client.execute(() -> {
                busy = null;
                if (!r.ok()) status = r.error();
                else {
                    nameField.setText("");
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
            .thenAccept(r -> client.execute(() -> {
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
        if (client.world != null) client.world.disconnect(Text.of(""));
        VanillaScreens.connectTo(client, address);
    }

    private void updateActionBtn() {
        if (actionBtn == null) return;
        McsdcFriendListWidget.Row row = listWidget != null ? listWidget.getSelectedRow() : null;
        if (locationsTab) {
            actionBtn.setMessage(Text.literal("Join"));
            actionBtn.active = row != null && canJoin(row.col2());
        } else {
            actionBtn.setMessage(Text.literal("Remove"));
            actionBtn.active = row != null && busy == null;
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 12, Colors.WHITE);
        if (locationsTab) {
            context.drawTextWithShadow(textRenderer, "Name", 20, 52, Colors.GRAY);
            context.drawTextWithShadow(textRenderer, "Server", 136, 52, Colors.GRAY);
        } else {
            context.drawTextWithShadow(textRenderer, "Name", 20, 52, Colors.GRAY);
            context.drawTextWithShadow(textRenderer, "Role", 136, 52, Colors.GRAY);
            context.drawTextWithShadow(textRenderer, "Stage", 236, 52, Colors.GRAY);
        }
        if (!status.isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, status, width / 2, height - 68, Colors.YELLOW);
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
