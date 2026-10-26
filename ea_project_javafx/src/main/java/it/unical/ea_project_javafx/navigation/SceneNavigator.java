package it.unical.ea_project_javafx.navigation;

import it.unical.ea_project_javafx.config.AppConfig;
import it.unical.ea_project_javafx.ui.animation.PageTransitions;
import it.unical.ea_project_javafx.ui.animation.UiEffects;
import it.unical.ea_project_javafx.ui.scroll.SmoothScrolling;
import java.io.IOException;
import java.net.URL;
import javafx.animation.PauseTransition;
import javafx.util.Duration;
import java.util.function.Consumer;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class SceneNavigator {

    private static SceneNavigator instance;
    private Stage mainStage;
    private final PauseTransition navigationDelay = new PauseTransition(Duration.millis(40));

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
        stage.addEventHandler(WindowEvent.WINDOW_HIDDEN, event -> {
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
        navigationDelay.stop();
        if (mainStage != null && mainStage.getScene() != null && mainStage.isShowing()) {
            navigationDelay.setOnFinished(e -> performLoad(fxmlPath, controllerInitializer));
            navigationDelay.playFromStart();
        } else
            performLoad(fxmlPath, controllerInitializer);
    }

    private void performLoad(String fxmlPath, Consumer<Object> controllerInitializer) {

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

            Scene scene = mainStage.getScene();
            if (scene == null)
                scene = new Scene(root, AppConfig.WINDOW_WIDTH, AppConfig.WINDOW_HEIGHT);
            scene.setUserData(fxmlPath);

            if (mainStage.getScene() != null) {
                Object previous = mainStage.getScene().getProperties().get("pageController");
                if (previous instanceof AutoCloseable closeable)
                    try {
                        closeable.close();
                    } catch (Exception ignored) {
                    }
            }
            scene.getProperties().put("pageController", loader.getController());
            if (scene.getRoot() != root)
                scene.setRoot(root);
            SmoothScrolling.install(scene);
            UiEffects.install(scene);
            UiEffects.prepare(root);
            PageTransitions.enter(root);
            if (mainStage.getScene() != scene)
                mainStage.setScene(scene);
            mainStage.show();

        } catch (IOException e) {
            System.err.println("Impossibile caricare la vista: " + fxmlPath);
            e.printStackTrace();
        }

    }

}
