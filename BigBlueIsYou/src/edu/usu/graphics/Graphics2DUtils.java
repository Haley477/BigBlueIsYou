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
import org.joml.Vector3f;

import java.util.List;

public class Graphics2DUtils {

    public static class BuffersColor {
        public BuffersColor(float[] positions, float[] colors, int[] indices) {
            this.positions = positions;
            this.colors = colors;
            this.indices = indices;
        }

        public float[] positions;
        public float[] colors;
        public int[] indices;
    }

    public static class BuffersTexture {
        public BuffersTexture(float[] positions, float[] coords, int[] indices) {
            this.positions = positions;
            this.coords = coords;
            this.indices = indices;
        }

        public float[] positions;
        public float[] coords;
        public int[] indices;
    }

    public static BuffersColor prepareLinesColorBuffers(List<RenderQueue.RenderLineOperation> operations) {
        float[] positions = new float[operations.size() * 6];
        float[] colors = new float[operations.size() * 6];
        int[] indices = new int[operations.size() * 2];

        int rIndex = 0;
        int iIndex = 0;
        for (var op : operations) {
            Vector3f start = op.getStart();
            Vector3f end = op.getEnd();
            Color color = op.getColor();

            positions[rIndex * 3 + 0] = start.x;
            positions[rIndex * 3 + 1] = start.y;
            positions[rIndex * 3 + 2] = start.z;

            positions[rIndex * 3 + 3] = end.x;
            positions[rIndex * 3 + 4] = end.y;
            positions[rIndex * 3 + 5] = end.z;

            colors[rIndex * 3 + 0] = color.r;
            colors[rIndex * 3 + 1] = color.g;
            colors[rIndex * 3 + 2] = color.b;

            colors[rIndex * 3 + 3] = color.r;
            colors[rIndex * 3 + 4] = color.g;
            colors[rIndex * 3 + 5] = color.b;

            indices[iIndex + 0] = rIndex + 0;
            indices[iIndex + 1] = rIndex + 1;

            rIndex += 2;
            iIndex += 2;
        }

        return new BuffersColor(positions, colors, indices);
    }

    public static BuffersColor prepareTrisSolidColorBuffers(List<RenderQueue.RenderSolidTriangleOperation> operations) {
        float[] positions = new float[operations.size() * 9];
        float[] colors = new float[operations.size() * 9];
        int[] indices = new int[operations.size() * 3];

        int rIndex = 0;
        int iIndex = 0;
        for (var op : operations) {
            Triangle triangle = op.getTriangle();
            Color color = op.getColor();

            positions[rIndex * 3 + 0] = triangle.pt1.x;
            positions[rIndex * 3 + 1] = triangle.pt1.y;
            positions[rIndex * 3 + 2] = triangle.pt1.z;

            positions[rIndex * 3 + 3] = triangle.pt2.x;
            positions[rIndex * 3 + 4] = triangle.pt2.y;
            positions[rIndex * 3 + 5] = triangle.pt2.z;

            positions[rIndex * 3 + 6] = triangle.pt3.x;
            positions[rIndex * 3 + 7] = triangle.pt3.y;
            positions[rIndex * 3 + 8] = triangle.pt3.z;

            colors[rIndex * 3 + 0] = color.r;
            colors[rIndex * 3 + 1] = color.g;
            colors[rIndex * 3 + 2] = color.b;

            colors[rIndex * 3 + 3] = color.r;
            colors[rIndex * 3 + 4] = color.g;
            colors[rIndex * 3 + 5] = color.b;

            colors[rIndex * 3 + 6] = color.r;
            colors[rIndex * 3 + 7] = color.g;
            colors[rIndex * 3 + 8] = color.b;

            indices[iIndex + 0] = rIndex + 0;
            indices[iIndex + 1] = rIndex + 1;
            indices[iIndex + 2] = rIndex + 2;

            rIndex += 3;
            iIndex += 3;
        }

        return new BuffersColor(positions, colors, indices);
    }

