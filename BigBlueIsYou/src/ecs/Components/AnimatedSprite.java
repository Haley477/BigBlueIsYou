package ecs.Components;

/**
 * Handles animation logic for sprites
 */
public class AnimatedSprite {
    private Appearance appearance;
    private float[] frameTimes;
    private double animationTime;
    private int currentFrame;

    /**
     * Creates a new AnimatedSprite
     * @param appearance The appearance to animate
     * @param frameTimes Array of times for each frame in seconds
     */
    public AnimatedSprite(Appearance appearance, float[] frameTimes) {
        this.appearance = appearance;
        this.frameTimes = frameTimes;
        this.animationTime = 0;
        this.currentFrame = 0;
        
        // Set up the appearance for animation
        appearance.setCurrentFrame(0);
        appearance.setTimeSinceLastFrame(0);
    }

    /**
     * Updates the animation based on elapsed time
     * @param elapsedTime Time elapsed since last update in seconds
     */
    public void update(double elapsedTime) {
        animationTime += elapsedTime;
        
        // Check if it's time to move to the next frame
        if (animationTime >= frameTimes[currentFrame]) {
            animationTime -= frameTimes[currentFrame];
            currentFrame = (currentFrame + 1) % frameTimes.length;
            
            // Update the appearance's current frame
            appearance.setCurrentFrame(currentFrame);
            appearance.setTimeSinceLastFrame((float)animationTime);
        }
    }

    /**
     * Gets the current frame index
     * @return Current frame index
     */
    public int getCurrentFrame() {
        return currentFrame;
    }

    /**
     * Gets the total number of frames
     * @return Total number of frames
     */
    public int getTotalFrames() {
        return frameTimes.length;
    }

    /**
     * Gets the appearance being animated
     * @return The appearance component
     */
    public Appearance getAppearance() {
        return appearance;
    }
} 