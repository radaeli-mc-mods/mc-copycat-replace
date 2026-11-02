package com.radaeli.copycatreplace.client;

import com.radaeli.copycatreplace.CopycatReplace;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;

/** Registers the bulk-replacement modifier in the Controls screen. */
@EventBusSubscriber(
        modid = CopycatReplace.MOD_ID,
        value = Dist.CLIENT,
        bus = EventBusSubscriber.Bus.MOD
)
public final class CopycatReplaceKeyRegistration {
    private CopycatReplaceKeyRegistration() {
    }

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(CopycatReplaceClientKeys.BULK_REPLACE);
    }

    @SubscribeEvent
    public static void setup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event) {
        event.enqueueWork(() -> net.createmod.ponder.foundation.PonderIndex.addPlugin(new CopycatReplacePonder()));
    }
}
