package edu.farmingdale.csc311capstone.Snake;

import edu.farmingdale.csc311capstone.Snake.SnakeMechanics.Event;
import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.net.URL;

/**
 * Plays Snake's sound effects.
 *
 * Short sounds that can overlap (apple crunch, golden apple coin) use AudioClip.
 * Longer sounds that have to play one after another (crash, funeral march,
 * "game over" voice, win music) use MediaPlayer so we know when each one ends.
 *
 * If the sounds can't be loaded, the game keeps working, just silently.
 */
final class SnakeSounds {

    private static final String FOLDER = "/edu/farmingdale/csc311capstone/SnakeSounds/";

    private AudioClip appleSound;
    private AudioClip goldenApple;
    private Media crash;
    private Media funeralMarch;
    private Media gameOverVoice;
    private Media win;

    private boolean enabled;
    private MediaPlayer current;   // whichever long sound is playing right now

    SnakeSounds() {
        try {
            appleSound = new AudioClip(url("apple.wav"));
            goldenApple = new AudioClip(url("golden-apple.mp3"));
            crash = new Media(url("crash.mp3"));
            funeralMarch = new Media(url("funeral-march.mp3"));
            gameOverVoice = new Media(url("game-over-voice.mp3"));
            win = new Media(url("win.mp3"));
            enabled = true;
        } catch (RuntimeException e) {
            System.err.println("Snake sounds are off: " + e.getMessage());
            enabled = false;
        }
    }

    /** Plays the right sound for something that happened in the game. */
    void play(Event event) {
        if (!enabled) {
            return;
        }
        switch (event) {
            case ATE_APPLE -> appleSound.play();
            case ATE_GOLDEN_APPLE -> goldenApple.play();
            case CRASHED -> playInOrder(crash, funeralMarch, gameOverVoice);
            case TIME_UP -> playInOrder(funeralMarch, gameOverVoice);
            case WON -> playInOrder(win);
        }
    }

    /**
     * Like play(), but a crash is just the crash sound, without the funeral march and
     * "game over" voice. Used by the demo on the start screen. Time running out makes no sound.
     */
    void playShort(Event event) {
        if (!enabled) {
            return;
        }
        switch (event) {
            case CRASHED -> playInOrder(crash);
            case TIME_UP -> { }
            default -> play(event);
        }
    }

    /** Silences everything, e.g. when starting a new game or leaving to the menu. */
    void stopAll() {
        if (!enabled) {
            return;
        }
        appleSound.stop();
        goldenApple.stop();
        if (current != null) {
            MediaPlayer player = current;
            current = null;
            player.stop();
            player.dispose();
        }
    }

    private void playInOrder(Media... sounds) {
        stopAll();
        playFrom(sounds, 0);
    }

    private void playFrom(Media[] sounds, int index) {
        if (index >= sounds.length) {
            current = null;
            return;
        }
        MediaPlayer player;
        try {
            player = new MediaPlayer(sounds[index]);
        } catch (RuntimeException e) {
            System.err.println("Couldn't play a Snake sound: " + e.getMessage());
            current = null;
            return;
        }
        current = player;
        player.setOnEndOfMedia(() -> {
            player.dispose();
            if (current == player) {          // still ours, not stopped by a new game
                playFrom(sounds, index + 1);
            }
        });
        player.play();
    }

    private static String url(String fileName) {
        URL url = SnakeSounds.class.getResource(FOLDER + fileName);
        if (url == null) {
            throw new IllegalStateException("missing " + FOLDER + fileName);
        }
        return url.toExternalForm();
    }
}
