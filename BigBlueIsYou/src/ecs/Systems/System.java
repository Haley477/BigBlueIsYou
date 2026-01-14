package ecs.Systems;

import ecs.Components.Component;
import ecs.Entities.Entity;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * The base class for all systems in this ECS environment.
 * Derived classes should provide ComponentTypes to specify the types of Component an Entity
 * must have in order for the system work with it, and update to implement system-specific
 * behavior with matching entities.
 */
public abstract class System {

    protected Map<Long, Entity> entities = new HashMap<>();
    private final Class<? extends Component>[] componentTypes;

    @SafeVarargs
    public System(Class<? extends Component>... types) {
        this.componentTypes = types;
    }

    /**
     * If the entity has all the component types associated with the system, this method
     * returns true, otherwise false.
     */
    protected boolean isInterested(Entity entity) {
        for (var type : componentTypes) {
            if (!entity.contains(type)) {
                return false;
            }
        }
        return true;
    }

    /**
     * If the system is interested in the entity, it is added to the tracking collection
     */
    public boolean add(Entity entity) {
        boolean interested = isInterested(entity);
        if (interested) {
            entities.put(entity.getId(), entity);
        }
        return interested;
    }

    /**
     * Removes the entity from the tracking collection.  If the entity was actually in
     * the system true is returned, false otherwise.
     */
    public boolean remove(long id) {
        return entities.remove(id) != null;
    }

    /**
     * Derived systems must override this method to perform update logic specific to that system.
     * @return A set of entities that were modified during the update, along with a boolean indicating
     *         whether the entity should be removed (true) or just updated (false)
     */
    public abstract Set<EntityUpdate> update(double elapsedTime);

    /**
     * Called when an entity has been updated by another system
     * @param entity The entity that was updated
     */
    public void updatedEntity(Entity entity) {
        if (isInterested(entity)) {
            entities.put(entity.getId(), entity);
        } else {
            entities.remove(entity.getId());
        }
    }

    /**
     * Data class to represent an entity update result
     */
    public static class EntityUpdate {
        public final Entity entity;
        public final boolean shouldRemove;

        public EntityUpdate(Entity entity, boolean shouldRemove) {
            this.entity = entity;
            this.shouldRemove = shouldRemove;
        }
    }
}
