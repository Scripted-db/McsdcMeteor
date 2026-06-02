package com.mcsdc.addon;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mcsdc.addon.system.McsdcSystem;
import meteordevelopment.meteorclient.utils.network.Http;
import org.jetbrains.annotations.Nullable;

public final class Api {
    private static final String[] STATUS_KEYS = {
        "visited", "griefed", "modded", "whitelist", "save_for_later", "banned", "cracked"
    };
    private static final String[] ARRAY_KEYS = { "servers", "results", "rows", "list", "items" };

    private Api() {}

    public static String url(String path) {
        return Main.apiBase + (path.startsWith("/") ? path : "/" + path);
    }

    public static Http.Request post(String path, @Nullable JsonObject body) {
        Http.Request req = Http.post(url(path));
        if (body != null) req.bodyJson(body);
        String token = McsdcSystem.get().getToken();
        if (!token.isEmpty()) req.header("Authorization", "Bearer " + token);
        return req;
    }

    public static Http.Request postPublic(String path, JsonObject body) {
        return Http.post(url(path)).bodyJson(body);
    }

    @Nullable
    public static String responseBody(@Nullable java.net.http.HttpResponse<String> response) {
        return response != null ? response.body() : null;
    }

    @Nullable
    public static String postJson(String path, @Nullable JsonObject body) {
        return responseBody(post(path, body).sendStringResponse());
    }

    @Nullable
    public static String postPublicJson(String path, JsonObject body) {
        return responseBody(postPublic(path, body).sendStringResponse());
    }

    public static JsonObject addressBody(String address) {
        JsonObject body = new JsonObject();
        body.addProperty("address", address);
        return body;
    }

    @Nullable
    public static String errorFrom(@Nullable JsonObject obj) {
        if (obj != null && obj.has("error") && obj.get("error").isJsonPrimitive()) {
            return obj.get("error").getAsString();
        }
        return null;
    }

    @Nullable
    public static String errorFrom(@Nullable String body) {
        if (body == null || body.isBlank()) return "no response";
        try {
            JsonElement parsed = parse(body);
            if (parsed.isJsonObject()) return errorFrom(parsed.getAsJsonObject());
        } catch (Exception ignored) {}
        return null;
    }

    public static final String BAN_LOGIN_MSG = "token banned. appeal here: discord.gg/TrsAk3Ay5T";

    public static boolean isBanError(@Nullable String err) {
        return err != null && err.toLowerCase().contains("ban");
    }

    public static String loginFailureMessage(String apiError) {
        return isBanError(apiError) ? BAN_LOGIN_MSG : apiError;
    }

    public static boolean isUserBanned(JsonObject data) {
        if (data.has("access") && data.get("access").isJsonObject()) {
            JsonObject access = data.getAsJsonObject("access");
            if (access.has("banned") && access.get("banned").getAsBoolean()) return true;
        }
        if (data.has("role") && data.get("role").isJsonPrimitive() && "banned".equals(data.get("role").getAsString())) {
            return true;
        }
        return data.has("perms") && !data.get("perms").isJsonNull() && data.get("perms").getAsInt() == 0;
    }

    public static JsonArray unwrapArray(String body) {
        JsonElement parsed = parse(body);
        if (parsed.isJsonArray()) return parsed.getAsJsonArray();
        if (!parsed.isJsonObject()) return new JsonArray();

        JsonObject root = parsed.getAsJsonObject();
        if (root.has("data") && root.get("data").isJsonArray()) {
            return root.getAsJsonArray("data");
        }
        for (String key : ARRAY_KEYS) {
            if (root.has(key) && root.get(key).isJsonArray()) return root.getAsJsonArray(key);
        }
        return new JsonArray();
    }

    public static JsonObject unwrapObject(String body) {
        JsonElement parsed = parse(body);
        if (!parsed.isJsonObject()) return new JsonObject();

        JsonObject root = parsed.getAsJsonObject();
        if (root.has("error")) return root;

        if (root.has("data") && root.get("data").isJsonObject()) {
            return root.getAsJsonObject("data");
        }
        if (root.has("info") && root.get("info").isJsonObject()) {
            return root.getAsJsonObject("info");
        }
        return root;
    }

