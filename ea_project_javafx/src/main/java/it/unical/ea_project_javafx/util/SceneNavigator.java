package it.unical.ea_project_javafx.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;

public class SceneNavigator {

    private static SceneNavigator instance;
    private Stage mainStage;

    private final Deque<String> history = new ArrayDeque<>();

    private SceneNavigator() {}


    public static synchronized SceneNavigator getInstance() {
        if (instance == null) {
            instance = new SceneNavigator();
        }
        return instance;
    }


    public void init(Stage stage) {
        this.mainStage = stage;
    }
    public void loadScene(String fxmlPath) {
        loadScene(fxmlPath, null);
    }

    public void loadScene(String fxmlPath, Consumer<Object> controllerInitializer) {
        if (mainStage == null) {
            throw new IllegalStateException("SceneNavigator non inizializzato.");
        }

        try {
            if (mainStage.getScene() != null && mainStage.getScene().getUserData() != null) {
                String currentView = (String) mainStage.getScene().getUserData();
                history.push(currentView);
            }

            java.net.URL fxmlUrl = getClass().getResource(fxmlPath);
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

    public void goBack() {
        if (hasHistory()) {
            String previousView = history.pop();
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(previousView));
                Parent root = loader.load();

                Scene scene = new Scene(root);
                scene.setUserData(previousView);

                mainStage.setScene(scene);
                mainStage.show();
            } catch (IOException e) {
                System.err.println("Impossibile tornare indietro a: " + previousView);
                e.printStackTrace();
            }
        }
    }


    public void goToHome(String homeFxmlPath) {
        history.clear();
        loadScene(homeFxmlPath);
    }

    public boolean hasHistory() {
        return !history.isEmpty();
    }
}