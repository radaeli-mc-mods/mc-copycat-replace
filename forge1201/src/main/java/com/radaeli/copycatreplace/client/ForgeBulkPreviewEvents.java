package com.radaeli.copycatreplace.client;

import com.radaeli.copycatreplace.CopycatReplace;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = CopycatReplace.MOD_ID, value = Dist.CLIENT)
public final class ForgeBulkPreviewEvents {
    private ForgeBulkPreviewEvents() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        ConnectedBulkIndicator.tick(Minecraft.getInstance(), CopycatReplaceClientKeys.BULK_REPLACE.isDown(),
                CopycatReplaceClientKeys.selection());
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        ConnectedBulkIndicator.render(Minecraft.getInstance(), event.getGuiGraphics(),
                CopycatReplaceClientKeys.BULK_REPLACE.isDown(), CopycatReplaceClientKeys.selection());
    }
}
