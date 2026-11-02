package com.radaeli.copycatreplace.tests;

import com.radaeli.copycatreplace.config.CopycatReplaceConfig;
import com.radaeli.copycatreplace.network.*;
import com.radaeli.copycatreplace.service.*;
import com.simibubi.create.AllBlocks;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import java.util.*;
import static com.radaeli.copycatreplace.tests.ReplacementScenarios.*;

final class InventorySelectionScenarios {
    static List<TestFunction> generate() {
        List<TestFunction> tests = new ArrayList<>();
        for (boolean reversed : List.of(false, true)) {
            for (boolean applying : List.of(false, true))
                tests.add(test("inventory_" + reversed + "_" + applying, h -> inventory(h, reversed, applying)));
            tests.add(test("inventory_large_" + reversed, h -> large(h, reversed)));
        }
        for (SelectionMode mode : SelectionMode.values()) {
            for (Direction face : Direction.values())
                tests.add(test("selection_" + mode + "_" + face, h -> geometry(h, mode, face)));
            tests.add(test("selection_permission_" + mode, h -> permission(h, mode)));
        }
        tests.add(test("selection_player_state", InventorySelectionScenarios::state));
        tests.add(test("inventory_refund_budget", InventorySelectionScenarios::budget));
        return tests;
    }
    private static void inventory(GameTestHelper h, boolean reversed, boolean applying) {
        var pos = nativeBlock(h, ORIGIN, applying ? null : Blocks.STONE.defaultBlockState());
        for (int i=1;i<4;i++) nativeBlock(h, ORIGIN.east(i), applying ? null : Blocks.STONE.defaultBlockState());
        var p = player(h, false);
        var hand = equip(p, reversed, new ItemStack(Items.GOLD_BLOCK));
        p.getInventory().setItem(9, new ItemStack(Items.GOLD_BLOCK, 2));
        var incompatible = new ItemStack(Items.GOLD_BLOCK, 4);
        TestPlatform.name(incompatible, Component.literal("different data"));
        p.getInventory().setItem(10, incompatible);
        var preview = BulkMaterialReplacement.previewConnected(h.getLevel(), pos, h.getLevel().getBlockState(pos), hit(pos), p, p.getItemInHand(hand), false, 64);
        check(preview.count()==3, "Preview included incompatible data or missed inventory");
        int n = applying ? BulkMaterialReplacement.applyConnected(h.getLevel(), pos, h.getLevel().getBlockState(pos), hit(pos), p, hand, p.getItemInHand(hand))
                : BulkMaterialReplacement.replaceConnected(h.getLevel(), pos, h.getLevel().getBlockState(pos), hit(pos), p, hand, p.getItemInHand(hand));
        check(n==3 && !p.getItemInHand(hand).is(Items.GOLD_BLOCK), "Inventory continuation failed: " + n);
        check(incompatible.getCount()==4 && p.getInventory().countItem(Items.GOLD_BLOCK)==4, "Incompatible stack consumed or material duplicated");
        check(p.getInventory().countItem(Items.STONE)==(applying?0:3), "Refund accounting mismatch");
        check(!target(h,pos.east(3)).containsMaterial(Blocks.GOLD_BLOCK), "Insufficient stock changed fourth position");
    }
    private static void large(GameTestHelper h, boolean reversed) {
        TestPlatform.prepareConfigMutation();
        int previous=CopycatReplaceConfig.MAX_CONNECTED_BLOCKS.get();
        try {
            CopycatReplaceConfig.MAX_CONNECTED_BLOCKS.set(256);
            BlockPos origin=null;
            for(int z=0;z<8;z++) for(int x=0;x<9;x++) {
                var pos=nativeBlock(h,ORIGIN.offset(x,0,z),null);
                if(origin==null) origin=pos;
            }
            var p=player(h,false);var hand=equip(p,reversed,new ItemStack(Items.GOLD_BLOCK,64));
            p.getInventory().setItem(9,new ItemStack(Items.GOLD_BLOCK,8));
            check(BulkMaterialReplacement.previewConnected(h.getLevel(),origin,h.getLevel().getBlockState(origin),hit(origin),p,p.getItemInHand(hand),false,256).count()==72,"Large preview missed inventory");
            check(BulkMaterialReplacement.applyConnected(h.getLevel(),origin,h.getLevel().getBlockState(origin),hit(origin),p,hand,p.getItemInHand(hand))==72,"Group stopped at first stack");
            check(p.getInventory().countItem(Items.GOLD_BLOCK)==0 && p.getItemInHand(hand).isEmpty(),"Exact stock lost or duplicated");
        } finally {
            CopycatReplaceConfig.MAX_CONNECTED_BLOCKS.set(previous);
        }
    }
    private static void geometry(GameTestHelper h, SelectionMode mode, Direction face) {
        var origin=nativeBlock(h,ORIGIN,null);
        for (Direction direction : Direction.values())
            h.getLevel().setBlockAndUpdate(origin.relative(direction), direction==Direction.NORTH
                    ? AllBlocks.COPYCAT_PANEL.getDefaultState() : AllBlocks.COPYCAT_STEP.getDefaultState());
        var p=player(h,false);var hand=equip(p,false,new ItemStack(Items.GOLD_BLOCK));
        p.getInventory().setItem(9,new ItemStack(Items.GOLD_BLOCK,6));
        var hit=new BlockHitResult(hit(origin).getLocation(),face,origin,false);
        BulkReplaceMode.setState(p,true,mode);
        try {
            int expected=mode==SelectionMode.ALL_TYPES?7:mode==SelectionMode.PLANE?(face.getAxis()==Direction.Axis.Z?5:4):6;
            var preview=BulkMaterialReplacement.previewConnected(h.getLevel(),origin,h.getLevel().getBlockState(origin),hit,p,p.getItemInHand(hand),false,64,mode);
            check(preview.count()==expected,"Wrong selection geometry: " + mode + "/" + face + ": " + preview.count());
            check(BulkMaterialReplacement.applyConnected(h.getLevel(),origin,h.getLevel().getBlockState(origin),hit,p,hand,p.getItemInHand(hand))==expected,"Apply diverged from preview");
            p.setItemInHand(hand,new ItemStack(Items.OAK_PLANKS,7));
            check(BulkMaterialReplacement.replaceConnected(h.getLevel(),origin,h.getLevel().getBlockState(origin),hit,p,hand,p.getItemInHand(hand))==expected,"Replace selection differs");
            check(BulkMaterialReplacement.removeConnected(h.getLevel(),origin,h.getLevel().getBlockState(origin),hit,p)==expected,"Remove selection differs");
        } finally { BulkReplaceMode.setDown(p,false); }
    }
    private static void permission(GameTestHelper h,SelectionMode mode) {
        var origin=nativeBlock(h,ORIGIN,null);nativeBlock(h,ORIGIN.east(),null);nativeBlock(h,ORIGIN.east(2),null);
        var p=player(h,false);var hand=equip(p,false,new ItemStack(Items.GOLD_BLOCK));
        p.getInventory().setItem(9,new ItemStack(Items.GOLD_BLOCK,3));
        BulkReplaceMode.setState(p,true,mode);
        try(var protection=TestPlatform.protect(p,Set.of(origin.east()),true,true)) {
            check(BulkMaterialReplacement.applyConnected(h.getLevel(),origin,h.getLevel().getBlockState(origin),hit(origin),p,hand,p.getItemInHand(hand))==1,"Permission barrier bypassed");
            check(p.getInventory().countItem(Items.GOLD_BLOCK)==3,"Denied positions consumed stock");
        } finally { BulkReplaceMode.setDown(p,false); }
    }
    private static void state(GameTestHelper h) {
        var a=player(h,false);var b=player(h,false);
        BulkReplaceMode.setState(a,true,SelectionMode.ALL_TYPES);BulkReplaceMode.setState(b,true,SelectionMode.PLANE);
        try {
            check(BulkReplaceMode.selection(a)==SelectionMode.ALL_TYPES&&BulkReplaceMode.selection(b)==SelectionMode.PLANE,"Modes leaked between players");
            BulkReplaceMode.setDown(a,false);
            check(!BulkReplaceMode.isDown(a)&&BulkReplaceMode.selection(a)==SelectionMode.CONNECTED,"Release retained server selection");
            check(SelectionMode.fromId(-1)==SelectionMode.CONNECTED&&SelectionMode.fromId(999)==SelectionMode.CONNECTED,"Invalid packet mode broadened selection");
        } finally { BulkReplaceMode.setDown(a,false);BulkReplaceMode.setDown(b,false); }
    }
    private static void budget(GameTestHelper h) {
        var p=player(h,false);var hand=equip(p,false,new ItemStack(Items.GOLD_BLOCK));
        var reserve=new ItemStack(Items.GOLD_BLOCK,2);p.getInventory().setItem(9,reserve);
        var stock=new MaterialStock(p,p.getItemInHand(hand));
        stock.next().shrink(1);stock.consumed(1);
        reserve.grow(4); // Model a refund into an existing eligible stack.
        stock.next().shrink(2);stock.consumed(2);
        check(stock.next().isEmpty()&&reserve.getCount()==4,"Refunds became fresh budget");
    }
}
