package com.radaeli.copycatreplace.tests;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.mojang.authlib.GameProfile;
import com.radaeli.copycatreplace.api.MaterialEditPermissionCallback;
import com.radaeli.copycatreplace.config.CopycatReplaceConfig;
import com.radaeli.copycatreplace.network.ModNetworking;
import com.radaeli.copycatreplace.service.MaterialEditPermission.Operation;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.fml.config.ConfigTracker;
import net.minecraftforge.fml.config.ModConfig;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class TestPlatform {
    private static CommentedConfig originalConfig;
    private static Protection activeProtection;

    @GameTestGenerator
    public static List<TestFunction> generate() {
        MaterialEditPermissionCallback.EVENT.register((player, world, pos, hit, operation) -> {
            var protection = activeProtection;
            return protection == null || protection.player != player || protection.allows(pos, operation);
        });
        var tests = new ArrayList<>(ReplacementScenarios.generate());
        if (FabricLoader.getInstance().isModLoaded("openpartiesandclaims")) tests.add(OpenPacScenarios.test());
        return tests;
    }

    static void syncLimit(ServerPlayer player) {
        ModNetworking.sendLimit(player);
    }

    static boolean hasCopycats() {
        return FabricLoader.getInstance().isModLoaded("copycats");
    }

    static ServerPlayer player(GameTestHelper helper, boolean creative) {
        var player = FakePlayer.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "copycat-test"));
        player.setGameMode(creative ? GameType.CREATIVE : GameType.SURVIVAL);
        var pos = helper.absolutePos(ReplacementScenarios.ORIGIN);
        player.setPos(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);
        return player;
    }

    static ServerPlayer feedbackPlayer(GameTestHelper helper, List<Component> messages) {
        var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "feedback-test")) {
            @Override
            public void displayClientMessage(Component message, boolean actionbar) {
                if (!actionbar) throw new AssertionError("Feedback must use actionbar");
                messages.add(message);
            }
        };
        player.setGameMode(GameType.SURVIVAL);
        player.getAbilities().mayBuild = true;
        return player;
    }

    static void name(ItemStack stack, Component name) {
        stack.setHoverName(name);
    }

    static boolean click(ServerPlayer player, InteractionHand hand, BlockHitResult hit) {
        return UseBlockCallback.EVENT.invoker().interact(player, player.serverLevel(), hand, hit).consumesAction();
    }

    private static ModConfig serverConfig() {
        return ConfigTracker.INSTANCE.configSets().get(ModConfig.Type.SERVER).stream()
                .filter(config -> config.getSpec() == CopycatReplaceConfig.SPEC)
                .findFirst().orElseThrow();
    }

    static Path serverConfigPath() {
        return serverConfig().getFullPath();
    }

    static void prepareConfigMutation() {
        originalConfig = serverConfig().getConfigData();
        var temporary = CommentedConfig.inMemory();
        temporary.set("max_connected_blocks", CopycatReplaceConfig.MAX_CONNECTED_BLOCKS.get());
        CopycatReplaceConfig.SPEC.setConfig(temporary);
    }

    static void finishConfigMutation() {
        CopycatReplaceConfig.SPEC.setConfig(originalConfig);
        originalConfig = null;
    }

    static Protection protect(ServerPlayer player, Set<BlockPos> denied, boolean denyBreak, boolean denyPlace) {
        activeProtection = new Protection(player, denied, denyBreak, denyPlace);
        return activeProtection;
    }

    static final class Protection implements AutoCloseable {
        private final ServerPlayer player;
        private final Set<BlockPos> denied;
        private final boolean denyBreak;
        private final boolean denyPlace;
        final Set<BlockPos> checked = new HashSet<>();
        int breaks;
        int places;

        Protection(ServerPlayer player, Set<BlockPos> denied, boolean denyBreak, boolean denyPlace) {
            this.player = player;
            this.denied = denied;
            this.denyBreak = denyBreak;
            this.denyPlace = denyPlace;
        }

        boolean allows(BlockPos pos, Operation operation) {
            checked.add(pos.immutable());
            if (operation != Operation.APPLY) {
                breaks++;
                if (denyBreak && denied.contains(pos)) return false;
            }
            if (operation != Operation.REMOVE) {
                places++;
                if (denyPlace && denied.contains(pos)) return false;
            }
            return true;
        }

        @Override
        public void close() {
            activeProtection = null;
        }
    }
}
