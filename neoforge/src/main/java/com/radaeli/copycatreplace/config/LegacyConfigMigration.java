package com.radaeli.copycatreplace.config;

import com.electronwill.nightconfig.toml.TomlParser;
import com.radaeli.copycatreplace.CopycatReplace;
import net.neoforged.fml.loading.FMLPaths;
import java.nio.file.*;
import java.io.IOException;

/** Seeds only missing server configuration files before the loader creates them. */
public final class LegacyConfigMigration {
    private LegacyConfigMigration() {}

    public static void prepare() {
        if (Files.exists(FMLPaths.GAMEDIR.get()
                .resolve(net.neoforged.fml.loading.FMLConfig.defaultConfigPath())
                .resolve("copycat_replace-server.toml"))) return;
        migrate(FMLPaths.CONFIGDIR.get().resolve("copycat_replace-common.toml"),
                FMLPaths.CONFIGDIR.get().resolve("copycat_replace-server.toml"));
    }

    public static boolean migrate(Path legacy, Path destination) {
        if (Files.exists(destination) || !Files.isRegularFile(legacy)) return false;
        try {
            var data = new TomlParser().parse(Files.readString(legacy));
            CopycatReplaceConfig.SPEC.correct(data);
            if (!CopycatReplaceConfig.SPEC.isCorrect(data)) return false;
            int limit = ((Number) data.get("max_connected_blocks")).intValue();
            Files.createDirectories(destination.toAbsolutePath().getParent());
            Files.writeString(destination, "#Maximum connected Copycat positions affected by one bulk action (1-256).\nmax_connected_blocks = " + limit + "\n",
                    StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            CopycatReplace.LOGGER.info("Ported the previous action limit to {}", destination);
            return true;
        } catch (FileAlreadyExistsException e) {
            return false;
        } catch (IOException | com.electronwill.nightconfig.core.io.ParsingException e) {
            CopycatReplace.LOGGER.warn("Could not import {}; using the loader's server configuration defaults", legacy, e);
            return false;
        }
    }
}
