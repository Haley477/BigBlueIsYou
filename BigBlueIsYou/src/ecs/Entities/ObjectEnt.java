package ecs.Entities;

import ecs.Components.Appearance;
import edu.usu.graphics.Texture;

import java.util.ArrayList;

public class ObjectEnt {

    public static Entity create(int x, int y, String name, Appearance appearance) {
        var obj = new Entity();

        obj.add(new ecs.Components.Position(x, y));
        obj.add(appearance);
        obj.add(new ecs.Components.Object(name));

        return obj;
    }
}
