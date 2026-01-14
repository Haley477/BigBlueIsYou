package ecs.Systems;

import Particle.ParticleEffectsManager;
import ecs.Components.Movable;
import ecs.Components.Property;
import ecs.Components.Position;
import ecs.Entities.Entity;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Collision extends System {
    private ParticleEffectsManager particleManager;
    private Set<EntityUpdate> changedEntities;
    private int gridNumCols;
    private int gridNumRows;

    public Collision() {
        super(ecs.Components.Position.class, ecs.Components.Object.class);
        this.changedEntities = new HashSet<>();
        particleManager = ParticleEffectsManager.getInstance();
    }

    public void setGridDimensions(int cols, int rows) {
        this.gridNumCols = cols;
        this.gridNumRows = rows;
    }

    @Override
    protected boolean isInterested(Entity entity) {
        if (entity.contains(ecs.Components.Noun.class)) {
            return true;
        }
        if (entity.contains(ecs.Components.Verb.class)) {
            return true;
        }
        if (entity.contains(ecs.Components.Object.class)) {
            return true;
        }
        return false;
    }

    @Override
    public Set<EntityUpdate> update(double elapsedTime) {
        Set<EntityUpdate> changedEntities = new HashSet<>();
        var allYou = findYou(entities);

        for (var entity : entities.values()) {
            for (var you : allYou) {
                if (checkCollision(entity, you)) {
                    if (entity.contains(Movable.class)) { // block is pushable
                        var moveYou = you.get(ecs.Components.Movable.class);
                        var moveEnt = entity.get(ecs.Components.Movable.class);
                        moveEnt.moveTo = moveYou.moveTo; // block moves same direction as "you"
                        changedEntities.add(new EntityUpdate(entity, false));
                    }
                    else if (entity.contains(ecs.Components.Object.class)) {
                        handleCollision(you, entity);
                    }
                }
            }
        }
        
        return changedEntities;
    }

    private List<Entity> findYou(Map<Long, Entity> entities) {
        List<Entity> you = new ArrayList<>();
        for (var entity : entities.values()) {
            if (entity.contains(ecs.Components.KeyboardControlled.class) && 
                entity.contains(ecs.Components.Movable.class)) {
                you.add(entity);
            }
        }
        return you;
    }

    private boolean checkCollision(Entity a, Entity b) {
        if (a != b) {
            var aPos = a.get(Position.class);
            var bPos = b.get(Position.class);

            if (aPos.x == bPos.x && aPos.y == bPos.y) {
                return true;
            }
        }
        return false;
    }

    private void handleCollision(Entity you, Entity entity) {
        if (entity.contains(ecs.Components.Object.class) && entity.contains(Position.class)) {
            var obj = entity.get(ecs.Components.Object.class);
            var pos = entity.get(Position.class);
            
            // Calculate screen position for particle effects
            float centerX = 1920.0f / 2.0f;  // Screen width / 2
            float centerY = 1080.0f / 2.0f;  // Screen height / 2
            float tileSize = 32.0f;
            float posX = pos.x * tileSize + tileSize / 2 + centerX - (gridNumCols * tileSize / 2);
            float posY = pos.y * tileSize + tileSize / 2 + centerY - (gridNumRows * tileSize / 2);
            Vector2f position = new Vector2f(posX, posY);

            // Allow walking on floor objects
            if (obj.name.equals("floor")) {
                return;
            }

            // Check for win condition
            if (obj.name.equals("flag") || obj.name.equals("baba")) {
                particleManager.objectWin(position);
                return;
            }

            // Check for kill condition (lava, water, etc.)
            if (obj.name.equals("lava") || obj.name.equals("water")) {
                particleManager.objectDeath(position);
                return;
            }
        }
    }
}
