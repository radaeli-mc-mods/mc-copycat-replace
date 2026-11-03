package com.radaeli.copycatreplace.interaction;

import com.radaeli.copycatreplace.service.CopycatReplacementTarget;
import com.radaeli.copycatreplace.network.BulkReplaceMode;
import com.radaeli.copycatreplace.service.BulkMaterialReplacement;
import com.radaeli.copycatreplace.service.CopycatBulkTarget;
import com.radaeli.copycatreplace.service.MaterialReplacement;
import com.radaeli.copycatreplace.service.MaterialEditPermission;
import com.simibubi.create.content.equipment.wrench.WrenchItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Handles wrench plus off-hand material interactions before Create's wrench does. */
public final class CopycatReplaceHandler {
    private CopycatReplaceHandler() {
    }

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (player.isShiftKeyDown() || !player.mayBuild()) {
            return;
        }

        boolean bulkMode = BulkReplaceMode.isDown(player);
        HandSelection selection = selectHands(player);
        InteractionHand wrenchHand = bulkMode ? selectWrenchOnly(player) : null;
        boolean bulkRemove = wrenchHand != null;
        if (selection == null && !bulkRemove) {
            return;
        }
        if (selection != null && event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        if (bulkRemove && event.getHand() != wrenchHand) {
            return;
        }

        Level level = event.getLevel();
        BlockState state = level.getBlockState(event.getPos());
        CopycatBulkTarget bulkTarget = MaterialReplacement.findBulkTarget(
                level, event.getPos(), state, event.getHitVec()
        );
        CopycatReplacementTarget target = selection == null || bulkMode
                ? null
                : MaterialReplacement.findTarget(level, event.getPos(), state, event.getHitVec());
        if (bulkTarget == null || (selection != null && !bulkMode && target == null)) {
            return;
        }

        // Consume even invalid replacement attempts so the wrench cannot remove
        // or rotate the existing material as a side effect.
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide()));

        if (!level.isClientSide()) {
            if (bulkRemove) {
                int changed = BulkMaterialReplacement.removeConnected(
                        level, event.getPos(), state, event.getHitVec(), player
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
                                level, event.getPos(), state, event.getHitVec(), player,
                                selection.materialHand(), selection.materialStack()
                        )
                        : BulkMaterialReplacement.replaceConnectedResult(
                                level, event.getPos(), state, event.getHitVec(), player,
                                selection.materialHand(), selection.materialStack()
                        );
                int changed = result.changed();
                if (result.shouldReportInvalidMaterial()) {
                    reportInvalidMaterial(player);
                }
                if (changed > 0) {
                    String message = applying
                            ? "message.copycat_replace.bulk_applied"
                            : "message.copycat_replace.bulk_replaced";
                    player.displayClientMessage(Component.translatable(message, changed), true);
                }
            } else {
                var operation = bulkTarget.hasCustomMaterial(event.getHitVec())
                        ? MaterialEditPermission.Operation.REPLACE : MaterialEditPermission.Operation.APPLY;
                if (MaterialEditPermission.mayEdit(level, event.getPos(), player, event.getHitVec(), operation)) {
                    if (!bulkTarget.acceptsMaterial(selection.materialStack(), event.getHitVec())) {
                        reportInvalidMaterial(player);
                    } else {
                        target.replaceMaterial(player, selection.materialHand(), selection.materialStack());
                    }
                }
            }
        }
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
