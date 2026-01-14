import Particle.ParticleEffectsManager;
import edu.usu.graphics.Color;
import edu.usu.graphics.Font;
import edu.usu.graphics.Graphics2D;
import org.joml.Vector2f;
import static org.lwjgl.glfw.GLFW.*;

public class GamePlayView extends GameStateView {

    private KeyboardInput inputKeyboard;
    private GameStateEnum nextGameState = GameStateEnum.GamePlay;
    private Font font;
    private Game game;
    private Manager manager;
    private String selectedLevel;

    // Particle system manager
    private ParticleEffectsManager particleManager;

    @Override
    public void initialize(Graphics2D graphics) {
        super.initialize(graphics);
        game = new Game(graphics);
        game.initialize();

        font = new Font("resources/fonts/Roboto-Regular.ttf", 48, false);

        inputKeyboard = new KeyboardInput(graphics.getWindow());
        // When ESC is pressed, set the appropriate new game state
        inputKeyboard.registerCommand(GLFW_KEY_ESCAPE, true, (double elapsedTime) -> {
            nextGameState = GameStateEnum.LevelSelect;
        });

        // Register Reset and Undo commands
        inputKeyboard.registerCommand(GLFW_KEY_R, true, (double elapsedTime) -> {
            game.resetLevel();
        });

        inputKeyboard.registerCommand(GLFW_KEY_Z, true, (double elapsedTime) -> {
            game.undoMove();
        });

        // Initialize particle manager
        particleManager = ParticleEffectsManager.getInstance();
        particleManager.initialize();
        
        // Start background music
        particleManager.playBackgroundMusic();
    }

    public void shutdown() {
        game.shutdown();
    }

    @Override
    public void initializeSession() {
        nextGameState = GameStateEnum.GamePlay;
        // Clear any existing particle effects
        particleManager.clearAllEffects();
        // Load the selected level when the session is initialized
        if (selectedLevel != null) {
            game.loadLevel(selectedLevel);
        } else {
            // Default to Level-1 if no level is selected
            game.loadLevel("Level-1");
        }
    }

    public void setSelectedLevel(String level) {
        this.selectedLevel = level;
    }

    @Override
    public GameStateEnum processInput(double elapsedTime) {
        // Updating the keyboard can change the nextGameState
        inputKeyboard.update(elapsedTime);
        
        // If we're transitioning away from gameplay, clear particle effects and stop sounds
        if (nextGameState != GameStateEnum.GamePlay) {
            particleManager.clearAllEffects();
            // Stop all sounds except background music
            particleManager.cleanup();
            // Restart background music
            particleManager.initialize();
            particleManager.playBackgroundMusic();
        }
        
        return nextGameState;
    }

    @Override
    public void update(double elapsedTime) {
        game.update(elapsedTime);
        particleManager.update(elapsedTime);
    }

    @Override
    public void render(double elapsedTime) {
        
        // Then render particles on top
        particleManager.render(graphics);
    }
}