import edu.usu.graphics.Color;
import edu.usu.graphics.Graphics2D;

public class BigBlueIsYou {
    public static void main(String[] args) {
        try (Graphics2D graphics = new Graphics2D(1920, 1080, "Big Blue is You")) {
            graphics.initialize(Color.BLACK);
            Manager game = new Manager(graphics);
            game.initialize();
            game.run();
            game.shutdown();
        }
    }
}