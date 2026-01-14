package Particle;

import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 * Enhanced ParticleSystem that manages particle generation and updates
 * with added functionality for bursts and position control.
 */
public class ParticleSystem {
    private final HashMap<Long, Particle> particles = new HashMap<>();
    private final MyRandom random = new MyRandom();

    // System parameters
    private Vector2f center;
    private final float sizeMean;
    private final float sizeStdDev;
    private final float speedMean;
    private final float speedStdDev;
    private final float lifetimeMean;
    private final float lifetimeStdDev;

    // Controls for emission
    private boolean isEmitting = true;
    private int emissionRate = 8;  // Particles per update
    private float rotationSpeed = 1.0f;

    /**
     * Create a new particle system
     *
     * @param center Initial center position
     * @param sizeMean Mean size of particles
     * @param sizeStdDev Standard deviation of particle size
     * @param speedMean Mean speed of particles
     * @param speedStdDev Standard deviation of particle speed
     * @param lifetimeMean Mean lifetime of particles
     * @param lifetimeStdDev Standard deviation of particle lifetime
     */
    public ParticleSystem(Vector2f center, float sizeMean, float sizeStdDev,
                          float speedMean, float speedStdDev,
                          float lifetimeMean, float lifetimeStdDev) {
        this.center = new Vector2f(center);
        this.sizeMean = sizeMean;
        this.sizeStdDev = sizeStdDev;
        this.speedMean = speedMean;
        this.speedStdDev = speedStdDev;
        this.lifetimeMean = lifetimeMean;
        this.lifetimeStdDev = lifetimeStdDev;
    }

    /**
     * Update all particles in the system
     *
     * @param elapsedTime Time elapsed since last update
     */
    public void update(double elapsedTime) {
        // Update existing particles
        List<Long> removeMe = new ArrayList<>();
        for (Particle p : particles.values()) {
            if (!p.update(elapsedTime)) {
                removeMe.add(p.name);
            }
        }

        // Remove dead particles
        for (Long key : removeMe) {
            particles.remove(key);
        }

        // Generate new particles if emitting
        if (isEmitting) {
            for (int i = 0; i < emissionRate; i++) {
                var particle = create();
                particles.put(particle.name, particle);
            }
        }
    }

    /**
     * Get all active particles in the system
     *
     * @return Collection of active particles
     */
    public Collection<Particle> getParticles() {
        return this.particles.values();
    }

    /**
     * Generate a burst of particles at once
     *
     * @param count Number of particles to generate
     */
    public void generateBurst(int count) {
        for (int i = 0; i < count; i++) {
            var particle = create();
            particles.put(particle.name, particle);
        }
    }

    /**
     * Set the center position of the particle system
     *
     * @param center New center position
     */
    public void setCenter(Vector2f center) {
        this.center = new Vector2f(center);
    }

    /**
     * Get the current center position of the particle system
     *
     * @return Current center position
     */
    public Vector2f getCenter() {
        return new Vector2f(this.center);
    }

    /**
     * Set whether the system is continuously emitting particles
     *
     * @param emitting True to enable continuous emission, false to disable
     */
    public void setEmitting(boolean emitting) {
        this.isEmitting = emitting;
    }

    /**
     * Set the emission rate (particles per update)
     *
     * @param rate Number of particles to emit per update
     */
    public void setEmissionRate(int rate) {
        this.emissionRate = rate;
    }

    /**
     * Set the rotation speed multiplier for particles
     *
     * @param speed Rotation speed multiplier
     */
    public void setRotationSpeed(float speed) {
        this.rotationSpeed = speed;
    }

    /**
     * Clear all particles from the system
     */
    public void clearParticles() {
        particles.clear();
    }

    /**
     * Create a new particle using the system parameters
     *
     * @return Newly created particle
     */
    private Particle create() {
        float size = (float) this.random.nextGaussian(this.sizeMean, this.sizeStdDev);

        // Generate position along the edge of the tile
        Vector2f edgePosition = new Vector2f(this.center);
        float angle = (float) (this.random.nextDouble() * 2.0 * Math.PI);
        float radius = 0.025f; // Reduced from 0.05f to make the circle smaller
        edgePosition.x += radius * (float) Math.cos(angle);
        edgePosition.y += radius * (float) Math.sin(angle);

        // Calculate direction vector pointing outward from the center
        Vector2f direction = new Vector2f(
            (float) Math.cos(angle),
            (float) Math.sin(angle)
        );

        // Create the particle
        var p = new Particle(
                edgePosition,
                direction,
                (float) this.random.nextGaussian(this.speedMean, this.speedStdDev),
                new Vector2f(size, size),
                this.random.nextGaussian(this.lifetimeMean, this.lifetimeStdDev));

        // Set rotation speed based on system setting
        p.setRotationSpeed(this.rotationSpeed * (p.getSpeed() / 0.5f));

        return p;
    }
}