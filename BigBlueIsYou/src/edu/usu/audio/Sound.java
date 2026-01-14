/**
 * Code adapted from: https://github.com/lwjglgamedev/lwjglbook-bookcontents
 *
 * License: https://www.apache.org/licenses/LICENSE-2.0
 */
package edu.usu.audio;

import static org.lwjgl.openal.AL10.*;

public class Sound {

    private final int sourceId;

    public Sound(int bufferId, boolean loop) {
        this.sourceId = alGenSources();
        alSourcei(sourceId, AL_LOOPING, loop ? AL_TRUE : AL_FALSE);
        alSourcei(sourceId, AL_SOURCE_RELATIVE, AL_FALSE);
        alSource3f(sourceId, AL_POSITION, 0, 0, 0);
        alSourcei(sourceId, AL_BUFFER, bufferId);
    }

    public void cleanup() {
        stop();
        alDeleteSources(sourceId);
    }

    public boolean isPlaying() {
        return alGetSourcei(sourceId, AL_SOURCE_STATE) == AL_PLAYING;
    }

    public void pause() {
        alSourcePause(sourceId);
    }

    public void play() {
        alSourcePlay(sourceId);
    }

    public void stop() {
        alSourceStop(sourceId);
    }

    public void setGain(float gain) {
        alSourcef(sourceId, AL_GAIN, gain);
    }
}
