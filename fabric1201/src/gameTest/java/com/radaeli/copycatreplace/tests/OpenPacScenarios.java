package com.radaeli.copycatreplace.tests;

import com.radaeli.copycatreplace.service.BulkMaterialReplacement;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.decoration.copycat.CopycatBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import xaero.pac.common.server.api.OpenPACServerAPI;
import xaero.pac.common.server.player.config.api.v2.PlayerConfigOptions;

import java.util.UUID;

import static com.radaeli.copycatreplace.tests.ReplacementScenarios.check;
import static com.radaeli.copycatreplace.tests.ReplacementScenarios.hit;

final class OpenPacScenarios {
    private OpenPacScenarios() {}

    static TestFunction test() {
        return new TestFunction("openpac", "copycat_replace.openpac_claim_boundary", "copycat_replace:empty",
                100, 0, true, helper -> {
                    var player = TestPlatform.player(helper, false);
                    verify(player, helper.absolutePos(ReplacementScenarios.ORIGIN), UUID.randomUUID());
                    helper.succeed();
                });
    }

    static void verify(ServerPlayer player, BlockPos anchor, UUID otherOwner) {
        var level = player.serverLevel();
        var dimension = level.dimension().location();
        var originChunk = new ChunkPos(anchor);
        var origin = new BlockPos(originChunk.getMaxBlockX(), anchor.getY(), anchor.getZ());
        var next = origin.east();
        var nextChunk = new ChunkPos(next);
        var api = OpenPACServerAPI.get(player.getServer());
        var claims = api.getServerClaimsManager();
        var protection = api.getChunkProtection();
        check(claims.get(dimension, originChunk) == null && claims.get(dimension, nextChunk) == null,
                "OpenPAC fixture needs two unclaimed chunks");
        var previousHand = player.getOffhandItem();
        var originState = level.getBlockState(origin);
        var nextState = level.getBlockState(next);
        try {
            check(claims.claim(dimension, player.getUUID(), 0, originChunk.x, originChunk.z, false) != null,
                    "Could not claim the origin chunk");
            var foreignClaim = claims.claim(dimension, otherOwner, 0, nextChunk.x, nextChunk.z, false);
            check(foreignClaim != null, "Could not claim the neighboring chunk");
            var config = protection.getConfig(foreignClaim);
            var protectedChunks = PlayerConfigOptions.PROTECT_CLAIMED_CHUNKS;
            var previousProtection = config.getRaw(protectedChunks);
            try {
                config.tryToSet(protectedChunks, true);
                check(config.getEffective(protectedChunks), "Foreign claim is not protected");
                level.setBlockAndUpdate(origin, AllBlocks.COPYCAT_STEP.getDefaultState());
                level.setBlockAndUpdate(next, AllBlocks.COPYCAT_STEP.getDefaultState());
                var material = new ItemStack(Items.STONE, 3);
                player.setItemInHand(InteractionHand.OFF_HAND, material);
                check(apply(player, next, material) == 0 && material.getCount() == 3,
                        "Denied origin consumed material");
                check(apply(player, origin, material) == 1 && material.getCount() == 2,
                        "Own claim must allow editing and stop at the neighboring protected claim");
                check(!((CopycatBlockEntity) level.getBlockEntity(next)).hasCustomMaterial(),
                        "Bulk edit crossed the OpenPAC claim boundary");

                config.tryToSet(protectedChunks, false);
                check(!config.getEffective(protectedChunks), "Could not allow access to the foreign claim");
                check(apply(player, next, material) == 1 && material.getCount() == 1,
                        "OpenPAC did not allow editing in a foreign claim with access");
                check(((CopycatBlockEntity) level.getBlockEntity(next)).getMaterial().is(Blocks.STONE),
                        "Allowed edit did not update the material");

                config.tryToSet(protectedChunks, true);
                var replacement = new ItemStack(Items.OAK_PLANKS, 1);
                player.setItemInHand(InteractionHand.OFF_HAND, replacement);
                int stoneBefore = player.getInventory().countItem(Items.STONE);
                var state = level.getBlockState(next);
                check(BulkMaterialReplacement.replaceConnected(level, next, state, hit(next), player,
                        InteractionHand.OFF_HAND, replacement) == 0, "Protected claim allowed replacement");
                check(BulkMaterialReplacement.removeConnected(level, next, state, hit(next), player) == 0,
                        "Protected claim allowed removal");
                check(replacement.getCount() == 1 && player.getInventory().countItem(Items.STONE) == stoneBefore,
                        "Protected claim changed stock or refunded material");
                check(((CopycatBlockEntity) level.getBlockEntity(next)).getMaterial().is(Blocks.STONE),
                        "Denied replacement or removal changed the material");
            } finally {
                if (previousProtection == null) config.tryToReset(protectedChunks);
                else config.tryToSet(protectedChunks, previousProtection);
            }
        } finally {
            claims.unclaim(dimension, originChunk.x, originChunk.z);
            claims.unclaim(dimension, nextChunk.x, nextChunk.z);
            player.setItemInHand(InteractionHand.OFF_HAND, previousHand);
            level.setBlockAndUpdate(origin, originState);
            level.setBlockAndUpdate(next, nextState);
        }
    }

    private static int apply(ServerPlayer player, BlockPos pos, ItemStack material) {
        var level = player.serverLevel();
        return BulkMaterialReplacement.applyConnected(level, pos, level.getBlockState(pos), hit(pos), player,
                InteractionHand.OFF_HAND, material);
    }
}
