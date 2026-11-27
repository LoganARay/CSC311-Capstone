package edu.farmingdale.csc311capstone.dashboard;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.nio.file.*;
import java.util.concurrent.TimeUnit;

/** Requires a desktop JavaFX environment, as does the application itself. */
class DashboardSceneTest {
    @Test
    void allScreensInteractionsAndGameRoundTrips() throws Exception {
        Path log = Path.of(System.getProperty("java.io.tmpdir"), "arcade-redesign-scene-probe.log");
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        String cp =
                System.getProperty(
                        "surefire.test.class.path", System.getProperty("java.class.path"));
        Process process =
                new ProcessBuilder(
                                java,
                                "--enable-native-access=ALL-UNNAMED",
                                "-cp",
                                cp,
                                DashboardSceneProbe.class.getName())
                        .redirectErrorStream(true)
                        .redirectOutput(log.toFile())
                        .start();
        boolean done = process.waitFor(90, TimeUnit.SECONDS);
        if (!done) process.destroyForcibly();
        assertTrue(done, "JavaFX probe timed out: " + log);
        assertEquals(
                0,
                process.exitValue(),
                () -> {
                    try {
                        return Files.readString(log);
                    } catch (Exception e) {
                        return e.toString();
                    }
                });
        assertTrue(Files.readString(log).contains("SCENE PROBE PASSED"));
    }
}
