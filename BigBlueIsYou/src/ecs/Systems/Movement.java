package ecs.Systems;

import ecs.Components.Movable;
import ecs.Components.Property;
import ecs.Entities.Entity;
import java.util.*;

public class Movement extends System {
    private String[][] grid;
    private boolean won;
    private ArrayList<Entity> pushChain;
    private Set<EntityUpdate> changedEntities;

    public Movement() {
        super(ecs.Components.Position.class, ecs.Components.Object.class,
                ecs.Components.Movable.class, ecs.Components.KeyboardControlled.class);

        this.won = false;
    }

    @Override
    protected boolean isInterested(Entity entity) {
        if (entity.contains(ecs.Components.Position.class) && entity.contains(ecs.Components.Object.class)
                && entity.contains(ecs.Components.Movable.class) && entity.contains(ecs.Components.KeyboardControlled.class)) {
            return true;
        }
        else if (entity.contains(ecs.Components.Position.class)) {
            return true;
        }
        return false;
    }

    private List<Entity> findYou(Map<Long, Entity> entities) {
        List<Entity> you = new ArrayList<>();
        for (var entity : entities.values()) {
            if (entity.contains(ecs.Components.Position.class) && entity.contains(ecs.Components.Object.class)
                    && entity.contains(ecs.Components.Movable.class) && entity.contains(ecs.Components.KeyboardControlled.class)) {

                you.add(entity);
            }
        }
        return you;
    }

    private Entity getEntityAtPosition(int x, int y) {
        for (var entity : entities.values()) {
            var pos = entity.get(ecs.Components.Position.class);

            if (pos.x == x && pos.y == y) {
                return entity;
            }
        }

        return null;
    }

    @Override
    public Set<EntityUpdate> update(double elapsedTime) {
        this.changedEntities = new HashSet<>();

        var allYou = findYou(entities);

        for (var you : allYou) {
            moveEntity(you, elapsedTime);
        }

        return changedEntities;
    }

    private void moveEntity(Entity entity, double elapsedTime) {
        if (!won) {
            var movable = entity.get(ecs.Components.Movable.class);
            var pos = entity.get(ecs.Components.Position.class);
            boolean moved = false;

            this.grid[pos.y][pos.x] = "";

            switch (movable.moveTo) {
                case Up:
                    if (pos.y > 1) {
                        pos.y -= 1;
                        checkPosition(entity);
                        moved = true;
                    }
                    break;
                case Down:
                    if (pos.y < grid.length - 2) {
                        pos.y += 1;
                        checkPosition(entity);
                        moved = true;
                    }
                    break;
                case Left:
                    if (pos.x > 1) {
                        pos.x -= 1;
                        checkPosition(entity);
                        moved = true;
                    }
                    break;
                case Right:
                    if (pos.x < grid[0].length - 2) {
                        pos.x += 1;
                        checkPosition(entity);
                        moved = true;
                    }
                    break;
            }

            movable.moveTo = Movable.MoveTo.Stopped;
            var obj = entity.get(ecs.Components.Object.class);
            this.grid[pos.y][pos.x] = obj.name;

            // Add to changed entities if moved
            if (moved) {
                changedEntities.add(new EntityUpdate(entity, false));
            }
        }
    }

    public String[][] getGrid() {
        return this.grid;
    }
    public void setGrid(String[][] grid) {
        this.grid = grid;
    }

    /**
     * Determine whether or not "you" can move based on the tile it is trying to move to
     * overlapEntity is an entity in the same tile as "you" (if there is one)
     */
    private void checkPosition(Entity you) {
        for (var overlapEntity : entities.values()) {
            if (overlapEntity != you) {
                var pos1 = you.get(ecs.Components.Position.class);
                var pos2 = overlapEntity.get(ecs.Components.Position.class);

                // same tile
                if (pos1.x == pos2.x && pos1.y == pos2.y) {
                    if (overlapEntity.contains(ecs.Components.Property.class)) {
                        var prop = overlapEntity.get(ecs.Components.Property.class);

                        // Stop condition
                        if (prop.hasProperty(Property.PropertyType.STOP)) {
                            movePlayerBack(you);
                        }

                        // Win condition
                        else if (prop.hasProperty(Property.PropertyType.WIN)) {
                            this.won = true;
                            changedEntities.add(new EntityUpdate(you, false));
                        }

                        // Defeat condition
                        else if (prop.hasProperty(Property.PropertyType.DEFEAT)) {
                            changedEntities.add(new EntityUpdate(you, true));
                        }

                        // Sink condition
                        else if (prop.hasProperty(Property.PropertyType.SINK)) {
                            changedEntities.add(new EntityUpdate(you, true));
                            changedEntities.add(new EntityUpdate(overlapEntity, true));
                        }
                    }

                    // Push condition
                    else if (overlapEntity.contains(ecs.Components.Movable.class)) {
                        handlePush(you, overlapEntity);
                    }
                }
            }
        }
    }

