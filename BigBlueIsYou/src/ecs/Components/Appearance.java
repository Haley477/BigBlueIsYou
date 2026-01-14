package ecs.Components;

import edu.usu.graphics.Texture;

public class Appearance extends Component {
    public Texture image;
    public float size;
    private boolean isAnimated;
    private int currentFrame;
    private int totalFrames;
    private float frameDuration;
    private float timeSinceLastFrame;

    public Appearance(Texture image, float size) {
        this.image = image;
        this.size = size;
        this.isAnimated = false;
        this.currentFrame = 0;
        this.totalFrames = 1;
        this.frameDuration = 0;
        this.timeSinceLastFrame = 0;
    }

    public Appearance(Texture image, float size, int totalFrames, float frameDuration) {
        this.image = image;
        this.size = size;
        this.isAnimated = true;
        this.currentFrame = 0;
        this.totalFrames = totalFrames;
        this.frameDuration = frameDuration;
        this.timeSinceLastFrame = 0;
    }

    public boolean isAnimated() {
        return isAnimated;
    }

    public int getCurrentFrame() {
        return currentFrame;
    }

    public void setCurrentFrame(int currentFrame) {
        this.currentFrame = currentFrame;
    }

    public int getTotalFrames() {
        return totalFrames;
    }

    public float getFrameDuration() {
        return frameDuration;
    }

    public float getTimeSinceLastFrame() {
        return timeSinceLastFrame;
    }

    public void setTimeSinceLastFrame(float timeSinceLastFrame) {
        this.timeSinceLastFrame = timeSinceLastFrame;
    }
}
