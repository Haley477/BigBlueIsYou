package ecs.Systems;
import ecs.Components.IgnoredRule;
import ecs.Components.Object;
import ecs.Components.Property;
import ecs.Components.Position;
import ecs.Components.Movable;
import ecs.Components.KeyboardControlled;
import ecs.Entities.Entity;
import edu.usu.graphics.Texture;
import java.util.*;
import Particle.ParticleEffectsManager;
import org.joml.Vector2f;

public class Rule extends System {

    private String[][] grid;
    private Map<String, Texture> textureCache;
    private Set<EntityUpdate> changedEntities;
    private KeyboardState keyboardState;
    private Set<String> playedSoundRules; // Track which rules have played their sounds
    private boolean hasWon; // Track if we've already won the level
    private int gridNumRows;
    private int gridNumCols;
    private Set<Long> entitiesWithYouEffect = new HashSet<>();
    private Set<Long> entitiesWithWinEffect = new HashSet<>();

    private String[] descriptors = {"wallname", "rockname", "flagname", "baba", "watername", "lavaname"};
    private Map<String, Integer> behaviors = Map.ofEntries(
            Map.entry("stop", 0x0004),
            Map.entry("push",0x0008),
            Map.entry("you", 0x0001),
            Map.entry("win", 0x0002),
            Map.entry("sink", 0x0020),
            Map.entry("kill", 0x0010),
            Map.entry("baba", 0x0001)
    );

    public Rule(String[][] grid, Map<String, Texture> textureCache) {
        super(ecs.Components.Position.class, ecs.Components.Object.class);

        this.grid = grid;
        this.textureCache = textureCache;
        this.keyboardState = new KeyboardState();
        this.changedEntities = new HashSet<>();
        this.playedSoundRules = new HashSet<>();
        this.hasWon = false;
        if (grid != null && grid.length > 0) {
            this.gridNumRows = grid.length;
            this.gridNumCols = grid[0].length;
        }
    }

    public void setGrid(String[][] grid) {
        this.grid = grid;
        this.playedSoundRules.clear(); // Clear played sounds when grid changes
        this.hasWon = false; // Reset win state when grid changes
        this.entitiesWithYouEffect.clear(); // Clear YOU effect tracking
        this.entitiesWithWinEffect.clear(); // Clear WIN effect tracking

        if (grid != null && grid.length > 0) {
            this.gridNumRows = grid.length;
            this.gridNumCols = grid[0].length;
        }
    }

    public Set<EntityUpdate> update(double elapsedTime) {
        changedEntities = new HashSet<>();
        resetEntities();
        checkForRules();
        checkWinCondition();
        checkHazardCollisions();
        return changedEntities;
    }

    private void resetEntities() {
        // First, store the current properties
        Map<Long, Set<Property.PropertyType>> storedProperties = new HashMap<>();
        for (var entity : entities.values()) {
            if (entity.contains(ecs.Components.Property.class)) {
                var prop = entity.get(ecs.Components.Property.class);
                storedProperties.put(entity.getId(), new HashSet<>(prop.getProperties()));
            }
        }

        // Remove all properties
        for (var entity : entities.values()) {
            if (entity.contains(ecs.Components.Property.class)) {
                // Clear effect tracking when entity loses properties
                entitiesWithYouEffect.remove(entity.getId());
                entitiesWithWinEffect.remove(entity.getId());
                entity.remove(ecs.Components.Property.class);
            }
            if (entity.contains(ecs.Components.Movable.class)) {
                entity.remove(ecs.Components.Movable.class);
            }
            if (entity.contains(ecs.Components.KeyboardControlled.class)) {
                entity.remove(ecs.Components.KeyboardControlled.class);
            }
        }

        // Restore properties that should persist
        for (var entity : entities.values()) {
            var storedProps = storedProperties.get(entity.getId());
            if (storedProps != null && !storedProps.isEmpty()) {
                var prop = new Property(entity);
                for (var p : storedProps) {
                    prop.addProperty(p);
                }
                entity.add(prop);
            }
        }
    }

    private void printGrid() {
        java.lang.System.out.println("\n--- Checking Rules ---");
        java.lang.System.out.println("Grid contents:");
        for (int i = 0; i < grid.length; i++) {
            StringBuilder row = new StringBuilder();
            for (int j = 0; j < grid[i].length; j++) {
                row.append("[").append(grid[i][j].isEmpty() ? " " : grid[i][j]).append("] ");
            }
            java.lang.System.out.println(row);
        }
    }

