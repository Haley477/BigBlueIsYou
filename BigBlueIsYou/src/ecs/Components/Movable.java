package ecs.Components;

public class Movable extends Component {

    public enum MoveTo {
        Stopped,
        Up,
        Down,
        Left,
        Right
    }

    public MoveTo moveTo;

    public Movable(MoveTo moveTo) {
        this.moveTo = moveTo;
    }
}
