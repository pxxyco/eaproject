package it.unical.ea_project_javafx.ui.text;

import java.util.List;
import java.util.regex.Pattern;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

/**
 * Uses platform emoji fonts with an outline fallback: bitmap-only CBDT fonts
 * are invisible in JavaFX on Linux.
 */
public final class EmojiText {
    private static final String EMOJI_FONT = List
            .of("Apple Color Emoji", "Segoe UI Emoji", "Noto Emoji", "Noto Sans Symbols 2")
            .stream().filter(Font.getFamilies()::contains).findFirst().orElse(Font.getDefault().getFamily());
    private static final Pattern GRAPHEME = Pattern.compile("\\X");

    private EmojiText() {
    }

    public static TextFlow create(String value) {
        TextFlow flow = new TextFlow();
        var matcher = GRAPHEME.matcher(value);
        StringBuilder plain = new StringBuilder();
        while (matcher.find()) {
            String unit = matcher.group();
            boolean emoji = unit.codePoints().anyMatch(c -> (c >= 0x1f000 && c <= 0x1faff)
                    || (c >= 0x2300 && c <= 0x27bf) || c == 0x200d || c == 0xfe0f || c == 0x20e3);
            if (emoji) {
                if (!plain.isEmpty()) {
                    flow.getChildren().add(text(plain.toString(), false));
                    plain.setLength(0);
                }
                flow.getChildren().add(text(unit, true));
            } else
                plain.append(unit);
        }
        if (!plain.isEmpty())
            flow.getChildren().add(text(plain.toString(), false));
        return flow;
    }

    private static Text text(String value, boolean emoji) {
        Text text = new Text(value);
        text.getStyleClass().add("chat-text-run");
        if (emoji) {
            text.setFont(Font.font(EMOJI_FONT, 20));
            text.setStyle("-fx-font-family: \"" + EMOJI_FONT + "\"; -fx-font-size: 20px;");
        }
        return text;
    }
}