    /** Checks for any existing rules in the grid
     * read top -> bottom OR left -> right
     * consists of <targetNoun> is <thing/rule>
     */
    public void checkForRules() {
        printGrid();

        for (int i = 0; i < grid.length; i++) {
            for (int j = 0; j < grid[i].length; j++) {
                // Check for vertical rules (top -> bottom)
                if (i > 0 && i < grid.length - 1) {
                    String top = grid[i-1][j];
                    String middle = grid[i][j];
                    String bottom = grid[i+1][j];

                    if (!top.isEmpty() && middle.equals("is") && !bottom.isEmpty()) {
                        java.lang.System.out.println("Found vertical rule: " + top + " is " + bottom);
                        applyRules(top, bottom);
                    }
                }

                // Check for horizontal rules (left -> right)
                if (j > 0 && j < grid[i].length - 1) {
                    String left = grid[i][j-1];
                    String middle = grid[i][j];
                    String right = grid[i][j+1];

                    if (!left.isEmpty() && middle.equals("is") && !right.isEmpty()) {
                        java.lang.System.out.println("Found horizontal rule: " + left + " is " + right);
                        applyRules(left, right);
                    }
                }
            }
        }
    }

    private void triggerEffects(String ruleType, Entity entity) {
        if (entity == null) return;

        ParticleEffectsManager particleManager = ParticleEffectsManager.getInstance();

        switch (ruleType.toLowerCase()) {
            case "you":
                // Show sparkles when YOU rule is formed or changed
                if (!entitiesWithYouEffect.contains(entity.getId())) {
                    particleManager.youChangeAtGridPosition(entity.get(Position.class).x, entity.get(Position.class).y);
                    particleManager.playIsYouSound();
                    entitiesWithYouEffect.add(entity.getId());
                }
                break;

            case "win":
                // Show sparkles when WIN rule is formed or changed
                if (!entitiesWithWinEffect.contains(entity.getId())) {
                    particleManager.winChangeAtGridPosition(entity.get(Position.class).x, entity.get(Position.class).y);
                    if (!playedSoundRules.contains("win")) {
                        particleManager.playIsWinSound();
                        playedSoundRules.add("win");
                    }
                    entitiesWithWinEffect.add(entity.getId());
                }
                break;

            case "push":
                if (!playedSoundRules.contains("push")) {
                    particleManager.playMoveSound();
                    playedSoundRules.add("push");
                }
                break;

            case "stop":
                if (!playedSoundRules.contains("stop")) {
                    particleManager.playMoveSound();
                    playedSoundRules.add("stop");
                }
                break;
        }
    }

    private List<Entity> getTargetEntities(String target, Map<Long, Entity> entities) {
        List<Entity> targets = new ArrayList<>();
        for (var entity : entities.values()) {
            var object = entity.get(Object.class);
            if (object == null) continue;

            // Special case: "baba" only matches with "BigBlue"
            if (target.equals("baba")) {
                if (object.name.equals("BigBlue")) {
                    targets.add(entity);
                }
                continue; // Skip other checks for "baba"
            }
            // If target is a descriptor (ends with "name"), match with base object names
            else if (target.endsWith("name")) {
                String baseName = target.replace("name", "");
                if (object.name.equals(baseName)) {
                    targets.add(entity);
                }
            }
            // If target is a base name, match with descriptors
            else if (Arrays.asList(descriptors).contains(target + "name")) {
                if (object.name.equals(target)) {
                    targets.add(entity);
                }
            }
            // Direct name match
            else if (object.name.equals(target)) {
                targets.add(entity);
            }
        }

        return targets;
    }

