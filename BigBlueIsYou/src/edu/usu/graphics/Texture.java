/*
Copyright (c) 2024 James Dean Mathias

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
*/
package edu.usu.graphics;

import org.lwjgl.system.MemoryStack;

import java.nio.*;

import static org.lwjgl.opengl.GL32.*;
import static org.lwjgl.stb.STBImage.*;

public class Texture {

    private final int textureId;
    private final int width;
    private final int height;

    public Texture(String texturePath) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            ByteBuffer buffer = stbi_load(texturePath, w, h, channels, 4);
            if (buffer == null) {
                throw new RuntimeException("Image file [" + texturePath + "] not loaded: " + stbi_failure_reason());
            }

            this.width = w.get();
            this.height = h.get();

            this.textureId = createTexture(buffer);
            stbi_image_free(buffer);
        }
    }

    // --------------------------------------------------------------
    //
    // Builds a texture from a ByteBuffer.  The purpose for this constructor
    // is for the font rendering.  The FontTexture generates a ByteBuffer of
    // data based on the font being used, and this constructor then takes that
    // data and build a texture that can be used for text rendering.
    //
    // --------------------------------------------------------------
    public Texture(ByteBuffer imageBuffer) throws Exception {
        // Load Texture file
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            ByteBuffer buffer = stbi_load_from_memory(imageBuffer, w, h, channels, 4);
            if (buffer == null) {
                throw new Exception("Image file not loaded: " + stbi_failure_reason());
            }

            this.width = w.get();
            this.height = h.get();

            this.textureId = createTexture(buffer);
            stbi_image_free(buffer);
        }
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public void bind() {
        glBindTexture(GL_TEXTURE_2D, textureId);
    }

    public void cleanup() {
        glDeleteTextures(textureId);
    }

    private int createTexture(ByteBuffer buffer) {
        int textureId = glGenTextures();

        glBindTexture(GL_TEXTURE_2D, textureId);
        glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR_MIPMAP_NEAREST);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);
        glGenerateMipmap(GL_TEXTURE_2D);

        return textureId;
    }

}
