package myGame.core;

import tage.audio.*;

/**
 * Handles sound system initialization and bounce sound creation.
 */
public class SoundManager {
    private final MyGame game;

    public SoundManager(MyGame game) {
        this.game = game;
    }

    public Sound setupSound() {
        IAudioManager audioMgr = MyGame.getEngine().getAudioManager();
        AudioResource bounceResource = audioMgr.createAudioResource("bounce.wav", AudioResourceType.AUDIO_SAMPLE);

        Sound bounceSound = new Sound(bounceResource, SoundType.SOUND_EFFECT, 25, false);
        bounceSound.initialize(audioMgr);

        return bounceSound;
    }
}
