package com.radaeli.copycatreplace.tests.mixin;

import net.minecraft.gametest.framework.GameTestRegistry;
import net.minecraft.gametest.framework.TestFunction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;

@Mixin(GameTestRegistry.class)
abstract class GameTestRegistryMixin {
    @Inject(method = "getAllTestFunctions", at = @At("RETURN"), cancellable = true)
    private static void copycatReplace$selectOwnTests(CallbackInfoReturnable<Collection<TestFunction>> cir) {
        if (Boolean.getBoolean("copycat_replace.onlyOwnGameTests")) {
            cir.setReturnValue(cir.getReturnValue().stream()
                    .filter(test -> test.getTestName().startsWith("copycat_replace.")).toList());
        }
    }
}
