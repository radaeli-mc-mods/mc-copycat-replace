package com.radaeli.copycatreplace.tests;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.decoration.copycat.CopycatBlockEntity;
import net.createmod.ponder.foundation.PonderIndex;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.ui.PonderUI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Blocks;
import java.util.List;

/** Real client compilation, structure loading and playback, without optional APIs. */
final class PonderValidation {
    static PonderScene compile() {
        PonderScene tutorial = null;
        for (var item : List.of(AllBlocks.COPYCAT_STEP.asItem(), AllBlocks.COPYCAT_PANEL.asItem())) {
            var scenes = PonderIndex.getSceneAccess().compile(BuiltInRegistries.ITEM.getKey(item));
            var own = scenes.stream().filter(s -> s.getId().getNamespace().equals("copycat_replace")).toList();
            check(own.size() == 1, "Expected one addon tutorial on " + item);
            tutorial = own.get(0);
            check(tutorial.getTitle().equals(I18n.get("copycat_replace.ponder.material_actions.header")), "Missing Ponder title");
            check(tutorial.getTotalTime() > 900, "Incomplete Ponder timeline");
            for (int x = 1; x <= 3; x++) check(tutorial.getWorld().getBlockEntity(new BlockPos(x, 1, 2))
                    instanceof CopycatBlockEntity, "Ponder structure lacks Copycat at " + x);
        }
        for (int i = 1; i <= 9; i++) check(I18n.exists("copycat_replace.ponder.material_actions.text_" + i), "Missing Ponder text " + i);
        var bulk = java.util.Arrays.stream(Minecraft.getInstance().options.keyMappings)
                .filter(k -> k.getName().equals("key.copycat_replace.bulk_replace")).findFirst().orElseThrow();
        String text = PonderIndex.getLangAccess().getSpecific(tutorial.getId(), "text_4", bulk.getTranslatedKeyMessage().getString());
        check(text.contains(bulk.getTranslatedKeyMessage().getString()) && !text.contains("%s"), "Remapped Ponder key was not substituted");
        tutorial.begin();
        boolean gold = false, oak = false, empty = false;
        for (int i = 0; i < tutorial.getTotalTime() + 5; i++) {
            tutorial.tick();
            gold |= count(tutorial, Blocks.GOLD_BLOCK) == 3;
            oak |= count(tutorial, Blocks.OAK_PLANKS) == 3;
            empty |= count(tutorial, AllBlocks.COPYCAT_BASE.get()) == 3;
        }
        check(gold && oak && empty, "Ponder did not demonstrate apply, replace and removal");
        check(count(tutorial, Blocks.STONE) == 2, "Ponder limit example is incorrect");
        tutorial.reset();
        return tutorial;
    }

    private static int count(PonderScene scene, net.minecraft.world.level.block.Block material) {
        int count = 0;
        for (int x = 1; x <= 3; x++) {
            var entity = scene.getWorld().getBlockEntity(new BlockPos(x, 1, 2));
            // Sneak deliberately dismantles the first Copycat before the bulk demonstration.
            if (entity == null && scene.getWorld().getBlockState(new BlockPos(x, 1, 2)).isAir()) continue;
            check(entity instanceof CopycatBlockEntity, "Ponder structure lacks Copycat at " + x);
            var current = ((CopycatBlockEntity) entity).getMaterial();
            ClientTestPlatform.verifyPonderMaterial(scene.getWorld(), new BlockPos(x, 1, 2), current);
            if (current.is(material)) count++;
        }
        return count;
    }

    static final class Screen extends PonderUI {
        Screen(PonderScene scene) { super(List.of(scene)); }
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
