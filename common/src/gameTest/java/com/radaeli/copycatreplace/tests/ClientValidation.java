package com.radaeli.copycatreplace.tests;

import com.mojang.blaze3d.platform.InputConstants;
import com.radaeli.copycatreplace.config.CopycatReplaceConfig;
import com.radaeli.copycatreplace.network.BulkReplaceMode;
import com.radaeli.copycatreplace.client.ConnectedBulkIndicator;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.decoration.copycat.CopycatBlockEntity;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

/** Opt-in real-client checks: input -> channel -> server action -> client block update. */
public final class ClientValidation {
    private static final Minecraft MC = Minecraft.getInstance();
    private static final BlockPos TARGET = new BlockPos(0, 65, 2);
    private static final Queue<Step> STEPS = new ArrayDeque<>();
    private static final List<String> PASSED = new ArrayList<>();
    private static final Set<String> SOUNDS = new HashSet<>();
    private static final InputConstants.Key KEY = InputConstants.getKey(298, 0);
    private static Step current;
    private static KeyMapping bulk;
    private static int ticks;
    private static boolean requested, prepared, finished;
    private static boolean holdModifier;
    private static net.createmod.ponder.foundation.PonderScene tutorial;
    private static CompletableFuture<Void> work;
    private static CompletableFuture<Boolean> keyCheck;
    private static BlockPos aim = TARGET;
    public static void sound(String id) { if (Boolean.getBoolean("copycat_replace.clientSmoke")) SOUNDS.add(id); }
    public static void tick() {
        if (!Boolean.getBoolean("copycat_replace.clientSmoke") || finished) return;
        try {
            if (!requested) {
                if (MC.screen != null && MC.screen.getClass().getSimpleName().contains("AccessibilityOnboarding")) MC.setScreen(new TitleScreen());
                if (!(MC.screen instanceof TitleScreen)) return;
                requested = true;
                MC.options.renderDistance().set(3); MC.options.simulationDistance().set(5); MC.options.guiScale().set(2);
                MC.options.pauseOnLostFocus = false;
                MC.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
                String world = "copycat-validation-" + System.currentTimeMillis();
                ClientTestPlatform.createWorld(world);
                return;
            }
            if (!prepared) {
                if (MC.player == null || MC.getSingleplayerServer() == null || MC.screen != null) return;
                prepare(); prepared = true;
            }
            if (MC.player != null) {
                KeyMapping.set(KEY, holdModifier);
                Vec3 v = Vec3.atLowerCornerOf(aim).add(.5, .25, .5).subtract(MC.player.getEyePosition());
                MC.player.setYRot((float) Math.toDegrees(Math.atan2(-v.x, v.z)));
                MC.player.setXRot((float) -Math.toDegrees(Math.atan2(v.y, Math.sqrt(v.x*v.x+v.z*v.z))));
                MC.hitResult = hit();
            }
            if (current == null) {
                current = STEPS.poll();
                if (current == null) { finish(null); return; }
                ticks = 0; current.start.run();
            }
            if (++ticks >= 20 && current.done.getAsBoolean()) {
                PASSED.add(current.name); System.out.println("COPYCAT CLIENT PASS: " + current.name); current = null;
            } else if (ticks > 400) throw new AssertionError("Timed out: " + current.name);
        } catch (Throwable failure) { finish(failure); }
    }
    private static void prepare() {
        bulk = Arrays.stream(MC.options.keyMappings).filter(k -> k.getName().equals("key.copycat_replace.bulk_replace")).findFirst().orElseThrow();
        bulk.setKey(KEY); KeyMapping.resetMapping();
        server("Prepare connected empty Copycats and server limit", p -> {
            for (int x=-3; x<9; x++) for (int z=-3; z<6; z++) p.serverLevel().setBlockAndUpdate(new BlockPos(x,64,z), Blocks.STONE.defaultBlockState());
            for (int i=0; i<4; i++) p.serverLevel().setBlockAndUpdate(TARGET.east(i), AllBlocks.COPYCAT_STEP.getDefaultState());
            p.teleportTo(.5,65,-.5); p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(AllItems.WRENCH.get()));
            p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.STONE,8));
            CopycatReplaceConfig.MAX_CONNECTED_BLOCKS.set(3); ClientTestPlatform.syncLimit(p);
        });
        step("Receive authoritative server limit", () -> {}, () -> field("serverMaximum").equals(3));
        step("Remapped modifier reaches server", () -> { holdModifier=true; KeyMapping.set(KEY,true); }, () -> serverKey(true));
        step("HUD and outline show three connected positions", () -> {}, () -> previewCount(3) && !((List<?>)field("highlightedLines")).isEmpty());
        capture("01-connected-preview.png");
        step("Real click applies material", ClientValidation::click, () -> material(Blocks.STONE,3));
        step("Placement sound is dispatched", () -> {}, () -> SOUNDS.stream().anyMatch(s -> s.contains("stone.place")));
        server("Replace material through other hand", p -> p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.OAK_PLANKS,8)));
        step("Real click replaces material", ClientValidation::click, () -> material(Blocks.OAK_PLANKS,3));
        capture("02-replaced-preview.png");
        server("Prepare wrench-only removal", p -> p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY));
        step("Real click removes connected material", ClientValidation::click, () -> empty(3));
        step("Moving to an empty target clears removable preview", () -> aim=TARGET.east(3), () -> previewCount(0) && ((List<?>)field("highlightedLines")).isEmpty());
        capture("03-moved-preview.png");
        step("Opening screen releases server modifier", () -> MC.setScreen(new PauseScreen(true)), () -> MC.screen instanceof PauseScreen && serverKey(false));
        step("Closing screen restores held modifier", () -> MC.setScreen(null), () -> serverKey(true));
        step("Releasing key clears preview and server state", () -> { holdModifier=false; KeyMapping.set(KEY,false); }, () -> serverKey(false) && ((List<?>)field("highlightedLines")).isEmpty());
        capture("04-released.png");
        server("Prepare rejected material", p -> p.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.STICK, 3)));
        step("Rejected material reaches client actionbar", ClientValidation::click, () -> {
            try {
                var message = net.minecraft.client.gui.Gui.class.getDeclaredField("overlayMessage");
                message.setAccessible(true);
                var value = (Component) message.get(MC.gui);
                return value != null && value.getString().equals(Component.translatable("message.copycat_replace.invalid_material").getString());
            } catch (ReflectiveOperationException e) { throw new RuntimeException(e); }
        });
        capture("05-rejected-material.png");
        step("Ponder loads for both Create Copycats and plays complete timeline", () -> tutorial = PonderValidation.compile(), () -> tutorial != null);
        step("Open addon Ponder scene", () -> MC.setScreen(new PonderValidation.Screen(tutorial)), () -> MC.screen instanceof PonderValidation.Screen);
        step("Seek Ponder replacement example", () -> ((PonderValidation.Screen) MC.screen).seekToTime(30), () -> true);
        capture("06-ponder-replacement.png");
        step("Seek Ponder connected key example", () -> ((PonderValidation.Screen) MC.screen).seekToTime(350), () -> true);
        capture("07-ponder-connected.png");
        step("Close Ponder", () -> MC.setScreen(null), () -> MC.screen == null);
    }
    private static boolean previewCount(int n) { return ((Component)field("estimateText")).getString().contains(Integer.toString(n)); }
    private static boolean serverKey(boolean expected) {
        // Read after a server-thread barrier, never inspect mutable server state directly on the render thread.
        var server = MC.getSingleplayerServer();
        if (keyCheck == null) {
            UUID id = MC.player.getUUID();
            keyCheck = server.submit(() -> BulkReplaceMode.isDown(server.getPlayerList().getPlayer(id)) == expected);
            return false;
        }
        if (!keyCheck.isDone()) return false;
        boolean result = keyCheck.join(); keyCheck = null; return result;
    }
    private static boolean material(net.minecraft.world.level.block.Block block,int count) {
        for(int i=0;i<count;i++) if (!(MC.level.getBlockEntity(TARGET.east(i)) instanceof CopycatBlockEntity be) || !be.getMaterial().is(block)) return false;
        return true;
    }
    private static boolean empty(int count) {
        for(int i=0;i<count;i++) if (!(MC.level.getBlockEntity(TARGET.east(i)) instanceof CopycatBlockEntity be) || be.hasCustomMaterial()) return false;
        return true;
    }
    private static BlockHitResult hit() { return new BlockHitResult(Vec3.atLowerCornerOf(aim).add(.5,.25,.5),Direction.UP,aim,false); }
    private static void click() { MC.gameMode.useItemOn(MC.player,InteractionHand.MAIN_HAND,hit()); }
    private static Object field(String name) {
        try { var f=ConnectedBulkIndicator.class.getDeclaredField(name); f.setAccessible(true); return f.get(null); }
        catch(ReflectiveOperationException e) { throw new RuntimeException(e); }
    }
    private static void server(String name, Consumer<ServerPlayer> action) {
        step(name, () -> work=MC.getSingleplayerServer().submit(() -> {
            ServerPlayer p=MC.getSingleplayerServer().getPlayerList().getPlayer(MC.player.getUUID()); action.accept(p); p.inventoryMenu.broadcastChanges();
        }), () -> { if(work.isDone()) { work.join(); return true; } return false; });
    }
    private static void capture(String name) {
        step("Prepare capture " + name, () -> MC.getToasts().clear(), () -> true);
        step("Capture " + name, () -> Screenshot.grab(MC.gameDirectory,name,MC.getMainRenderTarget(),ignored -> {}), () -> true);
    }
    private static void step(String name,Runnable start,BooleanSupplier done) { STEPS.add(new Step(name,start,done)); }
    private static void finish(Throwable failure) {
        finished=true;
        String text=(failure==null ? "PASS\n" : "FAIL " + (current==null ? "startup" : current.name) + "\n" + failure + "\n")+String.join("\n",PASSED);
        if(failure!=null) failure.printStackTrace();
        try { Files.writeString(MC.gameDirectory.toPath().resolve("result.txt"),text); }
        catch(Exception e) { throw new RuntimeException(e); }
        MC.stop();
    }
    private record Step(String name,Runnable start,BooleanSupplier done) {}
}
