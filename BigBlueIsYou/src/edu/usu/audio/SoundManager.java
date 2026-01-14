/**
 * Code adapted from: https://github.com/lwjglgamedev/lwjglbook-bookcontents
 *
 * License: https://www.apache.org/licenses/LICENSE-2.0
 */
package edu.usu.audio;

import org.lwjgl.openal.*;
import org.lwjgl.stb.STBVorbisInfo;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.*;
import java.util.*;

import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.openal.ALC10.*;
import static org.lwjgl.stb.STBVorbis.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class SoundManager {

    private final Map<String, SoundBuffer> soundBufferMap;
    private final Map<String, Sound> soundSourceMap;
    private final long context;
    private final long device;

    public SoundManager() {
        soundBufferMap = new HashMap<>();
        soundSourceMap = new HashMap<>();

        device = alcOpenDevice((ByteBuffer) null);
        if (device == NULL) {
            throw new IllegalStateException("Failed to open the default OpenAL device.");
        }
        ALCCapabilities deviceCaps = ALC.createCapabilities(device);
        this.context = alcCreateContext(device, (IntBuffer) null);
        if (context == NULL) {
            throw new IllegalStateException("Failed to create OpenAL context.");
        }
        alcMakeContextCurrent(context);
        AL.createCapabilities(deviceCaps);

        alDistanceModel(AL11.AL_EXPONENT_DISTANCE);

        alListener3f(AL_POSITION, 0, 0, 0);
        alListener3f(AL_VELOCITY, 0, 0, 0);
    }

    public Sound load(String name, String filename, boolean loop) {
        SoundBuffer buffer = new SoundBuffer(filename);
        Sound source = new Sound(buffer.getBufferId(), loop);

        soundBufferMap.put(name, buffer);
        soundSourceMap.put(name, source);

        return source;
    }

    public void cleanup() {
        soundSourceMap.values().forEach(Sound::cleanup);
        soundSourceMap.clear();
        soundBufferMap.values().forEach(SoundBuffer::cleanup);
        soundBufferMap.clear();
        if (context != NULL) {
            alcDestroyContext(context);
        }
        if (device != NULL) {
            alcCloseDevice(device);
        }
    }

    public void remove(String name) {
        soundSourceMap.get(name).cleanup();
        soundBufferMap.get(name).cleanup();
        soundSourceMap.remove(name);
        soundBufferMap.remove(name);
    }

    /**
     * Have nested this class because nothing outside the SoundManager needs to know about it
     */
    private static class SoundBuffer {
        private final int bufferId;
        private final ShortBuffer buffer;

        public SoundBuffer(String filePath) {
            this.bufferId = alGenBuffers();
            try (STBVorbisInfo info = STBVorbisInfo.malloc()) {
                buffer = readVorbis(filePath, info);

                // Copy to buffer
                alBufferData(bufferId, info.channels() == 1 ? AL_FORMAT_MONO16 : AL_FORMAT_STEREO16, buffer, info.sample_rate());
            }
        }

        public void cleanup() {
            alDeleteBuffers(this.bufferId);
            if (buffer != null) {
                MemoryUtil.memFree(buffer);
            }
        }

        public int getBufferId() {
            return this.bufferId;
        }

        private ShortBuffer readVorbis(String filePath, STBVorbisInfo info) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer error = stack.mallocInt(1);
                long decoder = stb_vorbis_open_filename(filePath, error, null);
                if (decoder == NULL) {
                    throw new RuntimeException("Failed to open Ogg Vorbis file. Error: " + error.get(0));
                }

                stb_vorbis_get_info(decoder, info);

                int channels = info.channels();
                int lengthSamples = stb_vorbis_stream_length_in_samples(decoder);

                ShortBuffer result = MemoryUtil.memAllocShort(lengthSamples * channels);
                result.limit(stb_vorbis_get_samples_short_interleaved(decoder, channels, result) * channels);

                stb_vorbis_close(decoder);

                return result;
            }
        }
    }
}


