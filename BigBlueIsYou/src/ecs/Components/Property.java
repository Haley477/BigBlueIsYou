package ecs.Components;

import ecs.Entities.Entity;
import java.util.HashSet;
import java.util.Set;

/**
 * Component that stores the properties applied to an entity through rules.
 * Uses bit flags to efficiently store multiple properties.
 */
public class Property extends Component {
    public enum PropertyType {
        YOU(0x0001),
        WIN(0x0002),
        STOP(0x0004),
        PUSH(0x0008),
        DEFEAT(0x0010),
        SINK(0x0020);

        private final int flag;

        PropertyType(int flag) {
            this.flag = flag;
        }

        public int getFlag() {
            return flag;
        }
    }

    private int properties;

    public Property(Entity owner) {
        this.properties = 0;
    }

    public void addProperty(PropertyType property) {
        properties |= property.getFlag();
    }

    public void removeProperty(PropertyType property) {
        properties &= ~property.getFlag();
    }

    public boolean hasProperty(PropertyType property) {
        return (properties & property.getFlag()) != 0;
    }

    public void clearProperties() {
        properties = 0;
    }

    public Set<PropertyType> getProperties() {
        Set<PropertyType> properties = new HashSet<>();
        for (PropertyType type : PropertyType.values()) {
            if (hasProperty(type)) {
                properties.add(type);
            }
        }
        return properties;
    }
}