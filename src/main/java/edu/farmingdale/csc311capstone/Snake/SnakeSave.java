package edu.farmingdale.csc311capstone.Snake;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * The player's Snake progress, saved in a small text file on this computer so it's still
 * there next time the game opens.
 *
 * Saved: apples to spend in the black market, the chance of finding the black market
 * (from hearts eaten in the current game), and the items owned.
 *
 * The file lives in the user's home folder: ~/.csc311-capstone/snake-save.properties
 */
final class SnakeSave {

    /** Every heart collected adds this much to the chance of finding the black market. */
    static final int HEART_CHANCE_BONUS = 15;

    private final Path file;

    int apples;            // apples collected and not yet spent
    int secretChance;      // percent, 0 to 100 (starts over at 0 every game)
    boolean clock;         // owns a clock (it's used up when it saves a crash)
    int hammerUses;        // 0 = no hammer
    int whipGrabs;         // 0 = no whip

    private SnakeSave(Path file) {
        this.file = file;
    }

    /** The save file in the user's home folder. */
    static SnakeSave load() {
        return load(Path.of(System.getProperty("user.home"), ".csc311-capstone", "snake-save.properties"));
    }

    /** Reads the save at 'file'. A missing or damaged file just means starting fresh. */
    static SnakeSave load(Path file) {
        SnakeSave save = new SnakeSave(file);
        if (!Files.exists(file)) {
            return save;
        }
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(file)) {
            p.load(in);
        } catch (IOException e) {
            System.err.println("Couldn't read the Snake save, starting fresh: " + e.getMessage());
            return save;
        }
        save.apples = Math.max(0, number(p, "apples"));
        save.secretChance = Math.max(0, Math.min(100, number(p, "secretChance")));
        save.clock = Boolean.parseBoolean(p.getProperty("clock", "false"));
        save.hammerUses = Math.max(0, number(p, "hammerUses"));
        save.whipGrabs = Math.max(0, number(p, "whipGrabs"));
        return save;
    }

    /** Writes everything to the save file. Problems are printed, never thrown, so the game keeps going. */
    void save() {
        Properties p = new Properties();
        p.setProperty("apples", String.valueOf(apples));
        p.setProperty("secretChance", String.valueOf(secretChance));
        p.setProperty("clock", String.valueOf(clock));
        p.setProperty("hammerUses", String.valueOf(hammerUses));
        p.setProperty("whipGrabs", String.valueOf(whipGrabs));
        try {
            Files.createDirectories(file.getParent());
            try (OutputStream out = Files.newOutputStream(file)) {
                p.store(out, "Snake save - apples, secret chance and black market items");
            }
        } catch (IOException e) {
            System.err.println("Couldn't save Snake progress: " + e.getMessage());
        }
    }

    /** A heart was collected: the black market gets more likely. */
    void addHeart() {
        secretChance = Math.min(100, secretChance + HEART_CHANCE_BONUS);
    }

    /** Rolls the dice after a crash: true means the player finds the black market. */
    boolean rollForMarket(java.util.Random random) {
        return secretChance > 0 && random.nextInt(100) < secretChance;
    }

    private static int number(Properties p, String key) {
        try {
            return Integer.parseInt(p.getProperty(key, "0").trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
