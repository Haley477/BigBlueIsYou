package ecs.Entities;

import ecs.Components.Appearance;

public class Bg {

    public static Entity create(int x, int y, String name, Appearance appearance) {
        var bg = new Entity();

        bg.add(new ecs.Components.Position(x, y));
        bg.add(appearance);

        return bg;
    }
}
