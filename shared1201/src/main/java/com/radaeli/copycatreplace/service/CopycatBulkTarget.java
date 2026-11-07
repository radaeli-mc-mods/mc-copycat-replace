package com.radaeli.copycatreplace.service;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** A Copycat position that can apply, replace, or remove a material in bulk. */
public interface CopycatBulkTarget {
    Block copycatBlock();

    /** Material selected by the original click; null for a neighbor candidate. */
    Block clickedMaterialBlock();

    /** Whether the interacted part (or the single-state block) has a custom material. */
    boolean hasCustomMaterial(BlockHitResult hit);

    boolean containsMaterial(Block materialBlock);

    /** Read-only acceptance check; call only after permission was granted. */
    boolean acceptsMaterial(ItemStack materialStack, BlockHitResult hit);

    /** Multipart replacement may affect a matching part other than the one under the hit. */
    default boolean acceptsReplacement(Block sourceMaterial, ItemStack materialStack, BlockHitResult hit) {
        return acceptsMaterial(materialStack, hit);
    }

    /** Applies the supplied material to this position's selected empty target. */
    BlockState applyToEmpty(
            Player player,
            InteractionHand materialHand,
            ItemStack materialStack,
            BlockHitResult hit
    );

    /** @return the first replacement state, or null when nothing changed. */
    BlockState replaceMatchingMaterial(
            Block sourceMaterial,
            Player player,
            InteractionHand materialHand,
            ItemStack materialStack,
            BlockHitResult hit
    );

    /** @return the first removed material state, or null when nothing changed. */
    BlockState removeMatchingMaterial(Block sourceMaterial, Player player, BlockHitResult hit);
}
