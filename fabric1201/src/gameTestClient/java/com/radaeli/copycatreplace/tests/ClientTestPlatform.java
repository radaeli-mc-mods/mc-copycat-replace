package com.radaeli.copycatreplace.tests;

import com.radaeli.copycatreplace.client.CopycatReplaceClientKeys;
import com.radaeli.copycatreplace.network.ModNetworking;
import net.createmod.ponder.api.level.PonderLevel;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockview.v2.FabricBlockView;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

import java.util.concurrent.CompletableFuture;

public final class ClientTestPlatform implements ClientModInitializer {
    private boolean listening;
    private CompletableFuture<Void> languageReload;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            String language = System.getProperty("copycat_replace.validationLanguage");
            if (language != null) {
                if (languageReload == null) {
                    if (client.getOverlay() != null) return;
                    client.options.languageCode = language;
                    client.getLanguageManager().setSelected(language);
                    languageReload = client.reloadResourcePacks();
                }
                if (!languageReload.isDone()) return;
                languageReload.join();
            }
            if (!listening && Boolean.getBoolean("copycat_replace.clientSmoke")) {
                client.getSoundManager().addListener((sound, event) -> ClientValidation.sound(sound.getLocation().toString()));
                listening = true;
            }
            ClientValidation.tick();
            if (client.getOverlay() == null) MultiplayerClientValidation.tick();
        });
    }

    static boolean scroll(double delta) {
        return CopycatReplaceClientKeys.scroll(Minecraft.getInstance().getWindow().getWindow(), delta);
    }

    static void syncLimit(ServerPlayer player) {
        ModNetworking.sendLimit(player);
    }

    static void verifyPonderMaterial(PonderLevel world, BlockPos pos, BlockState expected) {
        var data = ((FabricBlockView) world).getBlockEntityRenderData(pos);
        if (!expected.equals(data)) throw new AssertionError("Rendered Ponder material does not match at " + pos);
    }

    static void connect() {
        String address = System.getProperty("copycat_replace.address", "127.0.0.1:25587");
        ConnectScreen.startConnecting(new TitleScreen(), Minecraft.getInstance(), ServerAddress.parseString(address),
                new ServerData("Copycat validation", address, false), false);
    }

    static void disconnect() {
        var client = Minecraft.getInstance();
        client.level.disconnect();
        client.clearLevel(new TitleScreen());
    }

    static void createWorld(String world) {
        Minecraft.getInstance().createWorldOpenFlows().createFreshLevel(world,
                new LevelSettings(world, GameType.CREATIVE, false, Difficulty.PEACEFUL, true,
                        new GameRules(), WorldDataConfiguration.DEFAULT),
                new WorldOptions(42L, false, false), registries -> registries.registryOrThrow(Registries.WORLD_PRESET)
                        .getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
    }
}
