package com.kartersanamo.flappyBird;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class AudioManager {
    private final Map<String, Clip> clips = new HashMap<>();
    private boolean muted = false;

    public void setMuted(boolean muted) {
        this.muted = muted;
    }

    public boolean isMuted() {
        return muted;
    }

    public void load(String key, String resourcePath) {
        Clip clip = loadClip(resourcePath);
        if (clip != null) {
            clips.put(key, clip);
        }
    }

    public void play(String key) {
        if (muted) {
            return;
        }

        Clip clip = clips.get(key);
        if (clip == null) {
            return;
        }

        if (clip.isRunning()) {
            clip.stop();
        }
        clip.setFramePosition(0);
        clip.start();
    }

    private Clip loadClip(String resourcePath) {
        try (InputStream rawStream = getClass().getResourceAsStream(resourcePath)) {
            if (rawStream == null) {
                return null;
            }

            try (BufferedInputStream bufferedStream = new BufferedInputStream(rawStream);
                 AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(bufferedStream)) {
                Clip clip = AudioSystem.getClip();
                clip.open(audioInputStream);
                return clip;
            }
        } catch (Exception ignored) {
            return null;
        }
    }
}

