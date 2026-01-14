package Particle;

import edu.usu.graphics.Color;
import edu.usu.graphics.Graphics2D;
import edu.usu.graphics.Texture;

/**
 * Enhanced renderer for particle systems with support for alpha blending and color tinting
 */
public class ParticleSystemRenderer {
    private Texture texParticle;
    private Color tintColor = Color.WHITE;
    private boolean useParticleAlpha = true;

    /**
     * Initialize the renderer with a texture
     *
     * @param filenameTexture Path to the texture file
     */
    public void initialize(String filenameTexture) {
        texParticle = new Texture(filenameTexture);
    }

    /**
     * Clean up resources
     */
    public void cleanup() {
        if (texParticle != null) {
            texParticle.cleanup();
        }
    }

    /**
     * Render all particles in a particle system
     *
     * @param graphics Graphics context
     * @param system Particle system to render
     */
    public void render(Graphics2D graphics, ParticleSystem system) {
        for (var particle : system.getParticles()) {
            // Create color with appropriate alpha if enabled
            Color particleColor;
            if (useParticleAlpha) {
                particleColor = new Color(
                        tintColor.r,
                        tintColor.g,
                        tintColor.b,
                        tintColor.a * particle.getAlpha()
                );
            } else {
                particleColor = tintColor;
            }

            // Set z-value to ensure particles are rendered on top
            particle.area.z = 1.0f;  // Higher than the tile z-values

            // Draw the particle
            graphics.draw(texParticle, particle.area, particle.rotation, particle.center, particleColor);
        }
    }

    /**
     * Set the tint color for all particles
     *
     * @param color Color to tint particles with
     */
    public void setTintColor(Color color) {
        this.tintColor = color;
    }

    /**
     * Set whether to use per-particle alpha values
     *
     * @param useAlpha True to use per-particle alpha, false to use only tint color alpha
     */
    public void setUseParticleAlpha(boolean useAlpha) {
        this.useParticleAlpha = useAlpha;
    }

    /**
     * Get the current texture
     *
     * @return Current particle texture
     */
    public Texture getTexture() {
        return texParticle;
    }

    /**
     * Change the texture
     *
     * @param filenameTexture Path to the new texture file
     */
    public void setTexture(String filenameTexture) {
        if (texParticle != null) {
            texParticle.cleanup();
        }
        texParticle = new Texture(filenameTexture);
    }
}