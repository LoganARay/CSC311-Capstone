package edu.farmingdale.csc311capstone.Snake;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SnakeSaveTest {

    private static Path tempFile() throws IOException {
        return Files.createTempDirectory("snake-save-test").resolve("save.properties");
    }

    @Test
    void savedProgressComesBack() throws IOException {
        Path file = tempFile();
        SnakeSave save = SnakeSave.load(file);
        save.apples = 250;
        save.secretChance = 45;
        save.clock = true;
        save.hammerUses = 7;
        save.whipGrabs = 63;
        save.save();

        SnakeSave again = SnakeSave.load(file);
        assertEquals(250, again.apples);
        assertEquals(45, again.secretChance);
        assertTrue(again.clock);
        assertEquals(7, again.hammerUses);
        assertEquals(63, again.whipGrabs);
    }

    @Test
    void noSaveFileMeansStartingFresh() throws IOException {
        SnakeSave save = SnakeSave.load(tempFile());
        assertEquals(0, save.apples);
        assertEquals(0, save.secretChance);
    }

    @Test
    void damagedValuesAreIgnored() throws IOException {
        Path file = tempFile();
        Files.writeString(file, "apples=lots\nsecretChance=900\nhammerUses=-4\n");
        SnakeSave save = SnakeSave.load(file);
        assertEquals(0, save.apples);
        assertEquals(100, save.secretChance);
        assertEquals(0, save.hammerUses);
    }

    @Test
    void heartsAdd10PercentUpTo100() throws IOException {
        SnakeSave save = SnakeSave.load(tempFile());
        save.addHeart();
        assertEquals(10, save.secretChance);
        for (int i = 0; i < 10; i++) {
            save.addHeart();
        }
        assertEquals(100, save.secretChance);
    }

    @Test
    void zeroPercentNeverFindsTheMarketAndHundredAlwaysDoes() throws IOException {
        SnakeSave save = SnakeSave.load(tempFile());
        Random random = new Random(1);
        for (int i = 0; i < 1000; i++) {
            assertFalse(save.rollForMarket(random));
        }
        save.secretChance = 100;
        for (int i = 0; i < 1000; i++) {
            assertTrue(save.rollForMarket(random));
        }
    }

}
