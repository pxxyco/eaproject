package it.unical.ea_project_javafx.ui.file;

import java.io.File;
import java.util.List;
import java.util.function.Supplier;
import javafx.application.Platform;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;

public final class NativeFiles {
    private NativeFiles() {
    }

    public static File open(FileChooser chooser, Window owner) {
        return preserve(owner, () -> chooser.showOpenDialog(owner));
    }

    public static List<File> openMultiple(FileChooser chooser, Window owner) {
        return preserve(owner, () -> chooser.showOpenMultipleDialog(owner));
    }

    public static File save(FileChooser chooser, Window owner) {
        return preserve(owner, () -> chooser.showSaveDialog(owner));
    }

    public static <T> T preserve(Window owner, Supplier<T> dialog) {
        if (!(owner instanceof Stage stage))
            return dialog.get();
        double x = stage.getX(), y = stage.getY(), width = stage.getWidth(), height = stage.getHeight();
        boolean fullscreen = stage.isFullScreen(), maximized = stage.isMaximized();
        Runnable restore = () -> {
            if (!stage.isShowing())
                return;
            if (!stage.isFullScreen() && !stage.isMaximized()) {
                if (Double.isFinite(x) && Math.abs(stage.getX() - x) > .5)
                    stage.setX(x);
                if (Double.isFinite(y) && Math.abs(stage.getY() - y) > .5)
                    stage.setY(y);
                if (Double.isFinite(width) && Math.abs(stage.getWidth() - width) > .5)
                    stage.setWidth(width);
                if (Double.isFinite(height) && Math.abs(stage.getHeight() - height) > .5)
                    stage.setHeight(height);
            }
            if (stage.isMaximized() != maximized)
                stage.setMaximized(maximized);
            if (stage.isFullScreen() != fullscreen)
                stage.setFullScreen(fullscreen);
        };
        try {
            return dialog.get();
        } finally {
            restore.run();
            Platform.runLater(restore);
        }
    }
}
