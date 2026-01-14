package Particle;

import org.joml.Vector2f;
import edu.usu.graphics.Rectangle;

/**
 * Enhanced Particle class representing a single particle in a particle system
 */
public class Particle {
    // Public properties
    public long name;
    public Vector2f size;
    public Vector2f center;
    public Rectangle area;
    public float rotation;

    // Private properties
    private Vector2f direction;
    private float speed;
    private double lifetime;
    private double alive = 0;
    private float rotationSpeed;
    private static long nextName = 0;

    // Optional properties for advanced effects
    private float alpha = 1.0f;
    private float alphaDecay = 0.0f;
    private float scaleRate = 0.0f;

    /**
     * Create a new particle
     *
     * @param center Initial position
     * @param direction Direction vector (normalized)
     * @param speed Movement speed
     * @param size Size dimensions
     * @param lifetime Time to live in seconds
     */
    public Particle(Vector2f center, Vector2f direction, float speed, Vector2f size, double lifetime) {
        this.name = nextName++;
        this.center = new Vector2f(center);
        this.direction = new Vector2f(direction);
        this.speed = speed;
        this.size = new Vector2f(size);
        this.area = new Rectangle(center.x - size.x / 2, center.y - size.y / 2, size.x, size.y);
        this.lifetime = lifetime;
        this.rotation = 0;
        this.rotationSpeed = speed / 0.5f;

        // Alpha will decay to 0 by the end of lifetime
        this.alphaDecay = 1.0f / (float)lifetime;
    }

    /**
     * Update the particle state
     *
     * @param elapsedTime Time elapsed since last update
     * @return True if the particle is still alive, false if it should be removed
     */
    public boolean update(double elapsedTime) {
        // Update how long it has been alive
        alive += elapsedTime;

        // Return false if this particle is dead
        if (alive >= lifetime) {
            return false;
        }

        // Update its center
        center.x += (float) (elapsedTime * speed * direction.x);
        center.y += (float) (elapsedTime * speed * direction.y);

        // Update its area
        area.left = center.x - size.x / 2;
        area.top = center.y - size.y / 2;
        area.width = size.x;
        area.height = size.y;

        // Rotate proportional to its rotation speed
        rotation += rotationSpeed * elapsedTime;

        // Calculate alpha based on lifetime percentage for smooth fade-out
        // This ensures particles fade out as they approach the end of their lifetime
        alpha = 1.0f - (float)(alive / lifetime);
        if (alpha < 0) alpha = 0;

        // Update size if scaling
        if (scaleRate != 0) {
            size.x += scaleRate * elapsedTime;
            size.y += scaleRate * elapsedTime;

            // Ensure size doesn't go negative
            if (size.x < 0) size.x = 0;
            if (size.y < 0) size.y = 0;
        }

        // Particle is still alive
        return true;
    }

    /**
     * Get the current alpha value (opacity)
     *
     * @return Alpha value between 0.0 and 1.0
     */
    public float getAlpha() {
        return alpha;
    }

    /**
     * Set the rate at which the particle changes size
     *
     * @param rate Size change per second (positive grows, negative shrinks)
     */
    public void setScaleRate(float rate) {
        this.scaleRate = rate;
    }

    /**
     * Set the particle's rotation speed
     *
     * @param speed Rotation speed in radians per second
     */
    public void setRotationSpeed(float speed) {
        this.rotationSpeed = speed;
    }

    /**
     * Get the particle's current speed
     *
     * @return Current speed
     */
    public float getSpeed() {
        return speed;
    }

    /**
     * Set the particle's movement speed
     *
     * @param speed New speed
     */
    public void setSpeed(float speed) {
        this.speed = speed;
    }

    /**
     * Change the particle's direction
     *
     * @param direction New direction vector (should be normalized)
     */
    public void setDirection(Vector2f direction) {
        this.direction = new Vector2f(direction);
    }

    /**
     * Get the percentage of lifetime completed
     *
     * @return Value between 0.0 and 1.0 representing percentage complete
     */
    public float getLifetimePercentage() {
        return (float)(alive / lifetime);
    }
}