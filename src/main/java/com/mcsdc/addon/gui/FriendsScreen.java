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
import meteordevelopment.meteorclient.settings.SettingGroup;
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
    private static FriendsScreen instance = null;

    private MultiplayerScreen multiplayerScreen;
    private Screen parent;

    private int section = 0;
    private boolean listLoading = true;
    private boolean listLoaded = false;
    private String listError = "";
    private JsonArray friends = new JsonArray();

    private boolean locationsLoading = false;
    private boolean locationsLoaded = false;
    private String locationsError = "";
    private JsonArray locations = new JsonArray();

    @Nullable
    private String busy = null;

    private final Settings settings = new Settings();
    private final SettingGroup sg = settings.getDefaultGroup();
    private final Setting<String> nameSetting = sg.add(new StringSetting.Builder()
        .name("username")
        .description("")
        .defaultValue("")
        .build()
    );

    public static FriendsScreen instance(MultiplayerScreen multiplayerScreen, Screen parent) {
        if (instance == null) instance = new FriendsScreen();
        instance.multiplayerScreen = multiplayerScreen;
        instance.parent = parent;
        instance.resetList();
        return instance;
    }

    public FriendsScreen() {
        super(GuiThemes.get(), "Friends");
    }

    private void resetList() {
        listLoading = true;
        listLoaded = false;
        listError = "";
        friends = new JsonArray();
        locationsLoaded = false;
        locationsLoading = false;
        locationsError = "";
        locations = new JsonArray();
        busy = null;
        section = 0;
    }

    @Override
    public void initWidgets() {
        WHorizontalList tabs = add(theme.horizontalList()).expandX().widget();

        WButton listTab = tabs.add(theme.button("list")).expandX().widget();
        listTab.action = () -> {
            section = 0;
            reload();
        };

        WButton locationsTab = tabs.add(theme.button("on a server")).expandX().widget();
        locationsTab.action = () -> {
            section = 1;
            locationsLoaded = false;
            reload();
        };

        if (section == 0) {
            initListTab();
        } else {
            initLocationsTab();
        }
    }

    private void initListTab() {
        if (!listLoaded) {
            add(theme.label("loading…")).expandX();
            if (!listLoading) loadFriends();
            return;
        }

        if (!listError.isEmpty()) {
            add(theme.label(listError)).expandX();
        }

        WHorizontalList addRow = add(theme.horizontalList()).expandX().widget();
        addRow.add(theme.settings(settings)).expandX();
        if (busy != null) {
            addRow.add(theme.label("…"));
        } else {
            WButton addButton = addRow.add(theme.button("add")).widget();
            addButton.action = this::addFriend;
        }

        if (friends.isEmpty()) {
            add(theme.label("no friends yet")).expandX();
            return;
        }

        WTable table = add(theme.table()).expandX().widget();
        table.add(theme.label("Name"));
        table.add(theme.label("Role"));
        table.add(theme.label("Stage"));
        table.row();

        for (JsonElement el : friends) {
            if (!el.isJsonObject()) continue;
            JsonObject f = el.getAsJsonObject();
            String name = f.has("name") ? f.get("name").getAsString() : "";
            String role = f.has("role") ? f.get("role").getAsString() : "user";
            String stage = f.has("stage") && !f.get("stage").isJsonNull() ? f.get("stage").getAsString() : "";

            table.add(theme.label(name)).expandX();
            table.add(theme.label(roleLabel(role)));
            table.add(theme.label(stage.isEmpty() ? "—" : stage));

            if (busy != null) {
                table.add(theme.label(busy.equals(name) ? "…" : "—"));
            } else {
                WButton remove = theme.button("remove");
                remove.action = () -> removeFriend(name);
                table.add(remove);
            }
            table.row();
        }
    }

    private void initLocationsTab() {
        if (!locationsLoaded) {
            add(theme.label("loading…")).expandX();
            if (!locationsLoading) loadLocations();
            return;
        }

        if (!locationsError.isEmpty()) {
            add(theme.label(locationsError)).expandX();
            return;
        }

        if (locations.isEmpty()) {
            add(theme.label("no friends on a server right now")).expandX();
            return;
        }

        WTable table = add(theme.table()).expandX().widget();
        table.add(theme.label("Name"));
        table.add(theme.label("Server"));
        table.row();

        for (JsonElement el : locations) {
            if (!el.isJsonObject()) continue;
            JsonObject loc = el.getAsJsonObject();
            String name = loc.has("name") ? loc.get("name").getAsString() : "";
            String server = loc.has("server") ? loc.get("server").getAsString() : "";

            table.add(theme.label(name)).expandX();
            table.add(theme.label(server)).expandX();

            if (canJoinServer(server)) {
                String address = server;
                WButton join = theme.button("join");
                join.action = () -> joinServer(address);
                table.add(join);
            } else {
                table.add(theme.label("—"));
            }
            table.row();
        }
    }

    private void loadFriends() {
        listLoading = true;
        CompletableFuture.supplyAsync(() -> Api.getJson("/my/friends")).thenAccept(response -> mc.execute(() -> {
            listLoading = false;
            listLoaded = true;
            String err = Api.errorFrom(response);
            if (err != null) {
                listError = err;
                friends = new JsonArray();
            } else {
                listError = "";
                friends = response != null ? Api.unwrapArray(response) : new JsonArray();
            }
            reload();
        }));
    }

    private void loadLocations() {
        locationsLoading = true;
        CompletableFuture.supplyAsync(() -> Api.getJson("/my/friends/locations")).thenAccept(response -> mc.execute(() -> {
            locationsLoading = false;
            locationsLoaded = true;
            String err = Api.errorFrom(response);
            if (err != null) {
                locationsError = err;
                locations = new JsonArray();
            } else {
                locationsError = "";
                locations = response != null ? Api.unwrapArray(response) : new JsonArray();
            }
            reload();
        }));
    }

    private void addFriend() {
        String name = nameSetting.get().trim();
        if (name.isEmpty()) return;

        String self = McsdcSystem.get().getUsername();
        if (!self.isEmpty() && name.equals(self)) {
            listError = "cant add yourself lol";
            reload();
            return;
        }

        busy = "add";
        listError = "";
        reload();

        JsonObject body = new JsonObject();
        body.addProperty("name", name);

        CompletableFuture.supplyAsync(() -> Api.postJson("/my/friends", body)).thenAccept(response -> mc.execute(() -> {
            busy = null;
            String err = Api.errorFrom(response);
            if (err != null) {
                listError = err;
                reload();
                return;
            }
            nameSetting.set("");
            listLoaded = false;
            listLoading = true;
            reload();
        }));
    }

    private void removeFriend(String name) {
        busy = name;
        listError = "";
        reload();

        JsonObject body = new JsonObject();
        body.addProperty("name", name);

        CompletableFuture.supplyAsync(() -> Api.postJson("/my/friends/deny", body)).thenAccept(response -> mc.execute(() -> {
            busy = null;
            String err = Api.errorFrom(response);
            if (err != null) {
                listError = err;
                reload();
                return;
            }
            listLoaded = false;
            listLoading = true;
            reload();
        }));
    }

    private void joinServer(String address) {
        if (!canJoinServer(address)) return;
        if (mc.world != null) mc.world.disconnect(Text.of(""));
        ConnectScreen.connect(
            new MultiplayerScreen(new TitleScreen()), mc,
            ServerAddress.parse(address),
            new ServerInfo("", address, ServerInfo.ServerType.OTHER),
            false, null
        );
    }

    private static boolean canJoinServer(String server) {
        if (server == null || server.isBlank()) return false;
        return !server.equalsIgnoreCase("singleplayer");
    }

    private static String roleLabel(String role) {
        if (role == null || role.isBlank()) return "user";
        return switch (role) {
            case "mod", "admin", "owner", "banned" -> role;
            default -> "user";
        };
    }

    @Override
    public void close() {
        this.client.setScreen(parent);
    }
}
