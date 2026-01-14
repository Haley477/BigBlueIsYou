package Particle;

import org.joml.Vector2f;
import edu.usu.audio.Sound;
import edu.usu.audio.SoundManager;
import edu.usu.graphics.Graphics2D;

import java.util.HashMap;
import java.util.Map;

/**
 * A comprehensive particle system manager that handles different types of particle effects.
 * This manager centralizes all particle systems and their renderers for the game.
 */
public class ParticleEffectsManager {
    private static final float SCREEN_WIDTH = 1380.0f;
    private static final float SCREEN_HEIGHT = 1380.0f;
    private static final float ACTUAL_TILE_SIZE = 32.0f;
    private int gridNumRows = 20;
    private int gridNumCols = 20;

    // Singleton instance
    private static ParticleEffectsManager instance;

    // Maps to store particle systems and renderers
    private final Map<EffectType, ParticleSystem> particleSystems = new HashMap<>();
    private final Map<EffectType, ParticleSystemRenderer> renderers = new HashMap<>();

    // Map to track active effects and their durations
    private final Map<EffectType, Double> activeEffects = new HashMap<>();

    // Track which effects have been triggered
    private final Map<EffectType, Boolean> triggeredEffects = new HashMap<>();

    // Fixed effect duration in seconds
    private static final double EFFECT_DURATION = 3.0;

    // Sound manager for audio effects
    private SoundManager audioManager;
    private Map<EffectType, Sound> soundEffects = new HashMap<>();

    // Texture paths for different effects
    private static final String FIRE_TEXTURE = "resources/images/fire.png";
    private static final String SPARKLE_TEXTURE = "resources/images/sparkle.jpeg";
    private static final String FIREWORKS_TEXTURE = "resources/images/fireworks.jpg";
    private static final String EXPLOSION_TEXTURE = "resources/images/explosion.png";
    private static final String SMOKE_TEXTURE = "resources/images/smoke.png";

    // Sound paths
    private static final String WIN_SOUND = "resources/audio/win.ogg";
    private static final String DEATH_SOUND = "resources/audio/death.ogg";
    private static final String EXPLOSION_SOUND = "resources/audio/explosion.ogg";
    private static final String MOVE_SOUND = "resources/audio/move.ogg";
    private static final String BACKGROUND_MUSIC = "resources/audio/background_music.ogg";
    private static final String IS_WIN_SOUND = "resources/audio/is_win.ogg";

    // Effect types
    public enum EffectType {
        PLAYER_DEATH,
        OBJECT_DEATH,
        OBJECT_WIN,
        EXPLOSION,
        SPARKLE,
        FIRE,
        FIREWORKS,
        SMOKE,
        MOVE,
        BACKGROUND_MUSIC,
        IS_WIN,
        YOU_CHANGE,  // New effect type for YOU property changes
        WIN_CHANGE   // New effect type for WIN property changes
    }

    /**
     * Private constructor to enforce singleton pattern
     */
    private ParticleEffectsManager() {
        // Initialize audio manager
        audioManager = new SoundManager();
    }

    /**
     * Get the singleton instance of the particle effects manager
     * @return ParticleEffectsManager instance
     */
    public static synchronized ParticleEffectsManager getInstance() {
        if (instance == null) {
            instance = new ParticleEffectsManager();
        }
        return instance;
    }

    /**
     * Initialize all particle systems and renderers
     */
    public void initialize() {
        // Initialize audio manager first
        if (audioManager == null) {
            audioManager = new SoundManager();
        }

        // Create particle systems for different effects with adjusted parameters
        createParticleSystem(EffectType.OBJECT_DEATH,
                new Vector2f(0, 0), 0.02f, 0.005f, 0.15f, 0.04f, 1.0f, 0.0f);

        createParticleSystem(EffectType.FIREWORKS,
                new Vector2f(0, 0), 0.015f, 0.004f, 0.2f, 0.05f, 2.0f, 0.0f);

        createParticleSystem(EffectType.SPARKLE,
                new Vector2f(0, 0), 0.5f, 0.1f, 0.08f, 0.03f, 1.5f, 0.0f);

        // YOU_CHANGE effect - subtle sparkles around the border
        createParticleSystem(EffectType.YOU_CHANGE,
                new Vector2f(0, 0), 0.015f, 0.005f, 0.1f, 0.02f, 0.8f, 0.1f);

        // WIN_CHANGE effect - more prominent sparkles around the border
        createParticleSystem(EffectType.WIN_CHANGE,
                new Vector2f(0, 0), 0.02f, 0.008f, 0.12f, 0.03f, 1.0f, 0.15f);

        // Disable continuous emission for all particle systems
        for (ParticleSystem system : particleSystems.values()) {
            system.setEmitting(false);
        }

        // Create renderers for each effect
        createRenderer(EffectType.OBJECT_DEATH, EXPLOSION_TEXTURE);
        createRenderer(EffectType.FIREWORKS, FIREWORKS_TEXTURE);
        createRenderer(EffectType.SPARKLE, SPARKLE_TEXTURE);
        createRenderer(EffectType.YOU_CHANGE, SPARKLE_TEXTURE);
        createRenderer(EffectType.WIN_CHANGE, SPARKLE_TEXTURE);

        // Load sound effects
        loadSoundEffect(EffectType.OBJECT_DEATH, "ObjectDeath", EXPLOSION_SOUND, false);
        loadSoundEffect(EffectType.FIREWORKS, "Fireworks", WIN_SOUND, false);
        loadSoundEffect(EffectType.MOVE, "Move", MOVE_SOUND, false);
        loadSoundEffect(EffectType.BACKGROUND_MUSIC, "Music", BACKGROUND_MUSIC, true);
        loadSoundEffect(EffectType.IS_WIN, "IsWin", IS_WIN_SOUND, false);
    }