    private void handlePush(Entity you, Entity overlapEntity) {
        var movable = you.get(ecs.Components.Movable.class);
        pushChain = new ArrayList<>();
        getPushChain(overlapEntity, movable.moveTo);

        Entity last = pushChain.get(pushChain.size() - 1);
        var lastPos = last.get(ecs.Components.Position.class);

        switch (movable.moveTo) {
            case Up:
                if (lastPos.y > 1) {
                    if (grid[lastPos.y - 1][lastPos.x].isEmpty()) {
                        pushAll(movable.moveTo);
                    }
                    else {
                        Entity nextObj = getEntityAtPosition(lastPos.x, lastPos.y - 1);
                        checkPushConditions(last, nextObj, you);
                    }
                }
                else {
                    movable.moveTo = Movable.MoveTo.Down;
                    moveEntity(you, 0.0);
                }
                break;
            case Down:
                if (lastPos.y < grid.length - 2) {
                    if (grid[lastPos.y + 1][lastPos.x].isEmpty()) {
                        pushAll(movable.moveTo);
                    }
                    else {
                        Entity nextObj = getEntityAtPosition(lastPos.x, lastPos.y + 1);
                        checkPushConditions(last, nextObj, you);
                    }
                }
                else {
                    movable.moveTo = Movable.MoveTo.Up;
                    moveEntity(you, 0.0);
                }
                break;
            case Left:
                if (lastPos.x > 1) {
                    if (grid[lastPos.y][lastPos.x - 1].isEmpty()) {
                        pushAll(movable.moveTo);
                    }
                    else {
                        Entity nextObj = getEntityAtPosition(lastPos.x - 1, lastPos.y);
                        checkPushConditions(last, nextObj, you);
                    }
                }
                else {
                    movable.moveTo = Movable.MoveTo.Right;
                    moveEntity(you, 0.0);
                }
                break;
            case Right:
                if (lastPos.x < grid[0].length - 2) {
                    if (grid[lastPos.y][lastPos.x + 1].isEmpty()) {
                        pushAll(movable.moveTo);
                    }
                    else {
                        Entity nextObj = getEntityAtPosition(lastPos.x + 1, lastPos.y);
                        checkPushConditions(last, nextObj, you);
                    }
                }
                else {
                    movable.moveTo = Movable.MoveTo.Left;
                    moveEntity(you, 0.0);
                }
                break;
        }
    }

    /**
     * Push is blocked by stop or sink
     * Determine action based on which
     */
    private void checkPushConditions(Entity last, Entity entity, Entity you) {
        if (entity != null) {
            if (entity.contains(ecs.Components.Property.class)) {
                var prop = entity.get(ecs.Components.Property.class);
                var movable = you.get(ecs.Components.Movable.class);

                // Stop condition
                if (prop.hasProperty(Property.PropertyType.STOP)) {
                    movePlayerBack(you);
                }

                // take in last, pushAll, remove last and entity entities
                else if (prop.hasProperty(Property.PropertyType.SINK)) {
                    pushAll(movable.moveTo);
                    changedEntities.add(new EntityUpdate(last, true));
                    changedEntities.add(new EntityUpdate(entity, true));
                }
            }
        }
    }

