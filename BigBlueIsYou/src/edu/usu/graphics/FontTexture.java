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

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;

public class FontTexture {

    private static final String IMAGE_FORMAT = "png";
    private static final int CHAR_PADDING = 4;
    // Needed to add this because of the way some characters (e.g., 't') render.
    // I don't like it and would like to find a better solution, but haven't yet figured that out.
    public static final int CHAR_OFFSET = 2;
    // this is empty space added to help prevent characters from rendering into each other.
    // Things like 'j' rendering into the space of 'i'.  It isn't a perfect solution, but
    // for what I'm trying to do here, works just fine.
    private static final int CHAR_SPACING = 20;

    private final Font font;
    private final String charSetName;
    private final Map<Character, CharInfo> charMap;
    private Texture texture;

    public FontTexture(Font font, String charSetName, boolean outline) throws Exception {
        this.font = font;
        this.charSetName = charSetName;
        charMap = new HashMap<>();

        buildTexture(outline);
    }

    public int getWidth() {
        return texture.getWidth();
    }

    public int getHeight() {
        return texture.getHeight();
    }

    public Texture getTexture() {
        return texture;
    }

    public CharInfo getCharInfo(char c) {
        return charMap.get(c);
    }

    private String getAllAvailableChars(String charsetName) {
        CharsetEncoder ce = Charset.forName(charsetName).newEncoder();
        StringBuilder result = new StringBuilder();
        for (char c = 0; c < Character.MAX_VALUE; c++) {
            if (ce.canEncode(c)) {
                result.append(c);
            }
        }
        return result.toString();
    }

    private void buildTexture(boolean outline) throws Exception {
        // Get the font metrics for each character for the selected font by using image
        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2D = img.createGraphics();
        g2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2D.setFont(font);
        FontMetrics fontMetrics = g2D.getFontMetrics();

        String allChars = getAllAvailableChars(charSetName);
        int width = 0;
        int height = fontMetrics.getHeight();
        for (char c : allChars.toCharArray()) {
            // Get the size for each character and update global image size
            CharInfo charInfo = new CharInfo(width, fontMetrics.charWidth(c) + 0);
            charMap.put(c, charInfo);
            width += charInfo.getWidth() + CHAR_PADDING + CHAR_SPACING;
        }
        g2D.dispose();

        // Create the image associated to the charset
        img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        g2D = img.createGraphics();
        g2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2D.setFont(font);
        fontMetrics = g2D.getFontMetrics();
        int startX = 0;
        for (char c : allChars.toCharArray()) {
            if (outline) {
                // Draw the character 4 times, once in each direction to reasonably, but not perfectly,
                // create an outline.
                g2D.setColor(Color.BLACK);
                g2D.translate(-1, 0);    // Left
                g2D.drawString("" + c, startX + CHAR_OFFSET, fontMetrics.getAscent());
                g2D.translate(1, 0);
                g2D.translate(0, -1);    // Top
                g2D.drawString("" + c, startX + CHAR_OFFSET, fontMetrics.getAscent());
                g2D.translate(0, 1);
                g2D.translate(1, 0);     // Right
                g2D.drawString("" + c, startX + CHAR_OFFSET, fontMetrics.getAscent());
                g2D.translate(-1, 0);
                g2D.translate(0, 1);     // Bottom
                g2D.drawString("" + c, startX + CHAR_OFFSET, fontMetrics.getAscent());
                g2D.translate(0, -1);
            }

            g2D.setColor(Color.WHITE);
            g2D.drawString("" + c, startX + CHAR_OFFSET, fontMetrics.getAscent());
            // Leaving this debug code here in case I decide to come back and further work on the rendering
            // location of characters.
            //g2D.setColor(Color.BLUE);
            //g2D.drawRect(startX, 0, charMap.get(c).getWidth() + CHAR_PADDING + CHAR_SPACING, fontMetrics.getHeight());
            startX += charMap.get(c).getWidth() + CHAR_PADDING + CHAR_SPACING;
        }
        g2D.dispose();

        ByteBuffer buf = null;
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(img, IMAGE_FORMAT, out);
            // Uncomment to create a file of the rendered font for debugging purposes
            //ImageIO.write(img, IMAGE_FORMAT, new java.io.File("font.png"));
            out.flush();
            byte[] data = out.toByteArray();
            buf = ByteBuffer.allocateDirect(data.length);
            buf.put(data, 0, data.length);
            buf.flip();
        }
        texture = new Texture(buf);
    }

    public static class CharInfo {

        private final int startX;
        private final int width;

        public CharInfo(int startX, int width) {
            this.startX = startX;
            this.width = width;
        }

        public int getStartX() {
            return startX;
        }
        public int getWidth() {
            return width;
        }
    }
}
