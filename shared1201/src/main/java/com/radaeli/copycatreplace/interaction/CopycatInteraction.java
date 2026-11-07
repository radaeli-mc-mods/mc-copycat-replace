package com.radaeli.copycatreplace.interaction;

import com.radaeli.copycatreplace.service.CopycatReplacementTarget;
import com.radaeli.copycatreplace.service.BulkMaterialReplacement;
import com.radaeli.copycatreplace.service.CopycatBulkTarget;
import com.radaeli.copycatreplace.service.MaterialReplacement;
import com.radaeli.copycatreplace.service.MaterialEditPermission;
import com.simibubi.create.content.equipment.wrench.WrenchItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Handles wrench plus off-hand material interactions before Create's wrench does. */
public final class CopycatInteraction {
    private CopycatInteraction() {
    }

    /** Returns the server action when the loader should consume this click, even if the edit is later denied. */
    public static Runnable find(Player player, Level level, InteractionHand hand, BlockHitResult hit, boolean bulkMode) {
        if (player.isShiftKeyDown() || !player.mayBuild()) {
            return null;
        }

        HandSelection selection = selectHands(player);
        InteractionHand wrenchHand = bulkMode ? selectWrenchOnly(player) : null;
        boolean bulkRemove = wrenchHand != null;
        if (selection == null && !bulkRemove) {
            return null;
        }
        if (selection != null && hand != InteractionHand.MAIN_HAND) {
            return null;
        }
        if (bulkRemove && hand != wrenchHand) {
            return null;
        }

        BlockPos pos = hit.getBlockPos();
        BlockState state = level.getBlockState(pos);
        CopycatBulkTarget bulkTarget = MaterialReplacement.findBulkTarget(level, pos, state, hit);
        CopycatReplacementTarget target = selection == null || bulkMode
                ? null
                : MaterialReplacement.findTarget(level, pos, state, hit);
        if (bulkTarget == null || (selection != null && !bulkMode && target == null)) {
            return null;
        }

        return () -> {
            if (bulkRemove) {
                int changed = BulkMaterialReplacement.removeConnected(
                        level, pos, state, hit, player
                );
                if (changed > 0) {
                    player.displayClientMessage(
                            Component.translatable("message.copycat_replace.bulk_removed", changed),
                            true
                    );
                }
            } else if (bulkMode) {
                boolean applying = bulkTarget.clickedMaterialBlock() == null;
                var result = applying
                        ? BulkMaterialReplacement.applyConnectedResult(
                                level, pos, state, hit, player,
                                selection.materialHand(), selection.materialStack()
                        )
                        : BulkMaterialReplacement.replaceConnectedResult(
                                level, pos, state, hit, player,
                                selection.materialHand(), selection.materialStack()
                        );
                int changed = result.changed();
                if (result.shouldReportInvalidMaterial()) reportInvalidMaterial(player);
                if (changed > 0) {
                    String message = applying
                            ? "message.copycat_replace.bulk_applied"
                            : "message.copycat_replace.bulk_replaced";
                    player.displayClientMessage(Component.translatable(message, changed), true);
                }
            } else {
                var operation = bulkTarget.hasCustomMaterial(hit)
                        ? MaterialEditPermission.Operation.REPLACE : MaterialEditPermission.Operation.APPLY;
                if (MaterialEditPermission.mayEdit(level, pos, player, hit, operation)) {
                    if (!bulkTarget.acceptsMaterial(selection.materialStack(), hit)) {
                        reportInvalidMaterial(player);
                    } else {
                        target.replaceMaterial(player, selection.materialHand(), selection.materialStack());
                    }
                }
            }
        };
    }

    private static void reportInvalidMaterial(Player player) {
        player.displayClientMessage(Component.translatable("message.copycat_replace.invalid_material"), true);
    }

    private static HandSelection selectHands(Player player) {
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        boolean wrenchInMainHand = mainHand.getItem() instanceof WrenchItem;
        boolean wrenchInOffHand = offHand.getItem() instanceof WrenchItem;

        if (wrenchInMainHand && !wrenchInOffHand && !offHand.isEmpty()) {
            return new HandSelection(InteractionHand.OFF_HAND, offHand);
        }
        if (wrenchInOffHand && !wrenchInMainHand && !mainHand.isEmpty()) {
            return new HandSelection(InteractionHand.MAIN_HAND, mainHand);
        }
        return null;
    }

    private static InteractionHand selectWrenchOnly(Player player) {
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        boolean wrenchInMainHand = mainHand.getItem() instanceof WrenchItem;
        boolean wrenchInOffHand = offHand.getItem() instanceof WrenchItem;

        if (wrenchInMainHand && !wrenchInOffHand && offHand.isEmpty()) {
            return InteractionHand.MAIN_HAND;
        }
        if (wrenchInOffHand && !wrenchInMainHand && mainHand.isEmpty()) {
            return InteractionHand.OFF_HAND;
        }
        return null;
    }

    private record HandSelection(InteractionHand materialHand, ItemStack materialStack) {
    }
}
