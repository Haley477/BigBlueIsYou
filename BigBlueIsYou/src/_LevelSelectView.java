import edu.usu.graphics.Color;
import edu.usu.graphics.Font;
import edu.usu.graphics.Graphics2D;

import static org.lwjgl.glfw.GLFW.*;

public class _LevelSelectView extends GameStateView {

    private enum LevelState {
        Level1,
        Level2,
        Level3,
        Level4,
        Level5,
        Back;

        public LevelState next() {
            int nextOrdinal = (this.ordinal() + 1) % LevelState.values().length;
            return LevelState.values()[nextOrdinal];
        }

        public LevelState previous() {
            int previousOrdinal = (this.ordinal() - 1) % LevelState.values().length;
            if (previousOrdinal < 0) {
                previousOrdinal = Back.ordinal();
            }
            return LevelState.values()[previousOrdinal];
        }
    }

    private LevelState currentSelection = LevelState.Level1;
    private KeyboardInput inputKeyboard;
    private GameStateEnum nextGameState = GameStateEnum.LevelSelect;
    private Font fontMenu;
    private Font fontSelected;
    private String selectedLevel;

    public String getSelectedLevel() {
        return selectedLevel;
    }

    @Override
    public void initialize(Graphics2D graphics) {
        super.initialize(graphics);

        fontMenu = new Font("resources/fonts/Roboto-Regular.ttf", 48, false);
        fontSelected = new Font("resources/fonts/Roboto-Bold.ttf", 48, false);

        inputKeyboard = new KeyboardInput(graphics.getWindow());
        // Arrow keys to navigate the menu
        inputKeyboard.registerCommand(GLFW_KEY_UP, true, (double elapsedTime) -> {
            currentSelection = currentSelection.previous();
        });
        inputKeyboard.registerCommand(GLFW_KEY_DOWN, true, (double elapsedTime) -> {
            currentSelection = currentSelection.next();
        });
        // When Enter is pressed, set the appropriate new game state
        inputKeyboard.registerCommand(GLFW_KEY_ENTER, true, (double elapsedTime) -> {
            nextGameState = switch (currentSelection) {
                case Level1 -> {
                    selectedLevel = "Level-1";
                    yield GameStateEnum.GamePlay;
                }
                case Level2 -> {
                    selectedLevel = "Level-2";
                    yield GameStateEnum.GamePlay;
                }
                case Level3 -> {
                    selectedLevel = "Level-3";
                    yield GameStateEnum.GamePlay;
                }
                case Level4 -> {
                    selectedLevel = "Level-4";
                    yield GameStateEnum.GamePlay;
                }
                case Level5 -> {
                    selectedLevel = "Level-5";
                    yield GameStateEnum.GamePlay;
                }
                case Back -> GameStateEnum.MainMenu;
            };
        });
    }

    @Override
    public void initializeSession() {
        nextGameState = GameStateEnum.LevelSelect;
    }

    @Override
    public GameStateEnum processInput(double elapsedTime) {
        // Updating the keyboard can change the nextGameState
        inputKeyboard.update(elapsedTime);
        return nextGameState;
    }

    @Override
    public void update(double elapsedTime) {
    }

    @Override
    public void render(double elapsedTime) {
        final float HEIGHT_MENU_ITEM = 0.075f;
        float top = -0.25f;
        top = renderMenuItem(currentSelection == LevelState.Level1 ? fontSelected : fontMenu, "Level 1", top, HEIGHT_MENU_ITEM, currentSelection == LevelState.Level1 ? Color.YELLOW : Color.BLUE);
        top = renderMenuItem(currentSelection == LevelState.Level2 ? fontSelected : fontMenu, "Level 2", top, HEIGHT_MENU_ITEM, currentSelection == LevelState.Level2 ? Color.YELLOW : Color.BLUE);
        top = renderMenuItem(currentSelection == LevelState.Level3 ? fontSelected : fontMenu, "Level 3", top, HEIGHT_MENU_ITEM, currentSelection == LevelState.Level3 ? Color.YELLOW : Color.BLUE);
        top = renderMenuItem(currentSelection == LevelState.Level4 ? fontSelected : fontMenu, "Level 4", top, HEIGHT_MENU_ITEM, currentSelection == LevelState.Level4 ? Color.YELLOW : Color.BLUE);
        top = renderMenuItem(currentSelection == LevelState.Level5 ? fontSelected : fontMenu, "Level 5", top, HEIGHT_MENU_ITEM, currentSelection == LevelState.Level5 ? Color.YELLOW : Color.BLUE);
        top = renderMenuItem(currentSelection == LevelState.Back ? fontSelected : fontMenu, "Back", top, HEIGHT_MENU_ITEM, currentSelection == LevelState.Back ? Color.YELLOW : Color.BLUE);
    }

    /**
     * Centers the text horizontally, at the specified top position.
     * It also returns the vertical position to draw the next menu item
     */
    private float renderMenuItem(Font font, String text, float top, float height, Color color) {
        float width = font.measureTextWidth(text, height);
        graphics.drawTextByHeight(font, text, 0.0f - width / 2, top, height, color);

        return top + height;
    }
}