package com.mcsdc.addon;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;

public final class ServerListHelper {
    private static final String MCSdc_PREFIX = "Mcsdc";

    private ServerListHelper() {}

    public static ServerList load() {
        Minecraft mc = Minecraft.getInstance();
        ServerList list = openList(mc);
        if (list == null) {
            list = new ServerList(mc);
            list.load();
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
        list.save();
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof JoinMultiplayerScreen mp) {
            MultiplayerScreenUtils.reload(mp);
        }
    }

    private static ServerList openList(Minecraft mc) {
        if (mc.screen instanceof JoinMultiplayerScreen mp) {
            return mp.getServers();
        }
        return null;
    }

    private static boolean isMcsdcEntry(ServerData info) {
        return info.name.startsWith(MCSdc_PREFIX);
    }

    private static ServerData mcsdcServer(String ip) {
        return new ServerData(MCSdc_PREFIX + " " + ip, ip, ServerData.Type.OTHER);
    }
}
