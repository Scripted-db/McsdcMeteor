package com.mcsdc.addon;

import com.mcsdc.addon.commands.TicketIDCommand;
import com.mcsdc.addon.system.LocationReporter;
import com.mcsdc.addon.util.TicketIDGenerator;
import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.utils.misc.MeteorStarscript;
import org.meteordev.starscript.value.Value;
import org.slf4j.Logger;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class Main extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    public static String apiBase = "https://interact.mcsdc.online/api2";

    @Override
    public void onInitialize() {
        LOG.info("Initializing mcsdc.");
        Commands.add(new TicketIDCommand());
        LocationReporter.init();
        MeteorStarscript.ss.set("ticketID", () -> Value.string(getTicketID()));
    }

    @Override
    public String getPackage() {
        return "com.mcsdc.addon";
    }

    @Override
    public String getWebsite() {
        return "https://codeberg.org/Syu/McsdcMeteor";
    }

    public static String getTicketID(){
        if (mc == null || mc.getNetworkHandler() == null) return "";

        return TicketIDGenerator.generateTicketID(mc.getNetworkHandler().getServerInfo().address);
    }
}