    public static BuffersColor prepareRectsSolidColorBuffers(List<RenderQueue.RenderSolidRectangleOperation> operations) {
        float[] positions = new float[operations.size() * 12];
        float[] colors = new float[operations.size() * 12];
        int[] indices = new int[operations.size() * 6];

        int rIndex = 0;
        int iIndex = 0;
        for (var op : operations) {
            Rectangle rect = op.getRectangle();
            Color color = op.getColor();

            positions[rIndex * 3 + 0] = rect.left;
            positions[rIndex * 3 + 1] = rect.top;
            positions[rIndex * 3 + 2] = rect.z;

            positions[rIndex * 3 + 3] = rect.left + rect.width;
            positions[rIndex * 3 + 4] = rect.top;
            positions[rIndex * 3 + 5] = rect.z;

            positions[rIndex * 3 + 6] = rect.left + rect.width;
            positions[rIndex * 3 + 7] = rect.top + rect.height;
            positions[rIndex * 3 + 8] = rect.z;

            positions[rIndex * 3 + 9] = rect.left;
            positions[rIndex * 3 + 10] = rect.top + rect.height;
            positions[rIndex * 3 + 11] = rect.z;

            colors[rIndex * 3 + 0] = color.r;
            colors[rIndex * 3 + 1] = color.g;
            colors[rIndex * 3 + 2] = color.b;

            colors[rIndex * 3 + 3] = color.r;
            colors[rIndex * 3 + 4] = color.g;
            colors[rIndex * 3 + 5] = color.b;

            colors[rIndex * 3 + 6] = color.r;
            colors[rIndex * 3 + 7] = color.g;
            colors[rIndex * 3 + 8] = color.b;

            colors[rIndex * 3 + 9] = color.r;
            colors[rIndex * 3 + 10] = color.g;
            colors[rIndex * 3 + 11] = color.b;

            indices[iIndex + 0] = rIndex + 0;
            indices[iIndex + 1] = rIndex + 1;
            indices[iIndex + 2] = rIndex + 2;

            indices[iIndex + 3] = rIndex + 0;
            indices[iIndex + 4] = rIndex + 2;
            indices[iIndex + 5] = rIndex + 3;

            rIndex += 4;
            iIndex += 6;
        }

        return new BuffersColor(positions, colors, indices);
    }

    public static BuffersTexture prepareRectsTextureBuffers(List<RenderQueue.RenderTexturedRectangleOperation> operations) {
        float[] positions = new float[operations.size() * 12];
        float[] coords = new float[operations.size() * 8];
        int[] indices = new int[operations.size() * 6];

        int rIndex = 0;
        int iIndex = 0;
        for (var op : operations) {
            Texture texture = op.getTexture();
            Rectangle rect = op.getDestination();
            Rectangle subImage = op.getSubImage();

            positions[rIndex * 3 + 0] = rect.left;
            positions[rIndex * 3 + 1] = rect.top;
            positions[rIndex * 3 + 2] = rect.z;

            positions[rIndex * 3 + 3] = rect.left + rect.width;
            positions[rIndex * 3 + 4] = rect.top;
            positions[rIndex * 3 + 5] = rect.z;

            positions[rIndex * 3 + 6] = rect.left + rect.width;
            positions[rIndex * 3 + 7] = rect.top + rect.height;
            positions[rIndex * 3 + 8] = rect.z;

            positions[rIndex * 3 + 9] = rect.left;
            positions[rIndex * 3 + 10] = rect.top + rect.height;
            positions[rIndex * 3 + 11] = rect.z;

            // Convert pixel coordinates to texture coordinates
            float texWidth = texture.getWidth();
            float texHeight = texture.getHeight();

            if (subImage != null) {
                coords[rIndex * 2 + 0] = subImage.left / texWidth;
                coords[rIndex * 2 + 1] = subImage.top / texHeight;

                coords[rIndex * 2 + 2] = (subImage.left + subImage.width) / texWidth;
                coords[rIndex * 2 + 3] = subImage.top / texHeight;

                coords[rIndex * 2 + 4] = (subImage.left + subImage.width) / texWidth;
                coords[rIndex * 2 + 5] = (subImage.top + subImage.height) / texHeight;

                coords[rIndex * 2 + 6] = subImage.left / texWidth;
                coords[rIndex * 2 + 7] = (subImage.top + subImage.height) / texHeight;
            } else {
                coords[rIndex * 2 + 0] = 0.0f;
                coords[rIndex * 2 + 1] = 0.0f;

                coords[rIndex * 2 + 2] = 1.0f;
                coords[rIndex * 2 + 3] = 0.0f;

                coords[rIndex * 2 + 4] = 1.0f;
                coords[rIndex * 2 + 5] = 1.0f;

                coords[rIndex * 2 + 6] = 0.0f;
                coords[rIndex * 2 + 7] = 1.0f;
            }

            indices[iIndex + 0] = rIndex + 0;
            indices[iIndex + 1] = rIndex + 1;
            indices[iIndex + 2] = rIndex + 2;

            indices[iIndex + 3] = rIndex + 0;
            indices[iIndex + 4] = rIndex + 2;
            indices[iIndex + 5] = rIndex + 3;

            rIndex += 4;
            iIndex += 6;
        }

        return new BuffersTexture(positions, coords, indices);
    }

