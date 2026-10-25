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
        stage.addEventHandler(javafx.stage.WindowEvent.WINDOW_HIDDEN, event -> {
            if (stage.getScene() == null)
                return;
            Object controller = stage.getScene().getProperties().remove("pageController");
            if (controller instanceof AutoCloseable closeable) {
                try {
                    closeable.close();
                } catch (Exception ignored) {
                }
            }
        });
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
            SmoothScrolling.install(scene);
            UiEffects.install(scene);

            if (mainStage.getScene() != null) {
                Object previous = mainStage.getScene().getProperties().get("pageController");
                if (previous instanceof AutoCloseable closeable)
                    try {
                        closeable.close();
                    } catch (Exception ignored) {
                    }
            }
            scene.getProperties().put("pageController", loader.getController());
            mainStage.setScene(scene);
            mainStage.show();

        } catch (IOException e) {
            System.err.println("Impossibile caricare la vista: " + fxmlPath);
            e.printStackTrace();
        }

    }

}