    private void applyRules(String target, String rule) {
        // Auto replace noun names with object names
//        if (target.equals("baba")) {
//            target = "BigBlue";
//        }
//        else {
//            target = target.replace("name", "");
//        }

        // Check for nonsense rules
        if (isNonsenseRule(target, rule)) {
            java.lang.System.out.println("Rule rejected as nonsense: " + target + " is " + rule);
            return;
        }

        java.lang.System.out.println("Applying rule: " + target + " is " + rule);
        List<Entity> targets = getTargetEntities(target, entities);
        java.lang.System.out.println("Found " + targets.size() + " entities matching target: " + target);

        // First check if this is a property change rule
        if (behaviors.containsKey(rule.toLowerCase())) {
            for (var entity : targets) {
                var object = entity.get(ecs.Components.Object.class);
                java.lang.System.out.println("Processing entity " + entity.getId() + " with object name: " + object.name);

                // Skip if this is a text object and the rule is defeat/sink
                if (object.name.equals(target) && (rule.equals("kill") || rule.equals("sink"))) {
                    continue;
                }

                // Special case for win property - only allow it on actual flag objects
                if (rule.equals("win") && !object.name.equals("flag")) {
                    continue;
                }

                // Only add Property component if entity doesn't already have one
                if (!entity.contains(Property.class)) {
                    entity.add(new Property(entity));
                }
                var property = entity.get(Property.class);

                // Check for contradictions
                Property.PropertyType newProperty = getPropertyTypeFromRule(rule);
                if (hasContradictoryProperty(property, newProperty)) {
                    // Mark the rule as ignored
                    entity.add(new IgnoredRule());
                    continue;
                }

                // Check if the property is already applied
                boolean propertyChanged = !property.hasProperty(newProperty);

                // Add the behavior flag
                property.addProperty(newProperty);

                // Only trigger effects if the property actually changed
                if (propertyChanged) {
                    triggerEffects(rule, entity);
                }

                if (rule.equals("push")) {
                    entity.add(new ecs.Components.Movable(ecs.Components.Movable.MoveTo.Stopped));
                }

                // Special handling for "you" property
                if (rule.equals("you")) {
                    // Add keyboard control and movement components
                    java.lang.System.out.println("Adding 'you' rule to entity: " + entity.getId());
                    entity.add(new ecs.Components.Movable(ecs.Components.Movable.MoveTo.Stopped));
                    entity.add(new ecs.Components.KeyboardControlled(
                            Map.of(
                                    keyboardState.moveUpKey, ecs.Components.Movable.MoveTo.Up,
                                    keyboardState.moveDownKey, ecs.Components.Movable.MoveTo.Down,
                                    keyboardState.moveLeftKey, ecs.Components.Movable.MoveTo.Left,
                                    keyboardState.moveRightKey, ecs.Components.Movable.MoveTo.Right
                            )
                    ));
                }

                changedEntities.add(new EntityUpdate(entity, false));
            }
        }
        // Then check for object type changes
        else if (Arrays.asList(descriptors).contains(rule)) {
            for (var entity : targets) {
                var appearance = entity.get(ecs.Components.Appearance.class);
                var object = entity.get(ecs.Components.Object.class);

                String newType = rule.replace("name", "");
                // Only change if the new type is different from the current type
                if (!object.name.equals(newType)) {
                    appearance.image = textureCache.get(newType);
                    object.name = newType;
                    // Trigger transformation effect
                    triggerEffects(rule, entity);
                }

                changedEntities.add(new EntityUpdate(entity, false));
            }
        }
    }

    private boolean isNonsenseRule(String target, String rule) {
        // Define nonsense rules
        Set<String> nonsenseRules = Set.of("win", "push", "stop", "you", "sink", "kill", "is");

        // Special case: "baba is you" is a valid rule
        if (target.equals("baba") && rule.equals("you")) {
            return false;
        }

        // Special case: "baba is baba" is a nonsense rule
        if (target.equals("baba") && rule.equals("baba")) {
            return true;
        }

        // Ignore rules where "is" is used as target or rule
        if (target.equals("is") || rule.equals("is")) {
            return true;
        }

        // Check if the rule is trying to apply a property to a property
        if (nonsenseRules.contains(target.toLowerCase()) && behaviors.containsKey(rule)) {
            return true;
        }

        // Check if the rule is trying to apply a property to a property
        if (nonsenseRules.contains(rule.toLowerCase()) && behaviors.containsKey(target)) {
            return true;
        }

        return false;
    }

    /**
     * Remove all behaviors for objects not part of a ruleset
     */
    private void removeRules() {
        for (var entity : entities.values()) {
            if (!changedEntities.contains(entity)) {
                var object = entity.get(ecs.Components.Object.class);
                object.clearProperties();

                if (entity.contains(ecs.Components.Movable.class)) {
                    entity.remove(ecs.Components.Movable.class);
                }
                if (entity.contains(ecs.Components.KeyboardControlled.class)) {
                    entity.remove(ecs.Components.KeyboardControlled.class);
                }
            }
        }
    }

