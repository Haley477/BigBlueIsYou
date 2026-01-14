package ecs.Entities;

import ecs.Components.Appearance;
import ecs.Components.Object;
import ecs.Components.Position;
import ecs.Components.Property;
import ecs.Components.Text;
import edu.usu.graphics.Texture;
import java.util.Map;

public class EntityFactory {
    private Map<String, Texture> textureCache;
    private static final float TILE_SIZE = 32.0f;

    public EntityFactory(Map<String, Texture> textureCache) {
        this.textureCache = textureCache;
    }

    public Entity createWall(int x, int y) {
        Entity wall = new Entity();
        wall.add(new Position(x, y));
        wall.add(new Appearance(textureCache.get("wall"), TILE_SIZE));
        wall.add(new Object("wall"));
        return wall;
    }

    public Entity createRock(int x, int y, boolean pushable) {
        Entity rock = new Entity();
        rock.add(new Position(x, y));
        rock.add(new Appearance(textureCache.get("rock"), TILE_SIZE));
        rock.add(new Object("rock"));
        if (pushable) {
            rock.add(new Property(rock));
            rock.get(Property.class).addProperty(Property.PropertyType.PUSH);
        }
        return rock;
    }

    public Entity createFlag(int x, int y) {
        Entity flag = new Entity();
        flag.add(new Position(x, y));
        flag.add(new Appearance(textureCache.get("flag"), TILE_SIZE));
        flag.add(new Object("flag"));
        return flag;
    }

    public Entity createBigBlue(int x, int y) {
        Entity bigBlue = new Entity();
        bigBlue.add(new Position(x, y));
        bigBlue.add(new Appearance(textureCache.get("BigBlue"), TILE_SIZE));
        bigBlue.add(new Object("BigBlue"));
        return bigBlue;
    }

    public Entity createText(String text, String type, int x, int y) {
        Entity textEntity = new Entity();
        textEntity.add(new Position(x, y));
        textEntity.add(new Appearance(textureCache.get(text.toLowerCase()), TILE_SIZE));
        textEntity.add(new Object(text));
        textEntity.add(new Text(Text.TextType.valueOf(type.toUpperCase()), text));
        return textEntity;
    }
}