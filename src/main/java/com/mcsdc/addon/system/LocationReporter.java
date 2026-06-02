package com.mcsdc.addon.system;

import com.google.gson.JsonObject;
import com.mcsdc.addon.Api;
import com.mcsdc.addon.Main;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.network.ServerInfo;

import java.util.concurrent.CompletableFuture;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public final class LocationReporter {
    private static final LocationReporter INSTANCE = new LocationReporter();
    private static final int REPORT_INTERVAL_TICKS = 1200;

    private int tickCounter = 0;

    public static void init() {
        MeteorClient.EVENT_BUS.subscribe(INSTANCE);
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (McsdcSystem.get().getToken().isEmpty()) return;
        if (mc.player == null || mc.world == null) return;

        if (++tickCounter < REPORT_INTERVAL_TICKS) return;
        tickCounter = 0;

        JsonObject body = buildPayload();
        CompletableFuture.runAsync(() -> {
            try {
                Api.post("/my/location", body).ignoreExceptions().send();
            } catch (Exception e) {
                Main.LOG.debug("location report failed", e);
            }
        });
    }

    private static JsonObject buildPayload() {
        JsonObject body = new JsonObject();

        String server = "";
        ClientPlayNetworkHandler handler = mc.getNetworkHandler();
        if (handler != null) {
            ServerInfo info = handler.getServerInfo();
            if (info != null) server = info.address;
        }
        body.addProperty("server", server);

        body.addProperty("name", mc.player.getName().getString());
        body.addProperty("uuid", mc.player.getUuidAsString());

        JsonObject pos = new JsonObject();
        pos.addProperty("x", mc.player.getX());
        pos.addProperty("y", mc.player.getY());
        pos.addProperty("z", mc.player.getZ());
        body.add("pos", pos);

        int ping = 0;
        if (handler != null) {
            PlayerListEntry entry = handler.getPlayerListEntry(mc.player.getUuid());
            if (entry != null) ping = entry.getLatency();
        }
        body.addProperty("ping", ping);

        return body;
    }
}