    private Property.PropertyType getPropertyTypeFromRule(String rule) {
        switch (rule) {
            case "you": return Property.PropertyType.YOU;
            case "win": return Property.PropertyType.WIN;
            case "stop": return Property.PropertyType.STOP;
            case "push": return Property.PropertyType.PUSH;
            case "kill": return Property.PropertyType.DEFEAT;
            case "sink": return Property.PropertyType.SINK;
            default: throw new IllegalArgumentException("Unknown rule type: " + rule);
        }
    }

    private boolean hasContradictoryProperty(Property property, Property.PropertyType newProperty) {
        // Define contradictory properties
        Map<Property.PropertyType, Set<Property.PropertyType>> contradictions = Map.of(
                Property.PropertyType.YOU, Set.of(Property.PropertyType.DEFEAT, Property.PropertyType.SINK),
                Property.PropertyType.WIN, Set.of(Property.PropertyType.DEFEAT, Property.PropertyType.SINK),
                Property.PropertyType.STOP, Set.of(Property.PropertyType.PUSH)
        );

        // Check if the new property contradicts any existing properties
        if (contradictions.containsKey(newProperty)) {
            for (var existingProperty : property.getProperties()) {
                if (contradictions.get(newProperty).contains(existingProperty)) {
                    return true;
                }
            }
        }

        return false;
    }

    private void checkWinCondition() {
        if (hasWon) return; // Don't check if already won

        // Find all YOU entities
        List<Entity> youEntities = new ArrayList<>();
        for (var entity : entities.values()) {
            if (entity.contains(Property.class) && entity.get(Property.class).hasProperty(Property.PropertyType.YOU)) {
                youEntities.add(entity);
            }
        }

        // Find all WIN entities
        List<Entity> winEntities = new ArrayList<>();
        for (var entity : entities.values()) {
            if (entity.contains(Property.class) && entity.get(Property.class).hasProperty(Property.PropertyType.WIN)) {
                winEntities.add(entity);
            }
        }

        // Check for collisions between YOU and WIN entities
        for (var youEntity : youEntities) {
            var youPos = youEntity.get(Position.class);
            for (var winEntity : winEntities) {
                var winPos = winEntity.get(Position.class);
                if (youPos.x == winPos.x && youPos.y == winPos.y && !hasWon) {
                    // Trigger win effects
                    ParticleEffectsManager particleManager = ParticleEffectsManager.getInstance();
                    particleManager.objectWinAtGridPosition(winPos.x, winPos.y);
                    hasWon = true;
                    return;
                }
            }
        }
    }
    private void checkHazardCollisions() {
        List<Entity> youEntities = new ArrayList<>();
        List<Entity> hazardEntities = new ArrayList<>();

        for (var entity : entities.values()) {
            if (entity.contains(Property.class) && entity.contains(Position.class)) {
                var props = entity.get(Property.class);
                if (props.hasProperty(Property.PropertyType.YOU)) {
                    youEntities.add(entity);
                } else if (props.hasProperty(Property.PropertyType.DEFEAT) || props.hasProperty(Property.PropertyType.SINK)) {
                    hazardEntities.add(entity);
                }
            }
        }

        for (var youEntity : youEntities) {
            var youPos = youEntity.get(Position.class);

            for (var hazard : hazardEntities) {
                var hazardPos = hazard.get(Position.class);
                var hazardProps = hazard.get(Property.class);

                if (youPos.x == hazardPos.x && youPos.y == hazardPos.y) {
                    ParticleEffectsManager particleManager = ParticleEffectsManager.getInstance();

                    if (hazardProps.hasProperty(Property.PropertyType.DEFEAT)) {
                        particleManager.objectDeathAtGridPosition(youPos.x, youPos.y);
                        //youEntity.markForRemoval(); // Or however your ECS removes entities
                        return; // Exit after one interaction to avoid modifying list during iteration
                    }

                    if (hazardProps.hasProperty(Property.PropertyType.SINK)) {
                        particleManager.objectDeathAtGridPosition(youPos.x, youPos.y);
                        //youEntity.markForRemoval();
                        //hazard.markForRemoval(); // Sink destroys both
                        return;
                    }
                }
            }
        }
    }


}
