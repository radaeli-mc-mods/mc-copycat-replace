package com.radaeli.copycatreplace.tests;

import com.radaeli.copycatreplace.config.CopycatReplaceConfig;
import com.radaeli.copycatreplace.network.BulkReplaceMode;
import com.radaeli.copycatreplace.service.*;
import com.simibubi.create.AllBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import java.util.*;
import java.util.function.Consumer;

/** Identical acceptance scenarios compiled against each actual Minecraft/Create pair. */
public final class ReplacementScenarios {
    static final BlockPos ORIGIN = new BlockPos(2, 2, 2);
    public static List<TestFunction> generate() {
        List<TestFunction> tests = new ArrayList<>();
        for (boolean reversed : List.of(false, true)) {
            tests.add(test("single_" + reversed, h -> single(h, reversed, false)));
            tests.add(test("creative_single_" + reversed, h -> single(h, reversed, true)));
            tests.add(test("bulk_actions_" + reversed, h -> bulk(h, reversed)));
        }
        tests.add(test("invalid_material", h -> rejected(h, false, false)));
        tests.add(test("sneak_bypass", h -> rejected(h, true, false)));
        tests.add(test("adventure_bypass", h -> rejected(h, false, true)));
        tests.add(test("same_material_no_charge", ReplacementScenarios::unchanged));
        tests.add(test("face_type_material_boundaries", ReplacementScenarios::boundaries));
        tests.add(test("partial_stock", ReplacementScenarios::partial));
        tests.add(test("full_inventory_refund", ReplacementScenarios::fullInventory));
        tests.add(test("player_key_isolation", ReplacementScenarios::keyIsolation));
        for (int cap : List.of(1, 64, 256)) tests.add(test("limit_" + cap, h -> limit(h, cap)));
        tests.addAll(InventorySelectionScenarios.generate());
        tests.addAll(FeedbackScenarios.generate());
        tests.addAll(PermissionScenarios.generate());
        tests.addAll(MigrationScenarios.generate());
        tests.addAll(ServerConfigScenarios.generate());
        if (TestPlatform.hasCopycats()) tests.addAll(CopycatsScenarios.generate());
        return tests;
    }
    static TestFunction test(String name, Consumer<GameTestHelper> action) {
        return new TestFunction("replacement", "copycat_replace." + name, "copycat_replace:empty", 100, 0, true, h -> {
            action.accept(h);
            h.succeed();
        });
    }
    static ServerPlayer player(GameTestHelper h, boolean creative) {
        ServerPlayer p = TestPlatform.player(h, creative);
        p.getInventory().clearContent();
        p.getAbilities().mayBuild = true;
        p.setShiftKeyDown(false);
        return p;
    }
    static BlockPos place(GameTestHelper h, BlockPos relative, BlockState state, BlockState material) {
        BlockPos pos = h.absolutePos(relative);
        h.getLevel().setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
        h.getLevel().setBlockAndUpdate(pos, state);
        if (material != null) {
            var be = (com.simibubi.create.content.decoration.copycat.CopycatBlockEntity) h.getLevel().getBlockEntity(pos);
            be.setConsumedItem(new ItemStack(material.getBlock()));
            be.setMaterial(material);
        }
        return pos;
    }
    static BlockPos nativeBlock(GameTestHelper h, BlockPos relative, BlockState material) {
        return place(h, relative, AllBlocks.COPYCAT_STEP.getDefaultState(), material);
    }
    static BlockHitResult hit(BlockPos p) { return new BlockHitResult(Vec3.atLowerCornerOf(p).add(.5, .25, .5), Direction.UP, p, false); }
    static CopycatBulkTarget target(GameTestHelper h, BlockPos p) {
        return MaterialReplacement.findBulkTarget(h.getLevel(), p, h.getLevel().getBlockState(p), hit(p));
    }
    static InteractionHand equip(ServerPlayer p, boolean reversed, ItemStack material) {
        p.setItemInHand(reversed ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND, new ItemStack(com.simibubi.create.AllItems.WRENCH.get()));
        InteractionHand hand = reversed ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        p.setItemInHand(hand, material);
        return hand;
    }
    private static void single(GameTestHelper h, boolean reversed, boolean creative) {
        BlockPos pos = nativeBlock(h, ORIGIN, Blocks.STONE.defaultBlockState());
        ServerPlayer p = player(h, creative);
        InteractionHand hand = equip(p, reversed, new ItemStack(Items.OAK_PLANKS, 3));
        check(TestPlatform.click(p, InteractionHand.MAIN_HAND, hit(pos)), "Replacement event must be consumed");
        check(target(h, pos).containsMaterial(Blocks.OAK_PLANKS), "Selected material did not change");
        check(p.getItemInHand(hand).getCount() == (creative ? 3 : 2), "Wrong consumption");
        check(p.getInventory().countItem(Items.STONE) == (creative ? 0 : 1), "Wrong refund");
    }
    private static void rejected(GameTestHelper h, boolean sneak, boolean adventure) {
        BlockPos pos = nativeBlock(h, ORIGIN, Blocks.STONE.defaultBlockState());
        ServerPlayer p = player(h, false);
        equip(p, false, new ItemStack(sneak || adventure ? Items.OAK_PLANKS : Items.STICK, 3));
        p.setShiftKeyDown(sneak); p.getAbilities().mayBuild = !adventure;
        boolean canceled = TestPlatform.click(p, InteractionHand.MAIN_HAND, hit(pos));
        check(canceled == (!sneak && !adventure), "Wrong bypass/cancellation behavior");
        check(target(h, pos).containsMaterial(Blocks.STONE), "Rejected action changed material");
        check(p.getOffhandItem().getCount() == 3, "Rejected action consumed an item");
    }
    private static void unchanged(GameTestHelper h) {
        BlockPos pos = nativeBlock(h, ORIGIN, Blocks.STONE.defaultBlockState());
        ServerPlayer p = player(h, false); equip(p, false, new ItemStack(Items.STONE, 3));
        TestPlatform.click(p, InteractionHand.MAIN_HAND, hit(pos));
        check(p.getOffhandItem().getCount() == 3, "Same material charged an item");
    }
    private static void bulk(GameTestHelper h, boolean reversed) {
        BlockPos pos = nativeBlock(h, ORIGIN, null);
        for (int i = 1; i < 4; i++) nativeBlock(h, ORIGIN.east(i), null);
        ServerPlayer p = player(h, false); InteractionHand hand = equip(p, reversed, new ItemStack(Items.STONE, 4));
        BulkReplaceMode.setDown(p, true);
        try {
            TestPlatform.click(p, InteractionHand.MAIN_HAND, hit(pos));
            for (int i = 0; i < 4; i++) check(target(h, pos.east(i)).containsMaterial(Blocks.STONE), "Bulk apply skipped a position");
            check(p.getItemInHand(hand).isEmpty(), "Bulk apply did not consume four items");
            p.setItemInHand(hand, new ItemStack(Items.OAK_PLANKS, 4));
            TestPlatform.click(p, InteractionHand.MAIN_HAND, hit(pos));
            for (int i = 0; i < 4; i++) check(target(h, pos.east(i)).containsMaterial(Blocks.OAK_PLANKS), "Bulk replace skipped a position");
            check(p.getInventory().countItem(Items.STONE) == 4, "Bulk replace refund mismatch");
            TestPlatform.click(p, reversed ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND, hit(pos));
            for (int i = 0; i < 4; i++) check(!target(h, pos.east(i)).hasCustomMaterial(hit(pos.east(i))), "Bulk removal skipped a position");
            check(p.getInventory().countItem(Items.OAK_PLANKS) == 4, "Bulk remove refund mismatch");
        } finally { BulkReplaceMode.setDown(p, false); }
    }
    private static void boundaries(GameTestHelper h) {
        BlockPos pos = nativeBlock(h, ORIGIN, Blocks.STONE.defaultBlockState());
        nativeBlock(h, ORIGIN.east(), Blocks.STONE.defaultBlockState());
        BlockPos different = nativeBlock(h, ORIGIN.east(2), Blocks.ANDESITE.defaultBlockState());
        BlockPos beyond = nativeBlock(h, ORIGIN.east(3), Blocks.STONE.defaultBlockState());
        BlockPos diagonal = nativeBlock(h, ORIGIN.south().west(), Blocks.STONE.defaultBlockState());
        place(h, ORIGIN.west(), AllBlocks.COPYCAT_PANEL.getDefaultState(), Blocks.STONE.defaultBlockState());
        ServerPlayer p = player(h, false); InteractionHand hand = equip(p, false, new ItemStack(Items.OAK_PLANKS, 10));
        int n = BulkMaterialReplacement.replaceConnected(h.getLevel(), pos, h.getLevel().getBlockState(pos), hit(pos), p, hand, p.getItemInHand(hand));
        check(n == 2, "Traversal crossed a type/material/diagonal boundary: " + n);
        check(target(h, different).containsMaterial(Blocks.ANDESITE) && target(h, beyond).containsMaterial(Blocks.STONE) && target(h, diagonal).containsMaterial(Blocks.STONE), "Boundary material changed");
    }
    private static void partial(GameTestHelper h) {
        BlockPos pos = nativeBlock(h, ORIGIN, Blocks.STONE.defaultBlockState());
        for (int i = 1; i < 5; i++) nativeBlock(h, ORIGIN.east(i), Blocks.STONE.defaultBlockState());
        ServerPlayer p = player(h, false); InteractionHand hand = equip(p, false, new ItemStack(Items.OAK_PLANKS, 2));
        var preview = BulkMaterialReplacement.previewConnected(h.getLevel(), pos, h.getLevel().getBlockState(pos), hit(pos), p, p.getItemInHand(hand), false, 64);
        check(preview.count() == 2, "Preview ignored available stock");
        int n = BulkMaterialReplacement.replaceConnected(h.getLevel(), pos, h.getLevel().getBlockState(pos), hit(pos), p, hand, p.getItemInHand(hand));
        check(n == 2 && p.getInventory().countItem(Items.STONE) == 2, "Partial replacement or refund failed");
    }
    private static void fullInventory(GameTestHelper h) {
        BlockPos pos = nativeBlock(h, ORIGIN, Blocks.STONE.defaultBlockState());
        ServerPlayer p = player(h, false);
        for (int i = 0; i < 36; i++) p.getInventory().setItem(i, new ItemStack(Items.DIAMOND, 64));
        equip(p, false, new ItemStack(Items.OAK_PLANKS, 2));
        TestPlatform.click(p, InteractionHand.MAIN_HAND, hit(pos));
        var drops = h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, p.getBoundingBox().inflate(3));
        check(drops.stream().mapToInt(e -> e.getItem().is(Items.STONE) ? e.getItem().getCount() : 0).sum() == 1, "Full inventory must drop exactly one refund");
    }
    private static void keyIsolation(GameTestHelper h) {
        ServerPlayer a = player(h, false), b = player(h, false);
        BulkReplaceMode.setDown(a, true);
        try { check(BulkReplaceMode.isDown(a) && !BulkReplaceMode.isDown(b), "Key state leaked to another player"); }
        finally { BulkReplaceMode.setDown(a, false); }
        check(!BulkReplaceMode.isDown(a), "Released key remained active");
    }
    private static void limit(GameTestHelper h, int cap) {
        TestPlatform.prepareConfigMutation();
        int previous = CopycatReplaceConfig.MAX_CONNECTED_BLOCKS.get();
        try {
            CopycatReplaceConfig.MAX_CONNECTED_BLOCKS.set(cap);
            BlockPos pos = null;
            for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
                BlockPos p = nativeBlock(h, ORIGIN.offset(x, 0, z), Blocks.STONE.defaultBlockState());
                if (pos == null) pos = p;
            }
            ServerPlayer player = player(h, true); InteractionHand hand = equip(player, false, new ItemStack(Items.OAK_PLANKS));
            var estimate = BulkMaterialReplacement.previewConnected(h.getLevel(), pos, h.getLevel().getBlockState(pos), hit(pos), player, player.getItemInHand(hand), false, cap);
            check(estimate.count() == cap && estimate.positions().contains(pos), "Preview limit must include origin");
            int n = BulkMaterialReplacement.replaceConnected(h.getLevel(), pos, h.getLevel().getBlockState(pos), hit(pos), player, hand, player.getItemInHand(hand));
            check(n == cap, "Wrong configured limit: " + n);
            check(player.getItemInHand(hand).getCount() == 1 && player.getInventory().countItem(Items.STONE) == 0, "Creative bulk modified inventory");
        } finally {
            CopycatReplaceConfig.MAX_CONNECTED_BLOCKS.set(previous);
        }
    }
    static void check(boolean value, String message) { if (!value) throw new IllegalStateException(message); }
}
