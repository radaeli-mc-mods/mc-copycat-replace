package com.radaeli.copycatreplace;

import com.mojang.logging.LogUtils;
import com.radaeli.copycatreplace.config.ServerConfigReload;
import com.radaeli.copycatreplace.interaction.CopycatReplaceHandler;
import com.radaeli.copycatreplace.network.ModNetworking;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import org.slf4j.Logger;

public final class CopycatReplace implements ModInitializer {
    public static final String MOD_ID = "copycat_replace";
    public static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public void onInitialize() {
        ModNetworking.register();
        ServerConfigReload.register();
        UseBlockCallback.EVENT.register(CopycatReplaceHandler::interact);
        LOGGER.info("Create: Copycat Replace! initialized on Fabric");
    }
}
