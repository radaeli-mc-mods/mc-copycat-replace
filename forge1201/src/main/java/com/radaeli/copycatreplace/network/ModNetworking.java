package com.radaeli.copycatreplace.network;

import com.radaeli.copycatreplace.CopycatReplace;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/** The channel and both directions are registered on client and dedicated server. */
public final class ModNetworking {
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(CopycatReplace.MOD_ID, "main"), () -> "2", "2"::equals, "2"::equals);
    private ModNetworking() {}
    public static void register() {
        CHANNEL.messageBuilder(BulkReplaceKeyStatePayload.class, 0, NetworkDirection.PLAY_TO_SERVER)
                .encoder(BulkReplaceKeyStatePayload::encode).decoder(BulkReplaceKeyStatePayload::decode)
                .consumerMainThread(BulkReplaceKeyStatePayload::handle).add();
        CHANNEL.messageBuilder(ConnectedBlockLimitPayload.class, 1, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ConnectedBlockLimitPayload::encode).decoder(ConnectedBlockLimitPayload::decode)
                .consumerMainThread(ConnectedBlockLimitPayload::handle).add();
    }
}
