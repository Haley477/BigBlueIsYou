import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;

public class LevelParser {
    private final String levelsFilePath;

    // Maps for different types of objects
    public final Map<String, String> bgObjects = Map.ofEntries(
            Map.entry("l", "floor"),
            Map.entry("g", "grass"),
            Map.entry("h", "hedge")
    );

    public final Map<String, String> objects = Map.ofEntries(
            Map.entry("w", "wall"),
            Map.entry("r", "rock"),
            Map.entry("f", "flag"),
            Map.entry("b", "BigBlue"),
            Map.entry("a", "water"),
            Map.entry("v", "lava")
    );

    public final Map<String, String> nouns = Map.ofEntries(
            Map.entry("W", "wallname"),
            Map.entry("R", "rockname"),
            Map.entry("F", "flagname"),
            Map.entry("B", "baba"),
            Map.entry("S", "stop"),
            Map.entry("P", "push"),
            Map.entry("V", "lavaname"),
            Map.entry("A", "watername"),
            Map.entry("Y", "you"),
            Map.entry("X", "win"),
            Map.entry("N", "sink"),
            Map.entry("K", "kill")
    );

    private final String verb = "I";

    /**
     * Creates a new LevelParser with the specified levels file path
     * @param levelsFilePath Path to the levels file
     */
    public LevelParser(String levelsFilePath) {
        this.levelsFilePath = levelsFilePath;
    }

    /**
     * Creates a new LevelParser with the default levels file path
     */
    public LevelParser() {
        this("src/LevelInfo/levels-all.bbiy");
    }

    /**
     * Parses a level and returns a grid representation
     * @param levelName The name of the level to parse
     * @return A grid representation of the level, or null if the level couldn't be parsed
     */
    public String[][] parseLevel(String levelName) {
        int startLine = findLevel(levelName);

        if (startLine == -1) {
            System.out.println("Error: Level Not Found");
            return null;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(levelsFilePath))) {
            String line;
            int currentLine = 1;

            // Skip until we get to the level header's line
            while ((line = reader.readLine()) != null && currentLine < startLine) {
                currentLine++;
            }
            line = reader.readLine();

            // At this point we should be at the dimensions
            String[] dimensions = line.split("x");

            // Creating 1st "box" from the file
            String[][] objects1 = new String[Integer.parseInt(dimensions[0].strip())][Integer.parseInt(dimensions[1].strip())];
            for (int i = 0; i < objects1.length; i++) {
                line = reader.readLine();
                for (int j = 0; j < line.length(); j++) {
                    char item = line.charAt(j);
                    objects1[i][j] = Character.toString(item);
                }
            }

            // Creating 2nd "box" from the file
            String[][] objects2 = new String[Integer.parseInt(dimensions[0].strip())][Integer.parseInt(dimensions[1].strip())];
            for (int i = 0; i < objects2.length; i++) {
                line = reader.readLine();
                for (int j = 0; j < line.length(); j++) {
                    char item = line.charAt(j);
                    objects2[i][j] = Character.toString(item);
                }
            }

            return createGrid(objects1, objects2);

        } catch (Exception e) {
            System.out.println("Something went wrong while parsing the level...");
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Find the line where the level's header is
     * @param levelName The name of the level to find
     * @return The line number where the level header is, or -1 if not found
     */
    private int findLevel(String levelName) {
        String keyword = levelName;
        int lineNumber = 1;

        try (BufferedReader reader = new BufferedReader(new FileReader(levelsFilePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains(keyword)) {
                    return lineNumber;
                }
                lineNumber++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return -1;
    }

    /**
     * Convert the two object boxes into a single play area (grid)
     * Background objects are left out of grid since they don't do anything except get rendered
     * Grid will track movement and development of rules
     * @param obj1 First object box
     * @param obj2 Second object box
     * @return A grid representation of the level
     */
    private String[][] createGrid(String[][] obj1, String[][] obj2) {
        String[][] grid = new String[obj1.length][obj1[0].length];
        for (int i = 0; i < grid.length; i++) {
            for (int j = 0; j < grid[i].length; j++) {
                grid[i][j] = "";
            }
        }

        // Process first object box
        for (int i = 0; i < obj1.length; i++) {
            for (int j = 0; j < obj1[i].length; j++) {
                if (bgObjects.containsKey(obj1[i][j])) {
                    // Add background objects to the grid
                    grid[i][j] = bgObjects.get(obj1[i][j]);
                }
                else if (objects.containsKey(obj1[i][j])) {
                    grid[i][j] = objects.get(obj1[i][j]);
                }
                else if (nouns.containsKey(obj1[i][j])) {
                    grid[i][j] = nouns.get(obj1[i][j]);
                }
                else if (verb.equals(obj1[i][j])) {
                    grid[i][j] = "is";
                }
            }
        }

        // Process second object box
        for (int i = 0; i < obj2.length; i++) {
            for (int j = 0; j < obj2[i].length; j++) {
                if (bgObjects.containsKey(obj2[i][j])) {
                    // Add background objects to the grid
                    grid[i][j] = bgObjects.get(obj2[i][j]);
                }
                else if (objects.containsKey(obj2[i][j])) {
                    grid[i][j] = objects.get(obj2[i][j]);
                }
                else if (nouns.containsKey(obj2[i][j])) {
                    grid[i][j] = nouns.get(obj2[i][j]);
                }
                else if (verb.equals(obj2[i][j])) {
                    grid[i][j] = "is";
                }
            }
        }

        return grid;
    }
}