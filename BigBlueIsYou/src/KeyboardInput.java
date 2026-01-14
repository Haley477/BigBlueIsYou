import java.util.HashMap;

import static org.lwjgl.glfw.GLFW.*;

public class KeyboardInput {

    /**
     * The type of method to invoke when a keyboard event is invoked
     */
    public interface ICommand {
        void invoke(double elapsedTime);
    }

    public KeyboardInput(long window) {
        this.window = window;
    }

    public void registerCommand(int key, boolean keyPressOnly, ICommand callback) {
        commandEntries.put(key, new CommandEntry(key, keyPressOnly, callback));
        // Start out by assuming the key isn't currently pressed
        keysPressed.put(key, false);
    }

    /**
     * Go through all the registered command and invoke the callbacks as appropriate
     */
    public void update(double elapsedTime) {
        // First, update all key states
        for (var entry : commandEntries.entrySet()) {
            boolean isPressed = glfwGetKey(window, entry.getKey()) == GLFW_PRESS;
            boolean wasPressed = keysPressed.get(entry.getKey());
            
            // Only trigger the callback if it's a new press or if it's a hold command
            if (entry.getValue().keyPressOnly) {
                if (isPressed && !wasPressed) {
                    entry.getValue().callback.invoke(elapsedTime);
                }
            } else if (isPressed) {
                entry.getValue().callback.invoke(elapsedTime);
            }
            
            // Update the key state
            keysPressed.put(entry.getKey(), isPressed);
        }
    }

    /**
     * Returns true if the key is newly pressed.  If it was already pressed, then
     * it returns false
     */
    private boolean isKeyNewlyPressed(int key) {
        boolean isPressed = glfwGetKey(window, key) == GLFW_PRESS;
        boolean wasPressed = keysPressed.get(key);
        return isPressed && !wasPressed;
    }

    /**
     * Clears all key states, useful when transitioning between states
     */
    public void clearState() {
        for (var key : keysPressed.keySet()) {
            keysPressed.put(key, false);
        }

    }

    private final long window;
    // Table of registered callbacks
    private final HashMap<Integer, CommandEntry> commandEntries = new HashMap<>();
    // Table of registered callback keys previous pressed state
    private final HashMap<Integer, Boolean> keysPressed = new HashMap<>();

    /**
     * Used to keep track of the details associated with a registered command
     */
    private record CommandEntry(int key, boolean keyPressOnly, ICommand callback) {
    }
}

