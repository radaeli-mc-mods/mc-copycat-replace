package com.radaeli.copycatreplace.client;

import com.radaeli.copycatreplace.config.CopycatReplaceConfig;
import com.radaeli.copycatreplace.interaction.CopycatInteraction;
import com.radaeli.copycatreplace.network.ModNetworking;
import net.createmod.ponder.foundation.PonderIndex;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionResult;

import java.util.concurrent.CompletableFuture;

public final class CopycatReplaceClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        CopycatReplaceClientKeys.register();
        PonderIndex.addPlugin(new CopycatReplacePonder());

        ClientLoginNetworking.registerGlobalReceiver(ModNetworking.LOGIN, (client, handler, buffer, listener) -> {
            if (buffer.readableBytes() != 1 || buffer.readUnsignedByte() != ModNetworking.VERSION) {
                return CompletableFuture.completedFuture(null);
            }
            return CompletableFuture.completedFuture(ModNetworking.protocolVersion());
        });
        ClientPlayNetworking.registerGlobalReceiver(ModNetworking.BLOCK_LIMIT, (client, handler, buffer, sender) -> {
            if (buffer.readableBytes() != 2) return;
            int maximum = buffer.readUnsignedShort();
            if (maximum < 1 || maximum > CopycatReplaceConfig.MAX_CONNECTED_BLOCKS_LIMIT) return;
            client.execute(() -> {
                if (client.getConnection() == handler) ConnectedBulkIndicator.setServerMaximum(maximum);
            });
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            CopycatReplaceClientKeys.disconnect();
            ConnectedBulkIndicator.disconnect();
        });

        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (!level.isClientSide() || !CopycatReplaceClientKeys.isConnected()) return InteractionResult.PASS;
            boolean down = CopycatReplaceClientKeys.isActive(Minecraft.getInstance());
            // The modifier and the vanilla click travel on the same connection in this order.
            CopycatReplaceClientKeys.sync(down);
            return CopycatInteraction.find(player, level, hand, hit, down) != null
                    ? InteractionResult.SUCCESS : InteractionResult.PASS;
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            boolean active = CopycatReplaceClientKeys.isActive(client);
            CopycatReplaceClientKeys.sync(active);
            ConnectedBulkIndicator.tick(client, active, CopycatReplaceClientKeys.selection());
        });
        HudRenderCallback.EVENT.register((graphics, partialTick) -> {
            Minecraft client = Minecraft.getInstance();
            ConnectedBulkIndicator.render(client, graphics, CopycatReplaceClientKeys.isActive(client),
                    CopycatReplaceClientKeys.selection());
        });
    }
}
