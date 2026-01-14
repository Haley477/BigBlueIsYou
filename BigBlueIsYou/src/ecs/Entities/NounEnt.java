package ecs.Entities;

import ecs.Components.Appearance;
import ecs.Components.Movable;

public class NounEnt {

    public static Entity create(int x, int y, String name, Appearance appearance) {
        var noun = new Entity();

        noun.add(new ecs.Components.Position(x, y));
        noun.add(appearance);
        noun.add(new ecs.Components.Movable(Movable.MoveTo.Stopped));
        noun.add(new ecs.Components.Noun(name));

        return noun;
    }
}
