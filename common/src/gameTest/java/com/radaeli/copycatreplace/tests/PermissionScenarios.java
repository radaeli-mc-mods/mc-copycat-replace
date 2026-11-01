package com.radaeli.copycatreplace.tests;

import com.radaeli.copycatreplace.config.CopycatReplaceConfig;
import com.radaeli.copycatreplace.network.BulkReplaceMode;
import com.radaeli.copycatreplace.service.BulkMaterialReplacement;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import java.util.*;
import static com.radaeli.copycatreplace.tests.ReplacementScenarios.*;

/** Real loader veto events; no claim mod or production permission bypass is needed. */
final class PermissionScenarios {
    enum Action { APPLY, REPLACE, REMOVE }

    static List<TestFunction> generate() {
        List<TestFunction> tests = new ArrayList<>();
        for (Action action : Action.values()) for (int blocked : List.of(0, 1, 3)) {
            tests.add(test("permission_" + action.name().toLowerCase(Locale.ROOT) + "_" + blocked,
                    h -> barrier(h, action, blocked)));
        }
        for (boolean reversed : List.of(false, true)) for (boolean denyBreak : List.of(false, true)) {
            tests.add(test("permission_single_" + reversed + "_" + denyBreak,
                    h -> single(h, reversed, denyBreak)));
        }
        tests.add(test("permission_replace_requires_place", h -> replacementVeto(h, false)));
        tests.add(test("permission_replace_requires_break", h -> replacementVeto(h, true)));
        tests.add(test("permission_alternate_path", PermissionScenarios::alternatePath));
        tests.add(test("permission_preview_read_only", PermissionScenarios::preview));
        tests.add(test("permission_bounded_256", PermissionScenarios::bounded));
        return tests;
    }

    private static void barrier(GameTestHelper h, Action action, int blocked) {
        BlockPos pos = null;
        for (int i = 0; i < 4; i++) {
            BlockPos placed = nativeBlock(h, ORIGIN.east(i), action == Action.APPLY ? null : Blocks.STONE.defaultBlockState());
            if (i == 0) pos = placed;
        }
        ServerPlayer p = player(h, false);
        InteractionHand hand = equip(p, false, action == Action.REMOVE ? ItemStack.EMPTY : new ItemStack(Items.OAK_PLANKS, 8));
        BulkReplaceMode.setDown(p, true);
        try (var protection = TestPlatform.protect(p, Set.of(pos.east(blocked)), true, true)) {
            check(TestPlatform.click(p, InteractionHand.MAIN_HAND, hit(pos)), "Denied bulk click must still suppress wrench");
            for (int i = 0; i < 4; i++) {
                var target = target(h, pos.east(i));
                boolean changed = i < blocked;
                if (action == Action.APPLY) check(target.containsMaterial(Blocks.OAK_PLANKS) == changed, "Apply crossed a denied position");
                if (action == Action.REPLACE) check(target.containsMaterial(changed ? Blocks.OAK_PLANKS : Blocks.STONE), "Replace crossed a denied position");
                if (action == Action.REMOVE) check(target.hasCustomMaterial(hit(pos.east(i))) != changed, "Remove crossed a denied position");
            }
            check(p.getItemInHand(hand).getCount() == (action == Action.REMOVE ? 0 : 8 - blocked), "Denied positions changed consumption");
            check(p.getInventory().countItem(Items.STONE) == (action == Action.APPLY ? 0 : blocked), "Denied positions refunded material");
            check(protection.checked.size() == blocked + 1, "Search propagated through a denied position");
        } finally { BulkReplaceMode.setDown(p, false); }
    }

    private static void single(GameTestHelper h, boolean reversed, boolean denyBreak) {
        BlockPos pos = nativeBlock(h, ORIGIN, Blocks.STONE.defaultBlockState());
        ServerPlayer p = player(h, false);
        InteractionHand hand = equip(p, reversed, new ItemStack(Items.OAK_PLANKS, 3));
        try (var protection = TestPlatform.protect(p, Set.of(pos), denyBreak, !denyBreak)) {
            check(TestPlatform.click(p, InteractionHand.MAIN_HAND, hit(pos)), "Denied single click must suppress wrench");
            check(target(h, pos).containsMaterial(Blocks.STONE), "Single action ignored permission veto");
            check(p.getItemInHand(hand).getCount() == 3 && p.getInventory().countItem(Items.STONE) == 0, "Denied single action moved items");
        }
    }

