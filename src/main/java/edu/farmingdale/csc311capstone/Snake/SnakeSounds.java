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

    /** Black market volumes (0 to 1). The laugh is meant to sit under the music. */
    private static final double MARKET_MUSIC_VOLUME = 0.5;
    private static final double LAUGH_VOLUME = 0.7;

    private AudioClip appleSound;
    private AudioClip goldenApple;
    private AudioClip hammerBreak;
    private AudioClip whipSnap;
    private AudioClip rewind;
    private AudioClip notEnough;
    private AudioClip creepyLaugh;
    private AudioClip doorCreak;        // optional: skipped if door-creak.mp3 isn't in the folder
    private Media marketMusic;
    private MediaPlayer musicPlayer;
    private Media crash;
    private Media funeralMarch;
    private Media gameOverVoice;
    private Media win;
    private Media staticNoise;

    private boolean enabled;
    private MediaPlayer current;   // whichever long sound is playing right now
    private MediaPlayer staticPlayer;   // the TV static after a crash (plays alongside the others)

    SnakeSounds() {
        try {
            appleSound = new AudioClip(url("apple.wav"));
            goldenApple = new AudioClip(url("golden-apple.mp3"));
            hammerBreak = new AudioClip(url("hammer-break.mp3"));
            whipSnap = new AudioClip(url("whip-snap.mp3"));
            rewind = new AudioClip(url("rewind.mp3"));
            notEnough = new AudioClip(url("not-enough.mp3"));
            creepyLaugh = new AudioClip(url("creepy-laugh.mp3"));
            marketMusic = new Media(url("market-music.mp3"));
            if (SnakeSounds.class.getResource(FOLDER + "door-creak.mp3") != null) {
                doorCreak = new AudioClip(url("door-creak.mp3"));
            }
            crash = new Media(url("crash.mp3"));
            funeralMarch = new Media(url("funeral-march.mp3"));
            gameOverVoice = new Media(url("game-over-voice.mp3"));
            win = new Media(url("win.mp3"));
            staticNoise = new Media(url("static.mp3"));
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

    // ---------------------------------------------------------------- black market items

    /** The hammer breaking, the whip snapping, or the clock turning time back. */
    void playItem(Event event) {
        if (!enabled) {
            return;
        }
        switch (event) {
            case HAMMER_BROKE -> hammerBreak.play();
            case WHIP_SNAPPED -> whipSnap.play();
            case REWOUND -> {
                stopAll();               // cut off the crash sound: time is going backwards
                rewind.play();
            }
            default -> { }
        }
    }

    // ---------------------------------------------------------------- black market

    /** Starts the black market music, looping until stopMarketMusic(). */
    void startMarketMusic() {
        if (!enabled) {
            return;
        }
        stopMarketMusic();
        try {
            musicPlayer = new MediaPlayer(marketMusic);
            musicPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            musicPlayer.setVolume(MARKET_MUSIC_VOLUME);
            musicPlayer.play();
        } catch (RuntimeException e) {
            System.err.println("Couldn't play the black market music: " + e.getMessage());
            musicPlayer = null;
        }
    }

    void stopMarketMusic() {
        if (musicPlayer != null) {
            musicPlayer.stop();
            musicPlayer.dispose();
            musicPlayer = null;
        }
    }

    /** Bought something: a creepy laugh, once, under the music. */
    void playBought() {
        if (enabled) {
            creepyLaugh.play(LAUGH_VOLUME);
        }
    }

    /** Tried to buy without enough apples. */
    void playNotEnough() {
        if (enabled) {
            notEnough.play();
        }
    }

    /** The snake reached the door. */
    void playDoor() {
        if (enabled && doorCreak != null) {
            doorCreak.play();
        }
    }

    // ---------------------------------------------------------------- the crash sequence
    // SnakeController runs the timing: crash sound, then static fading in and out,
    // then the game-over music. These are the pieces it uses.

    /** Just the crash sound. */
    void playCrash() {
        if (enabled) {
            playInOrder(crash);
        }
    }

    /** Starts the TV static silently; raise it with setStaticVolume. */
    void startStatic() {
        if (!enabled) {
            return;
        }
        stopStatic();
        try {
            staticPlayer = new MediaPlayer(staticNoise);
            staticPlayer.setVolume(0);
            staticPlayer.play();
        } catch (RuntimeException e) {
            System.err.println("Couldn't play the static sound: " + e.getMessage());
            staticPlayer = null;
        }
    }

    /** 0 = silent, 1 = full volume. Called every frame to fade the static in and out. */
    void setStaticVolume(double volume) {
        if (staticPlayer != null) {
            staticPlayer.setVolume(Math.max(0, Math.min(1, volume)));
        }
    }

    void stopStatic() {
        if (staticPlayer != null) {
            staticPlayer.stop();
            staticPlayer.dispose();
            staticPlayer = null;
        }
    }

    /** The funeral march, then the "game over" voice. */
    void playGameOverMusic() {
        if (enabled) {
            playInOrder(funeralMarch, gameOverVoice);
        }
    }

    /** Silences everything, e.g. when starting a new game or leaving to the menu. */
    void stopAll() {
        if (!enabled) {
            return;
        }
        appleSound.stop();
        goldenApple.stop();
        hammerBreak.stop();
        whipSnap.stop();
        creepyLaugh.stop();
        stopStatic();
        stopMarketMusic();
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
