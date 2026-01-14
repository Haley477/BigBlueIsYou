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

import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.*;

import static org.lwjgl.opengl.GL32.*;

/**
 * Manages OpenGL buffer objects (VAOs and VBOs) to allow for reuse
 * rather than creating and destroying them each frame.
 * Implements advanced buffer pooling to reduce memory allocations.
 */
public class BufferManager implements AutoCloseable {
    
    // Buffer types
    public static final int BUFFER_TYPE_SOLID_COLOR = 0;
    public static final int BUFFER_TYPE_TEXTURE = 1;
    public static final int BUFFER_TYPE_FONT = 2;
    
    // Buffer configurations
    private static class BufferConfig {
        int vaoId;
        int vboPositions;
        int vboColors; // For solid color buffers
        int vboTexCoords; // For texture buffers
        int vboIndices;
        int maxVertices;
        int maxIndices;
        boolean inUse;
        long lastUsedTime;
        int useCount;
        
        public BufferConfig(int vaoId, int vboPositions, int vboColors, int vboTexCoords, int vboIndices, 
                           int maxVertices, int maxIndices) {
            this.vaoId = vaoId;
            this.vboPositions = vboPositions;
            this.vboColors = vboColors;
            this.vboTexCoords = vboTexCoords;
            this.vboIndices = vboIndices;
            this.maxVertices = maxVertices;
            this.maxIndices = maxIndices;
            this.inUse = false;
            this.lastUsedTime = System.currentTimeMillis();
            this.useCount = 0;
        }
    }
    
    // Maps to store buffer configurations by type
    private final Map<Integer, Map<Integer, BufferConfig>> buffersByType = new HashMap<>();
    
    // Buffer size categories for better pooling
    private static final int[] BUFFER_SIZE_TIERS = {
        64, 128, 256, 512, 1024, 2048, 4096, 8192, 16384
    };
    
    // Buffer management settings
    private static final long BUFFER_EXPIRATION_TIME = 10000; // 10 seconds
    private static final int MAX_UNUSED_BUFFERS_PER_SIZE = 3;
    
    // Statistics
    private int totalBuffersCreated = 0;
    private int totalBuffersReused = 0;
    private int totalBuffersResized = 0;
    private int totalBuffersCompacted = 0;
    
    // Reusable NIO buffers to reduce allocations
    private FloatBuffer reuseFloatBuffer;
    private IntBuffer reuseIntBuffer;
    private final int MAX_REUSE_BUFFER_SIZE = 16384;
    
    public BufferManager() {
        // Initialize buffer maps for each type
        buffersByType.put(BUFFER_TYPE_SOLID_COLOR, new HashMap<>());
        buffersByType.put(BUFFER_TYPE_TEXTURE, new HashMap<>());
        buffersByType.put(BUFFER_TYPE_FONT, new HashMap<>());
        
        // Initialize reusable buffers
        reuseFloatBuffer = MemoryUtil.memAllocFloat(MAX_REUSE_BUFFER_SIZE);
        reuseIntBuffer = MemoryUtil.memAllocInt(MAX_REUSE_BUFFER_SIZE);
    }
    
    /**
     * Gets or creates a buffer configuration for solid color rendering
     * @param requiredVertices Number of vertices needed
     * @param requiredIndices Number of indices needed
     * @return The buffer configuration ID
     */
    public int getSolidColorBuffer(int requiredVertices, int requiredIndices) {
        return getOrCreateBuffer(BUFFER_TYPE_SOLID_COLOR, requiredVertices, requiredIndices);
    }
    
    /**
     * Gets or creates a buffer configuration for texture rendering
     * @param requiredVertices Number of vertices needed
     * @param requiredIndices Number of indices needed
     * @return The buffer configuration ID
     */
    public int getTextureBuffer(int requiredVertices, int requiredIndices) {
        return getOrCreateBuffer(BUFFER_TYPE_TEXTURE, requiredVertices, requiredIndices);
    }
    
    /**
     * Gets or creates a buffer configuration for font rendering
     * @param requiredVertices Number of vertices needed
     * @param requiredIndices Number of indices needed
     * @return The buffer configuration ID
     */
    public int getFontBuffer(int requiredVertices, int requiredIndices) {
        return getOrCreateBuffer(BUFFER_TYPE_FONT, requiredVertices, requiredIndices);
    }
    
