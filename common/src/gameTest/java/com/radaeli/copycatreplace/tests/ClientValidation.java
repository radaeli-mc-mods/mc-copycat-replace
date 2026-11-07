package com.radaeli.copycatreplace.tests;

import com.mojang.blaze3d.platform.InputConstants;
import com.radaeli.copycatreplace.config.CopycatReplaceConfig;
import com.radaeli.copycatreplace.network.BulkReplaceMode;
import com.radaeli.copycatreplace.client.ConnectedBulkIndicator;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.decoration.copycat.CopycatBlockEntity;
import net.createmod.catnip.outliner.LineOutline;
import net.createmod.catnip.outliner.Outliner;
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
    private static boolean missTarget, watchOutlines, foreignOutline;
    private static final Object FOREIGN_OUTLINE = new Object();
    private static Map<Object, Outliner.OutlineEntry> savedOutlines = Map.of();
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
                MC.hitResult = missTarget ? BlockHitResult.miss(hit().getLocation(), Direction.UP, aim) : hit();
            }
            if (foreignOutline) Outliner.getInstance().keep(FOREIGN_OUTLINE);
            if (watchOutlines && !liveOutlines()) throw new AssertionError("Create outlines expired between preview refreshes");
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
        step("HUD and Create outlines follow three connected step shapes", () -> {},
                () -> previewCount(3) && liveOutlines() && outlineBoundsMatch(List.of(TARGET, TARGET.east(), TARGET.east(2))));
        capture("01-connected-preview.png");
        step("Create outlines remain alive and reuse entries across refreshes", () -> {
            savedOutlines = ownOutlines(); watchOutlines = true;
        }, () -> ticks >= 30 && ownOutlines().equals(savedOutlines));
        step("Stop continuous outline check", () -> watchOutlines = false, () -> true);
        server("Reduce server preview limit", p -> { CopycatReplaceConfig.MAX_CONNECTED_BLOCKS.set(1); ClientTestPlatform.syncLimit(p); });
        step("Shrinking selection removes old Create segments", () -> {}, () -> previewCount(1) && liveOutlines() && removedOldSegments());
        capture("08-single-step-preview.png");
        server("Restore server preview limit", p -> { CopycatReplaceConfig.MAX_CONNECTED_BLOCKS.set(3); ClientTestPlatform.syncLimit(p); });
        step("Expanded selection returns to three positions", () -> {}, () -> previewCount(3) && liveOutlines());
        step("Use small GUI scale", () -> { MC.options.guiScale().set(1); MC.resizeDisplay(); }, () -> true);
        capture("09-hud-scale-1.png");
        step("Restore GUI scale", () -> { MC.options.guiScale().set(2); MC.resizeDisplay(); }, () -> true);
        step("Hide GUI", () -> MC.options.hideGui = true, () -> true);
        capture("10-hidden-hud.png");
        step("Restore GUI", () -> MC.options.hideGui = false, () -> true);
        step("A missed BlockHitResult clears highlights", () -> missTarget = true, () -> previewCount(0) && ownOutlines().isEmpty());
        step("Looking back restores highlights", () -> missTarget = false, () -> previewCount(3) && liveOutlines());
        step("Held modifier consumes scroll without changing hotbar slot", () -> {
            int selected=MC.player.getInventory().selected;
            if (!ClientTestPlatform.scroll(1) || MC.player.getInventory().selected!=selected)
                throw new AssertionError("Mode wheel was not consumed safely");
        }, () -> selection(com.radaeli.copycatreplace.network.SelectionMode.PLANE));
        step("Second scroll selects all types on server", () -> ClientTestPlatform.scroll(1),
                () -> selection(com.radaeli.copycatreplace.network.SelectionMode.ALL_TYPES));
        step("Third scroll returns to connected", () -> ClientTestPlatform.scroll(1),
                () -> selection(com.radaeli.copycatreplace.network.SelectionMode.CONNECTED));
        step("Real click applies material", ClientValidation::click, () -> material(Blocks.STONE,3));
        step("Placement sound is dispatched", () -> {}, () -> SOUNDS.stream().anyMatch(s -> s.contains("stone.place")));
        server("Prepare one held material plus two inventory items", p -> {
            p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.OAK_PLANKS));
            p.getInventory().setItem(9,new ItemStack(Items.OAK_PLANKS,2));
        });
        step("Preview counts inventory material beyond hand stack", () -> {}, () -> previewCount(3));
        step("Real click replaces material", ClientValidation::click, () -> material(Blocks.OAK_PLANKS,3));
        capture("02-replaced-preview.png");
        server("Prepare wrench-only removal", p -> p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY));
        step("Real click removes connected material", ClientValidation::click, () -> empty(3));
        step("Moving to an empty target clears removable preview", () -> aim=TARGET.east(3), () -> previewCount(0) && ownOutlines().isEmpty());
        capture("03-moved-preview.png");
        server("Restore material for screen and key cleanup checks", p -> p.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.STONE, 8)));
        step("Preview is active before opening screen", () -> {
            aim = TARGET; foreignOutline = true;
            Outliner.getInstance().showLine(FOREIGN_OUTLINE, Vec3.ZERO, new Vec3(0, 1, 0));
        }, () -> previewCount(3) && liveOutlines());
        step("Opening screen removes only addon highlights and releases server modifier", () -> MC.setScreen(new PauseScreen(true)),
                () -> MC.screen instanceof PauseScreen && serverKey(false) && ownOutlines().isEmpty()
                        && Outliner.getInstance().getOutlines().containsKey(FOREIGN_OUTLINE));
        step("Screen scroll does not select a mode", () -> {
            if (ClientTestPlatform.scroll(1)) throw new AssertionError("Addon consumed screen scroll");
        }, () -> true);
        step("Closing screen restores held modifier and highlights", () -> MC.setScreen(null), () -> serverKey(true) && liveOutlines());
        step("Releasing key clears preview and server state", () -> { holdModifier=false; KeyMapping.set(KEY,false); },
                () -> serverKey(false) && ownOutlines().isEmpty() && Outliner.getInstance().getOutlines().containsKey(FOREIGN_OUTLINE));
        step("Remove independent test highlight", () -> { foreignOutline = false; Outliner.getInstance().remove(FOREIGN_OUTLINE); }, () -> true);
        capture("04-released.png");
        step("Released modifier leaves scroll to normal controls", () -> {
            if (ClientTestPlatform.scroll(1)) throw new AssertionError("Addon consumed normal scroll");
        }, () -> true);
        server("Prepare rejected material", p -> p.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.STICK, 3)));
        step("Rejected material reaches client actionbar", ClientValidation::click, () -> {
            try {
                var message = net.minecraft.client.gui.Gui.class.getDeclaredField("overlayMessageString");
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
        step("Seek Ponder connected key example", () -> ((PonderValidation.Screen) MC.screen).seekToTime(380), () -> PonderValidation.hasGold(tutorial));
        capture("07-ponder-connected.png");
        step("Close Ponder", () -> MC.setScreen(null), () -> MC.screen == null);
        visualSelectionChecks();
    }

    private static void visualSelectionChecks() {
        server("Prepare connected and plane geometry", p -> {
            p.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.STONE, 8));
            for (int i = 0; i < 4; i++) p.serverLevel().setBlockAndUpdate(TARGET.east(i), AllBlocks.COPYCAT_STEP.getDefaultState());
            p.serverLevel().setBlockAndUpdate(TARGET.above(), AllBlocks.COPYCAT_STEP.getDefaultState());
            p.serverLevel().setBlockAndUpdate(TARGET.east(4), AllBlocks.COPYCAT_PANEL.getDefaultState());
            CopycatReplaceConfig.MAX_CONNECTED_BLOCKS.set(64); ClientTestPlatform.syncLimit(p);
        });
        step("Connected highlights five same-type positions", () -> { aim = TARGET; holdModifier = true; KeyMapping.set(KEY, true); },
                () -> previewCount(5) && liveOutlines());
        step("Plane excludes the vertical neighbor", () -> ClientTestPlatform.scroll(1), () -> previewCount(4) && liveOutlines());
        capture("11-plane-preview.png");
        step("All types includes the panel", () -> ClientTestPlatform.scroll(1), () -> previewCount(6) && liveOutlines());
        capture("12-all-types-preview.png");
        step("Aim at a thin panel", () -> aim = TARGET.east(4), () -> previewCount(6) && liveOutlines());
        capture("13-panel-preview.png");
        step("Return to connected panel selection and preserve its thin shape", () -> ClientTestPlatform.scroll(1),
                () -> previewCount(1) && liveOutlines() && outlineBoundsMatch(List.of(TARGET.east(4))));
        capture("14-single-panel-preview.png");
        server("Survival stock limits the preview", p -> {
            p.setGameMode(GameType.SURVIVAL); p.getInventory().clearContent();
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(AllItems.WRENCH.get()));
            p.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.STONE, 2));
        });
        step("Partial stock highlights only two steps", () -> aim = TARGET, () -> previewCount(2) && liveOutlines());
        if (TestPlatform.hasCopycats()) {
            server("Prepare multipart slab preview", p -> {
                p.setGameMode(GameType.CREATIVE);
                MultiplayerCopycats.prepare(p.serverLevel(), TARGET.east(6));
                p.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.OAK_PLANKS, 8));
            });
            step("Multipart Copycat registers Create highlights", () -> aim = TARGET.east(6), () -> previewCount(1) && liveOutlines());
            capture("15-multipart-preview.png");
        }
        server("Change dimension with selection key held", p -> {
            p.setGameMode(GameType.CREATIVE);
            p.getServer().getCommands().performPrefixedCommand(p.createCommandSourceStack(),
                    "execute in minecraft:the_nether run tp @s 0.5 70 0.5");
        });
        step("Dimension change clears previous world highlights", () -> {}, () -> MC.level != null && MC.level.dimension().equals(Level.NETHER)
                && ownOutlines().isEmpty() && field("previewLevel") == MC.level);
        server("Place a Copycat in the new dimension without resending the limit", p ->
                p.serverLevel().setBlockAndUpdate(TARGET, AllBlocks.COPYCAT_PANEL.getDefaultState()));
        step("Preview resumes in the new dimension with the existing server limit", () -> aim = TARGET,
                () -> previewCount(1) && liveOutlines() && field("serverMaximum").equals(64));
        step("Disconnect clears highlights and server limit", ClientTestPlatform::disconnect,
                () -> MC.player == null && ownOutlines().isEmpty() && field("serverMaximum").equals(-1));
    }

    private static Map<Object, Outliner.OutlineEntry> ownOutlines() {
        Map<Object, Outliner.OutlineEntry> result = new HashMap<>();
        Outliner.getInstance().getOutlines().forEach((key, entry) -> {
            if (key.getClass().getName().equals(ConnectedBulkIndicator.class.getName() + "$OutlineSlot")) result.put(key, entry);
        });
        return result;
    }

    private static boolean liveOutlines() {
        var entries = ownOutlines();
        return !entries.isEmpty() && entries.values().stream().allMatch(entry -> !entry.isFading()
                && entry.getOutline() instanceof LineOutline && entry.getOutline().getParams().getLineWidth() == 1 / 32.0F);
    }

    private static boolean removedOldSegments() {
        Set<Object> removed = new HashSet<>(savedOutlines.keySet());
        removed.removeAll(ownOutlines().keySet());
        return !removed.isEmpty() && removed.stream().noneMatch(Outliner.getInstance().getOutlines()::containsKey);
    }

    private static boolean outlineBoundsMatch(List<BlockPos> positions) {
        AABB expected = null, actual = null;
        for (BlockPos pos : positions) {
            AABB shape = MC.level.getBlockState(pos).getShape(MC.level, pos).bounds().move(pos);
            expected = expected == null ? shape : expected.minmax(shape);
        }
        try {
            for (var entry : ownOutlines().values()) {
                for (String name : List.of("start", "end")) {
                    var field = LineOutline.class.getDeclaredField(name); field.setAccessible(true);
                    var point = (org.joml.Vector3d) field.get(entry.getOutline());
                    AABB endpoint = new AABB(point.x, point.y, point.z, point.x, point.y, point.z);
                    actual = actual == null ? endpoint : actual.minmax(endpoint);
                }
            }
        } catch (ReflectiveOperationException e) { throw new RuntimeException(e); }
        return expected != null && expected.equals(actual);
    }
    private static boolean selection(com.radaeli.copycatreplace.network.SelectionMode expected) {
        if (com.radaeli.copycatreplace.client.CopycatReplaceClientKeys.selection()!=expected) return false;
        if (keyCheck==null) {
            var server=MC.getSingleplayerServer(); UUID id=MC.player.getUUID();
            keyCheck=server.submit(() -> BulkReplaceMode.selection(server.getPlayerList().getPlayer(id))==expected);
            return false;
        }
        if(!keyCheck.isDone()) return false;
        boolean result=keyCheck.join(); keyCheck=null; return result;
    }
    private static boolean previewCount(int n) {
        return ((Component)field("estimateText")).getString().equals(Component.translatable("hud.copycat_replace.estimated_blocks", n).getString());
    }
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
