package com.mcsdc.addon.gui;

import com.google.gson.JsonObject;
import com.mcsdc.addon.Api;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.settings.*;

import java.util.concurrent.CompletableFuture;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class EditFlagsScreen extends WindowScreen {

    private final String ip;

    private final Settings settings = new Settings();
    private final SettingGroup sg = settings.createGroup("Flags");

    private final Setting<String> notesSetting = sg.add(new StringSetting.Builder()
        .name("notes")
        .description("")
        .defaultValue("")
        .build()
    );

    private final Setting<Boolean> griefedSetting = sg.add(new BoolSetting.Builder()
        .name("griefed")
        .description("")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> savedSetting = sg.add(new BoolSetting.Builder()
        .name("saved")
        .description("")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> visitedSetting = sg.add(new BoolSetting.Builder()
        .name("visited")
        .description("")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> moddedSetting = sg.add(new BoolSetting.Builder()
        .name("modded")
        .description("")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> whitelistSetting = sg.add(new BoolSetting.Builder()
        .name("whitelist")
        .description("")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> bannedSetting = sg.add(new BoolSetting.Builder()
        .name("banned")
        .description("")
        .defaultValue(false)
        .build()
    );

    public EditFlagsScreen(String ip) {
        super(GuiThemes.get(), "Edit Flags");
        this.ip = ip;
    }

    @Override
    public void initWidgets() {
        CompletableFuture.supplyAsync(() -> {
            String response = Api.postJson("/search/query", Api.addressBody(this.ip));
            if (response == null || response.isEmpty()) return null;
            JsonObject server = Api.normalizeServer(Api.unwrapObject(response));
            return server.has("error") ? null : server;
        }).thenAccept(server -> {
            if (server == null) {
                mc.execute(() -> add(theme.label("Not Valid")));
                return;
            }

            mc.execute(() -> {
                WTable table = add(theme.table()).widget();
                table.minWidth = 300;

                if (server.has("notes")) {
                    notesSetting.set(server.get("notes").getAsString());
                }

                JsonObject status = server.getAsJsonObject("status");
                griefedSetting.set(flag(status, "griefed"));
                savedSetting.set(flag(status, "save_for_later"));
                visitedSetting.set(flag(status, "visited"));
                moddedSetting.set(flag(status, "modded"));
                whitelistSetting.set(flag(status, "whitelist"));
                bannedSetting.set(flag(status, "banned"));
                table.add(theme.settings(settings)).expandX();
                table.row();
                table.add(theme.button("Save")).expandX().widget().action = this::setMarked;
                table.row();
            });
        });
    }

    public void setMarked(){
        JsonObject body = new JsonObject();
        JsonObject flagJson = new JsonObject();

        flagJson.addProperty("griefed", griefedSetting.get());
        flagJson.addProperty("modded", moddedSetting.get());
        flagJson.addProperty("whitelist", whitelistSetting.get());

        body.addProperty("address", ip);
        body.addProperty("note", notesSetting.get());
        body.add("flags", flagJson);

        CompletableFuture.runAsync(() -> Api.post("/update/server", body).send());
    }

    private static boolean flag(JsonObject status, String key) {
        return status.has(key) && status.get(key).getAsBoolean();
    }
}
