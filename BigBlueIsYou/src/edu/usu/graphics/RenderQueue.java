package edu.usu.graphics;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

import edu.usu.utils.Tuple3;
import edu.usu.utils.Tuple5;
import edu.usu.utils.Tuple8;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * A unified render queue system that handles sorting and batching of render operations
 * without changing the existing Graphics2D API.
 */
public class RenderQueue {
    // Different types of render operations
    public enum RenderType {
        SOLID_COLOR_RECTANGLE,
        SOLID_COLOR_TRIANGLE,
        TEXTURED_RECTANGLE,
        TEXTURED_TRIANGLE,
        TEXT_GLYPH,
        LINE
    }

    // There is no need to include RenderOperation constructors, only the base abstract class is needed for typing
    public static abstract class RenderOperation {
        private final RenderType type;
        private final float zOrder;

        protected RenderOperation(RenderType type, float zOrder) {
            this.type = type;
            this.zOrder = zOrder;
        }

        public float getZOrder() {
            return zOrder;
        }
    }

    // Specific operation class for solid color rectangles
    public static class RenderSolidRectangleOperation extends RenderOperation {
        private final Tuple3<Rectangle, Color, Matrix4f> data;

        public RenderSolidRectangleOperation(Rectangle rectangle, Color color, Matrix4f transform) {
            super(RenderType.SOLID_COLOR_RECTANGLE, rectangle.z);
            this.data = new Tuple3<>(rectangle, color, transform);
        }

        public Rectangle getRectangle() { return data.item1(); }
        public Color getColor() { return data.item2(); }
        public Matrix4f getTransform() { return data.item3(); }
    }

    // Specific operation class for textured rectangles
    public static class RenderTexturedRectangleOperation extends RenderOperation {
        private final Tuple5<Texture, Rectangle, Rectangle, Matrix4f, Vector3f> data;

        public RenderTexturedRectangleOperation(Texture texture, Rectangle destination, Rectangle subImage, Matrix4f transform, Vector3f color) {
            super(RenderType.TEXTURED_RECTANGLE, destination.z);
            this.data = new Tuple5<>(texture, destination, subImage, transform, color);
        }

        public Texture getTexture() { return data.item1(); }
        public Rectangle getDestination() { return data.item2(); }
        public Rectangle getSubImage() { return data.item3(); }
        public Matrix4f getTransform() { return data.item4(); }
        public Vector3f getColor() { return data.item5(); }
    }

    // Specific operation class for text glyphs
    public static class RenderTextGlyphOperation extends RenderOperation {
        private final Tuple8<Texture, Rectangle, Vector2f, Vector2f, Vector2f, Vector2f, Matrix4f, Vector3f> data;

        public RenderTextGlyphOperation(Texture texture, Rectangle destination, Vector2f p1, Vector2f p2, Vector2f p3, Vector2f p4, Matrix4f transform, Vector3f color) {
            super(RenderType.TEXT_GLYPH, destination.z);
            this.data = new Tuple8<>(texture, destination, p1, p2, p3, p4, transform, color);
        }
        
        public Texture getTexture() { return data.item1(); }
        public Rectangle getDestination() { return data.item2(); }
        public Vector2f getP1() { return data.item3(); }
        public Vector2f getP2() { return data.item4(); }
        public Vector2f getP3() { return data.item5(); }
        public Vector2f getP4() { return data.item6(); }
        public Matrix4f getTransform() { return data.item7(); }
        public Vector3f getColor() { return data.item8(); }
    }

    // Specific operation class for solid color triangles
    public static class RenderSolidTriangleOperation extends RenderOperation {
        private final Tuple3<Triangle, Color, Matrix4f> data;

        public RenderSolidTriangleOperation(Triangle triangle, Color color, Matrix4f transform) {
            super(RenderType.SOLID_COLOR_TRIANGLE, triangle.pt1.z);
            this.data = new Tuple3<>(triangle, color, transform);
        }
        
        public Triangle getTriangle() { return data.item1(); }
        public Color getColor() { return data.item2(); }
        public Matrix4f getTransform() { return data.item3(); }
    }

    // Specific operation class for textured triangles
    public static class RenderTexturedTriangleOperation extends RenderOperation {
        private final Tuple5<Texture, Triangle, TriangleTexCoords, Matrix4f, Vector3f> data;

        public RenderTexturedTriangleOperation(Texture texture, Triangle triangle, TriangleTexCoords texCoords, Matrix4f transform, Vector3f color) {
            super(RenderType.TEXTURED_TRIANGLE, triangle.pt1.z);
            this.data = new Tuple5<>(texture, triangle, texCoords, transform, color);
        }
        