    /**
     * Create a particle system for a specific effect type
     */
    private void createParticleSystem(EffectType type, Vector2f center, float sizeMean,
                                      float sizeStdDev, float speedMean, float speedStdDev,
                                      float lifetimeMean, float lifetimeStdDev) {
        ParticleSystem system = new ParticleSystem(center, sizeMean, sizeStdDev,
                speedMean, speedStdDev, lifetimeMean, lifetimeStdDev);
        particleSystems.put(type, system);
    }

    /**
     * Create a renderer for a specific effect type
     */
    private void createRenderer(EffectType type, String texturePath) {
        ParticleSystemRenderer renderer = new ParticleSystemRenderer();
        renderer.initialize(texturePath);
        renderers.put(type, renderer);
    }

    /**
     * Load a sound effect for a specific effect type
     */
    private void loadSoundEffect(EffectType type, String name, String soundPath, boolean loop) {
        try {
            Sound sound = audioManager.load(name, soundPath, loop);
            if (sound != null) {
                soundEffects.put(type, sound);
            } else {
                System.err.println("Failed to load sound: " + soundPath);
            }
        } catch (Exception e) {
            System.err.println("Error loading sound " + soundPath + ": " + e.getMessage());
        }
    }

    /**
     * Update all active particle systems and track effect durations
     * @param elapsedTime Time elapsed since last update
     */
    public void update(double elapsedTime) {
        // Update all systems
        for (ParticleSystem system : particleSystems.values()) {
            system.update(elapsedTime);
        }

        // Update active effect timers and remove expired effects
        Map<EffectType, Double> updatedEffects = new HashMap<>();
        for (Map.Entry<EffectType, Double> entry : activeEffects.entrySet()) {
            EffectType type = entry.getKey();
            double remainingTime = entry.getValue() - elapsedTime;

            if (remainingTime <= 0) {
                // Effect expired, clear the particles
                ParticleSystem system = particleSystems.get(type);
                if (system != null) {
                    system.clearParticles();
                }
            } else {
                // Effect still active
                updatedEffects.put(type, remainingTime);
            }
        }

        // Replace with updated timers
        activeEffects.clear();
        activeEffects.putAll(updatedEffects);
    }

    /**
     * Render all active particle systems
     * @param graphics Graphics context to render to
     */
    public void render(Graphics2D graphics) {
        for (Map.Entry<EffectType, ParticleSystem> entry : particleSystems.entrySet()) {
            EffectType type = entry.getKey();
            ParticleSystem system = entry.getValue();
            ParticleSystemRenderer renderer = renderers.get(type);

            // Only render if the effect is active (in the activeEffects map)
            if (activeEffects.containsKey(type) && renderer != null) {
                renderer.render(graphics, system);
            }
        }
    }

    /**
     * Clean up resources
     */
    public void cleanup() {
        // Stop all sounds
        for (Sound sound : soundEffects.values()) {
            if (sound != null) {
                sound.stop();
            }
        }

        // Clear sound effects
        soundEffects.clear();

        // Clear particle systems
        particleSystems.clear();

        // Clear renderers
        renderers.clear();

        // Clear active effects
        activeEffects.clear();

        // Clean up audio manager
        if (audioManager != null) {
            audioManager.cleanup();
            audioManager = null;
        }

        triggeredEffects.clear(); // Clear triggered effects tracking
    }

