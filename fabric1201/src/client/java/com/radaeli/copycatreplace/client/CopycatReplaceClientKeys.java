package com.radaeli.copycatreplace.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.radaeli.copycatreplace.network.ModNetworking;
import com.radaeli.copycatreplace.network.SelectionMode;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public final class CopycatReplaceClientKeys {
    static final KeyMapping BULK_REPLACE = new KeyMapping("key.copycat_replace.bulk_replace",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_ALT, "key.categories.copycat_replace");
    private static SelectionMode selection = SelectionMode.CONNECTED;
    private static SelectionMode lastSentSelection = SelectionMode.CONNECTED;
    private static boolean lastSentDown;

    private CopycatReplaceClientKeys() {}

    static void register() {
        KeyBindingHelper.registerKeyBinding(BULK_REPLACE);
    }

    public static SelectionMode selection() {
        return selection;
    }

    static boolean isConnected() {
        return Minecraft.getInstance().getConnection() != null && ClientPlayNetworking.canSend(ModNetworking.KEY_STATE);
    }

    static boolean isActive(Minecraft client) {
        return client.player != null && client.screen == null && client.getOverlay() == null
                && isConnected() && BULK_REPLACE.isDown();
    }

    public static boolean scroll(long window, double delta) {
        Minecraft client = Minecraft.getInstance();
        if (delta == 0 || window != client.getWindow().getWindow() || !isActive(client)) return false;
        selection = selection.cycle(delta > 0 ? 1 : -1);
        sync(true);
        ConnectedBulkIndicator.refresh();
        return true;
    }

    static void sync(boolean down) {
        if ((down == lastSentDown && selection == lastSentSelection) || !isConnected()) return;
        var buffer = PacketByteBufs.create();
        buffer.writeBoolean(down);
        buffer.writeByte(selection.ordinal());
        ClientPlayNetworking.send(ModNetworking.KEY_STATE, buffer);
        lastSentDown = down;
        lastSentSelection = selection;
    }

    static void disconnect() {
        selection = SelectionMode.CONNECTED;
        lastSentSelection = SelectionMode.CONNECTED;
        lastSentDown = false;
    }
}
