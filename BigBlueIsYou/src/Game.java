import ecs.Components.AnimatedSprite;
import ecs.Components.Appearance;
import ecs.Entities.*;
import ecs.Systems.*;
import ecs.Systems.KeyboardInput;
import ecs.Systems.System;
import edu.usu.graphics.Graphics2D;
import edu.usu.graphics.Texture;
import java.util.*;

public class Game {
    private final List<Entity> removeThese = new ArrayList<>();
    private final List<Entity> addThese = new ArrayList<>();
    private final List<Entity> updateThese = new ArrayList<>();

    private Graphics2D graphics;
    private LevelParser levelParser;
    private String currentLevel;
    private final float TILE_SIZE = 32.0f; // Default tile size in pixels

    private String[][] grid; // Store the current grid
    private String[][] initialGrid;
    private Stack<String[][]> gameStates;

    // Systems
    private TileRender renderSystem;
    private Movement movementSystem;
    private KeyboardInput keyboardSystem;
    private Rule ruleSystem;

    // Entity registry
    private Map<Long, Entity> entities = new HashMap<>();

    // Texture cache
    private Map<String, Texture> textureCache = new HashMap<>();

    private Map<Long, AnimatedSprite> animatedEntities = new HashMap<>();

    public Game(Graphics2D graphics) {
        this.graphics = graphics;
        this.levelParser = new LevelParser();
        this.renderSystem = new TileRender(graphics);
        this.movementSystem = new Movement();
        this.keyboardSystem = new KeyboardInput(graphics.getWindow());

        this.initialGrid = createDeepCopy();
        gameStates = new Stack<>();
        gameStates.add(createDeepCopy());

        // Preload textures
        loadTextures();
    }

    public void initialize() {
        this.levelParser = new LevelParser();
        this.renderSystem = new TileRender(graphics);
        this.movementSystem = new Movement();
        this.keyboardSystem = new KeyboardInput(graphics.getWindow());

        this.initialGrid = createDeepCopy();
        gameStates = new Stack<>();
        gameStates.add(createDeepCopy());

        // Preload textures
        loadTextures();
    }

    private void addEntity(Entity entity) {
        renderSystem.add(entity);
        movementSystem.add(entity);
        keyboardSystem.add(entity);
        ruleSystem.add(entity);
        entities.put(entity.getId(), entity);
    }

    private void removeEntity(Entity entity) {
        renderSystem.remove(entity.getId());
        movementSystem.remove(entity.getId());
        keyboardSystem.remove(entity.getId());
        ruleSystem.remove(entity.getId());
        entities.remove(entity.getId());
    }

    private void updateEntity(Entity entity) {
        renderSystem.updatedEntity(entity);
        movementSystem.updatedEntity(entity);
        keyboardSystem.updatedEntity(entity);
        ruleSystem.updatedEntity(entity);

        entities.remove(entity.getId());
        entities.put(entity.getId(), entity);
    }

