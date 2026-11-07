package com.radaeli.copycatreplace.client.mixin;

import com.radaeli.copycatreplace.client.CopycatReplaceClientKeys;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
abstract class MouseHandlerMixin {
    @Inject(method = "onScroll(JDD)V", at = @At("HEAD"), cancellable = true)
    private void copycatReplace$scroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (CopycatReplaceClientKeys.scroll(window, vertical)) ci.cancel();
    }
}
