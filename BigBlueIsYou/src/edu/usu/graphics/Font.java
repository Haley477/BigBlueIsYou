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

import edu.usu.utils.Tuple7;
import org.joml.Matrix4f;
import org.joml.Vector2f;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;

public class Font {
    private FontTexture texture;

    public Font(String name, int style, int size, boolean outline) {
        try {
            java.awt.Font font = new java.awt.Font(name, style, size);
            texture = new FontTexture(font, "ISO-8859-1", outline);
        } catch (Exception ignored) {
        }
    }

    public Font(String fontFile, int size, boolean outline) {
        try {
            InputStream is = new FileInputStream((new File(fontFile)));
            java.awt.Font font = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, is).deriveFont((float) size);
            texture = new FontTexture(font, "ISO-8859-1", outline);
        } catch (Exception ignored) {
        }
    }

    public float measureTextHeight(String text, float width) {
        char[] characters = text.toCharArray();
        int totalWidth = 0;

        for (char character : characters) {
            totalWidth += texture.getCharInfo(character).getWidth();
        }

        return (texture.getHeight() / (float) totalWidth) * width;
    }

    public float measureTextWidth(String text, float height) {
        char[] characters = text.toCharArray();
        int totalWidth = 0;

        for (char character : characters) {
            totalWidth += texture.getCharInfo(character).getWidth();
        }

        return (height / texture.getHeight()) * totalWidth;
    }

    public ArrayList<Tuple7<Texture, Rectangle, Vector2f, Vector2f, Vector2f, Vector2f, Matrix4f>> drawText(String text, float left, float top, float width, float z) {
        Matrix4f mIdentity = new Matrix4f();
        mIdentity.identity();

        return drawText(text, left, top, width, z, mIdentity);
    }

    public ArrayList<Tuple7<Texture, Rectangle, Vector2f, Vector2f, Vector2f, Vector2f, Matrix4f>> drawText(String text, float left, float top, float width, float z, float rotation, Vector2f center) {
        Matrix4f mRotation = new Matrix4f();
        mRotation.translate(center.x, center.y, 0);
        mRotation.rotateZ(rotation);
        mRotation.translate(-center.x, -center.y, 0);

        return drawText(text, left, top, width, z, mRotation);
    }

    private ArrayList<Tuple7<Texture, Rectangle, Vector2f, Vector2f, Vector2f, Vector2f, Matrix4f>> drawText(String text, float left, float top, float width, float z, Matrix4f mRotation) {
        ArrayList<Tuple7<Texture, Rectangle, Vector2f, Vector2f, Vector2f, Vector2f, Matrix4f>> tuples = new ArrayList<>();

        char[] characters = text.toCharArray();

        // Step 1: Compute the total width of the characters.  This allows us to compute
        //         a relative width for each character to be rendered
        int totalWidth = 0;
        for (char c : characters) {
            totalWidth += texture.getCharInfo(c).getWidth();
        }

        // Step 2: Compute rectangles for each character, along with the texture coordinates
        //         for where to find those characters in the FontTexture
        float currentLeft = left;
        for (char character : characters) {
            FontTexture.CharInfo info = texture.getCharInfo(character);

            // Compute the texture coords for this texture
            Vector2f v1 = new Vector2f(info.getStartX() / (float) texture.getWidth(), 0.0f);
            Vector2f v2 = new Vector2f((info.getStartX() + info.getWidth() + FontTexture.CHAR_OFFSET) / (float) texture.getWidth(), 0.0f);
            Vector2f v3 = new Vector2f((info.getStartX() + info.getWidth() + FontTexture.CHAR_OFFSET) / (float) texture.getWidth(), 1.0f);
            Vector2f v4 = new Vector2f(info.getStartX() / (float) texture.getWidth(), 1.0f);

            // Create a rectangle to render for this character
            float charWidth = (info.getWidth() / (float) totalWidth) * width;
            float charHeight = (texture.getHeight() / (float) totalWidth) * width;
            Rectangle r = new Rectangle(currentLeft, top, charWidth, charHeight, z);

            tuples.add(new Tuple7<>(texture.getTexture(), r, v1, v2, v3, v4, mRotation));

            currentLeft += charWidth;
        }

        return tuples;
    }

}
