package com.radaeli.copycatreplace.config;

import net.minecraftforge.common.ForgeConfigSpec;

/** Server-owned action settings; the loader supports per-world configuration. */
public final class CopycatReplaceConfig {
    public static final int MAX_CONNECTED_BLOCKS_LIMIT = 256;
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue MAX_CONNECTED_BLOCKS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        MAX_CONNECTED_BLOCKS = builder
                .comment("Maximum connected Copycat positions affected by one bulk action (1-256).")
                .translation("copycat_replace.config.max_connected_blocks")
                .defineInRange("max_connected_blocks", 64, 1, MAX_CONNECTED_BLOCKS_LIMIT);
        SPEC = builder.build();
    }

    private CopycatReplaceConfig() {
    }
}
