package ecs.Entities;

import ecs.Components.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A named entity that contains a collection of Component instances
 */
public final class Entity {
    private static long nextId = 0;

    private long id;
    private final Map<Class<? extends Component>, Component> components = new HashMap<>();

    public Entity() {
        id = nextId++;
    }

    public long getId() {
        return id;
    }

    public void add(Component component) {
        Objects.requireNonNull(component, "components cannot be null");
        if (this.components.containsKey(component.getClass())) {
            throw new IllegalArgumentException("cannot add the same component twice");
        }

        this.components.put(component.getClass(), component);
    }

    public <TComponent extends Component> void remove(Class<TComponent> type) {
        this.components.remove(type);
    }

    public <TComponent extends Component> boolean contains(Class<TComponent> type) {
        return components.containsKey(type) && components.get(type) != null;
    }

    public <TComponent extends Component> TComponent get(Class<TComponent> type) {
        if (!components.containsKey(type)) {
            throw new IllegalArgumentException(String.format("component of type %s is not a part of this entity", type.getName()));
        }
        // The use of generic to define TComponent is motivated by this code.  The use
        // of Class<? extends Component> won't return the actual component type, instead
        // it only returns Component, but we need the actual component type instead.
        return type.cast(this.components.get(type));
    }

    public void clear() {
        components.clear();
    }

    @Override
    public String toString() {
        return String.format("%d: %s", id, components.values().stream().map(c -> c.getClass().getSimpleName()).collect(Collectors.joining(", ")));
    }
}
