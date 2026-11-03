package com.radaeli.copycatreplace.tests;
import com.mojang.authlib.GameProfile;
import com.radaeli.copycatreplace.interaction.CopycatReplaceHandler;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.gametest.GameTestHolder;
import java.util.*;
@GameTestHolder("copycat_replace")
public final class TestPlatform {
    @GameTestGenerator public static List<TestFunction> generate() { return ReplacementScenarios.generate(); }
    static void syncLimit(ServerPlayer p) { com.radaeli.copycatreplace.network.ConnectedBlockLimitSync.onPlayerLoggedIn(new net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent(p)); }
    // Limit scenarios mutate an in-memory config so they do not race Forge's file watcher.
    private static com.electronwill.nightconfig.core.CommentedConfig originalConfig;
    static void prepareConfigMutation() {
        var config = serverConfig();
        originalConfig = config.getConfigData();
        int current = com.radaeli.copycatreplace.config.CopycatReplaceConfig.MAX_CONNECTED_BLOCKS.get();
        var temporary = com.electronwill.nightconfig.core.CommentedConfig.inMemory();
        temporary.set("max_connected_blocks", current);
        com.radaeli.copycatreplace.config.CopycatReplaceConfig.SPEC.setConfig(temporary);
    }
    static void finishConfigMutation() {
        com.radaeli.copycatreplace.config.CopycatReplaceConfig.SPEC.setConfig(originalConfig);
        originalConfig = null;
    }
    static void name(net.minecraft.world.item.ItemStack stack, net.minecraft.network.chat.Component name) {
        stack.setHoverName(name);
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
        var p = new net.minecraftforge.common.util.FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "feedback-test")) {
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

    private static net.minecraftforge.fml.config.ModConfig serverConfig() {
        var configs = net.minecraftforge.fml.config.ConfigTracker.INSTANCE.configSets().get(net.minecraftforge.fml.config.ModConfig.Type.SERVER);
        return configs.stream().filter(c -> c.getSpec() == com.radaeli.copycatreplace.config.CopycatReplaceConfig.SPEC)
                .findFirst().orElseThrow();
    }
    static java.nio.file.Path serverConfigPath() { return serverConfig().getFullPath(); }
    static Protection protect(ServerPlayer player, Set<net.minecraft.core.BlockPos> denied, boolean denyBreak, boolean denyPlace) {
        return new Protection(player, denied, denyBreak, denyPlace);
    }
    static final class Protection implements AutoCloseable {
        int breaks, places;
        final Set<net.minecraft.core.BlockPos> checked = new HashSet<>();
        final java.util.function.Consumer<net.minecraftforge.event.level.BlockEvent.BreakEvent> onBreak;
        final java.util.function.Consumer<net.minecraftforge.event.level.BlockEvent.EntityPlaceEvent> onPlace;
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
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(onBreak);
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(onPlace);
        }
        @Override public void close() {
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(onBreak);
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(onPlace);
        }
    }
}
