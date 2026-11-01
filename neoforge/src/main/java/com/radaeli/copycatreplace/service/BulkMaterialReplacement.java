package com.radaeli.copycatreplace.service;

import com.radaeli.copycatreplace.config.CopycatReplaceConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/** Finds and applies a bounded face-connected group of matching copycats. */
public final class BulkMaterialReplacement {
    private BulkMaterialReplacement() {
    }

    public static int applyConnected(
            Level level,
            BlockPos origin,
            BlockState originState,
            BlockHitResult originHit,
            Player player,
            InteractionHand materialHand,
            ItemStack materialStack
    ) {
        CopycatBulkTarget originTarget = findOrigin(level, origin, originState, originHit);
        if (originTarget == null || originTarget.hasCustomMaterial(originHit)) {
            return 0;
        }

        List<PositionTarget> cluster = findCluster(
                level, origin, originHit, originTarget,
                entry -> !entry.target().hasCustomMaterial(
                        translateHit(origin, entry.pos(), originHit)
                ),
                CopycatReplaceConfig.MAX_CONNECTED_BLOCKS.get(),
                pos -> MaterialEditPermission.mayEdit(level, pos, player,
                        translateHit(origin, pos, originHit), MaterialEditPermission.Operation.APPLY)
        );
        int changed = 0;
        BlockState soundMaterial = null;
        for (PositionTarget entry : cluster) {
            if (materialStack.isEmpty()) {
                break;
            }
            BlockHitResult targetHit = translateHit(origin, entry.pos(), originHit);
            BlockState applied = entry.target().applyToEmpty(
                    player, materialHand, materialStack, targetHit
            );
            if (applied != null) {
                changed++;
                if (soundMaterial == null) {
                    soundMaterial = applied;
                }
            }
        }
        playPlaceSound(level, origin, soundMaterial);
        return changed;
    }

    public static int replaceConnected(
            Level level,
            BlockPos origin,
            BlockState originState,
            BlockHitResult originHit,
            Player player,
            InteractionHand materialHand,
            ItemStack materialStack
    ) {
        CopycatBulkTarget originTarget = findOrigin(level, origin, originState, originHit);
        if (originTarget == null) {
            return 0;
        }
        Block sourceMaterial = originTarget.clickedMaterialBlock();
        if (sourceMaterial == null) {
            return 0;
        }

        List<PositionTarget> cluster = findCluster(
                level, origin, originHit, originTarget,
                entry -> entry.target().containsMaterial(sourceMaterial),
                CopycatReplaceConfig.MAX_CONNECTED_BLOCKS.get(),
                pos -> MaterialEditPermission.mayEdit(level, pos, player,
                        translateHit(origin, pos, originHit), MaterialEditPermission.Operation.REPLACE)
        );
        int changed = 0;
        BlockState soundMaterial = null;
        for (PositionTarget entry : cluster) {
            if (materialStack.isEmpty()) {
                break;
            }
            BlockState replaced = entry.target().replaceMatchingMaterial(
                    sourceMaterial,
                    player,
                    materialHand,
                    materialStack,
                    translateHit(origin, entry.pos(), originHit)
            );
            if (replaced != null) {
                changed++;
                if (soundMaterial == null) {
                    soundMaterial = replaced;
                }
            }
        }
        playPlaceSound(level, origin, soundMaterial);
        return changed;
    }

    public static int removeConnected(
            Level level,
            BlockPos origin,
            BlockState originState,
            BlockHitResult originHit,
            Player player
    ) {
        CopycatBulkTarget originTarget = findOrigin(level, origin, originState, originHit);
        if (originTarget == null) {
            return 0;
        }
        Block sourceMaterial = originTarget.clickedMaterialBlock();
        if (sourceMaterial == null) {
            return 0;
        }

        List<PositionTarget> cluster = findCluster(
                level, origin, originHit, originTarget,
                entry -> entry.target().containsMaterial(sourceMaterial),
                CopycatReplaceConfig.MAX_CONNECTED_BLOCKS.get(),
                pos -> MaterialEditPermission.mayEdit(level, pos, player,
                        translateHit(origin, pos, originHit), MaterialEditPermission.Operation.REMOVE)
        );
        int changed = 0;
        BlockState removedMaterial = null;
        for (PositionTarget entry : cluster) {
            BlockState removed = entry.target().removeMatchingMaterial(
                    sourceMaterial, player, translateHit(origin, entry.pos(), originHit)
            );
            if (removed != null) {
                changed++;
                if (removedMaterial == null) {
                    removedMaterial = removed;
                }
            }
        }
        if (removedMaterial != null) {
            level.levelEvent(2001, origin, Block.getId(removedMaterial));
        }
        return changed;
    }