    /**
     * Updates a solid color buffer with new data
     * @param bufferId The buffer ID
     * @param positions Vertex positions
     * @param colors Vertex colors
     * @param indices Vertex indices
     */
    public void updateSolidColorBuffer(int bufferId, float[] positions, float[] colors, int[] indices) {
        BufferConfig config = buffersByType.get(BUFFER_TYPE_SOLID_COLOR).get(bufferId);
        if (config == null) {
            throw new IllegalArgumentException("Invalid solid color buffer ID: " + bufferId);
        }
        
        glBindVertexArray(config.vaoId);
        
        // Update positions
        updateFloatBuffer(config.vboPositions, positions);
        
        // Update colors
        updateFloatBuffer(config.vboColors, colors);
        
        // Update indices
        updateIntBuffer(config.vboIndices, indices);
        
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }
    
    /**
     * Updates a texture buffer with new data
     * @param bufferId The buffer ID
     * @param positions Vertex positions
     * @param texCoords Texture coordinates
     * @param indices Vertex indices
     */
    public void updateTextureBuffer(int bufferId, float[] positions, float[] texCoords, int[] indices) {
        BufferConfig config = buffersByType.get(BUFFER_TYPE_TEXTURE).get(bufferId);
        if (config == null) {
            throw new IllegalArgumentException("Invalid texture buffer ID: " + bufferId);
        }
        
        glBindVertexArray(config.vaoId);
        
        // Update positions
        updateFloatBuffer(config.vboPositions, positions);
        
        // Update texture coordinates
        updateFloatBuffer(config.vboTexCoords, texCoords);
        
        // Update indices
        updateIntBuffer(config.vboIndices, indices);
        
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }
    
    /**
     * Updates a font buffer with new data
     * @param bufferId The buffer ID
     * @param positions Vertex positions
     * @param texCoords Texture coordinates
     * @param indices Vertex indices
     */
    public void updateFontBuffer(int bufferId, float[] positions, float[] texCoords, int[] indices) {
        BufferConfig config = buffersByType.get(BUFFER_TYPE_FONT).get(bufferId);
        if (config == null) {
            throw new IllegalArgumentException("Invalid font buffer ID: " + bufferId);
        }
        
        glBindVertexArray(config.vaoId);
        
        // Update positions
        updateFloatBuffer(config.vboPositions, positions);
        
        // Update texture coordinates
        updateFloatBuffer(config.vboTexCoords, texCoords);
        
        // Update indices
        updateIntBuffer(config.vboIndices, indices);
        
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }
    
    /**
     * Helper method to update a float buffer with minimal allocations
     */
    private void updateFloatBuffer(int vboId, float[] data) {
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        
        if (data.length <= MAX_REUSE_BUFFER_SIZE) {
            // Use the reusable buffer if possible
            reuseFloatBuffer.clear();
            reuseFloatBuffer.put(data);
            reuseFloatBuffer.flip();
            glBufferSubData(GL_ARRAY_BUFFER, 0, reuseFloatBuffer);
        } else {
            // Fall back to temporary allocation for large buffers
            FloatBuffer buffer = MemoryUtil.memAllocFloat(data.length);
            buffer.put(data).flip();
            glBufferSubData(GL_ARRAY_BUFFER, 0, buffer);
            MemoryUtil.memFree(buffer);
        }
    }
    
    /**
     * Helper method to update an int buffer with minimal allocations
     */
    private void updateIntBuffer(int vboId, int[] data) {
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vboId);
        