        public Texture getTexture() { return data.item1(); }
        public Triangle getTriangle() { return data.item2(); }
        public TriangleTexCoords getTexCoords() { return data.item3(); }
        public Matrix4f getTransform() { return data.item4(); }
        public Vector3f getColor() { return data.item5(); }
    }

    // Specific operation class for lines
    public static class RenderLineOperation extends RenderOperation {
        private final Tuple3<Vector3f, Vector3f, Color> data;

        public RenderLineOperation(Vector3f start, Vector3f end, Color color) {
            super(RenderType.LINE, Math.max(start.z, end.z));
            this.data = new Tuple3<>(start, end, color);
        }
        
        public Vector3f getStart() { return data.item1(); }
        public Vector3f getEnd() { return data.item2(); }
        public Color getColor() { return data.item3(); }
    }

    private final ArrayList<RenderSolidRectangleOperation> solidColorRectangles = new ArrayList<>();
    private final ArrayList<RenderSolidTriangleOperation> solidColorTriangles = new ArrayList<>();
    private final ArrayList<RenderTexturedRectangleOperation> texturedRectangles = new ArrayList<>();
    private final ArrayList<RenderTexturedTriangleOperation> texturedTriangles = new ArrayList<>();
    private final ArrayList<RenderTextGlyphOperation> textGlyphs = new ArrayList<>();
    private final ArrayList<RenderLineOperation> lines = new ArrayList<>();

    /**
     * Add a solid color rectangle operation to the queue
     */
    public void add(Rectangle rectangle, Color color, Matrix4f transform) {
        solidColorRectangles.add(new RenderSolidRectangleOperation(rectangle, color, transform));
    }

    /**
     * Add a textured rectangle operation to the queue
     */
    public void add(Texture texture, Rectangle destination, Rectangle subImage, Matrix4f transform, Vector3f color) {
        texturedRectangles.add(new RenderTexturedRectangleOperation(texture, destination, subImage, transform, color));
    }

    /**
     * Add a text glyph operation to the queue
     */
    public void add(Texture texture, Rectangle destination, Vector2f p1, Vector2f p2, Vector2f p3, Vector2f p4, Matrix4f transform, Vector3f color) {
        textGlyphs.add(new RenderTextGlyphOperation(texture, destination, p1, p2, p3, p4, transform, color));
    }

    /**
     * Add a solid color triangle operation to the queue
     */
    public void add(Triangle triangle, Color color, Matrix4f transform) {
        solidColorTriangles.add(new RenderSolidTriangleOperation(triangle, color, transform));
    }

    /**
     * Add a textured triangle operation to the queue
     */
    public void add(Texture texture, Triangle triangle, TriangleTexCoords texCoords, Matrix4f transform, Vector3f color) {
        texturedTriangles.add(new RenderTexturedTriangleOperation(texture, triangle, texCoords, transform, color));
    }

    /**
     * Add a line operation to the queue
     */
    public void add(Vector3f start, Vector3f end, Color color) {
        lines.add(new RenderLineOperation(start, end, color));
    }

    /**
     * Sort all render queues by z-order (back to front)
     */
    public void sort() {
        Comparator<RenderOperation> zOrderComparator = Comparator.comparing(RenderOperation::getZOrder);
        solidColorRectangles.sort(zOrderComparator);
        solidColorTriangles.sort(zOrderComparator);
        texturedRectangles.sort(zOrderComparator);
        texturedTriangles.sort(zOrderComparator);
        textGlyphs.sort(zOrderComparator);
        lines.sort(zOrderComparator);
    }

    /**
     * Get solid color rectangle operations
     */
    public List<RenderSolidRectangleOperation> getSolidRectangleOperations() {
        return solidColorRectangles;
    }

    /**
     * Get solid color triangle operations
     */
    public List<RenderSolidTriangleOperation> getSolidTriangleOperations() {
        return solidColorTriangles;
    }

    /**
     * Get textured rectangle operations
     */
    public List<RenderTexturedRectangleOperation> getTexturedRectangleOperations() {
        return texturedRectangles;
    }

    /**
     * Get textured triangle operations
     */
    public List<RenderTexturedTriangleOperation> getTexturedTriangleOperations() {
        return texturedTriangles;
    }

    /**
     * Get text glyph operations
     */
    public List<RenderTextGlyphOperation> getTextGlyphOperations() {
        return textGlyphs;
    }

    /**
     * Get line operations
     */
    public List<RenderLineOperation> getLineOperations() {
        return lines;
    }

    /**
     * Clear all operations in the queue
     */
    public void clear() {
        solidColorRectangles.clear();
        solidColorTriangles.clear();
        texturedRectangles.clear();
        texturedTriangles.clear();
        textGlyphs.clear();
        lines.clear();
    }
}