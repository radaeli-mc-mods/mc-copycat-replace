package com.radaeli.copycatreplace.service;

import com.simibubi.create.content.decoration.copycat.CopycatBlock;
import com.simibubi.create.content.decoration.copycat.CopycatBlockEntity;
import com.simibubi.create.AllBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Bridges the material replacement operation to Create's native copycats. */
public final class CreateCopycatAdapter {
    private CreateCopycatAdapter() {
    }

    public static CopycatReplacementTarget find(Level level, BlockPos pos, BlockState state, BlockHitResult hit) {
        if (!(state.getBlock() instanceof CopycatBlock copycatBlock)) {
            return null;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof CopycatBlockEntity copycatBlockEntity)) {
            return null;
        }

        return (player, materialHand, materialStack) -> replace(
                copycatBlock,
                copycatBlockEntity,
                level,
                pos,
                state,
                hit,
                player,
                materialHand,
                materialStack
        );
    }

    public static CopycatBulkTarget findBulkTarget(
            Level level,
            BlockPos pos,
            BlockState state,
            BlockHitResult hit
    ) {
        if (!(state.getBlock() instanceof CopycatBlock copycatBlock)) {
            return null;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof CopycatBlockEntity copycatBlockEntity)) {
            return null;
        }

        return new CopycatBulkTarget() {
            @Override
            public Block copycatBlock() {
                return state.getBlock();
            }

            @Override
            public Block clickedMaterialBlock() {
                return copycatBlockEntity.hasCustomMaterial()
                        ? copycatBlockEntity.getMaterial().getBlock()
                        : null;
            }

            @Override
            public boolean hasCustomMaterial(BlockHitResult targetHit) {
                return copycatBlockEntity.hasCustomMaterial();
            }

            @Override
            public boolean acceptsMaterial(ItemStack stack, BlockHitResult targetHit) {
                return copycatBlock.getAcceptedBlockState(level, pos, stack, targetHit.getDirection()) != null;
            }

            @Override
            public boolean containsMaterial(Block materialBlock) {
                return copycatBlockEntity.hasCustomMaterial()
                        && copycatBlockEntity.getMaterial().getBlock() == materialBlock;
            }

            @Override
            public BlockState applyToEmpty(
                    Player player,
                    InteractionHand materialHand,
                    ItemStack materialStack,
                    BlockHitResult targetHit
            ) {
                if (hasCustomMaterial(targetHit) || materialStack.isEmpty()) {
                    return null;
                }
                return applyMaterial(
                        copycatBlock, copycatBlockEntity, level, pos, state, targetHit,
                        player, materialHand, materialStack
                );
            }

            @Override
            public BlockState replaceMatchingMaterial(
                    Block sourceMaterial,
                    Player player,
                    InteractionHand materialHand,
                    ItemStack materialStack,
                    BlockHitResult targetHit
            ) {
                if (!containsMaterial(sourceMaterial) || materialStack.isEmpty()) {
                    return null;
                }

                BlockState accepted = copycatBlock.getAcceptedBlockState(
                        level, pos, materialStack, targetHit.getDirection()
                );
                if (accepted == null) {
                    return null;
                }
                accepted = copycatBlock.prepareMaterial(
                        level, pos, state, player, materialHand, targetHit, accepted
                );
                if (accepted == null || accepted.equals(copycatBlockEntity.getMaterial())) {
                    return null;
                }

                ItemStack previousMaterialItem = copycatBlockEntity.getConsumedItem().copy();
                copycatBlockEntity.setConsumedItem(materialStack);
                copycatBlockEntity.setMaterial(accepted);
                updateInventory(player, materialHand, materialStack, previousMaterialItem);
                return accepted;
            }

            @Override
            public BlockState removeMatchingMaterial(Block sourceMaterial, Player player, BlockHitResult targetHit) {
                if (!containsMaterial(sourceMaterial)) {
                    return null;
                }

                BlockState removed = copycatBlockEntity.getMaterial();
                ItemStack refund = copycatBlockEntity.getConsumedItem().copy();
                copycatBlockEntity.setMaterial(AllBlocks.COPYCAT_BASE.getDefaultState());
                copycatBlockEntity.setConsumedItem(ItemStack.EMPTY);
                if (!player.isCreative() && !refund.isEmpty()) {
                    player.getInventory().placeItemBackInInventory(refund);
                }
                return removed;
            }
        };
    }

    private static BlockState applyMaterial(
            CopycatBlock copycatBlock,
            CopycatBlockEntity blockEntity,
            Level level,
            BlockPos pos,
            BlockState copycatState,
            BlockHitResult hit,
            Player player,
            InteractionHand materialHand,
            ItemStack materialStack
    ) {
        BlockState accepted = copycatBlock.getAcceptedBlockState(level, pos, materialStack, hit.getDirection());
        if (accepted == null) {
            return null;
        }
        accepted = copycatBlock.prepareMaterial(
                level, pos, copycatState, player, materialHand, hit, accepted
        );
        if (accepted == null || accepted.equals(blockEntity.getMaterial())) {
            return null;
        }

        blockEntity.setConsumedItem(materialStack);
        blockEntity.setMaterial(accepted);
        updateInventory(player, materialHand, materialStack, ItemStack.EMPTY);
        return accepted;
    }

    private static void replace(
            CopycatBlock copycatBlock,
            CopycatBlockEntity blockEntity,
            Level level,
            BlockPos pos,
            BlockState copycatState,
            BlockHitResult hit,
            Player player,
            InteractionHand materialHand,
            ItemStack materialStack
    ) {
        BlockState accepted = copycatBlock.getAcceptedBlockState(level, pos, materialStack, hit.getDirection());
        if (accepted == null) {
            return;
        }

        accepted = copycatBlock.prepareMaterial(
                level,
                pos,
                copycatState,
                player,
                materialHand,
                hit,
                accepted
        );
        if (accepted == null || accepted.equals(blockEntity.getMaterial())) {
            return;
        }

        ItemStack previousMaterialItem = blockEntity.getConsumedItem().copy();

        // setMaterial is the synchronizing operation in Create. Store the new
        // consumed item first so the same update contains the complete state.
        blockEntity.setConsumedItem(materialStack);
        blockEntity.setMaterial(accepted);

        updateInventory(player, materialHand, materialStack, previousMaterialItem);
        playPlaceSound(level, pos, accepted);
    }

    private static void updateInventory(
            Player player,
            InteractionHand materialHand,
            ItemStack materialStack,
            ItemStack previousMaterialItem
    ) {
        if (player.isCreative()) {
            return;
        }

        materialStack.shrink(1);
        if (materialStack.isEmpty()) {
            player.setItemInHand(materialHand, ItemStack.EMPTY);
        }
        if (!previousMaterialItem.isEmpty()) {
            player.getInventory().placeItemBackInInventory(previousMaterialItem);
        }
    }

    private static void playPlaceSound(Level level, BlockPos pos, BlockState material) {
        level.playSound(
                null,
                pos,
                material.getSoundType().getPlaceSound(),
                SoundSource.BLOCKS,
                1.0F,
                0.75F
        );
    }
}
