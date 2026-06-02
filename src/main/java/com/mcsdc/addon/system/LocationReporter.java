package com.mcsdc.addon.system;

import com.google.gson.JsonObject;
import com.mcsdc.addon.Api;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.network.ServerInfo;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public final class LocationReporter {
    private static final LocationReporter INSTANCE = new LocationReporter();

    public static void init() {
        MeteorClient.EVENT_BUS.subscribe(INSTANCE);
    }

    @EventHandler
    private void onGameJoined(GameJoinedEvent event) {
        if (McsdcSystem.get().getToken().isEmpty()) return;
        String server = playSessionServer();
        if (server == null) return;
        CompletableFuture.runAsync(() -> Api.post("/my/location", buildPayload(server)).ignoreExceptions().send());
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        if (McsdcSystem.get().getToken().isEmpty()) return;
        CompletableFuture.runAsync(() -> Api.delete("/my/location"));
    }

    @Nullable
    private static String playSessionServer() {
        if (mc.player == null || mc.world == null) return null;
        if (mc.isInSingleplayer()) return "";
        ClientPlayNetworkHandler handler = mc.getNetworkHandler();
        if (handler == null) return null;
        ServerInfo info = handler.getServerInfo();
        if (info == null || info.address.isBlank()) return null;
        return info.address;
    }

    private static JsonObject buildPayload(String server) {
        JsonObject body = new JsonObject();
        body.addProperty("server", server);
        body.addProperty("name", mc.player.getName().getString());
        body.addProperty("uuid", mc.player.getUuidAsString());

        JsonObject pos = new JsonObject();
        pos.addProperty("x", mc.player.getX());
        pos.addProperty("y", mc.player.getY());
        pos.addProperty("z", mc.player.getZ());
        body.add("pos", pos);

        int ping = 0;
        ClientPlayNetworkHandler handler = mc.getNetworkHandler();
        if (handler != null) {
            PlayerListEntry entry = handler.getPlayerListEntry(mc.player.getUuid());
            if (entry != null) ping = entry.getLatency();
        }
        body.addProperty("ping", ping);
        return body;
    }
}
