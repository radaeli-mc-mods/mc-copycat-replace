package com.radaeli.copycatreplace.network;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Server-side view of each player's momentary bulk-replacement key state. */
public final class BulkReplaceMode {
    private static final Map<UUID, SelectionMode> ACTIVE_PLAYERS = new ConcurrentHashMap<>();

    private BulkReplaceMode() {
    }

    public static void setDown(Player player, boolean down) {
        setState(player, down, SelectionMode.CONNECTED);
    }

    public static void setState(Player player, boolean down, SelectionMode mode) {
        if (player instanceof ServerPlayer serverPlayer) {
            if (down) {
                ACTIVE_PLAYERS.put(serverPlayer.getUUID(), mode);
            } else {
                ACTIVE_PLAYERS.remove(serverPlayer.getUUID());
            }
        }
    }

    public static boolean isDown(Player player) {
        return player instanceof ServerPlayer && ACTIVE_PLAYERS.containsKey(player.getUUID());
    }

    public static SelectionMode selection(Player player) {
        return ACTIVE_PLAYERS.getOrDefault(player.getUUID(), SelectionMode.CONNECTED);
    }

    public static void disconnect(ServerPlayer player) {
        ACTIVE_PLAYERS.remove(player.getUUID());
    }
}