    private static void replacementVeto(GameTestHelper h, boolean denyBreak) {
        BlockPos pos = nativeBlock(h, ORIGIN, Blocks.STONE.defaultBlockState());
        ServerPlayer p = player(h, false);
        InteractionHand hand = equip(p, false, new ItemStack(Items.OAK_PLANKS, 3));
        try (var protection = TestPlatform.protect(p, Set.of(pos), denyBreak, !denyBreak)) {
            int n = BulkMaterialReplacement.replaceConnected(h.getLevel(), pos, h.getLevel().getBlockState(pos), hit(pos), p, hand, p.getItemInHand(hand));
            check(n == 0 && target(h, pos).containsMaterial(Blocks.STONE), "Replacement must require both permission probes");
            check(p.getItemInHand(hand).getCount() == 3 && p.getInventory().countItem(Items.STONE) == 0, "Denied replacement moved items");
        }
    }

    private static void alternatePath(GameTestHelper h) {
        BlockPos pos = nativeBlock(h, ORIGIN, null);
        for (BlockPos offset : List.of(ORIGIN.east(), ORIGIN.east(2), ORIGIN.south(), ORIGIN.east().south(), ORIGIN.east(2).south())) nativeBlock(h, offset, null);
        ServerPlayer p = player(h, false);
        InteractionHand hand = equip(p, false, new ItemStack(Items.OAK_PLANKS, 8));
        try (var protection = TestPlatform.protect(p, Set.of(pos.east()), true, true)) {
            int n = BulkMaterialReplacement.applyConnected(h.getLevel(), pos, h.getLevel().getBlockState(pos), hit(pos), p, hand, p.getItemInHand(hand));
            check(n == 5 && !target(h, pos.east()).hasCustomMaterial(hit(pos.east())), "Blocked node must remain intact with an alternate path");
            check(target(h, pos.east(2)).containsMaterial(Blocks.OAK_PLANKS), "Permitted alternate path was lost");
        }
    }

    private static void preview(GameTestHelper h) {
        BlockPos pos = nativeBlock(h, ORIGIN, null);
        ServerPlayer p = player(h, false);
        InteractionHand hand = equip(p, false, new ItemStack(Items.OAK_PLANKS, 3));
        try (var protection = TestPlatform.protect(p, Set.of(pos), true, true)) {
            var preview = BulkMaterialReplacement.previewConnected(h.getLevel(), pos, h.getLevel().getBlockState(pos), hit(pos), p, p.getItemInHand(hand), false, 64);
            check(preview.count() == 1 && protection.checked.isEmpty(), "Preview must not post permission events");
            check(!target(h, pos).hasCustomMaterial(hit(pos)), "Preview mutated the world");
        }
    }

    private static void bounded(GameTestHelper h) {
        TestPlatform.prepareConfigMutation();
        int previous = CopycatReplaceConfig.MAX_CONNECTED_BLOCKS.get();
        try {
            CopycatReplaceConfig.MAX_CONNECTED_BLOCKS.set(256);
            BlockPos pos = null;
            for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
                BlockPos placed = nativeBlock(h, ORIGIN.offset(x, 0, z), Blocks.STONE.defaultBlockState());
                if (pos == null) pos = placed;
            }
            ServerPlayer p = player(h, true);
            InteractionHand hand = equip(p, false, new ItemStack(Items.OAK_PLANKS));
            try (var protection = TestPlatform.protect(p, Set.of(), true, true)) {
                long start = System.nanoTime();
                int n = BulkMaterialReplacement.replaceConnected(h.getLevel(), pos, h.getLevel().getBlockState(pos), hit(pos), p, hand, p.getItemInHand(hand));
                long elapsed = System.nanoTime() - start;
                check(n == 256 && protection.breaks == 256 && protection.places == 256, "Permission probes must be bounded per eligible position");
                System.out.println("COPYCAT PERMISSION COST: 256 replacements, 512 veto events, " + elapsed / 1_000_000.0 + " ms (synthetic listener)");
            }
        } finally {
            CopycatReplaceConfig.MAX_CONNECTED_BLOCKS.set(previous);
        }
    }
}
