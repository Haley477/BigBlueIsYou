import edu.usu.graphics.Color;
import edu.usu.graphics.Font;
import edu.usu.graphics.Graphics2D;
import static org.lwjgl.glfw.GLFW.*;

public class ControlsView extends GameStateView {

    private KeyboardInput inputKeyboard;
    private GameStateEnum nextGameState = GameStateEnum.Controls;
    private Font font;

    private KeyboardState keyboardState;
    private Serializer serializer;


    private int selectedActionIndex = 0;  // Index of the action currently being edited (0 - Move Up, 1 - Move Down, etc.)
    private boolean isRemappingKey = false; // Flag to track if we are remapping a key



    @Override
    public void initialize(Graphics2D graphics) {
        super.initialize(graphics);
        this.inputKeyboard = new KeyboardInput(graphics.getWindow());
        this.serializer = new Serializer();

        font = new Font("resources/fonts/Roboto-Regular.ttf", 48, false);
        this.keyboardState = new KeyboardState();

        if (!this.keyboardState.initialized){
            this.keyboardState = new KeyboardState(
                    GLFW_KEY_S,  // Move Up (was W)
                    GLFW_KEY_W,  // Move Down (was S)
                    GLFW_KEY_D,  // Move Left (was A)
                    GLFW_KEY_A,  // Move Right (was D)
                    GLFW_KEY_R,  // Reset Level
                    GLFW_KEY_Z   // Undo
            );
        }

        serializer.loadGameState(this.keyboardState);

        // When ESC is pressed, set the appropriate new game state
        inputKeyboard.registerCommand(GLFW_KEY_ESCAPE, true, (double elapsedTime) -> {
            nextGameState = GameStateEnum.MainMenu;
        });

        // Register commands to change key bindings
        inputKeyboard.registerCommand(GLFW_KEY_DOWN, true, (double elapsed) -> {
            if(!isRemappingKey && selectedActionIndex != 5) {
                selectedActionIndex = selectedActionIndex + 1;
            }else if (!isRemappingKey) {
                selectedActionIndex = 0;
            }

        });
        inputKeyboard.registerCommand(GLFW_KEY_UP, true, (double elapsed) -> {
            if(!isRemappingKey && selectedActionIndex == 0 ) {
                selectedActionIndex = 5;
            }
            else if (!isRemappingKey) {
                selectedActionIndex = selectedActionIndex - 1;
            }
        });
        inputKeyboard.registerCommand(GLFW_KEY_ENTER, true, (double elapsed) -> {
            if(isRemappingKey) {
                isRemappingKey = false;
            } else if (selectedActionIndex >= 0) {
                startRemapping(selectedActionIndex);
            }
        });
    }

    private void startRemapping(int actionIndex){
        selectedActionIndex = actionIndex;
        isRemappingKey = true;
    }

    @Override
    public void initializeSession() {
        nextGameState = GameStateEnum.Controls;
    }

    @Override
    public GameStateEnum processInput(double elapsedTime) {
        // Updating the keyboard can change the nextGameState
        inputKeyboard.update(elapsedTime);

        // If we are in remapping mode
        if (isRemappingKey) {
            // Check for all possible keys
            for (int key = GLFW_KEY_SPACE; key <= GLFW_KEY_LAST; key++){
                // Skip key modifiers and unusable keys
                if(key == GLFW_KEY_LEFT_SHIFT || key == GLFW_KEY_RIGHT_SHIFT ||
                        key == GLFW_KEY_LEFT_CONTROL || key == GLFW_KEY_RIGHT_CONTROL ||
                        key == GLFW_KEY_LEFT_ALT || key == GLFW_KEY_RIGHT_ALT ||
                        key == GLFW_KEY_ESCAPE || key == GLFW_KEY_ENTER){
                    continue;
                }
                // If this key is pressed
                if (glfwGetKey(graphics.getWindow(), key) == GLFW_PRESS){
                    // Update key bindings
                    updateKeyBindings(key);
                    return nextGameState;
                }
            }
        }

        return nextGameState;
    }

    private int getCurrentKeyForSelectedAction() {
        return switch (selectedActionIndex) {
            case 0 -> keyboardState.moveUpKey;
            case 1 -> keyboardState.moveDownKey;
            case 2 -> keyboardState.moveLeftKey;
            case 3 -> keyboardState.moveRightKey;
            case 4 -> keyboardState.resetLevel;
            case 5 -> keyboardState.undo;
            default -> GLFW_KEY_UNKNOWN;
        };
    }

    private void updateKeyBindings(int newKey){
        switch (selectedActionIndex) {
            case 0 -> keyboardState.moveUpKey = newKey;
            case 1 -> keyboardState.moveDownKey = newKey;
            case 2 -> keyboardState.moveLeftKey = newKey;
            case 3 -> keyboardState.moveRightKey = newKey;
            case 4 -> keyboardState.resetLevel = newKey;
            case 5 -> keyboardState.undo = newKey;
        }
        isRemappingKey = false;
        serializer.saveGameState(keyboardState);
    }

