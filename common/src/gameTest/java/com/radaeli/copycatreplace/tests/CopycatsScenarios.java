package com.radaeli.copycatreplace.tests;

import com.copycatsplus.copycats.CCBlocks;
import com.copycatsplus.copycats.foundation.copycat.ICopycatBlock;
import com.copycatsplus.copycats.foundation.copycat.ICopycatBlockEntity;
import com.copycatsplus.copycats.foundation.copycat.multistate.*;
import com.radaeli.copycatreplace.service.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;
import static com.radaeli.copycatreplace.tests.ReplacementScenarios.*;

/** Only loaded after the platform confirms Copycats+ is installed. */
final class CopycatsScenarios {
    static List<TestFunction> generate() {
        return List.of(test("copycats_single", CopycatsScenarios::single),
                test("multipart_selection_connected", h -> selection(h, com.radaeli.copycatreplace.network.SelectionMode.CONNECTED)),
                test("multipart_selection_plane", h -> selection(h, com.radaeli.copycatreplace.network.SelectionMode.PLANE)),
                test("multipart_selection_all_types", h -> selection(h, com.radaeli.copycatreplace.network.SelectionMode.ALL_TYPES)),
                test("multipart_individual_tokens", CopycatsScenarios::individual),
                test("multipart_bulk_tokens", CopycatsScenarios::bulk),
                test("multipart_state_only", CopycatsScenarios::orientation),
                test("multipart_permission_veto", CopycatsScenarios::permission));
    }
    private static void selection(GameTestHelper h, com.radaeli.copycatreplace.network.SelectionMode mode) {
        Fixture a=fixture(h,ORIGIN,false), b=fixture(h,ORIGIN.east(),true);
        var different= nativeBlock(h,ORIGIN.east(2),Blocks.STONE.defaultBlockState());
        var p=player(h,false);var hand=equip(p,false,new ItemStack(Items.OAK_PLANKS));
        p.getInventory().setItem(9,new ItemStack(Items.OAK_PLANKS,2));
        com.radaeli.copycatreplace.network.BulkReplaceMode.setState(p,true,mode);
        try {
            int expected=mode==com.radaeli.copycatreplace.network.SelectionMode.ALL_TYPES?3:2;
            int changed=BulkMaterialReplacement.replaceConnected(h.getLevel(),a.pos,h.getLevel().getBlockState(a.pos),hit(a.pos),p,hand,p.getItemInHand(hand));
            check(changed==expected,"Multipart traversal or inventory continuation failed: " + changed);
            check(b.be.getMaterialItemStorage().getMaterialMap().values().stream().anyMatch(v -> v.is(Blocks.ANDESITE)),"All types changed unrelated multipart material");
            check(p.getInventory().countItem(Items.STONE)==expected,"Multipart shared token refund mismatch");
            check(BulkMaterialReplacement.removeConnected(h.getLevel(),a.pos,h.getLevel().getBlockState(a.pos),hit(a.pos),p)==expected,"Multipart removal selection mismatch");
            check(p.getInventory().countItem(Items.OAK_PLANKS)==3,"Multipart inventory lost or duplicated payment");
        } finally { com.radaeli.copycatreplace.network.BulkReplaceMode.setDown(p,false); }
    }
    private static void single(GameTestHelper h) {
        BlockPos pos = place(h, ORIGIN, CCBlocks.COPYCAT_BLOCK.getDefaultState(), null);
        ICopycatBlockEntity be = ((ICopycatBlock) h.getLevel().getBlockState(pos).getBlock()).getCopycatBlockEntity(h.getLevel(), pos);
        be.setConsumedItem(new ItemStack(Items.STONE)); be.setMaterial(Blocks.STONE.defaultBlockState());
        ServerPlayer p = player(h, false); equip(p, false, new ItemStack(Items.OAK_PLANKS, 2));
        TestPlatform.click(p, InteractionHand.MAIN_HAND, hit(pos));
        check(be.getMaterial().is(Blocks.OAK_PLANKS) && p.getInventory().countItem(Items.STONE) == 1, "Copycats single replacement failed");
    }
    private record Fixture(BlockPos pos, IMultiStateCopycatBlockEntity be, List<String> parts) {}
    private static Fixture fixture(GameTestHelper h, BlockPos relative, boolean mixed) {
        BlockState state = CCBlocks.COPYCAT_SLAB.getDefaultState().setValue(
                com.copycatsplus.copycats.content.copycat.slab.CopycatSlabBlock.SLAB_TYPE,
                net.minecraft.world.level.block.state.properties.SlabType.DOUBLE);
        BlockPos pos = place(h, relative, state, null);
        IMultiStateCopycatBlock block = (IMultiStateCopycatBlock) state.getBlock();
        IMultiStateCopycatBlockEntity be = block.getCopycatBlockEntity(h.getLevel(), pos);
        BlockState finalState = state;
        List<String> parts = be.getMaterialItemStorage().getAllProperties().stream().filter(p -> block.partExists(finalState, p)).sorted().toList();
        check(parts.size() >= 2, "Fixture needs a double multipart slab; got " + parts);
        for (String part : parts) { be.setConsumedItem(part, ItemStack.EMPTY); be.setMaterial(part, Blocks.STONE.defaultBlockState()); }
        be.setConsumedItem(parts.get(0), new ItemStack(Items.STONE));
        if (mixed) { be.setConsumedItem(parts.get(1), new ItemStack(Items.ANDESITE)); be.setMaterial(parts.get(1), Blocks.ANDESITE.defaultBlockState()); }
        return new Fixture(pos, be, parts);
    }
    private static void individual(GameTestHelper h) {
        Fixture f = fixture(h, ORIGIN, false);
        ServerPlayer p = player(h, false); equip(p, false, new ItemStack(Items.OAK_PLANKS, 3));
        MaterialReplacement.findTarget(h.getLevel(), f.pos, h.getLevel().getBlockState(f.pos), hit(f.pos))
                .replaceMaterial(p, InteractionHand.OFF_HAND, p.getOffhandItem());
        var storage = f.be.getMaterialItemStorage();
        check(storage.getMaterialMap().values().stream().filter(s -> s.is(Blocks.OAK_PLANKS)).count() == 1, "Individual action changed multiple parts");
        check(p.getInventory().countItem(Items.STONE) == 0, "Refunded a token still used by another part");
        check(storage.getAllConsumedItems().stream().filter(s -> s.is(Items.STONE)).count() == 1, "Previous token was lost");
        check(p.getOffhandItem().getCount() == 2, "New material not charged exactly once");
    }
    private static void bulk(GameTestHelper h) {
        Fixture a = fixture(h, ORIGIN, false), b = fixture(h, ORIGIN.east(), true);
        ServerPlayer p = player(h, false); equip(p, false, new ItemStack(Items.OAK_PLANKS, 4));
        BlockState changed = target(h, a.pos).replaceMatchingMaterial(Blocks.STONE, p, InteractionHand.OFF_HAND, p.getOffhandItem(), hit(a.pos));
        check(changed != null && p.getOffhandItem().getCount() == 3, "Repeated material was charged per part instead of per token");
        check(p.getInventory().countItem(Items.STONE) == 1, "Repeated material refund mismatch");
        target(h, b.pos).replaceMatchingMaterial(Blocks.STONE, p, InteractionHand.OFF_HAND, p.getOffhandItem(), hit(b.pos));
        check(b.be.getMaterialItemStorage().getMaterialMap().values().stream().anyMatch(s -> s.is(Blocks.ANDESITE)), "Bulk changed a different material");
        int beforeRemoval = p.getInventory().countItem(Items.OAK_PLANKS);
        target(h, a.pos).removeMatchingMaterial(Blocks.OAK_PLANKS, p, hit(a.pos));
        check(p.getInventory().countItem(Items.OAK_PLANKS) == beforeRemoval + 1, "Removal duplicated or lost a shared token");
        check(!a.be.getMaterialItemStorage().getMaterialMap().values().stream().anyMatch(s -> s.is(Blocks.OAK_PLANKS)), "Not all matching parts removed");
    }
    private static void permission(GameTestHelper h) {
        Fixture a = fixture(h, ORIGIN, false), b = fixture(h, ORIGIN.east(), true);
        ServerPlayer p = player(h, false);
        equip(p, false, new ItemStack(Items.OAK_PLANKS, 4));
        try (var protection = TestPlatform.protect(p, Set.of(b.pos), true, true)) {
            int replaced = BulkMaterialReplacement.replaceConnected(h.getLevel(), a.pos,
                    h.getLevel().getBlockState(a.pos), hit(a.pos), p, InteractionHand.OFF_HAND, p.getOffhandItem());
            check(replaced == 1 && p.getOffhandItem().getCount() == 3, "Denied multipart position consumed material");
            check(p.getInventory().countItem(Items.STONE) == 1, "Denied multipart position refunded a token");
            int removed = BulkMaterialReplacement.removeConnected(h.getLevel(), b.pos,
                    h.getLevel().getBlockState(b.pos), hit(b.pos), p);
            check(removed == 0, "Denied multipart origin was removed");
            var storage = b.be.getMaterialItemStorage();
            check(storage.getMaterialMap().values().stream().anyMatch(s -> s.is(Blocks.STONE))
                    && storage.getMaterialMap().values().stream().anyMatch(s -> s.is(Blocks.ANDESITE)), "Denied multipart materials changed");
            check(storage.getAllConsumedItems().stream().filter(s -> !s.isEmpty()).count() == 2,
                    "Denied multipart position lost its paid tokens");
        }
    }
    private static void orientation(GameTestHelper h) {
        Fixture f = fixture(h, ORIGIN, false);
        for (String part : f.parts) f.be.setMaterial(part, Blocks.OAK_LOG.defaultBlockState().setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS, net.minecraft.core.Direction.Axis.X));
        f.be.setConsumedItem(f.parts.get(0), new ItemStack(Items.OAK_LOG));
        ServerPlayer p = player(h, false); equip(p, false, new ItemStack(Items.OAK_LOG, 3));
        MaterialReplacement.findTarget(h.getLevel(), f.pos, h.getLevel().getBlockState(f.pos), hit(f.pos)).replaceMaterial(p, InteractionHand.OFF_HAND, p.getOffhandItem());
        check(p.getOffhandItem().getCount() == 3 && p.getInventory().countItem(Items.STONE) == 0, "Changing material orientation must not charge a token");
    }
}