    private static CopycatBulkTarget findOrigin(
            Level level,
            BlockPos origin,
            BlockState state,
            BlockHitResult hit
    ) {
        return MaterialReplacement.findBulkTarget(level, origin, state, hit);
    }

    /** Read-only, bounded estimate for the client HUD. */
    public static PreviewEstimate previewConnected(
            Level level,
            BlockPos origin,
            BlockState originState,
            BlockHitResult originHit,
            Player player,
            ItemStack materialStack,
            boolean removing,
            int configuredMaximum
    ) {
        CopycatBulkTarget originTarget = findOrigin(level, origin, originState, originHit);
        if (originTarget == null) {
            return new PreviewEstimate(0, List.of());
        }

        Block sourceMaterial = originTarget.clickedMaterialBlock();
        if (removing && sourceMaterial == null) {
            return new PreviewEstimate(0, List.of());
        }

        boolean applying = !removing && sourceMaterial == null;
        int previewMaximum = Math.min(configuredMaximum, PREVIEW_SCAN_LIMIT);
        Predicate<PositionTarget> isEligible = applying
                ? entry -> !entry.target().hasCustomMaterial(translateHit(origin, entry.pos(), originHit))
                : entry -> entry.target().containsMaterial(sourceMaterial);
        List<PositionTarget> cluster = findCluster(
                level, origin, originHit, originTarget, isEligible, previewMaximum, pos -> true
        );

        int count = cluster.size();
        if (!removing && !player.isCreative()) {
            count = Math.min(count, materialStack.getCount());
        }
        List<BlockPos> highlightedPositions = cluster.stream()
                .limit(count)
                .map(entry -> entry.pos().immutable())
                .toList();
        return new PreviewEstimate(
                count,
                highlightedPositions
        );
    }

    private static final int PREVIEW_SCAN_LIMIT = CopycatReplaceConfig.MAX_CONNECTED_BLOCKS_LIMIT;

    private static List<PositionTarget> findCluster(
            Level level,
            BlockPos origin,
            BlockHitResult originHit,
            CopycatBulkTarget originTarget,
            Predicate<PositionTarget> isEligible,
            int maximum,
            Predicate<BlockPos> mayVisit
    ) {
        List<PositionTarget> result = new ArrayList<>(Math.min(maximum, 64));
        ArrayDeque<BlockPos> pending = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        BlockPos immutableOrigin = origin.immutable();
        pending.add(immutableOrigin);
        visited.add(immutableOrigin);
        Block copycatBlock = originTarget.copycatBlock();

        while (!pending.isEmpty() && result.size() < maximum) {
            BlockPos pos = pending.removeFirst();
            CopycatBulkTarget target;
            if (pos.equals(immutableOrigin)) {
                target = originTarget;
            } else {
                if (!level.hasChunkAt(pos)) {
                    continue;
                }
                BlockState state = level.getBlockState(pos);
                target = MaterialReplacement.findBulkTarget(level, pos, state, null);
            }

            PositionTarget candidate = target == null ? null : new PositionTarget(pos, target);
            if (candidate == null || target.copycatBlock() != copycatBlock || !isEligible.test(candidate)
                    || !mayVisit.test(pos)) {
                continue;
            }

            result.add(candidate);
            for (Direction direction : Direction.values()) {
                BlockPos neighbor = pos.relative(direction).immutable();
                if (visited.add(neighbor)) {
                    pending.addLast(neighbor);
                }
            }
        }

        return result;
    }

    private static BlockHitResult translateHit(BlockPos origin, BlockPos target, BlockHitResult hit) {
        Vec3 relative = hit.getLocation().subtract(origin.getX(), origin.getY(), origin.getZ());
        Vec3 translated = relative.add(target.getX(), target.getY(), target.getZ());
        return new BlockHitResult(translated, hit.getDirection(), target, hit.isInside());
    }

    private static void playPlaceSound(Level level, BlockPos origin, BlockState material) {
        if (material != null) {
            level.playSound(
                    null,
                    origin,
                    material.getSoundType().getPlaceSound(),
                    SoundSource.BLOCKS,
                    1.0F,
                    0.75F
            );
        }
    }

    private record PositionTarget(BlockPos pos, CopycatBulkTarget target) {
    }

    public record PreviewEstimate(int count, List<BlockPos> positions) {
        public PreviewEstimate {
            positions = List.copyOf(positions);
        }
    }
}
