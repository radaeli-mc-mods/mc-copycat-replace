package com.radaeli.copycatreplace.tests;

import com.radaeli.copycatreplace.config.CopycatReplaceConfig;
import net.minecraft.gametest.framework.TestFunction;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import static com.radaeli.copycatreplace.tests.ReplacementScenarios.check;

/** Exercises the real loader file watcher, including cache invalidation and restoration. */
final class ServerConfigScenarios {
    static List<TestFunction> generate() {
        // NeoForge's GameTestServer advances ticks without real-time pacing. File
        // watching is wall-clock based, so a normal gameplay tick budget is too short.
        return List.of(new TestFunction("server_config", "copycat_replace.server_config_reload",
                "copycat_replace:empty", 100_000, 0, true, h -> {
            Path path = TestPlatform.serverConfigPath();
            check(path.getFileName().toString().equals("copycat_replace-server.toml"), "Expected a registered SERVER configuration");
            int previous = CopycatReplaceConfig.MAX_CONNECTED_BLOCKS.get();
            System.out.println("COPYCAT SERVER CONFIG: " + path.toAbsolutePath().normalize() + ", initial=" + previous);
            final String original;
            long[] restoredAt = {0};
            try { original = Files.readString(path); }
            catch (Exception e) { throw new RuntimeException(e); }
            h.startSequence().thenIdle(5)
                    .thenExecute(() -> write(path, "max_connected_blocks = " + (previous == 1 ? 3 : 1) + "\n"))
                    .thenWaitUntil(() -> h.assertTrue(CopycatReplaceConfig.MAX_CONNECTED_BLOCKS.get() == (previous == 1 ? 3 : 1), "Waiting for SERVER file reload"))
                    .thenExecute(() -> write(path, "max_connected_blocks = 256\n"))
                    .thenWaitUntil(() -> h.assertTrue(CopycatReplaceConfig.MAX_CONNECTED_BLOCKS.get() == 256, "Waiting for upper bound reload"))
                    .thenExecute(() -> write(path, "max_connected_blocks = 0\n"))
                    .thenWaitUntil(() -> h.assertTrue(CopycatReplaceConfig.MAX_CONNECTED_BLOCKS.get() == 1, "Waiting for invalid value clamping"))
                    .thenExecute(() -> write(path, "max_connected_blocks = " + (previous == 2 ? 3 : 2) + "\n"))
                    .thenWaitUntil(() -> h.assertTrue(CopycatReplaceConfig.MAX_CONNECTED_BLOCKS.get() == (previous == 2 ? 3 : 2), "Waiting for pre-restoration value"))
                    .thenExecute(() -> write(path, original))
                    .thenWaitUntil(() -> h.assertTrue(CopycatReplaceConfig.MAX_CONNECTED_BLOCKS.get() == previous, "Waiting for SERVER file restoration"))
                    .thenExecute(() -> restoredAt[0] = System.nanoTime())
                    .thenWaitUntil(() -> h.assertTrue(System.nanoTime() - restoredAt[0] >= 1_000_000_000L, "Waiting for queued file notifications to settle"))
                    .thenSucceed();
        }));
    }

    private static void write(Path path, String content) {
        try { Files.writeString(path, content); }
        catch (Exception e) { throw new RuntimeException(e); }
    }
}