    private void loadTextures() {
        // Load textures for different game objects
        loadTexture("wall", "resources/textures/wall.png");
        loadTexture("rock", "resources/textures/rock.png");
        loadTexture("flag", "resources/textures/flag.png");
        loadTexture("BigBlue", "resources/textures/BigBlue.png");
        loadTexture("water", "resources/textures/water.png");
        loadTexture("lava", "resources/textures/lava.png");
        loadTexture("floor", "resources/textures/floor.png");
        loadTexture("grass", "resources/textures/grass.png");
        loadTexture("hedge", "resources/textures/hedge.png");

        // Load textures for text objects (nouns and verbs)
        loadTexture("is", "resources/textures/word-is.png");
        loadTexture("stop", "resources/textures/word-stop.png");
        loadTexture("push", "resources/textures/word-push.png");
        loadTexture("you", "resources/textures/word-you.png");
        loadTexture("win", "resources/textures/word-win.png");
        loadTexture("sink", "resources/textures/word-sink.png");
        loadTexture("kill", "resources/textures/word-kill.png");
        loadTexture("baba", "resources/textures/word-baba.png");
        loadTexture("flagname", "resources/textures/word-flag.png");
        loadTexture("lavaname", "resources/textures/word-lava.png");
        loadTexture("rockname", "resources/textures/word-rock.png");
        loadTexture("wallname", "resources/textures/word-wall.png");
        loadTexture("watername", "resources/textures/word-water.png");
    }
    private void loadTexture(String name, String path) {
        try {
            textureCache.put(name, new Texture(path));
        } catch (Exception e) {
            e.printStackTrace();
            // Create a fallback texture
            textureCache.put(name, createFallbackTexture());
        }
    }
    private Texture createFallbackTexture() {
        // Create a simple colored square as fallback
        try {
            // Create a 32x32 white square as fallback
            return new Texture("resources/textures/floor.png"); // Using floor as fallback for now
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void loadLevel(String levelName) {
        currentLevel = levelName;
        initialize();
        grid = levelParser.parseLevel(levelName);

        if (grid == null) {
            return;
        }

        // Initialize or reinitialize the rule system with the new grid
        if (ruleSystem == null) {
            ruleSystem = new Rule(this.grid, this.textureCache);
        } else {
            ruleSystem.setGrid(this.grid);
        }

        // Clear existing entities
        clearEntities();

        // Create entities based on the grid
        createEntitiesFromGrid(grid);

        // Load initial rules
        Set<System.EntityUpdate> changed = ruleSystem.update(0.0);
        for (var entity : changed) {
            updateThese.add(entity.entity);
        }

        // Initialize game states stack with initial state
        initialGrid = createDeepCopy(grid);
        gameStates.clear();
        gameStates.add(createDeepCopy(grid));

        // Process entity updates
        for (var entity : removeThese) {
            removeEntity(entity);
        }
        removeThese.clear();

        for (var entity : addThese) {
            addEntity(entity);
        }
        addThese.clear();
    }

    private void clearEntities() {
        // Remove all entities from all systems
        for (Long id : entities.keySet()) {
            renderSystem.remove(id);
            movementSystem.remove(id);
            keyboardSystem.remove(id);
            ruleSystem.remove(id);
        }

        // Clear entity registry
        entities.clear();
        animatedEntities.clear();
    }

    private void createEntitiesFromGrid(String[][] grid) {
        for (int i = 0; i < grid.length; i++) {
            for (int j = 0; j < grid[i].length; j++) {
                String objectType = grid[i][j];
                if (objectType != null && !objectType.isEmpty()) {
                    createEntity(objectType, i, j);
                }
            }
        }
    }

    private void createEntity(String objectType, int row, int col) {
        int x = col;
        int y = row;

        /** Creating entities using the maps */
        Texture tex = textureCache.get(objectType);
        if (tex == null) {
            tex = createFallbackTexture();
        }

        // Check if this entity should be animated
        Appearance appearance;
        if (shouldBeAnimated(objectType)) {
            // Create animated appearance
            appearance = new Appearance(tex, TILE_SIZE, getTotalFrames(objectType), getFrameDuration(objectType));
        } else {
            // Create regular appearance
            appearance = new Appearance(tex, TILE_SIZE);
        }

        Entity entity = null;
        if (levelParser.bgObjects.containsValue(objectType)) {
            entity = Bg.create(x, y, objectType, appearance);
            addEntity(entity);
        }
        else if (levelParser.nouns.containsValue(objectType)) {
            // For nouns, we need to create both the noun entity and the corresponding object entity
            String baseName = objectType.replace("name", "");
            entity = NounEnt.create(x, y, objectType, appearance);
            addEntity(entity);

            // Also create the corresponding object entity if it doesn't exist
//            if (!levelParser.objects.containsValue(baseName)) {
//                Entity objEntity = ObjectEnt.create(x, y, baseName, appearance);
//                addEntity(objEntity);
//            }
        }
        else if (levelParser.objects.containsValue(objectType)) {
            entity = ObjectEnt.create(x, y, objectType, appearance);
            addEntity(entity);
        }
        else if (objectType.equalsIgnoreCase("is")) {
            entity = VerbEnt.create(x, y, objectType, appearance);
            addEntity(entity);
        }

        if (shouldBeAnimated(objectType) && entity != null) {
            // Create and store animated sprites
            float[] frameTimes = getFrameTimes(objectType);
            AnimatedSprite animatedSprite = new AnimatedSprite(appearance, frameTimes);
            animatedEntities.put(entity.getId(), animatedSprite);
        }
    }

    private boolean shouldBeAnimated(String objectType) {
        // Define which objects should be animated
        return objectType.equals("water") ||
                objectType.equals("lava") ||
                objectType.equals("rock") ||
                objectType.equals("flag") ||
                objectType.equals("wall") ||
                objectType.equals("floor") ||
                objectType.equals("grass") ||
                objectType.equals("is") ||
                objectType.equals("stop") ||
                objectType.equals("push") ||
                objectType.equals("you") ||
                objectType.equals("win") ||
                objectType.equals("sink") ||
                objectType.equals("kill") ||
                objectType.equals("baba") ||
                objectType.equals("flagname") ||
                objectType.equals("lavaname") ||
                objectType.equals("rockname") ||
                objectType.equals("wallname") ||
                objectType.equals("watername");
    }

    private int getTotalFrames(String objectType) {
        // Return number of frames for each animated object
        switch (objectType) {
            case "water": return 3;
            case "lava": return 3;
            case "rock": return 3;
            case "flag": return 3;
            case "wall": return 3;
            case "floor": return 3;
            case "grass": return 3;
            case "is": return 3;
            case "stop": return 3;
            case "push": return 3;
            case "you": return 3;
            case "win": return 3;
            case "sink": return 3;
            case "kill": return 3;
            case "baba": return 3;
            case "flagname": return 3;
            case "lavaname": return 3;
            case "rockname": return 3;
            case "wallname": return 3;
            case "watername": return 3;

            default: return 1;
        }
    }

    private float getFrameDuration(String objectType) {
        // Return frame duration for each animated object
        switch (objectType) {
            case "water": return 0.2f;  // Water animation
            case "lava": return 0.3f;   // Lava animation
            case "rock": return 0.1f;   // Rock animation
            case "flag": return 0.15f;  // Flag animation
            case "wall": return 0.2f;   // Wall animation
            case "floor": return 0.25f; // Floor animation
            case "grass": return 0.2f;  // Grass animation
            case "is": return 0.2f;
            case "stop": return 0.2f;
            case "push": return 0.2f;
            case "you": return 0.2f;
            case "win": return 0.2f;
            case "sink": return 0.2f;
            case "kill": return 0.2f;
            case "baba": return 0.2f;
            case "flagname": return 0.2f;
            case "lavaname": return 0.2f;
            case "rockname": return 0.2f;
            case "wallname": return 0.2f;
            case "watername": return 0.2f;
            default: return 0f;
        }
    }

    private float[] getFrameTimes(String objectType) {
        // Return array of frame times for each animated object
        int totalFrames = getTotalFrames(objectType);
        float duration = getFrameDuration(objectType);
        float[] times = new float[totalFrames];
        for (int i = 0; i < totalFrames; i++) {
            times[i] = duration;
        }
        return times;
    }
    
    public void update(double elapsedTime) {
        keyboardSystem.update(elapsedTime);

        // Save the current grid state before any movement
        String[][] previousGrid = createDeepCopy(this.grid);

        movementSystem.setGrid(this.grid); // ensures not null
        Set<System.EntityUpdate> moved = movementSystem.update(elapsedTime);
        String[][] newGrid = movementSystem.getGrid();

        // Only update grid and add to game states if there was a move
        if (!moved.isEmpty()) {
            // Save the state before applying the move
            gameStates.add(previousGrid);

            // Now apply the move
            this.grid = newGrid;
            ruleSystem.setGrid(this.grid);
            Set<System.EntityUpdate> changed = ruleSystem.update(elapsedTime);
            for (var entity : changed) {
                updateThese.add(entity.entity);
            }
        }

        for (var entity : removeThese) {
            removeEntity(entity);
        }
        removeThese.clear();

        for (var entity : addThese) {
            addEntity(entity);
        }
        addThese.clear();

        for (var entity : updateThese) {
            updateEntity(entity);
        }
        updateThese.clear();

        // Update animations
        for (AnimatedSprite animatedSprite : animatedEntities.values()) {
            animatedSprite.update(elapsedTime);
        }
        renderSystem.setNums(this.grid);
        renderSystem.update(0.0);
    }

    // Automatically assumes creating deep copy of this.grid
    private String[][] createDeepCopy() { // ChatGPT
        if (grid == null) return null;

        String[][] copy = new String[grid.length][];

        for (int i = 0; i < grid.length; i++) {
            if (grid[i] != null) {
                copy[i] = new String[grid[i].length];
                for (int j = 0; j < grid[i].length; j++) {
                    copy[i][j] = grid[i][j]; // Strings are immutable, so this is fine
                }
            }
        }

        return copy;
    }

    public void shutdown() {
        // Clean up resources
        textureCache.clear();
        clearEntities();
    }

    public void resetLevel() {
        if (initialGrid != null) {
            // Clear existing entities
            clearEntities();

            // Reset grid to initial state
            grid = createDeepCopy(initialGrid);

            // Recreate entities from initial grid
            createEntitiesFromGrid(grid);

            // Clear game states stack and add initial state
            gameStates.clear();
            gameStates.add(createDeepCopy(grid));

            // Reset rule system
            ruleSystem.setGrid(grid);
            Set<System.EntityUpdate> changed = ruleSystem.update(0.0);
            for (var entity : changed) {
                removeThese.add(entity.entity);
                addThese.add(entity.entity);
            }
        }
    }

    public void undoMove() {
        if (gameStates.size() > 1) {
            // Remove current state
            gameStates.pop();

            // Get previous state
            grid = createDeepCopy(gameStates.peek());

            // Clear existing entities
            clearEntities();

            // Recreate entities from previous grid
            createEntitiesFromGrid(grid);

            // Reset rule system with new grid
            ruleSystem.setGrid(grid);

            // Let the rule system process the new state
            Set<System.EntityUpdate> changed = ruleSystem.update(0.0);

            // Process any entity updates from the rule system
            for (var entity : changed) {
                if (entity.shouldRemove) {
                    removeEntity(entity.entity);
                } else {
                    // Re-add the entity to ensure it's properly registered
                    removeEntity(entity.entity);
                    addEntity(entity.entity);
                }
            }

            // Update render system
            renderSystem.setNums(this.grid);

            // Update movement system with the new grid
            movementSystem.setGrid(this.grid);

            // Update all entities in all systems to ensure proper registration
            for (var entity : entities.values()) {
                renderSystem.updatedEntity(entity);
                movementSystem.updatedEntity(entity);
                keyboardSystem.updatedEntity(entity);
                ruleSystem.updatedEntity(entity);
            }
        }
    }

    // Helper method to create a deep copy of a grid
    private String[][] createDeepCopy(String[][] source) {
        if (source == null) return null;

        String[][] copy = new String[source.length][];
        for (int i = 0; i < source.length; i++) {
            if (source[i] != null) {
                copy[i] = new String[source[i].length];
                for (int j = 0; j < source[i].length; j++) {
                    copy[i][j] = source[i][j];
                }
            }
        }
        return copy;
    }
}