    @Override
    public void update(double elapsedTime) {
    }

    @Override
    public void render(double elapsedTime) {
        // Draw title
        final String title = "CONTROLS CONFIGURATION";
        final float titleHeight = 0.1f;
        final float titleWidth = font.measureTextWidth(title, titleHeight);
        graphics.drawTextByHeight(font, title, -titleWidth/2, -0.5f, titleHeight, Color.YELLOW);

        // Draw instructions
        String message;
        if (isRemappingKey) {
            message = "Press any key to assign to " + getActionName(selectedActionIndex);
        } else {
            message = "Use UP/DOWN to select control. Press ENTER to remap. ESC to return to menu.";
        }
        final float msgHeight = 0.06f;
        final float msgWidth = font.measureTextWidth(message, msgHeight);
        graphics.drawTextByHeight(font, message, -msgWidth / 2, 0.3f, msgHeight, Color.WHITE);

        // Draw key bindings in a table format
        final float startX = -0.4f;
        final float startY = -0.3f;
        final float lineHeight = 0.08f;
        final float actionWidth = 0.3f;
        final float keyWidth = 0.2f;

        // Draw column headers
        graphics.drawTextByHeight(font, "ACTION", startX, startY, lineHeight, Color.YELLOW);
        graphics.drawTextByHeight(font, "KEY", startX + actionWidth + 0.1f, startY, lineHeight, Color.YELLOW);

        String[] keyBindings = {
                "Move Up",
                "Move Down",
                "Move Left",
                "Move Right",
                "Reset Level",
                "Undo"
        };

        for (int i = 0; i < keyBindings.length; i++) {
            float yPosition = startY + ((i + 1) * lineHeight);

            // Draw action name
            Color actionColor = (i == selectedActionIndex) ? Color.YELLOW : Color.WHITE;
            graphics.drawTextByHeight(font, keyBindings[i], startX, yPosition, lineHeight * 0.8f, actionColor);

            // Draw current key
            String keyName = getKeyName(getCurrentKeyForAction(i));
            Color keyColor;
            if (isRemappingKey && i == selectedActionIndex) {
                keyColor = Color.RED;
                keyName = "PRESS KEY";
            } else {
                keyColor = (i == selectedActionIndex) ? Color.YELLOW : Color.WHITE;
            }
            graphics.drawTextByHeight(font, keyName, startX + actionWidth + 0.1f, yPosition, lineHeight * 0.8f, keyColor);

            // Draw selection indicator
            if (i == selectedActionIndex) {
                final float indicatorSize = lineHeight * 0.6f;
                graphics.drawTextByHeight(font, ">", startX - indicatorSize, yPosition, indicatorSize, Color.YELLOW);
            }
        }

        // Draw remapping indicator
        if (isRemappingKey) {
            final String remappingMessage = "REMAPPING IN PROGRESS";
            final float remapMsgHeight = 0.05f;
            final float remapMsgWidth = font.measureTextWidth(remappingMessage, remapMsgHeight);
            graphics.drawTextByHeight(font, remappingMessage, -remapMsgWidth / 2, 0.8f, remapMsgHeight, Color.RED);
        }
    }

    @Override
    public void clearKeyboardState() {
        if (inputKeyboard != null) {
            inputKeyboard.clearState();
        }
        isRemappingKey = false;
    }

    private int getCurrentKeyForAction(int actionIndex) {
        return switch (actionIndex) {
            case 0 -> keyboardState.moveUpKey;
            case 1 -> keyboardState.moveDownKey;
            case 2 -> keyboardState.moveLeftKey;
            case 3 -> keyboardState.moveRightKey;
            case 4 -> keyboardState.resetLevel;
            case 5 -> keyboardState.undo;
            default -> GLFW_KEY_UNKNOWN;
        };
    }

    private String getActionName(int index) {
        return switch (index) {
            case 0 -> "Move Up";
            case 1 -> "Move Down";
            case 2 -> "Move Left";
            case 3 -> "Move Right";
            case 4 -> "Reset Level";
            case 5 -> "Undo";
            default -> "None selected";
        };
    }

    private String getKeyName(int key) {
        String keyName = glfwGetKeyName(key, 0);
        if (keyName == null) {
            return switch (key) {
                case GLFW_KEY_UP -> "Up Arrow";
                case GLFW_KEY_DOWN -> "Down Arrow";
                case GLFW_KEY_LEFT -> "Left Arrow";
                case GLFW_KEY_RIGHT -> "Right Arrow";
                case GLFW_KEY_SPACE -> "Space";
                case GLFW_KEY_ENTER -> "Enter";
                default -> "Key(" + key + ")";
            };
        }
        return keyName.toUpperCase();
    }

    public void shutdown() {
        if (serializer != null) {
            serializer.shutdown();
        }
    }
}
