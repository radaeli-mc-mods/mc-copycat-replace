package com.radaeli.copycatreplace.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.radaeli.copycatreplace.CopycatReplace;
import com.radaeli.copycatreplace.network.BulkReplaceKeyStatePayload;
import com.radaeli.copycatreplace.network.SelectionMode;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/** Client-only key mapping and state synchronization. */
@EventBusSubscriber(modid = CopycatReplace.MOD_ID, value = Dist.CLIENT)
public final class CopycatReplaceClientKeys {
    static final KeyMapping BULK_REPLACE = new KeyMapping(
            "key.copycat_replace.bulk_replace",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_ALT,
            "key.categories.copycat_replace"
    );
    private static boolean lastSentDown;
    private static SelectionMode selection = SelectionMode.CONNECTED;
    private static SelectionMode lastSentSelection = SelectionMode.CONNECTED;

    public static SelectionMode selection() { return selection; }

    @SubscribeEvent
    public static void onScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null || mc.screen != null
                || !BULK_REPLACE.isDown() || event.getScrollDeltaY() == 0) return;
        selection = selection.cycle(event.getScrollDeltaY() > 0 ? 1 : -1);
        syncState(true);
        ConnectedBulkIndicator.refresh();
        event.setCanceled(true);
    }

    private CopycatReplaceClientKeys() {
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        InputConstants.Key pressedKey = InputConstants.getKey(event.getKey(), event.getScanCode());
        if (!pressedKey.equals(BULK_REPLACE.getKey())) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.getConnection() == null) {
            return;
        }

        boolean down = event.getAction() == GLFW.GLFW_PRESS && minecraft.screen == null;
        if (event.getAction() == GLFW.GLFW_PRESS || event.getAction() == GLFW.GLFW_RELEASE) {
            syncState(down);
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        boolean connected = minecraft.player != null && minecraft.getConnection() != null;
        boolean down = connected && minecraft.screen == null && BULK_REPLACE.isDown();

        if (connected) {
            syncState(down);
        } else if (!connected) {
            lastSentDown = false;
            selection = SelectionMode.CONNECTED;
            lastSentSelection = SelectionMode.CONNECTED;
        }
    }

    private static void syncState(boolean down) {
        if (down == lastSentDown && selection == lastSentSelection) {
            return;
        }
        PacketDistributor.sendToServer(new BulkReplaceKeyStatePayload(down, selection));
        lastSentDown = down;
        lastSentSelection = selection;
    }
}
