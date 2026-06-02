package com.mcsdc.addon.gui;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mcsdc.addon.Api;
import com.mcsdc.addon.system.McsdcSystem;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.Settings;
import meteordevelopment.meteorclient.settings.StringSetting;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class FriendsScreen extends WindowScreen {
    private static FriendsScreen instance;

    private Screen parent;
    private boolean locationsTab;

    private final TabData list = new TabData();
    private final TabData locations = new TabData();

    @Nullable private String busy;

    private final Settings settings = new Settings();
    private final Setting<String> nameSetting = settings.getDefaultGroup().add(new StringSetting.Builder()
        .name("username")
        .description("")
        .defaultValue("")
        .build()
    );

    public static FriendsScreen instance(Screen parent) {
        if (instance == null) instance = new FriendsScreen();
        instance.parent = parent;
        instance.busy = null;
        instance.list.invalidate();
        return instance;
    }

    public FriendsScreen() {
        super(GuiThemes.get(), "Friends");
    }

    @Override
    public void initWidgets() {
        WHorizontalList tabs = add(theme.horizontalList()).expandX().widget();
        tabs.add(theme.button("list")).expandX().widget().action = () -> {
            locationsTab = false;
            reload();
        };
        tabs.add(theme.button("on a server")).expandX().widget().action = () -> {
            locationsTab = true;
            locations.invalidate();
            reload();
        };

        if (locationsTab) initLocationsTab();
        else initListTab();
    }

    private void initListTab() {
        if (pending(list, () -> fetchArray("/my/friends", list))) return;

        if (!list.error.isEmpty()) add(theme.label(list.error)).expandX();

        WHorizontalList addRow = add(theme.horizontalList()).expandX().widget();
        addRow.add(theme.settings(settings)).expandX();
        if (busy != null) addRow.add(theme.label("…"));
        else addRow.add(theme.button("add")).widget().action = this::addFriend;

        if (list.items.isEmpty()) {
            add(theme.label("no friends yet")).expandX();
            return;
        }

        WTable table = add(theme.table()).expandX().widget();
        table.add(theme.label("Name"));
        table.add(theme.label("Role"));
        table.add(theme.label("Stage"));
        table.row();

        for (JsonElement el : list.items) {
            if (!el.isJsonObject()) continue;
            JsonObject f = el.getAsJsonObject();
            String name = str(f, "name");
            String stage = str(f, "stage");

            table.add(theme.label(name)).expandX();
            table.add(theme.label(str(f, "role", "user")));
            table.add(theme.label(stage.isEmpty() ? "—" : stage));

            if (busy != null) table.add(theme.label(busy.equals(name) ? "…" : "—"));
            else {
                WButton remove = theme.button("remove");
                remove.action = () -> removeFriend(name);
                table.add(remove);
            }
            table.row();
        }
    }

    private void initLocationsTab() {
        if (pending(locations, () -> fetchArray("/my/friends/locations", locations))) return;

        if (!locations.error.isEmpty()) {
            add(theme.label(locations.error)).expandX();
            return;
        }

        if (locations.items.isEmpty()) {
            add(theme.label("no friends on a server right now")).expandX();
            return;
        }

        WTable table = add(theme.table()).expandX().widget();
        table.add(theme.label("Name"));
        table.add(theme.label("Server"));
        table.row();

        for (JsonElement el : locations.items) {
            if (!el.isJsonObject()) continue;
            JsonObject loc = el.getAsJsonObject();
            String name = str(loc, "name");
            String server = str(loc, "server");

            table.add(theme.label(name)).expandX();
            table.add(theme.label(server)).expandX();

            if (canJoin(server)) {
                WButton join = theme.button("join");
                join.action = () -> join(server);
                table.add(join);
            } else {
                table.add(theme.label("—"));
            }
            table.row();
        }
    }

    private boolean pending(TabData tab, Runnable fetch) {
        if (tab.loaded) return false;
        add(theme.label("loading…")).expandX();
        if (!tab.loading) fetch.run();
        return true;
    }

    private void fetchArray(String path, TabData tab) {
        tab.loading = true;
        CompletableFuture.supplyAsync(() -> Api.requestGet(path))
            .thenAccept(r -> ui(() -> {
                if (r.ok()) tab.ok(Api.unwrapArray(r.body()));
                else tab.fail(r.error());
            }))
            .exceptionally(e -> {
                ui(() -> tab.fail(e.getMessage()));
                return null;
            });
    }

    private void addFriend() {
        String name = nameSetting.get().trim();
        if (name.isEmpty()) return;
        if (name.equals(McsdcSystem.get().getUsername())) {
            list.error = "cant add yourself lol";
            reload();
            return;
        }
        mutate("add", "/my/friends", name, () -> nameSetting.set(""));
    }

    private void removeFriend(String name) {
        mutate(name, "/my/friends/deny", name, null);
    }

    private void mutate(String mark, String path, String name, @Nullable Runnable onOk) {
        busy = mark;
        list.error = "";
        reload();

        JsonObject body = new JsonObject();
        body.addProperty("name", name);

        CompletableFuture.supplyAsync(() -> Api.requestPost(path, body))
            .thenAccept(r -> ui(() -> {
                busy = null;
                if (!r.ok()) {
                    list.error = r.error();
                    return;
                }
                if (onOk != null) onOk.run();
                list.invalidate();
            }))
            .exceptionally(e -> {
                ui(() -> {
                    busy = null;
                    list.fail(e.getMessage());
                });
                return null;
            });
    }

    private void join(String address) {
        if (!canJoin(address)) return;
        if (mc.world != null) mc.world.disconnect(Text.of(""));
        ConnectScreen.connect(
            new MultiplayerScreen(new TitleScreen()), mc,
            ServerAddress.parse(address),
            new ServerInfo("", address, ServerInfo.ServerType.OTHER),
            false, null
        );
    }

    private static void ui(Runnable action) {
        mc.execute(() -> {
            action.run();
            if (mc.currentScreen instanceof FriendsScreen s) s.reload();
        });
    }

    private static boolean canJoin(String server) {
        return !server.isBlank() && !server.equalsIgnoreCase("singleplayer");
    }

    private static String str(JsonObject o, String key) {
        return str(o, key, "");
    }

    private static String str(JsonObject o, String key, String fallback) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : fallback;
    }

    @Override
    public void close() {
        client.setScreen(parent);
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
