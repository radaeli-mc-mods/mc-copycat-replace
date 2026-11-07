package com.radaeli.copycatreplace.tests;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameType;

public final class ServerTestPlatform implements ModInitializer {
    private boolean verifiedClaims;

    @Override
    public void onInitialize() {
        if (Boolean.getBoolean("copycat_replace.multiplayerServer")) {
            ServerTickEvents.END_SERVER_TICK.register(this::tick);
        }
    }

    private void tick(MinecraftServer server) {
        if (!verifiedClaims && FabricLoader.getInstance().isModLoaded("openpartiesandclaims")) {
            var actor = server.getPlayerList().getPlayerByName("CopycatActor");
            var observer = server.getPlayerList().getPlayerByName("CopycatObserver");
            if (actor == null || observer == null) return;
            var mode = actor.gameMode.getGameModeForPlayer();
            try {
                actor.setGameMode(GameType.SURVIVAL);
                OpenPacScenarios.verify(actor, actor.blockPosition().above(), observer.getUUID());
                MultiplayerValidation.marker("openpac.txt", "PASS\nOwn claim, allowed and denied foreign claim, and a bulk edit across the claim boundary.\n");
                verifiedClaims = true;
            } catch (Exception | AssertionError e) {
                MultiplayerValidation.marker("result.txt", "FAIL OpenPAC\n" + e);
                server.halt(false);
                return;
            } finally {
                actor.setGameMode(mode);
            }
        }
        MultiplayerValidation.tick(server);
    }
}
