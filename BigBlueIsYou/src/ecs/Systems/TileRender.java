package ecs.Systems;

import ecs.Entities.Entity;
import edu.usu.graphics.*;
import org.joml.Vector2f;
import java.util.HashSet;
import java.util.Set;
import static java.lang.System.out;

public class TileRender extends System {

    Graphics2D graphics;
    private static final float SCREEN_WIDTH = 1380.0f;  // Match the window width
    private static final float SCREEN_HEIGHT = 1380.0f; // Match the window height
    private final float TILE_SIZE = 32.0f; // Default tile size in pixels
    private int gridNumRows;
    private int gridNumCols;

    public TileRender(Graphics2D graphics) {
        super(ecs.Components.Appearance.class, ecs.Components.Position.class);
        this.graphics = graphics;
    }

    public void setNums(String[][] grid) {
        this.gridNumRows = grid.length;
        this.gridNumCols = grid[0].length;
    }

    @Override
    public Set<EntityUpdate> update(double elapsedTime) {
        for (var entity : entities.values()) {
            render(entity);
        }
        // Rendering systems typically don't modify entities, so return empty set
        return new HashSet<>();
    }

    public void render(Entity entity) {
        var appearance = entity.get(ecs.Components.Appearance.class);
        var position = entity.get(ecs.Components.Position.class);

        // Calculate center offset to move the grid to the center of the screen
        float centerX = SCREEN_WIDTH / 2.0f;
        float centerY = SCREEN_HEIGHT / 2.0f;

        int posX = (int)(position.x * TILE_SIZE + TILE_SIZE / 2 + centerX - (gridNumCols * TILE_SIZE / 2));
        int posY = (int)(position.y * TILE_SIZE + TILE_SIZE / 2 + centerY - (gridNumRows * TILE_SIZE / 2));

        // Convert pixel coordinates to normalized coordinates (-1 to 1)
        float normalizedX = (posX / SCREEN_WIDTH) * 2.0f - 1.0f;
        float normalizedY = ((posY / SCREEN_HEIGHT) * 2.0f - 1.0f);
        float normalizedWidth = (appearance.size / SCREEN_WIDTH) * 2.0f;
        float normalizedHeight = (appearance.size / SCREEN_HEIGHT) * 2.0f;

        // Determine z-value based on entity properties
        float zValue = 0.0f; // Default value
        if (entity.contains(ecs.Components.Property.class)) {
            var property = entity.get(ecs.Components.Property.class);
            if (property.hasProperty(ecs.Components.Property.PropertyType.YOU)) {
                zValue = 1.0f; // Entities with "you" property are drawn on top
            }
        }
        if (entity.contains(ecs.Components.Object.class)) {
            var object = entity.get(ecs.Components.Object.class);
            if (object.name.equals("floor")) {
                zValue = -1.0f; // Floor objects are drawn at the bottom
            }
        }

        // Create rectangle in normalized coordinates, centered at the position
        Rectangle destination = new Rectangle(
                normalizedX - (normalizedWidth / 2),
                normalizedY - (normalizedHeight / 2),
                normalizedWidth,
                normalizedHeight,
                zValue
        );

        // Create center point for rotation
        Vector2f center = new Vector2f(normalizedX, normalizedY);

        if (appearance.isAnimated()) {
            // Calculate sub-image dimensions
            int subImageWidth = appearance.image.getWidth() / appearance.getTotalFrames();
            int subImageHeight = appearance.image.getHeight();

            // Create sub-image rectangle
            Rectangle subImage = new Rectangle(
                    subImageWidth * appearance.getCurrentFrame(),
                    0,
                    subImageWidth,
                    subImageHeight
            );
            // Draw the sub-image with rotation and center
            graphics.draw(appearance.image, destination, subImage, 0.0f, center, Color.WHITE);
        } else {
            // Draw the full image
            graphics.draw(appearance.image, destination, Color.WHITE);
        }
    }
}
