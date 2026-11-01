package com.radaeli.copycatreplace;

import com.mojang.logging.LogUtils;
import com.radaeli.copycatreplace.config.CopycatReplaceConfig;
import com.radaeli.copycatreplace.config.LegacyConfigMigration;
import com.radaeli.copycatreplace.interaction.CopycatReplaceHandler;
import com.radaeli.copycatreplace.network.ConnectedBlockLimitSync;
import net.neoforged.fml.ModContainer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

/** Entry point for Create: Copycat Replace!. */
@Mod(CopycatReplace.MOD_ID)
public final class CopycatReplace {
    public static final String MOD_ID = "copycat_replace";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CopycatReplace(IEventBus modEventBus, ModContainer modContainer) {
        LegacyConfigMigration.prepare();
        modContainer.registerConfig(ModConfig.Type.SERVER, CopycatReplaceConfig.SPEC);
        modEventBus.addListener(ConnectedBlockLimitSync::onConfigReloading);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, CopycatReplaceHandler::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(ConnectedBlockLimitSync::onPlayerLoggedIn);
        LOGGER.info("Create: Copycat Replace! initialized");
    }
}
