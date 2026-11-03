package com.radaeli.copycatreplace.client;

import com.radaeli.copycatreplace.CopycatReplace;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

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
    public static void setup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
        event.enqueueWork(() -> net.createmod.ponder.foundation.PonderIndex.addPlugin(new CopycatReplacePonder()));
    }
}
