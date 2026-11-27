package edu.farmingdale.csc311capstone.Pacman;

import javafx.scene.media.AudioClip;
import java.util.HashMap;
import java.util.Map;

/** Owns short original cues, with no streaming players or background threads to leak. */
final class PacmanAudio {
    private final Map<String,AudioClip> clips=new HashMap<>();
    private boolean muted;
    private long lastCue;
    void play(String name){
        long now=System.nanoTime();
        boolean priority=name.equals("gameover")||name.equals("win")||name.equals("life")||name.equals("level");
        if(muted||(!priority&&now-lastCue<85_000_000))return;
        lastCue=now;
        try{
            if(!clips.containsKey(name)){
                var url=getClass().getResource("/edu/farmingdale/csc311capstone/PacmanSounds/"+name+".wav");
                clips.put(name,url==null?null:new AudioClip(url.toExternalForm()));
            }
            AudioClip clip=clips.get(name);if(clip!=null){stop();clip.play(.20);}
        }catch(RuntimeException|LinkageError ignored){clips.put(name,null);}
    }
    void toggleMute(){muted=!muted;stop();}
    boolean isMuted(){return muted;}
    void stop(){clips.values().forEach(c->{if(c!=null)c.stop();});}
}
