package com.radaeli.copycatreplace;

import com.mojang.logging.LogUtils;
import com.radaeli.copycatreplace.config.CopycatReplaceConfig;
import com.radaeli.copycatreplace.config.LegacyConfigMigration;
import com.radaeli.copycatreplace.config.ServerConfigReload;
import com.radaeli.copycatreplace.interaction.CopycatReplaceHandler;
import com.radaeli.copycatreplace.network.ConnectedBlockLimitSync;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import com.radaeli.copycatreplace.network.ModNetworking;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.config.ModConfig;
import org.slf4j.Logger;

/** Entry point for Create: Copycat Replace!. */
@Mod(CopycatReplace.MOD_ID)
public final class CopycatReplace {
    public static final String MOD_ID = "copycat_replace";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CopycatReplace() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModNetworking.register();
        LegacyConfigMigration.prepare();
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, CopycatReplaceConfig.SPEC);
        modEventBus.addListener(ConnectedBlockLimitSync::onConfigReloading);
        modEventBus.addListener(ServerConfigReload::onLoading);
        modEventBus.addListener(ServerConfigReload::onUnloading);
        MinecraftForge.EVENT_BUS.addListener(ServerConfigReload::onServerTick);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGH, CopycatReplaceHandler::onRightClickBlock);
        MinecraftForge.EVENT_BUS.addListener(ConnectedBlockLimitSync::onPlayerLoggedIn);
        LOGGER.info("Create: Copycat Replace! initialized");
    }
}
