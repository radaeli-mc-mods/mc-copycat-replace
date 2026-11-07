package com.radaeli.copycatreplace.client;

import com.radaeli.copycatreplace.CopycatReplace;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.decoration.copycat.CopycatBlockEntity;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Client-only tutorial; uses Create blocks and has no optional-addon references. */
public final class CopycatReplacePonder implements PonderPlugin {
    @Override
    public String getModId() { return CopycatReplace.MOD_ID; }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        helper.forComponents(BuiltInRegistries.ITEM.getKey(AllBlocks.COPYCAT_STEP.asItem()),
                        BuiltInRegistries.ITEM.getKey(AllBlocks.COPYCAT_PANEL.asItem()))
                .addStoryBoard("material_actions", CopycatReplacePonder::materialActions);
    }

    private static void materialActions(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("material_actions", "Changing Copycat materials");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.world().showSection(util.select().fromTo(1, 1, 2, 3, 1, 2), net.minecraft.core.Direction.DOWN);
        BlockPos first = new BlockPos(1, 1, 2);
        var pointer = util.vector().topOf(first);
        material(scene, first, Blocks.STONE.defaultBlockState());
        scene.idle(20);
        scene.overlay().showControls(pointer, Pointing.DOWN, 70).rightClick().withItem(new ItemStack(AllItems.WRENCH.get()));
        scene.overlay().showText(80).text("Hold a Wrench in your main hand and a material in your offhand. Right-click to replace the material.").pointAt(pointer).attachKeyFrame();
        scene.idle(50);
        material(scene, first, Blocks.OAK_PLANKS.defaultBlockState());
        scene.idle(40);
        scene.overlay().showControls(pointer, Pointing.DOWN, 70).rightClick().withItem(new ItemStack(Items.BRICKS));
        scene.overlay().showText(80).text("You can also hold the material in your main hand and the Wrench in your offhand.").pointAt(pointer).attachKeyFrame();
        scene.idle(50);
        material(scene, first, Blocks.BRICKS.defaultBlockState());
        scene.idle(40);
        scene.overlay().showControls(pointer, Pointing.DOWN, 80).rightClick().whileSneaking().withItem(new ItemStack(AllItems.WRENCH.get()));
        scene.overlay().showText(90).text("Sneak bypasses this addon's replacement. Create's normal Wrench action takes over.").pointAt(pointer).attachKeyFrame();
        scene.idle(50);
        scene.world().setBlock(first, Blocks.AIR.defaultBlockState(), false);
        scene.idle(50);
        scene.world().setBlock(first, AllBlocks.COPYCAT_STEP.getDefaultState(), false);
        material(scene, first, AllBlocks.COPYCAT_BASE.getDefaultState());
        scene.overlay().showText(110).text("Hold [%s], your Connected Action key, while right-clicking with Wrench and material to apply to connected empty Copycats.",
                CopycatReplaceClientKeys.BULK_REPLACE.getTranslatedKeyMessage().getString()).pointAt(pointer).attachKeyFrame();
        scene.overlay().showControls(pointer, Pointing.DOWN, 90).rightClick().withItem(new ItemStack(Items.GOLD_BLOCK));
        scene.idle(60);
        for (int x = 1; x <= 3; x++) material(scene, new BlockPos(x, 1, 2), Blocks.GOLD_BLOCK.defaultBlockState());
        scene.idle(60);
        scene.overlay().showText(90).text("With material already applied, the same action replaces matching materials in the group.").pointAt(pointer).attachKeyFrame();
        scene.overlay().showControls(pointer, Pointing.DOWN, 80).rightClick().withItem(new ItemStack(Items.OAK_PLANKS));
        scene.idle(50);
        for (int x = 1; x <= 3; x++) material(scene, new BlockPos(x, 1, 2), Blocks.OAK_PLANKS.defaultBlockState());
        scene.idle(50);
        scene.overlay().showText(100).text("Keep holding the Connected Action key, use only a Wrench and leave the other hand empty to remove matching materials.").pointAt(pointer).attachKeyFrame();
        scene.overlay().showControls(pointer, Pointing.DOWN, 90).rightClick().withItem(new ItemStack(AllItems.WRENCH.get()));
        scene.idle(60);
        for (int x = 1; x <= 3; x++) material(scene, new BlockPos(x, 1, 2), AllBlocks.COPYCAT_BASE.getDefaultState());
        scene.idle(50);
        scene.overlay().showText(110).text("Groups follow shared faces and the same Copycat type. The server limit, available stock and build permissions restrict each action.").pointAt(pointer).attachKeyFrame();
        scene.idle(120);
        material(scene, first, Blocks.STONE.defaultBlockState());
        scene.overlay().showText(110).text("A denied position blocks propagation through that route. The preview is an estimate; protected positions stay unchanged.").pointAt(util.vector().topOf(first.east())).attachKeyFrame();
        scene.idle(120);
        material(scene, first.east(), Blocks.STONE.defaultBlockState());
        scene.overlay().showText(100).text("For example, with a server limit of 2, only two positions change, including the clicked Copycat.").pointAt(util.vector().topOf(first.east(2))).attachKeyFrame();
        scene.idle(110);
        scene.overlay().showText(130).text("Survival uses the held stack first, then identical items from your main inventory. Different item data stays untouched.").pointAt(pointer).attachKeyFrame();
        scene.idle(140);
        scene.overlay().showText(130).text("While holding the Connected Action key, scroll to choose Connected, Plane or All types. The HUD shows your selection.").pointAt(pointer).attachKeyFrame();
        scene.idle(140);
        scene.overlay().showText(130).text("Plane stays on the clicked face plane and keeps the Copycat type. All types crosses supported Copycat shapes. Material and permission filters still apply.").pointAt(pointer).attachKeyFrame();
        scene.idle(140);
        scene.markAsFinished();
    }

    private static void material(SceneBuilder scene, BlockPos pos, BlockState state) {
        scene.world().modifyBlockEntity(pos, CopycatBlockEntity.class, be -> be.setMaterial(state));
        scene.addInstruction(ponder -> ponder.forEach(net.createmod.ponder.api.element.WorldSectionElement.class,
                net.createmod.ponder.api.element.WorldSectionElement::queueRedraw));
    }
}
