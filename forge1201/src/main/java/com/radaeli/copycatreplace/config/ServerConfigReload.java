package com.radaeli.copycatreplace.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.toml.TomlParser;
import com.radaeli.copycatreplace.CopycatReplace;
import com.radaeli.copycatreplace.network.ConnectedBlockLimitSync;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.config.ConfigFileTypeHandler;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import java.io.IOException;
import java.nio.file.Files;

/**
 * NightConfig 3.6.4 ignores coalesced ENTRY_MODIFY events (count > 1), common on
 * Windows. Poll only our active server file and reuse Forge's spec/cache rules.
 */
public final class ServerConfigReload {
    private static volatile ModConfig activeConfig;
    private static String appliedContents;
    private static String pendingContents;
    private static int ticks;

    private ServerConfigReload() {}

    public static void onLoading(ModConfigEvent.Loading event) {
        if (event.getConfig().getType() != ModConfig.Type.SERVER
                || event.getConfig().getSpec() != CopycatReplaceConfig.SPEC
                || !(event.getConfig().getConfigData() instanceof CommentedFileConfig)) return;
        activeConfig = event.getConfig();
        ticks = 0;
        pendingContents = null;
        try { appliedContents = Files.readString(activeConfig.getFullPath()); }
        catch (IOException e) { appliedContents = null; }
    }

    public static void onUnloading(ModConfigEvent.Unloading event) {
        if (event.getConfig() != activeConfig) return;
        activeConfig = null;
        appliedContents = pendingContents = null;
        ticks = 0;
    }

    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || activeConfig == null || ++ticks < 20) return;
        ticks = 0;
        ModConfig config = activeConfig;
        try {
            String contents = Files.readString(config.getFullPath());
            if (contents.equals(appliedContents)) { pendingContents = null; return; }
            // Require the same contents on two polls to avoid loading a partial save.
            if (!contents.equals(pendingContents)) { pendingContents = contents; return; }
            // Parse separately first: malformed input must not clear the live config.
            new TomlParser().parse(contents);
            var data = (CommentedFileConfig) config.getConfigData();
            int previous = CopycatReplaceConfig.MAX_CONNECTED_BLOCKS.get();
            data.load();
            if (!CopycatReplaceConfig.SPEC.isCorrect(data)) {
                ConfigFileTypeHandler.backUpConfig(data);
                CopycatReplaceConfig.SPEC.correct(data);
                data.save();
            }
            CopycatReplaceConfig.SPEC.afterReload();
            appliedContents = Files.readString(config.getFullPath());
            pendingContents = null;
            if (CopycatReplaceConfig.MAX_CONNECTED_BLOCKS.get() != previous) {
                ConnectedBlockLimitSync.sendToAll(event.getServer());
            }
        } catch (IOException | com.electronwill.nightconfig.core.io.ParsingException e) {
            // Retry only after the user changes the file again; retain the live value.
            appliedContents = pendingContents;
            pendingContents = null;
            CopycatReplace.LOGGER.warn("Could not reload {}; keeping the current action limit", config.getFullPath(), e);
        }
    }
}
