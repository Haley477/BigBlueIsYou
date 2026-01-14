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

import org.joml.Vector2f;

public class AnimatedSprite {

    private final Texture spriteSheet;
    private final float[] spriteTime;

    private double animationTime = 0;
    private int subImageIndex = 0;
    private final int subImageWidth;

    private final Vector2f size;
    protected Vector2f center;
    protected float rotation = 0;

    public AnimatedSprite(Texture spriteSheet, float[] spriteTime, Vector2f size, Vector2f center) {
        this.spriteSheet = spriteSheet;
        this.spriteTime = spriteTime;

        this.subImageWidth = spriteSheet.getWidth() / spriteTime.length;

        this.size = size;
        this.center = center;
    }

    public void update(double elapsedTime) {
        animationTime += elapsedTime;
        if (animationTime >= spriteTime[subImageIndex]) {
            animationTime -= spriteTime[subImageIndex];
            subImageIndex++;
            subImageIndex = subImageIndex % spriteTime.length;
        }
    }

    public void draw(Graphics2D graphics, Color color) {
        // Where to draw
        Rectangle destination = new Rectangle(center.x - size.x / 2, center.y - size.y / 2, size.x, size.y);
        // Which sub-rectangle of the spritesheet to draw
        Rectangle subImage = new Rectangle(
                subImageWidth * subImageIndex,
                0,
                subImageWidth,
                spriteSheet.getHeight());
        graphics.draw(spriteSheet, destination, subImage, rotation, center, color);
    }
}
