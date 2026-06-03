package com.mcsdc.addon;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.ServerList;

public final class ServerListHelper {
    private static final String MCSdc_PREFIX = "Mcsdc";

    private ServerListHelper() {}

    public static ServerList load() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ServerList list = openList(mc);
        if (list == null) {
            list = new ServerList(mc);
            list.loadFile();
        }
        return list;
    }

    public static void addMcsdcServer(String ip) {
        addAllMcsdcServers(java.util.List.of(ip));
    }

    public static void addAllMcsdcServers(Iterable<String> ips) {
        ServerList list = load();
        for (String ip : ips) list.add(mcsdcServer(ip), false);
        save(list);
    }

    public static void removeMcsdcServers() {
        ServerList list = load();
        for (int i = list.size() - 1; i >= 0; i--) {
            if (isMcsdcEntry(list.get(i))) list.remove(list.get(i));
        }
        save(list);
    }

    public static void save(ServerList list) {
        list.saveFile();
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.currentScreen instanceof MultiplayerScreen mp) {
            MultiplayerScreenUtils.reload(mp);
        }
    }

    private static ServerList openList(MinecraftClient mc) {
        if (mc.currentScreen instanceof MultiplayerScreen mp) {
            return mp.getServerList();
        }
        return null;
    }

    private static boolean isMcsdcEntry(ServerInfo info) {
        return info.name.startsWith(MCSdc_PREFIX);
    }

    private static ServerInfo mcsdcServer(String ip) {
        return new ServerInfo(MCSdc_PREFIX + " " + ip, ip, ServerInfo.ServerType.OTHER);
    }
}
