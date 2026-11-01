package com.radaeli.copycatreplace.service;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.level.BlockEvent;

/** Server-side veto checks for editing the material of an existing Copycat. */
public final class MaterialEditPermission {
    public enum Operation { APPLY, REPLACE, REMOVE }

    private MaterialEditPermission() {}

    public static boolean mayEdit(Level level, BlockPos pos, Player player,
                                  BlockHitResult hit, Operation operation) {
        if (!(level instanceof ServerLevel) || !player.mayBuild()
                || !level.hasChunkAt(pos) || !level.mayInteract(player, pos)) {
            return false;
        }
        // These are permission probes: the host block is never broken or placed.
        // Replacement requires both removal and placement permission.
        if (operation != Operation.APPLY) {
            var event = new BlockEvent.BreakEvent(level, pos, level.getBlockState(pos), player);
            MinecraftForge.EVENT_BUS.post(event);
            if (event.isCanceled()) return false;
        }
        if (operation != Operation.REMOVE) {
            var snapshot = BlockSnapshot.create(level.dimension(), level, pos);
            var against = level.getBlockState(pos.relative(hit.getDirection().getOpposite()));
            var event = new BlockEvent.EntityPlaceEvent(snapshot, against, player);
            MinecraftForge.EVENT_BUS.post(event);
            if (event.isCanceled()) return false;
        }
        return true;
    }
}
