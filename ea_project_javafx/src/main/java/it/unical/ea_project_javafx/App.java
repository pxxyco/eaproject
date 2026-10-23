package it.unical.ea_project_javafx;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

import java.io.IOException;

import it.unical.ea_project_javafx.util.AppConfig;
import it.unical.ea_project_javafx.util.SceneNavigator;
import it.unical.ea_project_javafx.util.SingleInstanceLock;

/**
 * JavaFX App
 */
public class App extends Application {

    @Override
    public void start(Stage stage) throws IOException {

        if (!SingleInstanceLock.acquireLock()) {
            Platform.exit();
            System.exit(0);
            return;
        }

        SingleInstanceLock.startListener(stage);

        stage.getIcons().add(AppConfig.APP_ICON);
        stage.setTitle(AppConfig.APP_TITLE);
        stage.setMinWidth(AppConfig.MIN_WINDOW_WIDTH);
        stage.setMinHeight(AppConfig.MIN_WINDOW_HEIGHT);

        SceneNavigator.getInstance().init(stage);
        SceneNavigator.getInstance().loadScene("/it/unical/ea_project_javafx/fxml/common/splash-screen.fxml", null);

        stage.show();

    }

    @Override
    public void stop() throws Exception {
        SingleInstanceLock.release();
        super.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }

}