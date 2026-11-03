package com.radaeli.copycatreplace.tests;
import com.mojang.authlib.GameProfile;
import com.radaeli.copycatreplace.interaction.CopycatReplaceHandler;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import java.util.*;
@GameTestHolder("copycat_replace")
public final class TestPlatform {
    @GameTestGenerator public static List<TestFunction> generate() { return ReplacementScenarios.generate(); }
    static void syncLimit(ServerPlayer p) { com.radaeli.copycatreplace.network.ConnectedBlockLimitSync.onPlayerLoggedIn(new net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent(p)); }
    static void prepareConfigMutation() {}
    static void finishConfigMutation() {}
    static void name(net.minecraft.world.item.ItemStack stack, net.minecraft.network.chat.Component name) {
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, name);
    }
    static boolean hasCopycats() { return ModList.get().isLoaded("copycats"); }
    static ServerPlayer player(GameTestHelper h, boolean creative) {
        var p = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), "copycat-test"));
        p.setGameMode(creative ? GameType.CREATIVE : GameType.SURVIVAL);
        var pos = h.absolutePos(ReplacementScenarios.ORIGIN);
        p.setPos(pos.getX()+.5, pos.getY()+1, pos.getZ()+.5);
        return p;
    }
    static ServerPlayer feedbackPlayer(GameTestHelper h, List<net.minecraft.network.chat.Component> messages) {
        var p = new net.neoforged.neoforge.common.util.FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "feedback-test")) {
            @Override public void displayClientMessage(net.minecraft.network.chat.Component message, boolean actionbar) {
                if (!actionbar) throw new AssertionError("Feedback must use actionbar");
                messages.add(message);
            }
        };
        p.setGameMode(GameType.SURVIVAL);
        p.getAbilities().mayBuild = true;
        return p;
    }
    static boolean click(ServerPlayer p, InteractionHand hand, BlockHitResult hit) {
        var event = new PlayerInteractEvent.RightClickBlock(p, hand, hit.getBlockPos(), hit);
        CopycatReplaceHandler.onRightClickBlock(event);
        return event.isCanceled();
    }

    static java.nio.file.Path serverConfigPath() {
        var configs = net.neoforged.fml.config.ModConfigs.getConfigSet(net.neoforged.fml.config.ModConfig.Type.SERVER);
        return configs.stream().filter(c -> c.getSpec() == com.radaeli.copycatreplace.config.CopycatReplaceConfig.SPEC)
                .findFirst().orElseThrow().getFullPath();
    }
    static Protection protect(ServerPlayer player, Set<net.minecraft.core.BlockPos> denied, boolean denyBreak, boolean denyPlace) {
        return new Protection(player, denied, denyBreak, denyPlace);
    }
    static final class Protection implements AutoCloseable {
        int breaks, places;
        final Set<net.minecraft.core.BlockPos> checked = new HashSet<>();
        final java.util.function.Consumer<net.neoforged.neoforge.event.level.BlockEvent.BreakEvent> onBreak;
        final java.util.function.Consumer<net.neoforged.neoforge.event.level.BlockEvent.EntityPlaceEvent> onPlace;
        Protection(ServerPlayer player, Set<net.minecraft.core.BlockPos> denied, boolean denyBreak, boolean denyPlace) {
            onBreak = event -> {
                if (event.getPlayer() != player) return;
                breaks++; checked.add(event.getPos().immutable());
                if (denyBreak && denied.contains(event.getPos())) event.setCanceled(true);
            };
            onPlace = event -> {
                if (event.getEntity() != player) return;
                places++; checked.add(event.getPos().immutable());
                if (denyPlace && denied.contains(event.getPos())) event.setCanceled(true);
            };
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(onBreak);
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(onPlace);
        }
        @Override public void close() {
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(onBreak);
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(onPlace);
        }
    }
}
