package ecs.Components;

public class Text extends Component {
    public enum TextType {
        NOUN,
        VERB,
        ADJECTIVE
    }

    private final TextType type;
    private final String text;

    public Text(TextType type, String text) {
        this.type = type;
        this.text = text;
    }

    public TextType getType() {
        return type;
    }

    public String getText() {
        return text;
    }
}