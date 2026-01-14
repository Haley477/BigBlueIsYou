package ecs.Components;

public class Object extends Component {
    public String name;
    private Property properties;

    public Object(String name) {
        this.name = name;
        this.properties = new Property(null); // Initialize with null owner, will be set by Component constructor
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Property getProperties() {
        return properties;
    }

    public boolean hasProperty(Property.PropertyType property) {
        return properties.hasProperty(property);
    }

    public void addProperty(Property.PropertyType property) {
        properties.addProperty(property);
    }

    public void removeProperty(Property.PropertyType property) {
        properties.removeProperty(property);
    }

    public void clearProperties() {
        properties.clearProperties();
    }
}
