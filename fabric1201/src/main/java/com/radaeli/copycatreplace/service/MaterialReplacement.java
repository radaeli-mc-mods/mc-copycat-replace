package com.radaeli.copycatreplace.service;

import com.radaeli.copycatreplace.compat.copycats.CopycatsPlusCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.fabricmc.loader.api.FabricLoader;

/** Locates the appropriate implementation without loading optional APIs early. */
public final class MaterialReplacement {
    private MaterialReplacement() {
    }

    public static CopycatReplacementTarget findTarget(
            Level level,
            BlockPos pos,
            BlockState state,
            BlockHitResult hit
    ) {
        CopycatReplacementTarget createTarget = CreateCopycatAdapter.find(level, pos, state, hit);
        if (createTarget != null) {
            return createTarget;
        }

        if (FabricLoader.getInstance().isModLoaded(CopycatsPlusCompat.MOD_ID)) {
            return CopycatsPlusCompat.find(level, pos, state, hit);
        }

        return null;
    }

    public static CopycatBulkTarget findBulkTarget(
            Level level,
            BlockPos pos,
            BlockState state,
            BlockHitResult hit
    ) {
        CopycatBulkTarget createTarget = CreateCopycatAdapter.findBulkTarget(level, pos, state, hit);
        if (createTarget != null) {
            return createTarget;
        }

        if (FabricLoader.getInstance().isModLoaded(CopycatsPlusCompat.MOD_ID)) {
            return CopycatsPlusCompat.findBulkTarget(level, pos, state, hit);
        }

        return null;
    }
}
