import edu.usu.graphics.*;
import static org.lwjgl.glfw.GLFW.*;

import java.util.HashMap;

public class Manager {
    private final Graphics2D graphics;
    private HashMap<GameStateEnum, IGameState> states;
    private IGameState currentState;
    GameStateEnum nextStateEnum = GameStateEnum.MainMenu;
    GameStateEnum prevStateEnum = GameStateEnum.MainMenu;

    public Manager(Graphics2D graphics) {
        this.graphics = graphics;
    }

    public void initialize() {
        states = new HashMap<>() {
            {
                put(GameStateEnum.MainMenu, new MainMenuView());
                put(GameStateEnum.LevelSelect, new _LevelSelectView());
                put(GameStateEnum.GamePlay, new GamePlayView());
                put(GameStateEnum.Controls, new ControlsView());
                put(GameStateEnum.Credits, new CreditsView());
            }
        };

        // Give all game states a chance to initialize, other than the constructor
        for (var state : states.values()) {
            state.initialize(graphics);
        }

        currentState = states.get(GameStateEnum.MainMenu);
        currentState.initializeSession();
    }

    public void shutdown() {
        for (var state : states.values()) {
            if (state instanceof ControlsView) {
                ((ControlsView) state).shutdown();
            }
            if (state instanceof GamePlayView) {
                ((GamePlayView) state).shutdown();
            }
        }
    }

    public void run() {
        // Grab the first time
        double previousTime = glfwGetTime();

        // Run the rendering loop until the user has attempted to close
        // the window or has pressed the ESCAPE key.
        while (!graphics.shouldClose()) {
            double currentTime = glfwGetTime();
            double elapsedTime = currentTime - previousTime;    // elapsed time is in seconds
            previousTime = currentTime;

            processInput(elapsedTime);
            update(elapsedTime);
            render(elapsedTime);
        }
    }

    private void processInput(double elapsedTime) {
        // Poll for window events: required in order for window, keyboard, etc events are captured.
        glfwPollEvents();
        nextStateEnum = currentState.processInput(elapsedTime);
    }

    private void update(double elapsedTime) {
        // Special case for exiting the game
        if (nextStateEnum == GameStateEnum.Quit) {
            glfwSetWindowShouldClose(graphics.getWindow(), true);
        } else {
            if (nextStateEnum == prevStateEnum) {
                currentState.update(elapsedTime);
            } else {
                // Clear keyboard state before transitioning
                if (currentState instanceof GameStateView) {
                    ((GameStateView) currentState).clearKeyboardState();
                }
                
                // If transitioning from LevelSelect to GamePlay, pass the selected level
                if (currentState instanceof _LevelSelectView && nextStateEnum == GameStateEnum.GamePlay) {
                    String selectedLevel = ((_LevelSelectView) currentState).getSelectedLevel();
                    ((GamePlayView) states.get(GameStateEnum.GamePlay)).setSelectedLevel(selectedLevel);
                }

                // Store the next state before changing
                IGameState nextState = states.get(nextStateEnum);
                
                // Initialize the new state before making it current
                nextState.initializeSession();
                
                // Now make the transition
                currentState = nextState;
                prevStateEnum = nextStateEnum;
                
                // Clear any pending key states in the new state
                if (currentState instanceof GameStateView) {
                    ((GameStateView) currentState).clearKeyboardState();
                }
            }
        }
    }

    private void render(double elapsedTime) {
        graphics.begin();

        currentState.render(elapsedTime);

        graphics.end();
    }
}
