package it.unical.ea_project_javafx.ui.style;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;

/** Protects the reusable theme boundary when future screens are added. */
class StylesheetArchitectureTest {
    private final Path css;

    StylesheetArchitectureTest() throws Exception {
        css = Path.of(getClass().getResource("/it/unical/ea_project_javafx/css/app.css").toURI()).getParent();
    }

    private List<Path> files(Path root, String suffix) throws Exception {
        try (var stream = Files.walk(root)) {
            return stream.filter(p -> p.toString().endsWith(suffix)).toList();
        }
    }

    private List<Path> imports(Path file) throws Exception {
        var matcher = Pattern.compile("@import\\s+\"([^\"]+)\"\\s*;").matcher(Files.readString(file));
        var result = new ArrayList<Path>();
        while (matcher.find())
            result.add(file.getParent().resolve(matcher.group(1)).normalize());
        return result;
    }

    private void acyclic(Path file, Set<Path> visiting) throws Exception {
        assertTrue(Files.isRegularFile(file), "Missing stylesheet: " + file);
        assertTrue(visiting.add(file), "CSS import cycle: " + file);
        for (Path imported : imports(file))
            acyclic(imported, visiting);
        visiting.remove(file);
    }

    @Test
    void sharedComponentsNeverDependOnScreensAndImportsHaveNoCycles() throws Exception {
        for (Path file : files(css, ".css")) {
            acyclic(file, new HashSet<>());
            if (file.startsWith(css.resolve("components")) || file.equals(css.resolve("theme.css")))
                assertTrue(imports(file).isEmpty(), "Shared component must be independent: " + file);
            if (file.startsWith(css.resolve("screens")))
                assertEquals(List.of(css.resolve("app.css")), imports(file),
                        "Screen must inherit only app.css: " + file);
        }
        assertEquals(css.resolve("theme.css"), imports(css.resolve("app.css")).getFirst());
    }

    @Test
    void everyFxmlHasOneExplicitExistingStylesheetIncludingFragments() throws Exception {
        var builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        for (Path file : files(css.getParent().resolve("fxml"), ".fxml")) {
            var document = builder.parse(file.toFile());
            String entry = document.getDocumentElement().getAttribute("stylesheets");
            assertTrue(entry.startsWith("@"), "Missing explicit entry: " + file);
            assertTrue(Files.isRegularFile(file.getParent().resolve(entry.substring(1))), "Missing entry: " + file);
            assertEquals(0, document.getElementsByTagName("stylesheets").getLength(),
                    "Avoid extra screen imports: " + file);
            assertTrue(document.getDocumentElement().getAttribute("style").isEmpty(),
                    "Root style belongs in CSS: " + file);
        }
    }

    @Test
    void colorAndFontLiteralsHaveOneOwner() throws Exception {
        var colors = Pattern.compile("#[0-9a-fA-F]{3,8}\\b|rgba?\\(");
        for (Path file : files(css, ".css")) {
            String source = Files.readString(file).replaceAll("(?s)/\\*.*?\\*/", "");
            if (!file.equals(css.resolve("theme.css"))) {
                assertFalse(colors.matcher(source).find(), "Color literal outside the theme: " + file);
                assertFalse(source.contains("-fx-font-family:"), "Font family outside the theme: " + file);
            }
        }
    }

    @Test
    void everyLookedUpColorIsDefinedByTheTheme() throws Exception {
        var names = Pattern.compile("(?<![\\w-])(-(?:color|tone|ea|app)-[\\w-]+)");
        var declarations = Pattern.compile("(-(?:color|tone|ea|app)-[\\w-]+)\\s*:");
        Set<String> defined = new HashSet<>();
        var matches = declarations.matcher(Files.readString(css.resolve("theme.css")));
        while (matches.find())
            defined.add(matches.group(1));
        for (Path file : files(css, ".css")) {
            var reference = names.matcher(Files.readString(file).replaceAll("(?s)/\\*.*?\\*/", ""));
            while (reference.find())
                assertTrue(defined.contains(reference.group(1)),
                        "Undefined color " + reference.group(1) + " in " + file);
        }
    }
}
