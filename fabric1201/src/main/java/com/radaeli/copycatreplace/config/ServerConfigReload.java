package com.radaeli.copycatreplace.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.file.FileWatcher;
import com.electronwill.nightconfig.core.io.ParsingException;
import com.electronwill.nightconfig.toml.TomlParser;
import com.radaeli.copycatreplace.CopycatReplace;
import com.radaeli.copycatreplace.network.ModNetworking;
import fuzs.forgeconfigapiport.api.config.v2.ForgeConfigPaths;
import fuzs.forgeconfigapiport.api.config.v2.ForgeConfigRegistry;
import fuzs.forgeconfigapiport.api.config.v2.ModConfigEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.config.ModConfig;

import java.io.IOException;
import java.nio.file.Files;

/** Applies stable server-file snapshots on the server thread. */
public final class ServerConfigReload {
    public static final String FILE_NAME = "copycat_replace-server.toml";

    private static ModConfig activeConfig;
    private static String lastContents;
    private static String pendingContents;
    private static int ticks;

    private ServerConfigReload() {}

    public static void register() {
        var paths = ForgeConfigPaths.INSTANCE;
        LegacyConfigMigration.migrate(paths.getCommonConfigDirectory().resolve("copycat_replace-common.toml"),
                paths.getDefaultConfigsDirectory().resolve(FILE_NAME));
        ModConfigEvents.loading(CopycatReplace.MOD_ID).register(ServerConfigReload::onLoading);
        ModConfigEvents.unloading(CopycatReplace.MOD_ID).register(config -> {
            if (config == activeConfig) activeConfig = null;
        });
        ForgeConfigRegistry.INSTANCE.register(CopycatReplace.MOD_ID, ModConfig.Type.SERVER,
                CopycatReplaceConfig.SPEC, FILE_NAME);
        ServerTickEvents.END_SERVER_TICK.register(ServerConfigReload::poll);
    }

    private static void onLoading(ModConfig config) {
        if (config.getSpec() != CopycatReplaceConfig.SPEC
                || !(config.getConfigData() instanceof CommentedFileConfig)) return;
        activeConfig = config;
        ticks = 0;
        pendingContents = null;
        lastContents = null;
        // Own the reload of this file so the loader's watcher cannot apply a partial write.
        FileWatcher.defaultInstance().removeWatch(config.getFullPath());
        try {
            lastContents = Files.readString(config.getFullPath());
        } catch (IOException e) {
            CopycatReplace.LOGGER.warn("Could not read {}; using the loaded action limit", config.getFullPath(), e);
        }
    }

    private static void poll(MinecraftServer server) {
        if (activeConfig == null || ++ticks < 20) return;
        ticks = 0;
        var path = activeConfig.getFullPath();
        final String contents;
        try {
            contents = Files.readString(path);
        } catch (IOException e) {
            pendingContents = null;
            return;
        }
        if (contents.equals(lastContents)) {
            pendingContents = null;
            return;
        }
        if (!contents.equals(pendingContents)) {
            pendingContents = contents;
            return;
        }
        lastContents = contents;
        pendingContents = null;
        try {
            var parsed = new TomlParser().parse(contents);
            if (!parsed.contains("max_connected_blocks")) throw new ParsingException("Missing max_connected_blocks");
            CopycatReplaceConfig.SPEC.correct(parsed);
            int maximum = ((Number) parsed.get("max_connected_blocks")).intValue();
            int previous = CopycatReplaceConfig.MAX_CONNECTED_BLOCKS.get();
            CopycatReplaceConfig.MAX_CONNECTED_BLOCKS.set(maximum);
            if (maximum != previous) ModNetworking.broadcastLimit(server);
        } catch (ParsingException e) {
            CopycatReplace.LOGGER.warn("Could not reload {}; keeping the current action limit", path, e);
        }
    }
}
