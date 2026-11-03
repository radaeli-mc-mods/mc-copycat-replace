package com.radaeli.copycatreplace.network;

import com.radaeli.copycatreplace.config.CopycatReplaceConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Sends the live server limit; native SERVER config sync only covers the handshake. */
public final class ConnectedBlockLimitSync {
    private ConnectedBlockLimitSync() {
    }

    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sendToPlayer(player);
        }
    }

    public static void onConfigReloading(ModConfigEvent.Reloading event) {
        if (event.getConfig().getType() != net.neoforged.fml.config.ModConfig.Type.SERVER
                || event.getConfig().getSpec() != CopycatReplaceConfig.SPEC) {
            return;
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            server.execute(() -> server.getPlayerList().getPlayers().forEach(ConnectedBlockLimitSync::sendToPlayer));
        }
    }

    private static void sendToPlayer(ServerPlayer player) {
        PacketDistributor.sendToPlayer(
                player,
                new ConnectedBlockLimitPayload(CopycatReplaceConfig.MAX_CONNECTED_BLOCKS.get())
        );
    }
}
