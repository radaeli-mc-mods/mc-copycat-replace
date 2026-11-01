package com.radaeli.copycatreplace.tests;

import com.electronwill.nightconfig.toml.TomlParser;
import com.radaeli.copycatreplace.config.LegacyConfigMigration;
import java.nio.file.*;
import java.util.*;
import net.minecraft.gametest.framework.TestFunction;
import static com.radaeli.copycatreplace.tests.ReplacementScenarios.*;

final class MigrationScenarios {
    static List<TestFunction> generate() {
        return List.of(test("automatic_config_migration", h -> {
            try {
                Path dir = Files.createTempDirectory(Path.of("."), "build-migration-test-");
                Path legacy = dir.resolve("common.toml"), target = dir.resolve("server.toml");
                check(!LegacyConfigMigration.migrate(legacy, target) && !Files.exists(target), "Missing COMMON must use normal defaults");
                for (int value : List.of(1, 17, 64, 256)) {
                    target = dir.resolve("server-" + value + ".toml");
                    String original = "max_connected_blocks = " + value + "\n";
                    Files.writeString(legacy, original);
                    check(LegacyConfigMigration.migrate(legacy, target), "Expected automatic import");
                    check(((Number)new TomlParser().parse(Files.readString(target)).get("max_connected_blocks")).intValue() == value, "Lost previous limit");
                    check(Files.readString(legacy).equals(original), "COMMON was modified");
                    String existing = Files.readString(target);
                    Files.writeString(legacy, "max_connected_blocks = 9\n");
                    check(!LegacyConfigMigration.migrate(legacy, target) && Files.readString(target).equals(existing), "Existing SERVER/template overwritten");
                }
                target = dir.resolve("invalid.toml");
                Files.writeString(legacy, "max_connected_blocks = [\n");
                check(!LegacyConfigMigration.migrate(legacy, target) && !Files.exists(target), "Malformed COMMON produced SERVER");
                Files.writeString(legacy, "max_connected_blocks = 0\n");
                check(LegacyConfigMigration.migrate(legacy, target), "Range correction failed");
                check(((Number)new TomlParser().parse(Files.readString(target)).get("max_connected_blocks")).intValue() == 1, "Expected loader range correction");
            } catch (java.io.IOException e) { throw new RuntimeException(e); }
        }));
    }
}
