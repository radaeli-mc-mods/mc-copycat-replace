package com.radaeli.copycatreplace.interaction;

import com.radaeli.copycatreplace.network.BulkReplaceMode;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

/** Intercepts the Forge click before Create handles the wrench. */
public final class CopycatReplaceHandler {
    private CopycatReplaceHandler() {}

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        var player = event.getEntity();
        var level = event.getLevel();
        var action = CopycatInteraction.find(player, level, event.getHand(), event.getHitVec(),
                BulkReplaceMode.isDown(player));
        if (action == null) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide()));
        if (!level.isClientSide()) action.run();
    }
}
