package ecs.Systems;

import ecs.Components.Movable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static org.lwjgl.glfw.GLFW.*;

public class KeyboardInput extends System {

    private final long window;
    private final HashMap<Integer, Boolean> keysPressed;
    private boolean registered;

    public KeyboardInput(long window) {
        super(ecs.Components.Movable.class, ecs.Components.KeyboardControlled.class);

        this.window = window;
        keysPressed = new HashMap<>();
        registerCommands();
    }

    public void registerCommand(int key, Movable.MoveTo action, boolean keyPressOnly) {
        keysPressed.put(key, false);
    }

    private void registerCommands() {
        for (var entity : entities.values()) {
            var input = entity.get(ecs.Components.KeyboardControlled.class);
            for (var entry : input.keys.entrySet()) {
                registerCommand(entry.getKey(), entry.getValue(), true);
            }
            break;
        }
    }

    @Override
    public Set<EntityUpdate> update(double elapsedTime) {
        Set<EntityUpdate> changedEntities = new HashSet<>();

        for (var entity : entities.values()) {
            var movable = entity.get(ecs.Components.Movable.class);
            var input = entity.get(ecs.Components.KeyboardControlled.class);
            boolean moved = false;

            // Check each movement direction using the key mappings from KeyboardControlled
            for (var entry : input.keys.entrySet()) {
                int key = entry.getKey();
                boolean isPressed = glfwGetKey(window, key) == GLFW_PRESS;
                Boolean wasPressed = keysPressed.get(key);

                // Initialize key state if it doesn't exist
                if (wasPressed == null) {
                    wasPressed = false;
                    keysPressed.put(key, false);
                }

                // Only move if this is a new key press
                if (isPressed && !wasPressed) {
                    movable.moveTo = entry.getValue();
                    moved = true;
                }

                // Update the key state
                keysPressed.put(key, isPressed);
            }

            if (moved) {
                changedEntities.add(new EntityUpdate(entity, false));
            }
        }

        return changedEntities;
    }
}