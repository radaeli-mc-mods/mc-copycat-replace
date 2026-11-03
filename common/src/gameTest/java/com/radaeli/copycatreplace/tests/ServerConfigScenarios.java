package com.radaeli.copycatreplace.tests;

import com.radaeli.copycatreplace.config.CopycatReplaceConfig;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestSequence;
import net.minecraft.gametest.framework.TestFunction;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static com.radaeli.copycatreplace.tests.ReplacementScenarios.check;

/** Exercises the real loader file watcher, including cache invalidation and restoration. */
final class ServerConfigScenarios {
    private static final Pattern LIMIT = Pattern.compile("(?m)^\\h*max_connected_blocks\\h*=\\h*(\\d+)\\h*(?:#.*)?$");

    static List<TestFunction> generate() {
        // NeoForge's GameTestServer advances ticks without real-time pacing. File
        // watching is wall-clock based, so a normal gameplay tick budget is too short.
        return List.of(new TestFunction("server_config", "copycat_replace.server_config_reload",
                "copycat_replace:empty", 400_000, 0, true, h -> {
            Path path = TestPlatform.serverConfigPath();
            check(path.getFileName().toString().equals("copycat_replace-server.toml"), "Expected a registered SERVER configuration");
            final String original;
            try { original = Files.readString(path); }
            catch (Exception e) { throw new RuntimeException(e); }
            Matcher matcher = LIMIT.matcher(original);
            check(matcher.find(), "SERVER config is missing max_connected_blocks");
            int persistedLimit = Integer.parseInt(matcher.group(1));
            String prefix = original.substring(0, matcher.start(1));
            String suffix = original.substring(matcher.end(1));
            int first = persistedLimit == 1 ? 3 : 1;
            int beforeRestore = persistedLimit == 2 ? 3 : 2;
            GameTestSequence sequence = h.startSequence().thenIdle(5)
                    .thenExecute(() -> write(path, prefix + first + suffix));
            waitForStableValue(sequence, h, first, "SERVER file reload");
            sequence.thenExecute(() -> write(path, prefix + 256 + suffix));
            waitForStableValue(sequence, h, 256, "upper bound reload");
            sequence.thenExecute(() -> write(path, prefix + "0" + suffix));
            // Forge can restore the default, while NeoForge can clamp to the minimum.
            waitForStableValue(sequence, h, 1, 64, "invalid value correction");
            sequence.thenExecute(() -> write(path, prefix + beforeRestore + suffix));
            waitForStableValue(sequence, h, beforeRestore, "pre-restoration value");
            sequence.thenExecute(() -> write(path, original));
            waitForStableValue(sequence, h, persistedLimit, "SERVER file restoration");
            sequence.thenSucceed();
        }));
    }

    private static void waitForStableValue(GameTestSequence sequence, GameTestHelper helper,
                                           int expected, String stage) {
        waitForStableValue(sequence, helper, expected, expected, stage);
    }

    private static void waitForStableValue(GameTestSequence sequence, GameTestHelper helper,
                                           int expected, int alternative, String stage) {
        long[] stableSince = {0};
        int[] lastValue = {-1};
        sequence.thenWaitUntil(() -> {
            long now = System.nanoTime();
            int actual = CopycatReplaceConfig.MAX_CONNECTED_BLOCKS.get();
            if (actual != lastValue[0]) {
                lastValue[0] = actual;
                stableSince[0] = 0;
            }
            helper.assertTrue(actual == expected || actual == alternative,
                    "Waiting for " + stage + ": expected " + expected + " or " + alternative + ", got " + actual);
            if (stableSince[0] == 0) stableSince[0] = now;
            helper.assertTrue(now - stableSince[0] >= 750_000_000L,
                    "Waiting for " + stage + " to remain stable");
        });
    }

    private static void write(Path path, String content) {
        Path temporary = path.resolveSibling(path.getFileName() + "." + UUID.randomUUID() + ".tmp");
        try {
            Files.writeString(temporary, content);
            try { Files.move(temporary, path, java.nio.file.StandardCopyOption.ATOMIC_MOVE, java.nio.file.StandardCopyOption.REPLACE_EXISTING); }
            catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, path, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        }
        catch (Exception e) { throw new RuntimeException(e); }
    }
}
