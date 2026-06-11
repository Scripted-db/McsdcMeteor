package com.mcsdc.addon.gui.vanilla;

import com.google.gson.JsonObject;
import com.mcsdc.addon.Api;
import com.mcsdc.addon.Main;
import com.mcsdc.addon.ServerListHelper;
import com.mcsdc.addon.system.MOTD;
import com.mcsdc.addon.system.McsdcSystem;
import com.mcsdc.addon.system.ServerSearchBuilder;
import com.mcsdc.addon.system.ServerStorage;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class McsdcBrowseScreen extends McsdcParentScreen {
    private static final BrowseState LAST_SEARCH = new BrowseState();

    private final BrowseState state = LAST_SEARCH;

    private McsdcServerListWidget serverList;
    private EditBox versionField;
    private boolean searching;

    private Button joinBtn;
    private Button addBtn;
    private Button infoBtn;
    private Button addAllBtn;
    private Button shuffleBtn;

    public McsdcBrowseScreen(Screen parent) {
        super(Component.literal("Find Servers"), parent);
    }

    @Override
    protected void init() {
        int margin = 16;
        int filterW = Math.min(160, width / 3);
        int listX = margin + filterW + 12;
        int listW = width - listX - margin;
        int top = 48;
        int bottom = height - 52;
        int listH = bottom - top;

        serverList = new McsdcServerListWidget(listX, top, listW, listH);
        addRenderableWidget(serverList);

        int fx = margin;
        int fy = top;
        int rowH = 22;

        addRenderableWidget(FilterWidgets.cycleFlag("Visited", state.visited, f -> state.visited = f, fx, fy));
        fy += rowH;
        addRenderableWidget(FilterWidgets.cycleFlag("Modded", state.modded, f -> state.modded = f, fx, fy));
        fy += rowH;
        addRenderableWidget(FilterWidgets.cycleFlag("Whitelist", state.whitelist, f -> state.whitelist = f, fx, fy));
        fy += rowH;
        addRenderableWidget(FilterWidgets.cycleFlag("Cracked", state.cracked, f -> state.cracked = f, fx, fy));
        fy += rowH;
        addRenderableWidget(FilterWidgets.cycleFlag("Griefed", state.griefed, f -> state.griefed = f, fx, fy));
        fy += rowH;
        addRenderableWidget(FilterWidgets.cycleFlag("Saved", state.saved, f -> state.saved = f, fx, fy));
        fy += rowH;
        addRenderableWidget(FilterWidgets.cycleFlag("Active", state.active, f -> state.active = f, fx, fy));
        fy += rowH;
        addRenderableWidget(FilterWidgets.cycleFlag("History", state.hasHistory, f -> state.hasHistory = f, fx, fy));
        fy += rowH;
        addRenderableWidget(FilterWidgets.cycleFlag("Notes", state.hasNotes, f -> state.hasNotes = f, fx, fy));
        fy += rowH;
        addRenderableWidget(FilterWidgets.toggle("Hide offline", state.hideOffline, v -> state.hideOffline = v, fx, fy));
        fy += rowH;

        addRenderableWidget(FilterWidgets.toggle("Adv version", state.advancedVersion, v -> {
            state.advancedVersion = v;
            rebuildUi();
        }, fx, fy));
        fy += rowH;

        if (state.advancedVersion) {
            versionField = new EditBox(font, fx, fy, filterW, 18, Component.literal("version"));
            versionField.setMaxLength(64);
            versionField.setValue(state.advancedVersionText);
            addRenderableWidget(versionField);
            fy += rowH;
        } else {
            addRenderableWidget(Button.builder(Component.literal("Version: " + state.version.version), b ->
                minecraft.setScreen(new McsdcVersionSelectScreen(this, state.version, v -> state.version = v)))
                .bounds(fx, fy, FilterWidgets.FILTER_WIDTH, 20).build());
            fy += rowH;
            addRenderableWidget(FilterWidgets.toggle("Vanilla", state.vanilla, v -> state.vanilla = v, fx, fy));
            fy += rowH;
        }

        addRenderableWidget(FilterWidgets.toggle("Adv MOTD", state.advancedMotd, v -> {
            state.advancedMotd = v;
            rebuildUi();
        }, fx, fy));
        fy += rowH;

        if (state.advancedMotd) {
            addRenderableWidget(FilterWidgets.cycleFlag("Default", state.defaultMotd, f -> state.defaultMotd = f, fx, fy));
            fy += rowH;
            addRenderableWidget(FilterWidgets.cycleFlag("Community", state.communityMotd, f -> state.communityMotd = f, fx, fy));
            fy += rowH;
            addRenderableWidget(FilterWidgets.cycleFlag("Creative", state.creativeMotd, f -> state.creativeMotd = f, fx, fy));
            fy += rowH;
            addRenderableWidget(FilterWidgets.cycleFlag("Bigotry", state.bigotryMotd, f -> state.bigotryMotd = f, fx, fy));
            fy += rowH;
            addRenderableWidget(FilterWidgets.cycleFlag("Furry", state.furryMotd, f -> state.furryMotd = f, fx, fy));
            fy += rowH;
            addRenderableWidget(FilterWidgets.cycleFlag("LGBT", state.lgbtMotd, f -> state.lgbtMotd = f, fx, fy));
            fy += rowH;
        }

        addRenderableWidget(Button.builder(Component.literal("Search"), b -> runSearch())
            .bounds(fx, bottom - 22, filterW, 20).build());

        int bx = listX;
        joinBtn = addRenderableWidget(Button.builder(Component.literal("Join"), b -> ServerListActions.join(serverList))
            .bounds(bx, height - 28, 72, 20).build());
        addBtn = addRenderableWidget(Button.builder(Component.literal("Add"), b -> addSelected())
            .bounds(bx + 76, height - 28, 72, 20).build());
        infoBtn = addRenderableWidget(Button.builder(Component.literal("Info"), b -> ServerListActions.info(minecraft, serverList))
            .bounds(bx + 152, height - 28, 72, 20).build());
        addAllBtn = addRenderableWidget(Button.builder(Component.literal("Add all"), b -> addAll())
            .bounds(bx + 228, height - 28, 72, 20).build());
        shuffleBtn = addRenderableWidget(Button.builder(Component.literal("Shuffle"), b -> shuffle())
            .bounds(bx + 304, height - 28, 72, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Back"), b -> onClose())
            .bounds(width - 76, height - 28, 60, 20).build());

        serverList.setOnSelectionChanged(this::updateActionButtons);
        if (!state.results.isEmpty()) serverList.setServers(state.results);
        updateActionButtons();
    }

    private void rebuildUi() {
        captureVersionField();
        clearWidgets();
        init();
    }

    private void runSearch() {
        if (searching) return;
        captureVersionField();
        if (!state.advancedMotd && allCoreFlagsAny()) {
            state.statusMessage = "Pick at least one filter (not all Any).";
            return;
        }

        searching = true;
        state.statusMessage = "Searching...";
        BrowseState submittedSearch = state.copy();

        CompletableFuture.supplyAsync(() -> {
            Object ver = resolveVersion();
            if (ver instanceof String s && s.isEmpty()) return null;

            HashMap<MOTD, Boolean> motds = null;
            if (state.advancedMotd) {
                motds = new HashMap<>();
                motds.put(MOTD.DEFAULT, state.defaultMotd.bool);
                motds.put(MOTD.COMMUNITY, state.communityMotd.bool);
                motds.put(MOTD.CREATIVE, state.creativeMotd.bool);
                motds.put(MOTD.BIGOTRY, state.bigotryMotd.bool);
                motds.put(MOTD.FURRY, state.furryMotd.bool);
                motds.put(MOTD.LGBT, state.lgbtMotd.bool);
            }

            ServerSearchBuilder.Extra extra = new ServerSearchBuilder.Extra(state.hasHistory.bool, state.hasNotes.bool, motds);
            ServerSearchBuilder.Flags flags = new ServerSearchBuilder.Flags(
                state.visited.bool, state.griefed.bool, state.modded.bool, state.saved.bool,
                state.whitelist.bool, state.active.bool, state.cracked.bool
            );
            ServerSearchBuilder.Search search = new ServerSearchBuilder.Search(
                new ServerSearchBuilder.Version(ver), flags, extra
            );
            JsonObject json = ServerSearchBuilder.createFilter(search);
            Main.LOG.info(json.toString());
            return Api.postJson("/search/filter", json);
        }).thenAccept(response -> minecraft.execute(() -> {
            searching = false;
            if (response == null) {
                rememberSearch(submittedSearch, "Enter a version string.");
                return;
            }
            ServerSearchResults.ParseResult parsed = ServerSearchResults.parse(response);
            if (!parsed.ok()) {
                rememberSearch(submittedSearch, parsed.error());
                return;
            }
            List<ServerStorage> results = new ArrayList<>(parsed.serversOrEmpty());
            if (submittedSearch.hideOffline) results.removeIf(ServerStorage::isStale);
            state.results = results;
            if (results.isEmpty()) {
                state.statusMessage = "No servers found.";
                serverList.setServers(List.of());
            } else {
                state.statusMessage = ServerSearchResults.statusFor(results);
                McsdcSystem.get().setServerQueue(results);
                serverList.setServers(results);
            }
            submittedSearch.results = new ArrayList<>(results);
            rememberSearch(submittedSearch, state.statusMessage);
            updateActionButtons();
        })).exceptionally(ex -> {
            Main.LOG.error("Failed to search", ex);
            minecraft.execute(() -> {
                searching = false;
                Throwable root = ex.getCause() != null ? ex.getCause() : ex;
                String msg = root.getMessage();
                state.statusMessage = "Error: " + (msg != null ? msg : "Unknown error");
                updateActionButtons();
            });
            return null;
        });
    }

    private Object resolveVersion() {
        if (state.advancedVersion) {
            return versionField != null ? versionField.getValue() : state.advancedVersionText;
        }
        int number = state.version.number;
        if (state.vanilla && number != -1) return state.version.version;
        if (number != -1) return number;
        return null;
    }

    private boolean allCoreFlagsAny() {
        for (SearchFlag flag : List.of(
            state.visited, state.griefed, state.modded, state.saved,
            state.whitelist, state.active, state.cracked
        )) {
            if (flag.bool != null) return false;
        }
        return true;
    }

    private void addSelected() {
        ServerStorage s = serverList.getSelectedServer();
        if (s == null) return;
        ServerListHelper.addMcsdcServer(s.ip());
        state.statusMessage = "Added " + s.ip();
    }

    private void addAll() {
        if (state.results.isEmpty()) return;
        ServerListHelper.addAllMcsdcServers(state.results.stream().map(ServerStorage::ip).toList());
        state.statusMessage = "Added all servers.";
    }

    private void shuffle() {
        if (state.results.isEmpty()) return;
        Collections.shuffle(state.results);
        serverList.setServers(state.results);
    }

    private void updateActionButtons() {
        if (joinBtn == null) return;
        boolean sel = serverList.getSelectedServer() != null;
        ServerListActions.setActive(sel, joinBtn, addBtn, infoBtn);
        boolean hasResults = !state.results.isEmpty();
        if (addAllBtn != null) addAllBtn.active = hasResults;
        if (shuffleBtn != null) shuffleBtn.active = hasResults;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);
        context.centeredText(font, title, width / 2, 12, CommonColors.WHITE);
        context.text(font, "Filters", 16, 36, CommonColors.LIGHT_GRAY, true);
        if (serverList != null) {
            int lx = serverList.getX();
            int lw = serverList.getWidth();
            int ly = serverList.getY();
            int lh = serverList.getHeight();
            context.text(font, "Address", lx + 4, 36, CommonColors.GRAY, true);
            context.text(font, "Version", lx + lw / 2, 36, CommonColors.GRAY, true);
            if (state.results.isEmpty() && state.statusMessage.isEmpty()) {
                context.centeredText(font, "Set filters and hit Search", lx + lw / 2, ly + lh / 2, CommonColors.DARK_GRAY);
            }
        }
        if (!state.statusMessage.isEmpty()) {
            context.centeredText(font, state.statusMessage, width / 2, height - 42, CommonColors.YELLOW);
        }
    }

    private void captureVersionField() {
        if (versionField != null) state.advancedVersionText = versionField.getValue();
    }

    private void rememberSearch(BrowseState submitted, String status) {
        state.statusMessage = status;
        submitted.statusMessage = status;
        LAST_SEARCH.copyFrom(submitted);
    }

    private static final class BrowseState {
        private SearchFlag visited = SearchFlag.ANY;
        private SearchFlag modded = SearchFlag.ANY;
        private SearchFlag whitelist = SearchFlag.ANY;
        private SearchFlag cracked = SearchFlag.ANY;
        private SearchFlag griefed = SearchFlag.ANY;
        private SearchFlag saved = SearchFlag.ANY;
        private SearchFlag active = SearchFlag.YES;
        private SearchFlag hasHistory = SearchFlag.ANY;
        private SearchFlag hasNotes = SearchFlag.ANY;
        private SearchFlag defaultMotd = SearchFlag.ANY;
        private SearchFlag communityMotd = SearchFlag.ANY;
        private SearchFlag creativeMotd = SearchFlag.ANY;
        private SearchFlag bigotryMotd = SearchFlag.ANY;
        private SearchFlag furryMotd = SearchFlag.ANY;
        private SearchFlag lgbtMotd = SearchFlag.ANY;
        private boolean hideOffline = true;
        private boolean advancedMotd = false;
        private boolean advancedVersion = false;
        private boolean vanilla = false;
        private SearchVersion version = SearchVersion.ANY;
        private String advancedVersionText = "Paper 1.21.4";
        private List<ServerStorage> results = new ArrayList<>();
        private String statusMessage = "";

        private BrowseState copy() {
            BrowseState other = new BrowseState();
            other.copyFrom(this);
            return other;
        }

        private void copyFrom(BrowseState other) {
            visited = other.visited;
            modded = other.modded;
            whitelist = other.whitelist;
            cracked = other.cracked;
            griefed = other.griefed;
            saved = other.saved;
            active = other.active;
            hasHistory = other.hasHistory;
            hasNotes = other.hasNotes;
            defaultMotd = other.defaultMotd;
            communityMotd = other.communityMotd;
            creativeMotd = other.creativeMotd;
            bigotryMotd = other.bigotryMotd;
            furryMotd = other.furryMotd;
            lgbtMotd = other.lgbtMotd;
            hideOffline = other.hideOffline;
            advancedMotd = other.advancedMotd;
            advancedVersion = other.advancedVersion;
            vanilla = other.vanilla;
            version = other.version;
            advancedVersionText = other.advancedVersionText;
            results = new ArrayList<>(other.results);
            statusMessage = other.statusMessage;
        }
    }
}