    /**
     * Trigger player death effect
     */
    public void playerDeath(Vector2f position) {
        triggerEffect(EffectType.PLAYER_DEATH, position);
    }

    /**
     * Trigger object death effect
     */
    public void objectDeath(Vector2f position) {
        ParticleSystem system = particleSystems.get(EffectType.OBJECT_DEATH);
        if (system != null) {
            system.setCenter(position);
            system.generateBurst(20);  // Generate a burst of particles
            activeEffects.put(EffectType.OBJECT_DEATH, EFFECT_DURATION);
            playSound(EffectType.OBJECT_DEATH);
        }
    }

    /**
     * Trigger object win effect
     */
    public void objectWin(Vector2f position) {
        ParticleSystem system = particleSystems.get(EffectType.FIREWORKS);
        if (system != null) {
            system.setCenter(position);
            system.generateBurst(30);  // Generate more particles for fireworks
            activeEffects.put(EffectType.FIREWORKS, EFFECT_DURATION * 2);  // Longer duration for fireworks
            playSound(EffectType.FIREWORKS);
        }
    }

    /**
     * Trigger explosion effect
     */
    public void explosion(Vector2f position) {
        triggerEffect(EffectType.EXPLOSION, position);
    }

    /**
     * Trigger fire effect
     */
    public void fire(Vector2f position) {
        triggerEffect(EffectType.FIRE, position);
    }

    /**
     * Trigger sparkle effect at specified position
     * @param position Position to trigger the effect
     */
    public void sparkle(Vector2f position) {
        // Create a sparkle effect with smaller, slower, shorter-lived particles
        ParticleSystem sparkle = new ParticleSystem(
                position,
                0.02f,  // Very small mean size
                0.01f, // Very small size variation
                0.002f,  // Very slow speed
                0.001f,  // Small speed variation
                0.01f,  // Very short lifetime
                0.02f   // Small lifetime variation
        );
        sparkle.setEmissionRate(3);  // Fewer particles per update
        sparkle.generateBurst(15);   // Fewer particles in burst
        sparkle.setRotationSpeed(0.5f);  // Very slow rotation
        particleSystems.put(EffectType.SPARKLE, sparkle);
        activeEffects.put(EffectType.SPARKLE, EFFECT_DURATION);
    }

    /**
     * Trigger smoke effect at specified position
     * @param position Position to trigger the effect
     */
    public void smoke(Vector2f position) {
        triggerEffect(EffectType.SMOKE, position);
    }

    /**
     * Play the move sound effect
     */
    public void playMoveSound() {
        playSound(EffectType.MOVE);
    }

    /**
     * Play the background music
     */
    public void playBackgroundMusic() {
        playSound(EffectType.BACKGROUND_MUSIC);
    }

    /**
     * Stop the background music
     */
    public void stopBackgroundMusic() {
        stopSound(EffectType.BACKGROUND_MUSIC);
    }

    /**
     * Play the is-win sound effect
     */
    public void playIsWinSound() {
        playSound(EffectType.IS_WIN);
        // Trigger fireworks effect at the center of the screen
        float screenX = 960.0f; // Middle of 1920 width
        float screenY = 540.0f; // Middle of 1080 height
        ParticleSystem fireworks = new ParticleSystem(
                new Vector2f(screenX, screenY),
                0.03f,  // Small mean size
                0.01f,  // Small size variation
                0.2f,   // Moderate speed
                0.1f,   // Moderate speed variation
                0.5f,   // Longer lifetime
                0.2f    // Moderate lifetime variation
        );
        fireworks.setEmissionRate(10);  // More particles per update
        fireworks.generateBurst(50);    // More particles in burst
        fireworks.setRotationSpeed(2.0f);  // Moderate rotation
        particleSystems.put(EffectType.FIREWORKS, fireworks);
        activeEffects.put(EffectType.FIREWORKS, EFFECT_DURATION);
    }

    public void playIsYouSound() {
        playSound(EffectType.YOU_CHANGE);
    }

    public void playIsKillSound() {
        playSound(EffectType.OBJECT_DEATH);
    }

    /**
     * Trigger YOU property change effect (sparkle)
     */
    public void youChange(Vector2f position) {
        ParticleSystem system = particleSystems.get(EffectType.YOU_CHANGE);
        if (system != null) {
            system.setCenter(position);
            system.generateBurst(12);  // Generate fewer, more tasteful sparkle particles
            activeEffects.put(EffectType.YOU_CHANGE, EFFECT_DURATION * 0.8);  // Shorter duration
        }
    }

