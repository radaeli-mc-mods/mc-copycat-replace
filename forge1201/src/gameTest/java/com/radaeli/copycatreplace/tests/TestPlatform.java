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
    // Forge autosaves ConfigValue.set and its watcher can reload a partially written file.
    // Only the isolated GameTest process disables this watch; gameplay behavior is unchanged.
    private static boolean configWatchRemoved;
    static void prepareConfigMutation() {
        if (configWatchRemoved) return;
        com.electronwill.nightconfig.core.file.FileWatcher.defaultInstance().removeWatch(
                net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get().resolve("copycat_replace-common.toml"));
        configWatchRemoved = true;
    }
    static boolean hasCopycats() { return ModList.get().isLoaded("copycats"); }
    static ServerPlayer player(GameTestHelper h, boolean creative) {
        var p = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), "copycat-test"));
        p.setGameMode(creative ? GameType.CREATIVE : GameType.SURVIVAL);
        var pos = h.absolutePos(ReplacementScenarios.ORIGIN);
        p.setPos(pos.getX()+.5, pos.getY()+1, pos.getZ()+.5);
        return p;
    }
    static boolean click(ServerPlayer p, InteractionHand hand, BlockHitResult hit) {
        var event = new PlayerInteractEvent.RightClickBlock(p, hand, hit.getBlockPos(), hit);
        CopycatReplaceHandler.onRightClickBlock(event);
        return event.isCanceled();
    }

    static java.nio.file.Path serverConfigPath() {
        var configs = net.minecraftforge.fml.config.ConfigTracker.INSTANCE.configSets().get(net.minecraftforge.fml.config.ModConfig.Type.SERVER);
        return configs.stream().filter(c -> c.getSpec() == com.radaeli.copycatreplace.config.CopycatReplaceConfig.SPEC)
                .findFirst().orElseThrow().getFullPath();
    }
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
