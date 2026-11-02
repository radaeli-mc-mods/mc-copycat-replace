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