    public static JsonObject normalizeServer(JsonObject s) {
        if (s.has("note") && !s.has("notes")) {
            s.addProperty("notes", s.get("note").getAsString());
        }

        if (s.has("history") && !s.has("historical")) {
            s.add("historical", s.get("history"));
        }
        if (s.has("historical") && s.get("historical").isJsonArray()) {
            for (JsonElement el : s.getAsJsonArray("historical")) {
                if (!el.isJsonObject()) continue;
                JsonObject player = el.getAsJsonObject();
                patchTime(player, "last_seen");
                patchTime(player, "discovered");
            }
        }

        patchTime(s, "last_seen_online");
        patchTime(s, "last_scanned");
        patchTime(s, "last_joined");
        aliasTime(s, "last_online", "last_seen_online");
        aliasTime(s, "scanned", "last_scanned");

        if (!s.has("last_joined")) s.addProperty("last_joined", 0);

        JsonElement version = s.get("version");
        if (!s.has("version") || version.isJsonNull() || version.isJsonObject()) {
            s.addProperty("version", displayVersion(s));
        }

        JsonObject status = s.has("status") && s.get("status").isJsonObject()
            ? s.getAsJsonObject("status")
            : new JsonObject();

        for (String key : STATUS_KEYS) {
            mergeFlag(status, s, key);
            if (!status.has(key)) status.addProperty(key, false);
        }

        s.add("status", status);
        return s;
    }

    private static void aliasTime(JsonObject s, String from, String to) {
        if (!s.has(to) && s.has(from)) {
            patchTime(s, from);
            s.add(to, s.get(from));
        }
    }

    private static void mergeFlag(JsonObject status, JsonObject s, String key) {
        if (s.has(key) && !s.get(key).isJsonNull()) {
            status.addProperty(key, s.get(key).getAsBoolean());
            return;
        }
        if (s.has("flags") && s.get("flags").isJsonObject()) {
            JsonObject flags = s.getAsJsonObject("flags");
            if (flags.has(key) && !flags.get(key).isJsonNull()) {
                status.addProperty(key, flags.get(key).getAsBoolean());
            }
        }
    }

    public static String displayVersion(JsonObject s) {
        if (!s.has("version")) return fallbackVersion(s);
        JsonElement v = s.get("version");
        if (v.isJsonPrimitive()) {
            if (v.getAsJsonPrimitive().isNumber()) return v.getAsString();
            String str = v.getAsString();
            if (!str.isBlank()) return str;
        } else if (v.isJsonObject()) {
            JsonObject vo = v.getAsJsonObject();
            if (vo.has("name")) return vo.get("name").getAsString();
            if (vo.has("protocol")) return vo.get("protocol").getAsString();
        }
        return fallbackVersion(s);
    }

    private static String fallbackVersion(JsonObject s) {
        if (s.has("version_name")) return s.get("version_name").getAsString();
        if (s.has("protocol")) return s.get("protocol").getAsString();
        return "";
    }

    public static long timeToMs(JsonObject o, String key) {
        if (!o.has(key) || o.get(key).isJsonNull()) return 0;
        long n = o.get(key).getAsLong();
        if (n <= 0) return 0;
        if (n >= 1_000_000_000_000L) return n;
        return n * 1000;
    }

    public static int roleToPerms(@Nullable JsonElement roleEl) {
        if (roleEl == null || roleEl.isJsonNull()) return 1;
        if (roleEl.isJsonPrimitive() && roleEl.getAsJsonPrimitive().isNumber()) {
            return roleEl.getAsInt();
        }
        return switch (roleEl.getAsString()) {
            case "banned" -> 0;
            case "user" -> 1;
            case "mod" -> 2;
            case "admin" -> 3;
            case "owner" -> 4;
            default -> 1;
        };
    }

    private static void patchTime(JsonObject o, String key) {
        if (!o.has(key) || o.get(key).isJsonNull()) return;
        o.addProperty(key, timeToMs(o, key));
    }

    private static JsonElement parse(String body) {
        return JsonParser.parseString(body);
    }
}
