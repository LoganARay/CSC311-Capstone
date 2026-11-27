package edu.farmingdale.csc311capstone.Pong;

import javafx.scene.media.AudioClip;
import java.util.HashMap;
import java.util.Map;

/** Short original synthesized cues. Missing resources/devices degrade to silence. */
final class PongAudio {
    private final Map<String, AudioClip> clips = new HashMap<>();
    private boolean muted;
    private long lastCue;
    void play(String name) {
        if (muted || System.nanoTime() - lastCue < 45_000_000) return;
        lastCue = System.nanoTime();
        try {
            if (!clips.containsKey(name)) {
                var url = getClass().getResource("/edu/farmingdale/csc311capstone/PongSounds/" + name + ".wav");
                clips.put(name, url == null ? null : new AudioClip(url.toExternalForm()));
            }
            AudioClip clip = clips.get(name);
            if (clip != null) { stop(); clip.play(.22); }
        } catch (RuntimeException | LinkageError ignored) { clips.put(name, null); }
    }
    void toggleMute() { muted = !muted; stop(); }
    boolean isMuted() { return muted; }
    void stop() { clips.values().forEach(c -> { if (c != null) c.stop(); }); }
}
