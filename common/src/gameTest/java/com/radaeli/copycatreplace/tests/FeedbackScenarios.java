package com.radaeli.copycatreplace.tests;

import com.radaeli.copycatreplace.network.BulkReplaceMode;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import java.util.*;
import static com.radaeli.copycatreplace.tests.ReplacementScenarios.*;

/** Captures actual actionbar delivery at the server player boundary. */
final class FeedbackScenarios {
    static List<TestFunction> generate() {
        List<TestFunction> tests = new ArrayList<>();
        for (boolean reversed : List.of(false, true)) for (boolean bulk : List.of(false, true)) {
            tests.add(test("feedback_invalid_" + reversed + "_" + bulk, h -> attempt(h, reversed, bulk, false, false)));
            tests.add(test("feedback_denied_" + reversed + "_" + bulk, h -> attempt(h, reversed, bulk, true, false)));
            tests.add(test("feedback_same_" + reversed + "_" + bulk, h -> attempt(h, reversed, bulk, false, true)));
        }
        tests.add(test("feedback_apply_invalid", FeedbackScenarios::apply));
        tests.add(test("feedback_empty_stock", h -> stock(h, 0)));
        tests.add(test("feedback_partial_stock", h -> stock(h, 1)));
        return tests;
    }

    private static void attempt(GameTestHelper h, boolean reversed, boolean bulk, boolean denied, boolean same) {
        var pos = nativeBlock(h, ORIGIN, Blocks.STONE.defaultBlockState());
        nativeBlock(h, ORIGIN.east(), Blocks.STONE.defaultBlockState());
        List<Component> messages = new ArrayList<>();
        var p = TestPlatform.feedbackPlayer(h, messages);
        var hand = equip(p, reversed, new ItemStack(same ? Items.STONE : Items.STICK, 3));
        BulkReplaceMode.setDown(p, bulk);
        try (var protection = TestPlatform.protect(p, denied ? Set.of(pos) : Set.of(), true, true)) {
            check(TestPlatform.click(p, InteractionHand.MAIN_HAND, hit(pos)), "Rejected click did not suppress wrench");
            TestPlatform.click(p, InteractionHand.OFF_HAND, hit(pos));
            check(messages.size() == (denied || same ? 0 : 1), "Feedback missing, duplicated, or mislabeled");
            if (!messages.isEmpty()) check(messages.get(0).getContents() instanceof TranslatableContents text
                    && text.getKey().equals("message.copycat_replace.invalid_material"), "Wrong feedback translation");
            check(target(h, pos).containsMaterial(Blocks.STONE) && target(h, pos.east()).containsMaterial(Blocks.STONE), "Refusal changed the world");
            check(p.getItemInHand(hand).getCount() == 3 && p.getInventory().countItem(Items.STONE) == (same ? 3 : 0), "Refusal changed inventory");
        } finally { BulkReplaceMode.setDown(p, false); }
    }

    private static void apply(GameTestHelper h) {
        var pos = nativeBlock(h, ORIGIN, null);
        List<Component> messages = new ArrayList<>();
        var p = TestPlatform.feedbackPlayer(h, messages);
        var hand = equip(p, false, new ItemStack(Items.STICK, 3));
        BulkReplaceMode.setDown(p, true);
        try {
            check(TestPlatform.click(p, InteractionHand.MAIN_HAND, hit(pos)), "Invalid bulk apply was not consumed");
            check(messages.size() == 1 && !target(h, pos).hasCustomMaterial(hit(pos)), "Invalid bulk apply feedback/world mismatch");
            check(p.getItemInHand(hand).getCount() == 3, "Invalid apply charged material");
        } finally { BulkReplaceMode.setDown(p, false); }
    }

    private static void stock(GameTestHelper h, int count) {
        var pos = nativeBlock(h, ORIGIN, Blocks.STONE.defaultBlockState());
        nativeBlock(h, ORIGIN.east(), Blocks.STONE.defaultBlockState());
        var p = player(h, false);
        var hand = equip(p, false, new ItemStack(Items.OAK_PLANKS, count));
        var result = com.radaeli.copycatreplace.service.BulkMaterialReplacement.replaceConnectedResult(
                h.getLevel(), pos, h.getLevel().getBlockState(pos), hit(pos), p, hand, p.getItemInHand(hand));
        check(result.changed() == count && !result.shouldReportInvalidMaterial(), "Stock shortage was labeled invalid material");
    }
}
