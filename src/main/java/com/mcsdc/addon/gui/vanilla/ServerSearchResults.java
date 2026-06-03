package com.mcsdc.addon.gui.vanilla;

import com.mcsdc.addon.Api;
import com.mcsdc.addon.system.ServerStorage;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class ServerSearchResults {
    private ServerSearchResults() {}

    public record ParseResult(@Nullable List<ServerStorage> servers, @Nullable String error) {
        public boolean ok() {
            return error == null;
        }

        public List<ServerStorage> serversOrEmpty() {
            return servers != null ? servers : List.of();
        }
    }

    public static ParseResult parse(@Nullable String response) {
        if (response == null) return new ParseResult(null, "No response.");
        String err = Api.errorFrom(response);
        if (err != null) return new ParseResult(null, err);

        String json = response;
        if (json.endsWith(",]")) json = json.substring(0, json.length() - 2) + "]";

        try {
            return new ParseResult(ServerStorage.fromJsonArray(json), null);
        } catch (Exception e) {
            return new ParseResult(null, "Error parsing response.");
        }
    }

    public static String statusFor(List<ServerStorage> servers) {
        return servers.isEmpty() ? "No servers found." : servers.size() + " server(s) found.";
    }
}