    /**
     * Trigger WIN property change effect (sparkle)
     */
    public void winChange(Vector2f position) {
        ParticleSystem system = particleSystems.get(EffectType.WIN_CHANGE);
        if (system != null) {
            system.setCenter(position);
            system.generateBurst(15);  // Generate more sparkle particles for WIN
            activeEffects.put(EffectType.WIN_CHANGE, EFFECT_DURATION * 1.2);  // Longer duration
            playSound(EffectType.IS_WIN);
        }
    }

    /**
     * Trigger a particle effect at a specific position
     */
    public void triggerEffect(EffectType type, Vector2f position) {
        // Check if this effect has already been triggered
        if (triggeredEffects.containsKey(type) && triggeredEffects.get(type)) {
            return; // Skip if already triggered
        }

        ParticleSystem system = particleSystems.get(type);
        if (system != null) {
            system.setCenter(position);
            system.setEmitting(true);
            activeEffects.put(type, EFFECT_DURATION);

            // Play associated sound effect
            switch (type) {
                case PLAYER_DEATH:
                    playSound(EffectType.PLAYER_DEATH);
                    break;
                case OBJECT_DEATH:
                    playSound(EffectType.OBJECT_DEATH);
                    break;
                case OBJECT_WIN:
                    playSound(EffectType.OBJECT_WIN);
                    break;
                case EXPLOSION:
                    playSound(EffectType.EXPLOSION);
                    break;
            }
        } else {
            System.out.println("[DEBUG] No particle system found for effect type: " + type);
        }
    }

    /**
     * Play a sound effect
     */
    private void playSound(EffectType type) {
        Sound sound = soundEffects.get(type);
        if (sound != null) {
            sound.play();
        }
    }

    /**
     * Stop a sound effect
     */
    private void stopSound(EffectType type) {
        Sound sound = soundEffects.get(type);
        if (sound != null) {
            sound.stop();
        }
    }

    /**
     * Trigger YOU property change effect at a grid position
     * @param gridX X coordinate in the grid
     * @param gridY Y coordinate in the grid
     */
    public void youChangeAtGridPosition(float gridX, float gridY) {
        Vector2f screenPos = gridToScreenCoordinates(gridX, gridY);
        youChange(screenPos);
    }

    /**
     * Trigger WIN property change effect at a grid position
     * @param gridX X coordinate in the grid
     * @param gridY Y coordinate in the grid
     */
    public void winChangeAtGridPosition(float gridX, float gridY) {
        Vector2f screenPos = gridToScreenCoordinates(gridX, gridY);
        winChange(screenPos);
    }

    /**
     * Trigger object death effect at a grid position
     * @param gridX X coordinate in the grid
     * @param gridY Y coordinate in the grid
     */
    public void objectDeathAtGridPosition(float gridX, float gridY) {
        Vector2f screenPos = gridToScreenCoordinates(gridX, gridY);
        objectDeath(screenPos);
    }

    /**
     * Trigger object win effect at a grid position
     * @param gridX X coordinate in the grid
     * @param gridY Y coordinate in the grid
     */
    public void objectWinAtGridPosition(float gridX, float gridY) {
        Vector2f screenPos = gridToScreenCoordinates(gridX, gridY);
        objectWin(screenPos);
    }

    /**
     * Convert grid coordinates to screen coordinates
     * @param gridX X coordinate in the grid
     * @param gridY Y coordinate in the grid
     * @return Vector2f containing screen coordinates
     */
    private Vector2f gridToScreenCoordinates(float gridX, float gridY) {
        float centerX = SCREEN_WIDTH / 2.0f;
        float centerY = SCREEN_HEIGHT / 2.0f;
        float posX = (int)(gridX * ACTUAL_TILE_SIZE + ACTUAL_TILE_SIZE / 2 + centerX - (gridNumCols * ACTUAL_TILE_SIZE / 2));
        float posY = (int)(gridY * ACTUAL_TILE_SIZE + ACTUAL_TILE_SIZE / 2 + centerY - (gridNumRows * ACTUAL_TILE_SIZE / 2));
        float normalizedX = (posX / SCREEN_WIDTH) * 2.0f - 1.0f;
        float normalizedY = (posY / SCREEN_HEIGHT) * 2.0f - 1.0f;
        return new Vector2f(normalizedX, normalizedY);
    }

    /**
     * Set the grid dimensions for coordinate conversion
     * @param rows Number of rows in the grid
     * @param cols Number of columns in the grid
     */
    public void setGridDimensions(int rows, int cols) {
        this.gridNumRows = rows;
        this.gridNumCols = cols;
    }

    /**
     * Clears all particle effects from all systems
     */
    public void clearAllEffects() {
        for (ParticleSystem system : particleSystems.values()) {
            system.clearParticles();
        }
    }
}