package ecs.Entities;

import ecs.Components.Appearance;
import ecs.Components.Movable;
import edu.usu.graphics.Texture;

public class VerbEnt {

    public static Entity create(int x, int y, String name, Appearance appearance) {
        var verb = new Entity();

        verb.add(new ecs.Components.Position(x, y));
        verb.add(appearance);
        verb.add(new ecs.Components.Movable(Movable.MoveTo.Stopped));
        verb.add(new ecs.Components.Verb(name));

        return verb;
    }
}
