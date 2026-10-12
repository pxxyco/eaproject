package it.unical.ea_project_javafx;

import javafx.application.Application;
import javafx.stage.Stage;

import java.io.IOException;

import it.unical.ea_project_javafx.util.AppConfig;
import it.unical.ea_project_javafx.util.SceneNavigator;

/**
 * JavaFX App
 */
public class App extends Application {

    @Override
    public void start(Stage stage) throws IOException {

        stage.getIcons().add(AppConfig.APP_ICON);
        stage.setTitle(AppConfig.APP_TITLE);
        stage.setMinWidth(AppConfig.MIN_WINDOW_WIDTH);
        stage.setMinHeight(AppConfig.MIN_WINDOW_HEIGHT);

        SceneNavigator.getInstance().init(stage);

        SceneNavigator.getInstance().loadScene("/it/unical/ea_project_javafx/fxml/SplashScreen.fxml");

        stage.show();

    }

    public static void main(String[] args) {
        launch(args);
    }

}