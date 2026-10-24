package it.unical.ea_project_javafx.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;
import java.io.IOException;
import java.util.function.Consumer;

public class SceneNavigator {

    private static SceneNavigator instance;
    private Stage mainStage;

    private SceneNavigator() {
    }

    public static synchronized SceneNavigator getInstance() {

        if (instance == null) {
            instance = new SceneNavigator();
        }

        return instance;

    }

    public void init(Stage stage) {
        this.mainStage = stage;
    }

    public void loadScene(String fxmlPath, Consumer<Object> controllerInitializer) {

        if (mainStage == null) {
            throw new IllegalStateException("SceneNavigator non inizializzato.");
        }
        
        try {

            URL fxmlUrl = getClass().getResource(fxmlPath);
            if (fxmlUrl == null) {
                throw new IllegalArgumentException("Risorsa FXML non trovata al percorso: " + fxmlPath);
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();

            if (controllerInitializer != null) {
                controllerInitializer.accept(loader.getController());
            }

            Scene scene;

            if (mainStage.getScene() != null) {
                scene = new Scene(root, mainStage.getScene().getWidth(), mainStage.getScene().getHeight());
            } else {
                scene = new Scene(root, AppConfig.WINDOW_WIDTH, AppConfig.WINDOW_HEIGHT);
            }

            scene.setUserData(fxmlPath);

            mainStage.setScene(scene);
            mainStage.show();

        } catch (IOException e) {
            System.err.println("Impossibile caricare la vista: " + fxmlPath);
            e.printStackTrace();
        }

    }

}