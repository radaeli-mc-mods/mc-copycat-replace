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
    static void prepareConfigMutation() {
        com.electronwill.nightconfig.core.file.FileWatcher.defaultInstance().removeWatch(
                net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get().resolve("copycat_replace-common.toml"));
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
}
