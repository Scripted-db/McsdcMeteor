package com.mcsdc.addon.system;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mcsdc.addon.Api;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public record ServerStorage(String ip, String version, @Nullable Long lastScanned, @Nullable Long lastSeen) {

    public static List<ServerStorage> fromJsonArray(String jsonResponse) {
        List<ServerStorage> list = new ArrayList<>();
        JsonArray array = Api.unwrapArray(jsonResponse);

        array.forEach(node -> {
            JsonObject obj = Api.normalizeServer(node.getAsJsonObject());
            String address = obj.get("address").getAsString();
            String version = Api.displayVersion(obj);

            long lastScanned = Api.timeToMs(obj, "last_scanned");
            long lastSeen = Api.timeToMs(obj, "last_seen_online");
            if (lastSeen == 0) lastSeen = maxHistoryLastSeen(obj);

            list.add(new ServerStorage(
                address,
                version,
                lastScanned > 0 ? lastScanned : null,
                lastSeen > 0 ? lastSeen : null
            ));
        });

        return list;
    }

    private static long maxHistoryLastSeen(JsonObject server) {
        if (!server.has("historical") || !server.get("historical").isJsonArray()) return 0;
        long max = 0;
        for (var el : server.getAsJsonArray("historical")) {
            if (!el.isJsonObject()) continue;
            max = Math.max(max, Api.timeToMs(el.getAsJsonObject(), "last_seen"));
        }
        return max;
    }
}
