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

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL32.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Graphics2D  implements AutoCloseable {

    private final int width;
    private final int height;
    private final String title;
    private long window;
    private int frameBufferWidth;
    private int frameBufferHeight;

    // Add the render queue
    private final RenderQueue renderQueue = new RenderQueue();
    
    private Matrix4f mProjection;
    private Matrix4f mModelIdentity;
    private ShaderProgram shaderSolidColor;
    private ShaderProgram shaderTexture;
    private ShaderProgram shaderFont;
    
    // Cached uniform locations
    private int uniformSolidColorProjectionLocation;
    private int uniformSolidColorModelLocation;
    private int uniformTextureProjectionLocation;
    private int uniformTextureModelLocation;
    private int uniformTextureColorLocation;
    private int uniformFontProjectionLocation;
    private int uniformFontModelLocation;
    private int uniformFontColorLocation;
    
    // Buffer manager for reusing VAOs and VBOs
    private BufferManager bufferManager;
    
    // Frame counter for periodic buffer compaction
    private int frameCount = 0;
    private static final int COMPACT_INTERVAL = 300; // Compact buffers every 300 frames (5 seconds at 60 FPS)

    public Graphics2D(int width, int height, String title) {
        this.width = width;
        this.height = height;
        this.title = title;
    }

    public void initialize(Color clearColor) {
        this.window = prepareWindow(width, height, title);

        // Need this setting to be false, otherwise the way I have implemented the input handling
        // gets messed up.
        glfwSetInputMode(window, GLFW_STICKY_KEYS, GLFW_FALSE);

        // Set the clear color
        glClearColor(clearColor.r, clearColor.g, clearColor.b, clearColor.a);
        // Enable support for blending so that alpha is handled in textures correctly
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        glEnable(GL_LINE_SMOOTH);

        // Prepare depth testing
        glClearDepth(1.0);
        glDepthFunc(GL_LEQUAL);
        glEnable(GL_DEPTH_TEST);

        // Set up framebuffer size callback
        glfwSetFramebufferSizeCallback(window, (windowHandle, w, h) -> {
            frameBufferWidth = w;
            frameBufferHeight = h;
            glViewport(0, 0, w, h);
            updateProjectionMatrix();
        });

        // Initial framebuffer size setup
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1);
            IntBuffer pHeight = stack.mallocInt(1);
            glfwGetFramebufferSize(window, pWidth, pHeight);
            frameBufferWidth = pWidth.get(0);
            frameBufferHeight = pHeight.get(0);
            glViewport(0, 0, frameBufferWidth, frameBufferHeight);
        }

        this.mProjection = new Matrix4f();
        updateProjectionMatrix();

        this.mModelIdentity = new Matrix4f();
        this.mModelIdentity.identity();

        shaderSolidColor = createShader("resources/shaders/solid-color.vert", "resources/shaders/solid-color.frag");
        shaderTexture = createShader("resources/shaders/texture.vert", "resources/shaders/texture.frag");
        shaderFont = createShader("resources/shaders/font.vert", "resources/shaders/font.frag");
        
        // Cache uniform locations
        uniformSolidColorProjectionLocation = shaderSolidColor.getUniformLocation("mProjection");
        uniformSolidColorModelLocation = shaderSolidColor.getUniformLocation("mModel");
        
        uniformTextureProjectionLocation = shaderTexture.getUniformLocation("mProjection");
        uniformTextureModelLocation = shaderTexture.getUniformLocation("mModel");
        uniformTextureColorLocation = shaderTexture.getUniformLocation("color");
        
        uniformFontProjectionLocation = shaderFont.getUniformLocation("mProjection");
        uniformFontModelLocation = shaderFont.getUniformLocation("mModel");
        uniformFontColorLocation = shaderFont.getUniformLocation("color");
        
        // Initialize the buffer manager
        bufferManager = new BufferManager();

        // OSX needs this to be done before doing anything else in order for the
        // font rendering to work correctly.
        System.setProperty("java.awt.headless", "true");
    }

    public boolean shouldClose() {
        return glfwWindowShouldClose(window);
    }

    public long getWindow() {
        return this.window;
    }

    public void begin() {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glViewport(0, 0, frameBufferWidth, frameBufferHeight);
    }

    public void end() {
        // Sort the render queue by z-order
        renderQueue.sort();
        
        // Process the render queue
        processRenderQueue();
        
        // Clear the render queue
        renderQueue.clear();

        // Increment frame counter and periodically compact buffers
        frameCount++;
        if (frameCount >= COMPACT_INTERVAL) {
            bufferManager.compactBuffers();
            frameCount = 0;
        }

        glfwSwapBuffers(window);
    }

    public void close() {
        shaderSolidColor.cleanup();
        shaderTexture.cleanup();
        shaderFont.cleanup();
        
        // Clean up the buffer manager
        bufferManager.close();

        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);

        // Terminate GLFW and free the error callback
        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }

    public void draw(Vector3f pt1, Vector3f pt2, Color color) {
        renderQueue.add(pt1, pt2, color);
    }

    public void draw(Rectangle destination, Color color) {
        renderQueue.add(destination, color, mModelIdentity);
    }

    public void draw(Triangle triangle, Color color) {
        renderQueue.add(triangle, color, mModelIdentity);
    }

    public void draw(Triangle triangle, float rotation, Vector2f center, Color color) {
        Matrix4f mRotation = new Matrix4f();
        mRotation.translate(center.x, center.y, 0);
        mRotation.rotateZ(rotation);
        mRotation.translate(-center.x, -center.y, 0);
        renderQueue.add(triangle, color, mRotation);
    }

    public void draw(Texture texture, Triangle triangle, Color color) {
        // Default texture coordinates: (0,0), (1,0), (0,1)
        TriangleTexCoords texCoords = new TriangleTexCoords(
            new Vector2f(0.0f, 0.0f),
            new Vector2f(1.0f, 0.0f),
            new Vector2f(0.0f, 1.0f)
        );
        renderQueue.add(texture, triangle, texCoords, mModelIdentity, new Vector3f(color.r, color.g, color.b));
    }

    public void draw(Texture texture, Triangle triangle, float rotation, Vector2f center, Color color) {
        // Default texture coordinates: (0,0), (1,0), (0,1)
        TriangleTexCoords texCoords = new TriangleTexCoords(
            new Vector2f(0.0f, 0.0f),
            new Vector2f(1.0f, 0.0f),
            new Vector2f(0.0f, 1.0f)
        );
        Matrix4f mRotation = new Matrix4f();
        mRotation.translate(center.x, center.y, 0);
        mRotation.rotateZ(rotation);
        mRotation.translate(-center.x, -center.y, 0);
        renderQueue.add(texture, triangle, texCoords, mRotation, new Vector3f(color.r, color.g, color.b));
    }

    public void draw(Texture texture, Triangle triangle, TriangleTexCoords texCoords, Color color) {
        renderQueue.add(texture, triangle, texCoords, mModelIdentity, new Vector3f(color.r, color.g, color.b));
    }

    public void draw(Texture texture, Triangle triangle, TriangleTexCoords texCoords, float rotation, Vector2f center, Color color) {
        Matrix4f mRotation = new Matrix4f();
        mRotation.translate(center.x, center.y, 0);
        mRotation.rotateZ(rotation);
        mRotation.translate(-center.x, -center.y, 0);
        renderQueue.add(texture, triangle, texCoords, mRotation, new Vector3f(color.r, color.g, color.b));
    }

    public void draw(Rectangle destination, float rotation, Vector2f center, Color color) {
        Matrix4f mRotation = new Matrix4f();
        mRotation.translate(center.x, center.y, 0);
        mRotation.rotateZ(rotation);
        mRotation.translate(-center.x, -center.y, 0);
        renderQueue.add(destination, color, mRotation);
    }

    public void draw(Texture texture, Rectangle destination, Color color) {
        renderQueue.add(texture, destination, null, mModelIdentity, new Vector3f(color.r, color.g, color.b));
    }

    public void draw(Texture texture, Rectangle destination, float rotation, Vector2f center, Color color) {
        Matrix4f mRotation = new Matrix4f();
        mRotation.translate(center.x, center.y, 0);
        mRotation.rotateZ(rotation);
        mRotation.translate(-center.x, -center.y, 0);
        renderQueue.add(texture, destination, null, mRotation, new Vector3f(color.r, color.g, color.b));
    }

    public void draw(Texture texture, Rectangle destination, Rectangle subImage, float rotation, Vector2f center, Color color) {
        Matrix4f mRotation = new Matrix4f();
        mRotation.translate(center.x, center.y, 0);
        mRotation.rotateZ(rotation);
        mRotation.translate(-center.x, -center.y, 0);
        renderQueue.add(texture, destination, subImage, mRotation, new Vector3f(color.r, color.g, color.b));
    }

    public void drawTextByWidth(Font font, String text, float left, float top, float width, float z, Color color) {
        var tuples = font.drawText(text, left, top, width, z);
        for (var tuple : tuples) {
            renderQueue.add(
                    tuple.item1(),
                    tuple.item2(),
                    tuple.item3(),
                    tuple.item4(),
                    tuple.item5(),
                    tuple.item6(),
                    tuple.item7(), // Rotation
                    new Vector3f(color.r, color.g, color.b));
        }
    }

    public void drawTextByWidth(Font font, String text, float left, float top, float width, Color color) {
        var tuples = font.drawText(text, left, top,  width, 0.0f);
        for (var tuple : tuples) {
            renderQueue.add(
                    tuple.item1(),
                    tuple.item2(),
                    tuple.item3(),
                    tuple.item4(),
                    tuple.item5(),
                    tuple.item6(),
                    tuple.item7(), // Rotation
                    new Vector3f(color.r, color.g, color.b));
        }
    }

    public void drawTextByWidth(Font font, String text, float left, float top, float width, float z, float rotation, Vector2f center, Color color) {
        var tuples = font.drawText(text, left, top, width, z, rotation, center);
        for (var tuple : tuples) {
            renderQueue.add(
                    tuple.item1(),
                    tuple.item2(),
                    tuple.item3(),
                    tuple.item4(),
                    tuple.item5(),
                    tuple.item6(),
                    tuple.item7(), // Rotation
                    new Vector3f(color.r, color.g, color.b));
        }
    }

    public void drawTextByWidth(Font font, String text, float left, float top, float width, float rotation, Vector2f center, Color color) {
        var tuples = font.drawText(text, left, top, width, 0.0f, rotation, center);
        for (var tuple : tuples) {
            renderQueue.add(
                    tuple.item1(),
                    tuple.item2(),
                    tuple.item3(),
                    tuple.item4(),
                    tuple.item5(),
                    tuple.item6(),
                    tuple.item7(), // Rotation
                    new Vector3f(color.r, color.g, color.b));
        }
    }

    public void drawTextByHeight(Font font, String text, float left, float top, float height, float z, Color color) {
        float width = font.measureTextWidth(text, height);
        var tuples = font.drawText(text, left, top, width, z);
        for (var tuple : tuples) {
            renderQueue.add(
                    tuple.item1(),
                    tuple.item2(),
                    tuple.item3(),
                    tuple.item4(),
                    tuple.item5(),
                    tuple.item6(),
                    tuple.item7(), // Rotation
                    new Vector3f(color.r, color.g, color.b));
        }
    }

    public void drawTextByHeight(Font font, String text, float left, float top, float height, Color color) {
        float width = font.measureTextWidth(text, height);
        var tuples = font.drawText(text, left, top, width, 0.0f);
        for (var tuple : tuples) {
            renderQueue.add(
                    tuple.item1(),
                    tuple.item2(),
                    tuple.item3(),
                    tuple.item4(),
                    tuple.item5(),
                    tuple.item6(),
                    tuple.item7(), // Rotation
                    new Vector3f(color.r, color.g, color.b));
        }
    }

    public void drawTextByHeight(Font font, String text, float left, float top, float height, float z, float rotation, Vector2f center, Color color) {
        float width = font.measureTextWidth(text, height);
        var tuples = font.drawText(text, left, top, width, z, rotation, center);
        for (var tuple : tuples) {
            renderQueue.add(
                    tuple.item1(),
                    tuple.item2(),
                    tuple.item3(),
                    tuple.item4(),
                    tuple.item5(),
                    tuple.item6(),
                    tuple.item7(), // Rotation
                    new Vector3f(color.r, color.g, color.b));
        }
    }

    public void drawTextByHeight(Font font, String text, float left, float top, float height, float rotation, Vector2f center, Color color) {
        float width = font.measureTextWidth(text, height);
        var tuples = font.drawText(text, left, top, width, 0.0f, rotation, center);
        for (var tuple : tuples) {
            renderQueue.add(
                    tuple.item1(),
                    tuple.item2(),
                    tuple.item3(),
                    tuple.item4(),
                    tuple.item5(),
                    tuple.item6(),
                    tuple.item7(), // Rotation
                    new Vector3f(color.r, color.g, color.b));
        }
    }

    private static long prepareWindow(int width, int height, String title) {
        // Setup an error callback. The default implementation
        // will print the error message in System.err.
        GLFWErrorCallback.createPrint(System.err).set();

        // Initialize GLFW. Most GLFW functions will not work before doing this.
        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        // Configure GLFW
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);
        // Add hints for high DPI support on macOS
        if (System.getProperty("os.name").toLowerCase().contains("mac")) {
            glfwWindowHint(GLFW_COCOA_RETINA_FRAMEBUFFER, GLFW_TRUE);
            glfwWindowHint(GLFW_SCALE_TO_MONITOR, GLFW_TRUE);
        }

        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 2);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);

        // Create the window
        long window = glfwCreateWindow(width, height, title, NULL, NULL);
        if (window == NULL) {
            throw new RuntimeException("Failed to create the GLFW window");
        }

        // Center the window
        centerWindow(window, width, height);

        // Make the OpenGL context current
        glfwMakeContextCurrent(window);

        // This line is critical for LWJGL's interoperation with GLFW's
        // OpenGL context, or any context that is managed externally.
        // LWJGL detects the context that is current in the current thread,
        // creates the GLCapabilities instance and makes the OpenGL
        // bindings available for use.
        GL.createCapabilities();

        // Enable v-sync
        glfwSwapInterval(1);

        // Set up framebuffer size callback to be sure the window and framebuffer sizes match
        // correctly on high DPI systems.
        glfwSetFramebufferSizeCallback(window, (windowHandle, w, h) -> {
            glViewport(0, 0, w, h);
        });
        // Initial viewport setup
        setupViewport(window);

        // Make the window visible
        glfwShowWindow(window);

        return window;
    }

    private static void centerWindow(long window, int width, int height) {
        // Get resolution of the primary monitor
        GLFWVidMode videoMode = glfwGetVideoMode(glfwGetPrimaryMonitor());
        if (videoMode != null) {
            // Center the window
            glfwSetWindowPos(
                    window,
                    (videoMode.width() - width) / 2,
                    (videoMode.height() - height) / 2
            );
        }
    }

    private static void setupViewport(long window) {
        // Get the thread stack and push a new frame
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1);
            IntBuffer pHeight = stack.mallocInt(1);

            // Set the viewport to match the scaled framebuffer size
            glfwGetFramebufferSize(window, pWidth, pHeight);
            glViewport(0, 0, pWidth.get(0), pHeight.get(0));
        }
    }

    private ShaderProgram createShader(String vertexShaderPath, String fragmentShaderPath) {
        List<ShaderProgram.ShaderModuleData> shaderModuleDataList = new ArrayList<>();

        shaderModuleDataList.add(new ShaderProgram.ShaderModuleData(vertexShaderPath, GL_VERTEX_SHADER));
        shaderModuleDataList.add(new ShaderProgram.ShaderModuleData(fragmentShaderPath, GL_FRAGMENT_SHADER));

        return new ShaderProgram(shaderModuleDataList);
    }

    private void updateProjectionMatrix() {
        float aspectRatio = (float) frameBufferWidth / frameBufferHeight;
        this.mProjection.setOrtho2D(-1, 1, 1 / aspectRatio, -1 / aspectRatio);
    }

    /**
     * Process the render queue
     */
    private void processRenderQueue() {
        // Get operations using the type-safe methods
        List<RenderQueue.RenderLineOperation> lines = renderQueue.getLineOperations();
        List<RenderQueue.RenderSolidTriangleOperation> trianglesSolidColor = renderQueue.getSolidTriangleOperations();
        List<RenderQueue.RenderSolidRectangleOperation> rectanglesSolidColor = renderQueue.getSolidRectangleOperations();
        List<RenderQueue.RenderTexturedRectangleOperation> rectanglesTextured = renderQueue.getTexturedRectangleOperations();
        List<RenderQueue.RenderTexturedTriangleOperation> trianglesTextured = renderQueue.getTexturedTriangleOperations();
        List<RenderQueue.RenderTextGlyphOperation> textGlyphs = renderQueue.getTextGlyphOperations();

        renderTrianglesSolidColor(trianglesSolidColor);
        renderRectanglesSolidColor(rectanglesSolidColor);
        renderRectanglesTextured(rectanglesTextured);
        renderTrianglesTextured(trianglesTextured);
        renderTextGlyphs(textGlyphs);
        renderLines(lines);
    }

    private void renderTextGlyphs(List<RenderQueue.RenderTextGlyphOperation> textGlyphs) {
        if (!textGlyphs.isEmpty()) {
            Graphics2DUtils.BuffersTexture buffersTextGlyphs = Graphics2DUtils.prepareTextGlyphBuffers(textGlyphs);
            int bufferId = bufferManager.getFontBuffer(buffersTextGlyphs.positions.length / 3, buffersTextGlyphs.indices.length);
            bufferManager.updateFontBuffer(bufferId, buffersTextGlyphs.positions, buffersTextGlyphs.coords, buffersTextGlyphs.indices);

            bufferManager.bindBuffer(BufferManager.BUFFER_TYPE_FONT, bufferId);
            shaderFont.bind();

            try (var stack = MemoryStack.stackPush()) {
                var matrixBuffer = stack.mallocFloat(16);
                mProjection.get(matrixBuffer);
                glUniformMatrix4fv(uniformFontProjectionLocation, false, matrixBuffer);

                Texture currentTexture = null;
                for (int i = 0; i < textGlyphs.size(); i++) {
                    var op = textGlyphs.get(i);
                    Texture texture = op.getTexture();
                    Matrix4f modelMatrix = op.getTransform();
                    Vector3f color = op.getColor();

                    // Only bind the texture if it's different from the current one
                    if (currentTexture != texture) {
                        if (currentTexture != null) {
                            glBindTexture(GL_TEXTURE_2D, 0);
                        }
                        texture.bind();
                        currentTexture = texture;
                    }

                    modelMatrix.get(matrixBuffer);
                    glUniformMatrix4fv(uniformFontModelLocation, false, matrixBuffer);
                    glUniform3f(uniformFontColorLocation, color.x, color.y, color.z);

                    glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, i * 6 * Integer.BYTES);
                }

                if (currentTexture != null) {
                    glBindTexture(GL_TEXTURE_2D, 0);
                }
            }

            shaderFont.unbind();
            bufferManager.unbindBuffer();
            bufferManager.releaseBuffer(BufferManager.BUFFER_TYPE_FONT, bufferId);
        }
    }

    private void renderTrianglesTextured(List<RenderQueue.RenderTexturedTriangleOperation> triangles) {
        if (!triangles.isEmpty()) {
            Graphics2DUtils.BuffersTexture buffersTrianglesTexture = Graphics2DUtils.prepareTrianglesTextureBuffers(triangles);
            int bufferId = bufferManager.getTextureBuffer(buffersTrianglesTexture.positions.length / 3, buffersTrianglesTexture.indices.length);
            bufferManager.updateTextureBuffer(bufferId, buffersTrianglesTexture.positions, buffersTrianglesTexture.coords, buffersTrianglesTexture.indices);

            bufferManager.bindBuffer(BufferManager.BUFFER_TYPE_TEXTURE, bufferId);
            shaderTexture.bind();

            try (var stack = MemoryStack.stackPush()) {
                var matrixBuffer = stack.mallocFloat(16);
                mProjection.get(matrixBuffer);
                glUniformMatrix4fv(uniformTextureProjectionLocation, false, matrixBuffer);

                Texture currentTexture = null;
                for (int i = 0; i < triangles.size(); i++) {
                    var op = triangles.get(i);
                    Texture texture = op.getTexture();
                    Matrix4f modelMatrix = op.getTransform();
                    Vector3f color = op.getColor();

                    // Only bind the texture if it's different from the current one
                    if (currentTexture != texture) {
                        if (currentTexture != null) {
                            glBindTexture(GL_TEXTURE_2D, 0);
                        }
                        texture.bind();
                        currentTexture = texture;
                    }

                    modelMatrix.get(matrixBuffer);
                    glUniformMatrix4fv(uniformTextureModelLocation, false, matrixBuffer);
                    glUniform3f(uniformTextureColorLocation, color.x, color.y, color.z);

                    glDrawElements(GL_TRIANGLES, 3, GL_UNSIGNED_INT, i * 3 * Integer.BYTES);
                }

                if (currentTexture != null) {
                    glBindTexture(GL_TEXTURE_2D, 0);
                }
            }

            shaderTexture.unbind();
            bufferManager.unbindBuffer();
            bufferManager.releaseBuffer(BufferManager.BUFFER_TYPE_TEXTURE, bufferId);
        }
    }

    private void renderRectanglesTextured(List<RenderQueue.RenderTexturedRectangleOperation> rectangles) {
        if (!rectangles.isEmpty()) {
            Graphics2DUtils.BuffersTexture buffersTexture = Graphics2DUtils.prepareRectsTextureBuffers(rectangles);
            int bufferId = bufferManager.getTextureBuffer(buffersTexture.positions.length / 3, buffersTexture.indices.length);
            bufferManager.updateTextureBuffer(bufferId, buffersTexture.positions, buffersTexture.coords, buffersTexture.indices);

            bufferManager.bindBuffer(BufferManager.BUFFER_TYPE_TEXTURE, bufferId);
            shaderTexture.bind();

            try (var stack = MemoryStack.stackPush()) {
                var matrixBuffer = stack.mallocFloat(16);
                mProjection.get(matrixBuffer);
                glUniformMatrix4fv(uniformTextureProjectionLocation, false, matrixBuffer);

                Texture currentTexture = null;
                for (int i = 0; i < rectangles.size(); i++) {
                    var op = rectangles.get(i);
                    Texture texture = op.getTexture();
                    Matrix4f modelMatrix = op.getTransform();
                    Vector3f color = op.getColor();

                    // Only bind the texture if it's different from the current one
                    if (currentTexture != texture) {
                        if (currentTexture != null) {
                            glBindTexture(GL_TEXTURE_2D, 0);
                        }
                        texture.bind();
                        currentTexture = texture;
                    }

                    modelMatrix.get(matrixBuffer);
                    glUniformMatrix4fv(uniformTextureModelLocation, false, matrixBuffer);
                    glUniform3f(uniformTextureColorLocation, color.x, color.y, color.z);

                    glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, i * 6 * Integer.BYTES);
                }

                if (currentTexture != null) {
                    glBindTexture(GL_TEXTURE_2D, 0);
                }
            }

            shaderTexture.unbind();
            bufferManager.unbindBuffer();
            bufferManager.releaseBuffer(BufferManager.BUFFER_TYPE_TEXTURE, bufferId);
        }
    }

    private void renderRectanglesSolidColor(List<RenderQueue.RenderSolidRectangleOperation> rectangles) {
        if (!rectangles.isEmpty()) {
            Graphics2DUtils.BuffersColor buffersSolidColor = Graphics2DUtils.prepareRectsSolidColorBuffers(rectangles);
            int bufferId = bufferManager.getSolidColorBuffer(buffersSolidColor.positions.length / 3, buffersSolidColor.indices.length);
            bufferManager.updateSolidColorBuffer(bufferId, buffersSolidColor.positions, buffersSolidColor.colors, buffersSolidColor.indices);

            bufferManager.bindBuffer(BufferManager.BUFFER_TYPE_SOLID_COLOR, bufferId);
            shaderSolidColor.bind();

            try (var stack = MemoryStack.stackPush()) {
                var matrixBuffer = stack.mallocFloat(16);
                mProjection.get(matrixBuffer);
                glUniformMatrix4fv(uniformSolidColorProjectionLocation, false, matrixBuffer);

                for (int i = 0; i < rectangles.size(); i++) {
                    var op = rectangles.get(i);
                    Matrix4f modelMatrix = op.getTransform();
                    modelMatrix.get(matrixBuffer);
                    glUniformMatrix4fv(uniformSolidColorModelLocation, false, matrixBuffer);
                    glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, i * 6 * Integer.BYTES);
                }
            }

            shaderSolidColor.unbind();
            bufferManager.unbindBuffer();
            bufferManager.releaseBuffer(BufferManager.BUFFER_TYPE_SOLID_COLOR, bufferId);
        }
    }

    private void renderTrianglesSolidColor(List<RenderQueue.RenderSolidTriangleOperation> triangles) {
        if (!triangles.isEmpty()) {
            Graphics2DUtils.BuffersColor buffersSolidColor = Graphics2DUtils.prepareTrisSolidColorBuffers(triangles);
            int bufferId = bufferManager.getSolidColorBuffer(buffersSolidColor.positions.length / 3, buffersSolidColor.indices.length);
            bufferManager.updateSolidColorBuffer(bufferId, buffersSolidColor.positions, buffersSolidColor.colors, buffersSolidColor.indices);

            bufferManager.bindBuffer(BufferManager.BUFFER_TYPE_SOLID_COLOR, bufferId);
            shaderSolidColor.bind();

            try (var stack = MemoryStack.stackPush()) {
                var matrixBuffer = stack.mallocFloat(16);
                mProjection.get(matrixBuffer);
                glUniformMatrix4fv(uniformSolidColorProjectionLocation, false, matrixBuffer);

                for (int i = 0; i < triangles.size(); i++) {
                    var op = triangles.get(i);
                    Matrix4f modelMatrix = op.getTransform();
                    modelMatrix.get(matrixBuffer);
                    glUniformMatrix4fv(uniformSolidColorModelLocation, false, matrixBuffer);
                    glDrawElements(GL_TRIANGLES, 3, GL_UNSIGNED_INT, i * 3 * Integer.BYTES);
                }
            }

            shaderSolidColor.unbind();
            bufferManager.unbindBuffer();
            bufferManager.releaseBuffer(BufferManager.BUFFER_TYPE_SOLID_COLOR, bufferId);
        }
    }

    private void renderLines(List<RenderQueue.RenderLineOperation> linex) {
        if (!linex.isEmpty()) {
            Graphics2DUtils.BuffersColor buffersLines = Graphics2DUtils.prepareLinesColorBuffers(linex);
            int bufferId = bufferManager.getSolidColorBuffer(buffersLines.positions.length / 3, buffersLines.indices.length);
            bufferManager.updateSolidColorBuffer(bufferId, buffersLines.positions, buffersLines.colors, buffersLines.indices);

            bufferManager.bindBuffer(BufferManager.BUFFER_TYPE_SOLID_COLOR, bufferId);
            shaderSolidColor.bind();

            try (var stack = MemoryStack.stackPush()) {
                var matrixBuffer = stack.mallocFloat(16);
                mProjection.get(matrixBuffer);
                glUniformMatrix4fv(uniformSolidColorProjectionLocation, false, matrixBuffer);
                mModelIdentity.get(matrixBuffer);
                glUniformMatrix4fv(uniformSolidColorModelLocation, false, matrixBuffer);

                glLineWidth(1.5f);
                glDrawElements(GL_LINES, linex.size() * 2, GL_UNSIGNED_INT, 0);
            }

            shaderSolidColor.unbind();
            bufferManager.unbindBuffer();
            bufferManager.releaseBuffer(BufferManager.BUFFER_TYPE_SOLID_COLOR, bufferId);
        }
    }
}
