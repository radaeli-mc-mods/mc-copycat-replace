package com.radaeli.copycatreplace.network;

import com.radaeli.copycatreplace.CopycatReplace;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Reports the client's held bulk-replacement key state to the server. */
public record BulkReplaceKeyStatePayload(boolean down, SelectionMode mode) implements CustomPacketPayload {
    public BulkReplaceKeyStatePayload(boolean down) { this(down, SelectionMode.CONNECTED); }
    public static final Type<BulkReplaceKeyStatePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CopycatReplace.MOD_ID, "bulk_replace_key_state")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, BulkReplaceKeyStatePayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL,
                    BulkReplaceKeyStatePayload::down,
                    ByteBufCodecs.VAR_INT.map(SelectionMode::fromId, SelectionMode::ordinal),
                    BulkReplaceKeyStatePayload::mode,
                    BulkReplaceKeyStatePayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(BulkReplaceKeyStatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> BulkReplaceMode.setState(context.player(), payload.down(), payload.mode()));
    }
}
