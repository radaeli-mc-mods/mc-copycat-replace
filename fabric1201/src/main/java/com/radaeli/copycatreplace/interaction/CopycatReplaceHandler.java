package com.radaeli.copycatreplace.interaction;

import com.radaeli.copycatreplace.network.BulkReplaceMode;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

public final class CopycatReplaceHandler {
    private CopycatReplaceHandler() {}

    public static InteractionResult interact(Player player, Level level, InteractionHand hand, BlockHitResult hit) {
        if (!(level instanceof ServerLevel) || !level.hasChunkAt(hit.getBlockPos())
                || !level.getWorldBorder().isWithinBounds(hit.getBlockPos())) {
            return InteractionResult.PASS;
        }
        var action = CopycatInteraction.find(player, level, hand, hit, BulkReplaceMode.isDown(player));
        if (action == null) return InteractionResult.PASS;
        action.run();
        return InteractionResult.CONSUME;
    }
}
