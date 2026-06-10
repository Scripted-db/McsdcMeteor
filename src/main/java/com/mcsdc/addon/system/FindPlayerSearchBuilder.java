package com.mcsdc.addon.system;

import com.google.gson.JsonObject;

public class FindPlayerSearchBuilder {
    private static final String UUID_PATTERN =
        "(?i)[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";

    public static JsonObject create(String player) {
        JsonObject body = new JsonObject();
        String key = player.matches(UUID_PATTERN) ? "uuid" : "name";
        body.addProperty(key, player);
        return body;
    }
}
