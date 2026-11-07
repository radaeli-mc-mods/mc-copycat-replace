package com.radaeli.copycatreplace.compat.openpac;

import com.radaeli.copycatreplace.service.MaterialEditPermission.Operation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import xaero.pac.common.server.api.OpenPACServerAPI;

public final class OpenPacProtection {
    private OpenPacProtection() {}

    public static boolean mayEdit(ServerPlayer player, ServerLevel world, BlockPos pos, BlockHitResult hit,
                                  Operation operation) {
        var protection = OpenPACServerAPI.get(world.getServer()).getChunkProtection();
        if (operation != Operation.APPLY && protection.onBlockInteraction(
                player, InteractionHand.MAIN_HAND, null, world, pos, hit.getDirection(), true, false, true)) {
            return false;
        }
        return operation == Operation.REMOVE || !protection.onEntityPlaceBlock(player, world, pos);
    }
}