        if (data.length <= MAX_REUSE_BUFFER_SIZE) {
            // Use the reusable buffer if possible
            reuseIntBuffer.clear();
            reuseIntBuffer.put(data);
            reuseIntBuffer.flip();
            glBufferSubData(GL_ELEMENT_ARRAY_BUFFER, 0, reuseIntBuffer);
        } else {
            // Fall back to temporary allocation for large buffers
            IntBuffer buffer = MemoryUtil.memAllocInt(data.length);
            buffer.put(data).flip();
            glBufferSubData(GL_ELEMENT_ARRAY_BUFFER, 0, buffer);
            MemoryUtil.memFree(buffer);
        }
    }
    
    /**
     * Binds a buffer for rendering
     * @param bufferType The buffer type
     * @param bufferId The buffer ID
     */
    public void bindBuffer(int bufferType, int bufferId) {
        BufferConfig config = buffersByType.get(bufferType).get(bufferId);
        if (config == null) {
            throw new IllegalArgumentException("Invalid buffer ID: " + bufferId + " for buffer type: " + bufferType);
        }
        glBindVertexArray(config.vaoId);
    }
    
    /**
     * Unbinds the current buffer
     */
    public void unbindBuffer() {
        glBindVertexArray(0);
    }
    
    /**
     * Releases a buffer back to the pool
     * @param bufferType The buffer type
     * @param bufferId The buffer ID
     */
    public void releaseBuffer(int bufferType, int bufferId) {
        BufferConfig config = buffersByType.get(bufferType).get(bufferId);
        if (config == null) {
            throw new IllegalArgumentException("Invalid buffer ID: " + bufferId + " for buffer type: " + bufferType);
        }
        config.inUse = false;
        config.lastUsedTime = System.currentTimeMillis();
        config.useCount++;
    }
    
    /**
     * Gets or creates a buffer of the specified type and size
     */
    private int getOrCreateBuffer(int bufferType, int requiredVertices, int requiredIndices) {
        Map<Integer, BufferConfig> buffers = buffersByType.get(bufferType);
        
        // Find the appropriate size tier for this request
        int sizeVertices = getNextSizeTier(requiredVertices);
        int sizeIndices = getNextSizeTier(requiredIndices);
        
        // First try to find an exact match (optimal case)
        for (Map.Entry<Integer, BufferConfig> entry : buffers.entrySet()) {
            BufferConfig config = entry.getValue();
            if (!config.inUse && 
                config.maxVertices == sizeVertices && 
                config.maxIndices == sizeIndices) {
                config.inUse = true;
                config.lastUsedTime = System.currentTimeMillis();
                config.useCount++;
                totalBuffersReused++;
                return entry.getKey();
            }
        }
        
        // Next, try to find a buffer that's large enough but not too much larger
        BufferConfig bestFit = null;
        int bestFitId = -1;
        int bestWastedSpace = Integer.MAX_VALUE;
        
        for (Map.Entry<Integer, BufferConfig> entry : buffers.entrySet()) {
            BufferConfig config = entry.getValue();
            if (!config.inUse && 
                config.maxVertices >= sizeVertices && 
                config.maxIndices >= sizeIndices) {
                
                // Calculate wasted space as a metric for fit
                int wastedVertices = config.maxVertices - sizeVertices;
                int wastedIndices = config.maxIndices - sizeIndices;
                int wastedSpace = wastedVertices + wastedIndices;
                
                if (wastedSpace < bestWastedSpace) {
                    bestWastedSpace = wastedSpace;
                    bestFit = config;
                    bestFitId = entry.getKey();
                }
            }
        }
        
        if (bestFit != null) {
            bestFit.inUse = true;
            bestFit.lastUsedTime = System.currentTimeMillis();
            bestFit.useCount++;
            totalBuffersReused++;
            return bestFitId;
        }
        
        // If no suitable buffer found, create a new one
        int newBufferId = buffers.size();
        BufferConfig newConfig = createBuffer(bufferType, sizeVertices, sizeIndices);
        buffers.put(newBufferId, newConfig);
        newConfig.inUse = true;
        newConfig.lastUsedTime = System.currentTimeMillis();
        newConfig.useCount++;
        totalBuffersCreated++;
        
        // Periodically clean up expired buffers
        if (totalBuffersCreated % 100 == 0) {
            compactBuffers();
        }
        
        return newBufferId;
    }
    
    /**
     * Find the next size tier for a buffer
     */
    private int getNextSizeTier(int requiredSize) {
        for (int tier : BUFFER_SIZE_TIERS) {
            if (tier >= requiredSize) {
                return tier;
            }
        }
        // If larger than our largest tier, round up to next power of 2
        int size = 1;
        while (size < requiredSize) {
            size *= 2;
        }
        return size;
    }
    
    /**
     * Creates a new buffer configuration
     */
    private BufferConfig createBuffer(int bufferType, int maxVertices, int maxIndices) {
        int vaoId = glGenVertexArrays();
        glBindVertexArray(vaoId);
        
        // Create position VBO
        int vboPositions = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboPositions);
        glBufferData(GL_ARRAY_BUFFER, (long) maxVertices * 3 * Float.BYTES, GL_DYNAMIC_DRAW);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
        
        // Create color or texture coordinate VBO based on buffer type
        int vboColors = 0;
        int vboTexCoords = 0;
        
        if (bufferType == BUFFER_TYPE_SOLID_COLOR) {
            vboColors = glGenBuffers();
            glBindBuffer(GL_ARRAY_BUFFER, vboColors);
            glBufferData(GL_ARRAY_BUFFER, (long) maxVertices * 3 * Float.BYTES, GL_DYNAMIC_DRAW);
            glEnableVertexAttribArray(1);
            glVertexAttribPointer(1, 3, GL_FLOAT, false, 0, 0);
        } else {
            vboTexCoords = glGenBuffers();
            glBindBuffer(GL_ARRAY_BUFFER, vboTexCoords);
            glBufferData(GL_ARRAY_BUFFER, (long) maxVertices * 2 * Float.BYTES, GL_DYNAMIC_DRAW);
            glEnableVertexAttribArray(1);
            glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
        }
        
        // Create index VBO
        int vboIndices = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vboIndices);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, (long) maxIndices * Integer.BYTES, GL_DYNAMIC_DRAW);
        
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
        
        return new BufferConfig(vaoId, vboPositions, vboColors, vboTexCoords, vboIndices, maxVertices, maxIndices);
    }
    
    /**
     * Compacts the buffer pool by removing expired buffers
     */
    public void compactBuffers() {
        long currentTime = System.currentTimeMillis();
        
        for (int bufferType : buffersByType.keySet()) {
            Map<Integer, BufferConfig> buffers = buffersByType.get(bufferType);
            
            // Count buffers by size tier
            Map<Integer, List<Integer>> buffersBySize = new HashMap<>();
            
            // Identify candidates for removal
            List<Integer> buffersToRemove = new ArrayList<>();
            
            for (Map.Entry<Integer, BufferConfig> entry : buffers.entrySet()) {
                int bufferId = entry.getKey();
                BufferConfig config = entry.getValue();
                
                // Skip buffers that are in use
                if (config.inUse) {
                    continue;
                }
                
                // Group by vertex size (as the primary metric)
                int sizeKey = config.maxVertices;
                if (!buffersBySize.containsKey(sizeKey)) {
                    buffersBySize.put(sizeKey, new ArrayList<>());
                }
                buffersBySize.get(sizeKey).add(bufferId);
                
                // Check if buffer has expired
                if (currentTime - config.lastUsedTime > BUFFER_EXPIRATION_TIME) {
                    buffersToRemove.add(bufferId);
                }
            }
            
            // For each size tier, keep only MAX_UNUSED_BUFFERS_PER_SIZE buffers
            for (List<Integer> sizeGroup : buffersBySize.values()) {
                if (sizeGroup.size() > MAX_UNUSED_BUFFERS_PER_SIZE) {
                    // Sort by last used time (oldest first)
                    sizeGroup.sort((id1, id2) -> {
                        long time1 = buffers.get(id1).lastUsedTime;
                        long time2 = buffers.get(id2).lastUsedTime;
                        return Long.compare(time1, time2);
                    });
                    
                    // Mark excess buffers for removal
                    for (int i = 0; i < sizeGroup.size() - MAX_UNUSED_BUFFERS_PER_SIZE; i++) {
                        if (!buffersToRemove.contains(sizeGroup.get(i))) {
                            buffersToRemove.add(sizeGroup.get(i));
                        }
                    }
                }
            }
            
            // Remove the identified buffers
            for (int bufferId : buffersToRemove) {
                BufferConfig config = buffers.get(bufferId);
                
                glDeleteBuffers(config.vboPositions);
                if (config.vboColors != 0) {
                    glDeleteBuffers(config.vboColors);
                }
                if (config.vboTexCoords != 0) {
                    glDeleteBuffers(config.vboTexCoords);
                }
                glDeleteBuffers(config.vboIndices);
                glDeleteVertexArrays(config.vaoId);
                
                buffers.remove(bufferId);
                totalBuffersCompacted++;
            }
        }
    }
    
    /**
     * Returns statistics about buffer usage
     */
    public Map<String, Integer> getStatistics() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("totalBuffersCreated", totalBuffersCreated);
        stats.put("totalBuffersReused", totalBuffersReused);
        stats.put("totalBuffersResized", totalBuffersResized);
        stats.put("totalBuffersCompacted", totalBuffersCompacted);
        
        int currentBufferCount = 0;
        for (Map<Integer, BufferConfig> buffers : buffersByType.values()) {
            currentBufferCount += buffers.size();
        }
        stats.put("currentBufferCount", currentBufferCount);
        
        return stats;
    }
    
    /**
     * Cleans up all OpenGL resources
     */
    @Override
    public void close() {
        // Free the reusable buffers
        if (reuseFloatBuffer != null) {
            MemoryUtil.memFree(reuseFloatBuffer);
            reuseFloatBuffer = null;
        }
        
        if (reuseIntBuffer != null) {
            MemoryUtil.memFree(reuseIntBuffer);
            reuseIntBuffer = null;
        }
        
        // Delete all OpenGL buffers
        for (Map<Integer, BufferConfig> buffers : buffersByType.values()) {
            for (BufferConfig config : buffers.values()) {
                glDeleteBuffers(config.vboPositions);
                
                if (config.vboColors != 0) {
                    glDeleteBuffers(config.vboColors);
                }
                
                if (config.vboTexCoords != 0) {
                    glDeleteBuffers(config.vboTexCoords);
                }
                
                glDeleteBuffers(config.vboIndices);
                glDeleteVertexArrays(config.vaoId);
            }
            buffers.clear();
        }
        buffersByType.clear();
    }
} 