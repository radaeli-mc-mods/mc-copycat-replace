package com.radaeli.copycatreplace.service;

import com.radaeli.copycatreplace.CopycatReplace;
import com.radaeli.copycatreplace.api.MaterialEditPermissionCallback;
import com.radaeli.copycatreplace.compat.openpac.OpenPacProtection;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

public final class MaterialEditPermission {
    public enum Operation { APPLY, REPLACE, REMOVE }

    private static final boolean HAS_OPEN_PAC = FabricLoader.getInstance().isModLoaded("openpartiesandclaims");
    private static boolean loggedOpenPacFailure;

    private MaterialEditPermission() {}

    public static boolean mayEdit(Level level, BlockPos pos, Player player, BlockHitResult hit, Operation operation) {
        if (!(level instanceof ServerLevel world) || !(player instanceof ServerPlayer serverPlayer)
                || !player.mayBuild() || !world.hasChunkAt(pos) || !world.getWorldBorder().isWithinBounds(pos)
                || !world.mayInteract(player, pos)) {
            return false;
        }
        if (HAS_OPEN_PAC) {
            try {
                if (!OpenPacProtection.mayEdit(serverPlayer, world, pos, hit, operation)) return false;
            } catch (LinkageError | RuntimeException e) {
                if (!loggedOpenPacFailure) {
                    CopycatReplace.LOGGER.error("OpenPAC protection query failed; denying Copycat edits", e);
                    loggedOpenPacFailure = true;
                }
                return false;
            }
        }
        return MaterialEditPermissionCallback.EVENT.invoker().mayEdit(serverPlayer, world, pos, hit, operation);
    }
}
