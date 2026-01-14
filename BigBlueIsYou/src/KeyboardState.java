public class KeyboardState {

    public KeyboardState() {
        initialized = false;
    }

    public KeyboardState(int moveUpKey, int moveDownKey, int moveLeftKey, int moveRightKey, int undo, int resetLevel) {
        this.moveUpKey = moveUpKey;
        this.moveDownKey = moveDownKey;
        this.moveLeftKey = moveLeftKey;
        this.moveRightKey = moveRightKey;
        this.undo = undo;
        this.resetLevel = resetLevel;
        this.initialized = true;
    }

    public int moveUpKey;
    public int moveDownKey;
    public int moveLeftKey;
    public int moveRightKey;
    public int undo;
    public int resetLevel;

    public boolean initialized = false;
}
