package com.mcsdc.addon.system;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;

public class ServerSearchBuilder {
    public static class Version {
        Object value;

        public Version(Object value) {
            this.value = value;
        }

        public JsonElement toJson() {
            if (value == null) {
                return null;
            }
            JsonObject versionObject = new JsonObject();
            if (value instanceof Integer) {
                versionObject.addProperty("protocol", (Integer) value);
            } else {
                versionObject.addProperty("name", value.toString());
            }
            return versionObject;
        }
    }

    public static class Flags {
        Boolean visited, griefed, modded, saved, whitelist, active, cracked;

        public Flags(Boolean visited, Boolean griefed, Boolean modded, Boolean saved,
                Boolean whitelist, Boolean active, Boolean cracked) {
            this.visited = visited;
            this.griefed = griefed;
            this.modded = modded;
            this.saved = saved;
            this.whitelist = whitelist;
            this.active = active;
            this.cracked = cracked;
        }
    }

    public static class Extra {
        Boolean hasHistory, hasNotes;
        HashMap<MOTD, Boolean> motds = null;

        public Extra(Boolean hasHistory, Boolean hasNotes, @Nullable HashMap<MOTD, Boolean> motds) {
            this.hasHistory = hasHistory;
            this.hasNotes = hasNotes;
            this.motds = motds;
        }
    }

    public static class Search {
        Version version;
        Flags flags;
        Extra extra;

        public Search(Version version, Flags flags, Extra extra) {
            this.version = version;
            this.flags = flags;
            this.extra = extra;
        }
    }

    public static JsonObject createFilter(Search search) {
        JsonObject filters = new JsonObject();

        if (search.flags != null) {
            addBool(filters, "active", search.flags.active);
            addBool(filters, "cracked", search.flags.cracked);
            addBool(filters, "griefed", search.flags.griefed);
            addBool(filters, "modded", search.flags.modded);
            addBool(filters, "whitelist", search.flags.whitelist);
            addBool(filters, "has_note", search.flags.saved);
        }

        if (search.version != null) {
            JsonElement versionElement = search.version.toJson();
            if (versionElement != null) filters.add("version", versionElement);
        }

        if (filters.size() == 0) filters.addProperty("active", true);

        return filters;
    }

    private static void addBool(JsonObject obj, String key, Boolean value) {
        if (value != null) obj.addProperty(key, value);
    }
}
