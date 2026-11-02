package com.radaeli.copycatreplace.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/** Only carries a momentary modifier; the server chooses and validates action targets. */
public record BulkReplaceKeyStatePayload(boolean down, SelectionMode mode) {
    public BulkReplaceKeyStatePayload(boolean down) { this(down, SelectionMode.CONNECTED); }
    public static void encode(BulkReplaceKeyStatePayload message, FriendlyByteBuf buffer) { buffer.writeBoolean(message.down); buffer.writeVarInt(message.mode.ordinal()); }
    public static BulkReplaceKeyStatePayload decode(FriendlyByteBuf buffer) { return new BulkReplaceKeyStatePayload(buffer.readBoolean(), SelectionMode.fromId(buffer.readVarInt())); }
    public static void handle(BulkReplaceKeyStatePayload message, Supplier<NetworkEvent.Context> context) {
        var sender = context.get().getSender();
        if (sender != null) BulkReplaceMode.setState(sender, message.down, message.mode);
        context.get().setPacketHandled(true);
    }
}