    /**
     * Push all entities in the chain
     */
    private void pushAll(Movable.MoveTo moveTo) {
        Entity first = pushChain.get(0);
        var firstPos = first.get(ecs.Components.Position.class);
        var allYou = findYou(entities);
        for (var you : allYou) {
            var youPos = you.get(ecs.Components.Position.class);

            if (youPos.x == firstPos.x && youPos.y == firstPos.y) {
                changedEntities.add(new EntityUpdate(you, false));
                break;
            }
        }

        for (var entity : pushChain) {
            changedEntities.add(new EntityUpdate(entity, false));
            var pos = entity.get(ecs.Components.Position.class);

            switch (moveTo) {
                case Up:
                    pos.y -= 1;
                    break;
                case Down:
                    pos.y += 1;
                    break;
                case Left:
                    pos.x -= 1;
                    break;
                case Right:
                    pos.x += 1;
                    break;
            }

            String name = "";
            if (entity.contains(ecs.Components.Object.class)) {
                name = entity.get(ecs.Components.Object.class).name;
            }
            else if (entity.contains(ecs.Components.Verb.class)) {
                name = entity.get(ecs.Components.Verb.class).name;
            }
            else if (entity.contains(ecs.Components.Noun.class)) {
                name = entity.get(ecs.Components.Noun.class).name;
            }
            this.grid[pos.y][pos.x] = name;
        }
    }

    /**
     * Prevent player movement during a STOP condition
     */
    private void movePlayerBack(Entity you) {
        var movable = you.get(ecs.Components.Movable.class);

        if (movable.moveTo == Movable.MoveTo.Up) {
            movable.moveTo = Movable.MoveTo.Down;
            moveEntity(you, 0.0);
        }
        if (movable.moveTo == Movable.MoveTo.Down) {
            movable.moveTo = Movable.MoveTo.Up;
            moveEntity(you, 0.0);
        }
        if (movable.moveTo == Movable.MoveTo.Left) {
            movable.moveTo = Movable.MoveTo.Right;
            moveEntity(you, 0.0);
        }
        if (movable.moveTo == Movable.MoveTo.Right) {
            movable.moveTo = Movable.MoveTo.Left;
            moveEntity(you, 0.0);
        }
    }

    /**
     * allows side-by-side push-ables to all get pushed
     * ex. pushing an entire noun-verb-noun rule, 2 side-by-side rocks, etc.
     */
    private void getPushChain(Entity entity, Movable.MoveTo moveTo) {
        if (entity.contains(ecs.Components.Object.class)) {
            var pos1 = entity.get(ecs.Components.Position.class);
            var name = entity.get(ecs.Components.Object.class).name;
            java.lang.System.out.printf("%s @ [%d, %d]\n", name, pos1.y, pos1.x);
        }
        if (entity.contains(ecs.Components.Verb.class)) {
            var pos1 = entity.get(ecs.Components.Position.class);
            var name = entity.get(ecs.Components.Verb.class).name;
            java.lang.System.out.printf("%s @ [%d, %d]\n", name, pos1.y, pos1.x);
        }
        if (entity.contains(ecs.Components.Noun.class)) {
            var pos1 = entity.get(ecs.Components.Position.class);
            var name = entity.get(ecs.Components.Noun.class).name;
            java.lang.System.out.printf("%s @ [%d, %d]\n", name, pos1.y, pos1.x);
        }

        pushChain.add(entity);
        for (var nextEntity : entities.values()) {
            if (nextEntity.contains(ecs.Components.Movable.class)) {
                var pos1 = entity.get(ecs.Components.Position.class);
                var pos2 = nextEntity.get(ecs.Components.Position.class);

                switch (moveTo) {
                    case Up:
                        if (pos2.x == pos1.x && pos2.y == pos1.y - 1) {
                            getPushChain(nextEntity, moveTo);
                        }
                        break;
                    case Down:
                        if (pos2.x == pos1.x && pos2.y == pos1.y + 1) {
                            getPushChain(nextEntity, moveTo);
                        }
                        break;
                    case Left:
                        if (pos2.x == pos1.x - 1 && pos2.y == pos1.y) {
                            getPushChain(nextEntity, moveTo);
                        }
                        break;
                    case Right:
                        if (pos2.x == pos1.x + 1 && pos2.y == pos1.y) {
                            getPushChain(nextEntity, moveTo);
                        }
                        break;
                }
            }
        }
    }
}
