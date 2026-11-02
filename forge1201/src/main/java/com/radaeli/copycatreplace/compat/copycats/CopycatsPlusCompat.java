package com.radaeli.copycatreplace.compat.copycats;

import com.copycatsplus.copycats.foundation.copycat.ICopycatBlock;
import com.copycatsplus.copycats.foundation.copycat.ICopycatBlockEntity;
import com.copycatsplus.copycats.foundation.copycat.multistate.IMultiStateCopycatBlock;
import com.copycatsplus.copycats.foundation.copycat.multistate.IMultiStateCopycatBlockEntity;
import com.copycatsplus.copycats.foundation.copycat.multistate.MaterialItemStorage;
import com.simibubi.create.AllBlocks;
import com.radaeli.copycatreplace.service.CopycatBulkTarget;
import com.radaeli.copycatreplace.service.CopycatReplacementTarget;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.Comparator;

/** Optional integration. This class must only be loaded after checking mod presence. */
public final class CopycatsPlusCompat {
    public static final String MOD_ID = "copycats";

    private CopycatsPlusCompat() {
    }

    public static CopycatReplacementTarget find(Level level, BlockPos pos, BlockState state, BlockHitResult hit) {
        if (state.getBlock() instanceof IMultiStateCopycatBlock multiStateBlock) {
            IMultiStateCopycatBlockEntity blockEntity = multiStateBlock.getCopycatBlockEntity(level, pos);
            if (blockEntity == null) {
                return null;
            }

            String property = multiStateBlock.getPropertyFromInteraction(state, level, pos, hit, true);
            if (!multiStateBlock.partExists(state, property)
                    || !blockEntity.getMaterialItemStorage().getAllProperties().contains(property)) {
                return null;
            }

            return (player, materialHand, materialStack) -> {
                BlockState replaced = replaceMultiState(
                        multiStateBlock,
                        blockEntity,
                        property,
                        level,
                        pos,
                        state,
                        hit,
                        player,
                        materialHand,
                        materialStack
                );
                if (replaced != null) {
                    playPlaceSound(level, pos, replaced);
                }
            };
        }

        if (state.getBlock() instanceof ICopycatBlock copycatBlock) {
            ICopycatBlockEntity blockEntity = copycatBlock.getCopycatBlockEntity(level, pos);
            if (blockEntity == null) {
                return null;
            }

            return (player, materialHand, materialStack) -> {
                BlockState replaced = replaceSingleState(
                        copycatBlock,
                        blockEntity,
                        level,
                        pos,
                        state,
                        hit,
                        player,
                        materialHand,
                        materialStack
                );
                if (replaced != null) {
                    playPlaceSound(level, pos, replaced);
                }
            };
        }

        return null;
    }

