package com.radaeli.copycatreplace.api;

import com.radaeli.copycatreplace.service.MaterialEditPermission.Operation;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;

/** Server-side permission query. Return false to veto an edit at this position. */
@FunctionalInterface
public interface MaterialEditPermissionCallback {
    Event<MaterialEditPermissionCallback> EVENT = EventFactory.createArrayBacked(
            MaterialEditPermissionCallback.class, listeners -> (player, level, pos, hit, operation) -> {
                for (var listener : listeners) {
                    if (!listener.mayEdit(player, level, pos, hit, operation)) return false;
                }
                return true;
            });

    boolean mayEdit(ServerPlayer player, ServerLevel level, BlockPos pos, BlockHitResult hit, Operation operation);
}
