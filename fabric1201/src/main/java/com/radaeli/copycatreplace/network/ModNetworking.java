package com.radaeli.copycatreplace.network;

import com.radaeli.copycatreplace.CopycatReplace;
import com.radaeli.copycatreplace.config.CopycatReplaceConfig;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerLoginConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerLoginNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class ModNetworking {
    public static final int VERSION = 1;
    public static final ResourceLocation LOGIN = channel("protocol");
    public static final ResourceLocation KEY_STATE = channel("bulk_key_state");
    public static final ResourceLocation BLOCK_LIMIT = channel("connected_block_limit");

    private ModNetworking() {}

    private static ResourceLocation channel(String name) {
        return new ResourceLocation(CopycatReplace.MOD_ID, name);
    }

    public static void register() {
        ServerLoginConnectionEvents.QUERY_START.register((handler, server, sender, synchronizer) ->
                sender.sendPacket(LOGIN, protocolVersion()));
        ServerLoginNetworking.registerGlobalReceiver(LOGIN, (server, handler, understood, buffer, synchronizer, sender) -> {
            if (!understood || buffer.readableBytes() != 1 || buffer.readUnsignedByte() != VERSION) {
                handler.disconnect(Component.translatableWithFallback("disconnect.copycat_replace.incompatible",
                        "This server requires a compatible version of Create: Copycat Replace! for Fabric."));
            }
        });
        ServerPlayNetworking.registerGlobalReceiver(KEY_STATE, (server, player, handler, buffer, sender) -> {
            if (buffer.readableBytes() != 2) return;
            int down = buffer.readUnsignedByte();
            int mode = buffer.readUnsignedByte();
            if (down > 1 || mode >= SelectionMode.values().length) return;
            server.execute(() -> {
                if (player.connection == handler && !player.hasDisconnected()) {
                    BulkReplaceMode.setState(player, down == 1, SelectionMode.fromId(mode));
                }
            });
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sendLimit(handler.player));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> BulkReplaceMode.disconnect(handler.player));
    }

    public static FriendlyByteBuf protocolVersion() {
        var buffer = PacketByteBufs.create();
        buffer.writeByte(VERSION);
        return buffer;
    }

    public static void broadcastLimit(MinecraftServer server) {
        server.getPlayerList().getPlayers().forEach(ModNetworking::sendLimit);
    }

    public static void sendLimit(ServerPlayer player) {
        if (ServerPlayNetworking.canSend(player, BLOCK_LIMIT)) {
            var buffer = PacketByteBufs.create();
            buffer.writeShort(CopycatReplaceConfig.MAX_CONNECTED_BLOCKS.get());
            ServerPlayNetworking.send(player, BLOCK_LIMIT, buffer);
        }
    }
}
