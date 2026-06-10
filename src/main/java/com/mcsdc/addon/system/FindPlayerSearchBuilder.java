package com.mcsdc.addon.system;

import com.google.gson.JsonObject;
import java.util.Optional;
import java.util.UUID;

public class FindPlayerSearchBuilder {
    public static JsonObject create(String player) {
        JsonObject body = new JsonObject();
        Optional<UUID> uuid = fromString(player);

        if (uuid.isPresent()) {
            body.addProperty("uuid", uuid.get().toString());
        } else {
            body.addProperty("name", player);
        }

        return body;
    }

    private static UUID parseUuid(String value) {
        return UUID.fromString(value);
    }

    private static Optional<UUID> fromString(String value) {
        try {
            return Optional.of(parseUuid(value));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }
}