    public static CopycatBulkTarget findBulkTarget(
            Level level,
            BlockPos pos,
            BlockState state,
            BlockHitResult hit
    ) {
        if (state.getBlock() instanceof IMultiStateCopycatBlock multiStateBlock) {
            IMultiStateCopycatBlockEntity blockEntity = multiStateBlock.getCopycatBlockEntity(level, pos);
            if (blockEntity == null) {
                return null;
            }

            Block clickedMaterialBlock = null;
            if (hit != null) {
                String property = multiStateBlock.getPropertyFromInteraction(state, level, pos, hit, true);
                if (!multiStateBlock.partExists(state, property)
                        || !blockEntity.getMaterialItemStorage().getAllProperties().contains(property)) {
                    return null;
                }
                clickedMaterialBlock = blockEntity.getMaterialItemStorage()
                        .getMaterialItem(property).material().getBlock();
                if (!blockEntity.getMaterialItemStorage().hasCustomMaterial(property)) {
                    clickedMaterialBlock = null;
                }
            }

            Block selectedMaterial = clickedMaterialBlock;
            return new CopycatBulkTarget() {
                @Override
                public Block copycatBlock() {
                    return state.getBlock();
                }

                @Override
                public Block clickedMaterialBlock() {
                    return selectedMaterial;
                }

                @Override
                public boolean hasCustomMaterial(BlockHitResult targetHit) {
                    if (targetHit == null) {
                        return false;
                    }
                    String property = multiStateBlock.getPropertyFromInteraction(
                            state, level, pos, targetHit, true
                    );
                    return multiStateBlock.partExists(state, property)
                            && blockEntity.getMaterialItemStorage().hasCustomMaterial(property);
                }

                @Override
                public boolean acceptsMaterial(ItemStack stack, BlockHitResult targetHit) {
                    String property = multiStateBlock.getPropertyFromInteraction(state, level, pos, targetHit, true);
                    return multiStateBlock.partExists(state, property)
                            && multiStateBlock.getAcceptedBlockState(property, level, pos, stack, targetHit.getDirection()) != null;
                }

                @Override
                public boolean acceptsReplacement(Block sourceMaterial, ItemStack stack, BlockHitResult targetHit) {
                    MaterialItemStorage storage = blockEntity.getMaterialItemStorage();
                    return storage.getAllProperties().stream().anyMatch(property ->
                            multiStateBlock.partExists(state, property) && storage.hasCustomMaterial(property)
                            && storage.getMaterialItem(property).material().getBlock() == sourceMaterial
                            && multiStateBlock.getAcceptedBlockState(property, level, pos, stack, targetHit.getDirection()) != null);
                }

                @Override
                public boolean containsMaterial(Block materialBlock) {
                    MaterialItemStorage storage = blockEntity.getMaterialItemStorage();
                    for (String property : storage.getAllProperties()) {
                        if (multiStateBlock.partExists(state, property)
                                && storage.hasCustomMaterial(property)
                                && storage.getMaterialItem(property).material().getBlock() == materialBlock) {
                            return true;
                        }
                    }
                    return false;
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

                    MaterialItemStorage storage = blockEntity.getMaterialItemStorage();
                    String property = multiStateBlock.getPropertyFromInteraction(
                            state, level, pos, targetHit, true
                    );
                    if (!multiStateBlock.partExists(state, property)
                            || storage.hasCustomMaterial(property)) {
                        return null;
                    }
                    return replaceMultiState(
                            multiStateBlock, blockEntity, property, level, pos, state,
                            targetHit, player, materialHand, materialStack
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
                    if (materialStack.isEmpty()) {
                        return null;
                    }

                    MaterialItemStorage storage = blockEntity.getMaterialItemStorage();
                    BlockState firstChanged = null;
                    for (String property : storage.getAllProperties().stream().sorted(Comparator.naturalOrder()).toList()) {
                        if (!multiStateBlock.partExists(state, property)
                                || storage.getMaterialItem(property).material().getBlock() != sourceMaterial) {
                            continue;
                        }
                        BlockState replaced = replaceMultiState(
                                multiStateBlock,
                                blockEntity,
                                property,
                                level,
                                pos,
                                state,
                                targetHit,
                                player,
                                materialHand,
                                materialStack
                        );
                        if (firstChanged == null && replaced != null) {
                            firstChanged = replaced;
                        }
                    }
                    return firstChanged;
                }

                @Override
                public BlockState removeMatchingMaterial(
                        Block sourceMaterial,
                        Player player,
                        BlockHitResult targetHit
                ) {
                    MaterialItemStorage storage = blockEntity.getMaterialItemStorage();
                    BlockState firstRemoved = null;
                    for (String property : storage.getAllProperties().stream()
                            .sorted(Comparator.naturalOrder()).toList()) {
                        if (!multiStateBlock.partExists(state, property)
                                || !storage.hasCustomMaterial(property)
                                || storage.getMaterialItem(property).material().getBlock() != sourceMaterial) {
                            continue;
                        }
                        BlockState removed = removeMultiState(
                                blockEntity, property, player
                        );
                        if (firstRemoved == null && removed != null) {
                            firstRemoved = removed;
                        }
                    }
                    return firstRemoved;
                }
            };
        }

        if (state.getBlock() instanceof ICopycatBlock copycatBlock) {
            ICopycatBlockEntity blockEntity = copycatBlock.getCopycatBlockEntity(level, pos);
            if (blockEntity == null) {
                return null;
            }

            return new CopycatBulkTarget() {
                @Override
                public Block copycatBlock() {
                    return state.getBlock();
                }

                @Override
                public Block clickedMaterialBlock() {
                    return blockEntity.hasCustomMaterial() ? blockEntity.getMaterial().getBlock() : null;
                }

                @Override
                public boolean hasCustomMaterial(BlockHitResult targetHit) {
                    return blockEntity.hasCustomMaterial();
                }

                @Override
                public boolean acceptsMaterial(ItemStack stack, BlockHitResult targetHit) {
                    return copycatBlock.getAcceptedBlockState(level, pos, stack, targetHit.getDirection()) != null;
                }

                @Override
                public boolean containsMaterial(Block materialBlock) {
                    return blockEntity.hasCustomMaterial()
                            && blockEntity.getMaterial().getBlock() == materialBlock;
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
                    return replaceSingleState(
                            copycatBlock, blockEntity, level, pos, state, targetHit,
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
                    return !materialStack.isEmpty() && containsMaterial(sourceMaterial)
                            ? replaceSingleState(
                                    copycatBlock,
                                    blockEntity,
                                    level,
                                    pos,
                                    state,
                                    targetHit,
                                    player,
                                    materialHand,
                                    materialStack
                            )
                            : null;
                }

                @Override
                public BlockState removeMatchingMaterial(
                        Block sourceMaterial,
                        Player player,
                        BlockHitResult targetHit
                ) {
                    if (!containsMaterial(sourceMaterial)) {
                        return null;
                    }
                    BlockState removed = blockEntity.getMaterial();
                    ItemStack refund = blockEntity.getConsumedItem().copy();
                    blockEntity.setMaterial(AllBlocks.COPYCAT_BASE.getDefaultState());
                    blockEntity.setConsumedItem(ItemStack.EMPTY);
                    if (!player.isCreative() && !refund.isEmpty()) {
                        player.getInventory().placeItemBackInInventory(refund);
                    }
                    return removed;
                }
            };
        }

        return null;
    }

    private static BlockState replaceSingleState(
            ICopycatBlock copycatBlock,
            ICopycatBlockEntity blockEntity,
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
                level,
                pos,
                copycatState,
                player,
                materialHand,
                hit,
                accepted
        );
        if (accepted == null || accepted.equals(blockEntity.getMaterial())) {
            return null;
        }

        ItemStack previousMaterialItem = blockEntity.getConsumedItem().copy();
        blockEntity.setMaterial(accepted);
        blockEntity.setConsumedItem(materialStack);

        updateInventory(player, materialHand, materialStack, previousMaterialItem, false);
        return accepted;
    }

    private static BlockState replaceMultiState(
            IMultiStateCopycatBlock copycatBlock,
            IMultiStateCopycatBlockEntity blockEntity,
            String property,
            Level level,
            BlockPos pos,
            BlockState copycatState,
            BlockHitResult hit,
            Player player,
            InteractionHand materialHand,
            ItemStack materialStack
    ) {
        BlockState accepted = copycatBlock.getAcceptedBlockState(
                property,
                level,
                pos,
                materialStack,
                hit.getDirection()
        );
        if (accepted == null) {
            return null;
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
        if (accepted == null) {
            return null;
        }

        MaterialItemStorage storage = blockEntity.getMaterialItemStorage();
        MaterialItemStorage.MaterialItem currentEntry = storage.getMaterialItem(property);
        BlockState previousMaterial = currentEntry.material();
        if (accepted.equals(previousMaterial)) {
            return null;
        }

        // Changing only the orientation/state of the same material keeps its
        // existing paid item token and does not touch the player's inventory.
        if (accepted.getBlock() == previousMaterial.getBlock()) {
            blockEntity.setMaterial(property, accepted);
            return accepted;
        }

        ItemStack previousMaterialItem = currentEntry.consumedItem().copy();
        ItemStack refund = transferPreviousToken(
                blockEntity,
                storage,
                property,
                previousMaterial,
                previousMaterialItem
        );
        boolean newMaterialAlreadyPaid = storage.getAllConsumedItems().stream()
                .anyMatch(stack -> stack.getItem() == materialStack.getItem());

        blockEntity.setMaterial(property, accepted);
        blockEntity.setConsumedItem(
                property,
                newMaterialAlreadyPaid ? ItemStack.EMPTY : materialStack
        );

        updateInventory(player, materialHand, materialStack, refund, newMaterialAlreadyPaid);
        return accepted;
    }

    private static BlockState removeMultiState(
            IMultiStateCopycatBlockEntity blockEntity,
            String property,
            Player player
    ) {
        MaterialItemStorage storage = blockEntity.getMaterialItemStorage();
        MaterialItemStorage.MaterialItem current = storage.getMaterialItem(property);
        BlockState removed = current.material();
        ItemStack refund = current.consumedItem().copy();

        if (!refund.isEmpty()) {
            for (String otherProperty : storage.getAllProperties()) {
                if (otherProperty.equals(property)) {
                    continue;
                }
                MaterialItemStorage.MaterialItem other = storage.getMaterialItem(otherProperty);
                if (other.material().getBlock() == removed.getBlock() && other.consumedItem().isEmpty()) {
                    blockEntity.setConsumedItem(otherProperty, refund);
                    refund = ItemStack.EMPTY;
                    break;
                }
            }
        }

        blockEntity.setMaterial(property, AllBlocks.COPYCAT_BASE.getDefaultState());
        blockEntity.setConsumedItem(property, ItemStack.EMPTY);
        if (!player.isCreative() && !refund.isEmpty()) {
            player.getInventory().placeItemBackInInventory(refund);
        }
        return removed;
    }

    private static ItemStack transferPreviousToken(
            IMultiStateCopycatBlockEntity blockEntity,
            MaterialItemStorage storage,
            String replacedProperty,
            BlockState previousMaterial,
            ItemStack previousMaterialItem
    ) {
        if (previousMaterialItem.isEmpty()) {
            return ItemStack.EMPTY;
        }

        for (String otherProperty : storage.getAllProperties()) {
            if (otherProperty.equals(replacedProperty)) {
                continue;
            }

            MaterialItemStorage.MaterialItem otherEntry = storage.getMaterialItem(otherProperty);
            if (otherEntry.material().getBlock() == previousMaterial.getBlock()
                    && otherEntry.consumedItem().isEmpty()) {
                blockEntity.setConsumedItem(otherProperty, previousMaterialItem);
                return ItemStack.EMPTY;
            }
        }

        return previousMaterialItem;
    }

    private static void updateInventory(
            Player player,
            InteractionHand materialHand,
            ItemStack materialStack,
            ItemStack previousMaterialItem,
            boolean newMaterialAlreadyPaid
    ) {
        if (player.isCreative()) {
            return;
        }

        if (!newMaterialAlreadyPaid) {
            materialStack.shrink(1);
            if (materialStack.isEmpty()) {
                player.setItemInHand(materialHand, ItemStack.EMPTY);
            }
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