    public static BuffersTexture prepareTrianglesTextureBuffers(List<RenderQueue.RenderTexturedTriangleOperation> operations) {
        float[] positions = new float[operations.size() * 9];
        float[] coords = new float[operations.size() * 6];
        int[] indices = new int[operations.size() * 3];

        int tIndex = 0;
        int iIndex = 0;
        for (var op : operations) {
            Triangle triangle = op.getTriangle();
            TriangleTexCoords texCoords = op.getTexCoords();

            positions[tIndex * 3 + 0] = triangle.pt1.x;
            positions[tIndex * 3 + 1] = triangle.pt1.y;
            positions[tIndex * 3 + 2] = triangle.pt1.z;

            positions[tIndex * 3 + 3] = triangle.pt2.x;
            positions[tIndex * 3 + 4] = triangle.pt2.y;
            positions[tIndex * 3 + 5] = triangle.pt2.z;

            positions[tIndex * 3 + 6] = triangle.pt3.x;
            positions[tIndex * 3 + 7] = triangle.pt3.y;
            positions[tIndex * 3 + 8] = triangle.pt3.z;

            coords[tIndex * 2 + 0] = texCoords.pt1.x;
            coords[tIndex * 2 + 1] = texCoords.pt1.y;

            coords[tIndex * 2 + 2] = texCoords.pt2.x;
            coords[tIndex * 2 + 3] = texCoords.pt2.y;

            coords[tIndex * 2 + 4] = texCoords.pt3.x;
            coords[tIndex * 2 + 5] = texCoords.pt3.y;

            indices[iIndex + 0] = tIndex + 0;
            indices[iIndex + 1] = tIndex + 1;
            indices[iIndex + 2] = tIndex + 2;

            tIndex += 3;
            iIndex += 3;
        }

        return new BuffersTexture(positions, coords, indices);
    }

    public static BuffersTexture prepareTextGlyphBuffers(List<RenderQueue.RenderTextGlyphOperation> operations) {
        float[] positions = new float[operations.size() * 12];
        float[] coords = new float[operations.size() * 8];
        int[] indices = new int[operations.size() * 6];

        int rIndex = 0;
        int iIndex = 0;
        for (var op : operations) {
            Rectangle rect = op.getDestination();
            Vector2f p1 = op.getP1();
            Vector2f p2 = op.getP2();
            Vector2f p3 = op.getP3();
            Vector2f p4 = op.getP4();

            positions[rIndex * 3 + 0] = rect.left;
            positions[rIndex * 3 + 1] = rect.top;
            positions[rIndex * 3 + 2] = rect.z;

            positions[rIndex * 3 + 3] = rect.left + rect.width;
            positions[rIndex * 3 + 4] = rect.top;
            positions[rIndex * 3 + 5] = rect.z;

            positions[rIndex * 3 + 6] = rect.left + rect.width;
            positions[rIndex * 3 + 7] = rect.top + rect.height;
            positions[rIndex * 3 + 8] = rect.z;

            positions[rIndex * 3 + 9] = rect.left;
            positions[rIndex * 3 + 10] = rect.top + rect.height;
            positions[rIndex * 3 + 11] = rect.z;

            coords[rIndex * 2 + 0] = p1.x;
            coords[rIndex * 2 + 1] = p1.y;

            coords[rIndex * 2 + 2] = p2.x;
            coords[rIndex * 2 + 3] = p2.y;

            coords[rIndex * 2 + 4] = p3.x;
            coords[rIndex * 2 + 5] = p3.y;

            coords[rIndex * 2 + 6] = p4.x;
            coords[rIndex * 2 + 7] = p4.y;

            indices[iIndex + 0] = rIndex + 0;
            indices[iIndex + 1] = rIndex + 1;
            indices[iIndex + 2] = rIndex + 2;

            indices[iIndex + 3] = rIndex + 0;
            indices[iIndex + 4] = rIndex + 2;
            indices[iIndex + 5] = rIndex + 3;

            rIndex += 4;
            iIndex += 6;
        }

        return new BuffersTexture(positions, coords, indices);
    }
}
