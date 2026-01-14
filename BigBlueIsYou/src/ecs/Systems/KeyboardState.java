package ecs.Systems;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Class to store configurable keyboard controls
 */
public class KeyboardState {
    public int moveUpKey;
    public int moveDownKey;
    public int moveLeftKey;
    public int moveRightKey;
    

    public KeyboardState(int up, int down, int left, int right) {
        moveUpKey = up;
        moveDownKey = down;
        moveLeftKey = left;
        moveRightKey = right;
    }
}