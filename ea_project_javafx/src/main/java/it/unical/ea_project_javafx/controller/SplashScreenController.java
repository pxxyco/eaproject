package it.unical.ea_project_javafx.controller;

import it.unical.ea_project_javafx.util.ApiService;
import it.unical.ea_project_javafx.util.AppErrorCode;
import it.unical.ea_project_javafx.util.SceneNavigator;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.Objects;

public class SplashScreenController {

    @FXML private VBox splashContent;
    @FXML private Label iconLabel;
    @FXML private ImageView errorImageView;
    @FXML private ProgressIndicator progressIndicator;
    @FXML private Label errorTitleLabel;
    @FXML private Label statusLabel;
    @FXML private Label errorMessageLabel;
    @FXML private HBox buttonBox;
    @FXML private Button retryButton;
    @FXML private Button exitButton;

    private static boolean isMonitoringConnection = false;

    @FXML
    public void initialize() {
        ApiService.setOnNetworkFailureGlobal(this::restartAppManually);

        retryButton.setOnAction(e -> checkBackendConnection());
        exitButton.setOnAction(e -> Platform.exit());

        checkBackendConnection();
    }

    private void checkBackendConnection() {
        setLoadingState();
        Task<Boolean> healthCheck = createHealthCheckTask();
        new Thread(healthCheck).start();
    }

    private Task<Boolean> createHealthCheckTask() {
        Task<Boolean> healthCheck = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                Thread.sleep(1000);
                return ApiService.isBackendReachable();
            }
        };

        healthCheck.setOnSucceeded(e -> {
            if (Boolean.TRUE.equals(healthCheck.getValue())) {
                loadMainApp();
            } else {
                showErrorState(AppErrorCode.ERR_NETWORK_UNREACHABLE);
            }
        });

        healthCheck.setOnFailed(e -> showErrorState(AppErrorCode.ERR_UNKNOWN));
        return healthCheck;
    }

    private void setLoadingState() {
        // Mostra componenti di caricamento
        progressIndicator.setVisible(true);
        progressIndicator.setManaged(true);
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);

        // Nasconde componenti di errore
        errorTitleLabel.setVisible(false);
        errorTitleLabel.setManaged(false);
        errorMessageLabel.setVisible(false);
        errorMessageLabel.setManaged(false);
        errorImageView.setVisible(false);
        errorImageView.setManaged(false);
        buttonBox.setVisible(false);
        buttonBox.setManaged(false);
    }

    private void showErrorState(AppErrorCode errorCode) {
        Platform.runLater(() -> {
            // Nasconde spinner e messaggio di caricamento
            progressIndicator.setVisible(false);
            progressIndicator.setManaged(false);
            statusLabel.setVisible(false);
            statusLabel.setManaged(false);

            // Carica l'immagine d'errore
            try {
                Image dinoImage = new Image(Objects.requireNonNull(
                    getClass().getResourceAsStream("/it/unical/ea_project_javafx/images/sad-dino.png")
                ));
                errorImageView.setImage(dinoImage);
                errorImageView.setVisible(true);
                errorImageView.setManaged(true);
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Imposta il messaggio dinamico combinato
            String fullErrorMessage = String.format("Codice Errore: %s. %s", 
                    errorCode.getCode(), 
                    errorCode.getDefaultDescription());
            errorMessageLabel.setText(fullErrorMessage);
            errorMessageLabel.setVisible(true);
            errorMessageLabel.setManaged(true);

            errorTitleLabel.setVisible(true);
            errorTitleLabel.setManaged(true);

            // Gestione visibilità Esci: visibile SOLO se in Fullscreen
            boolean isFullScreen = isWindowFullScreen();
            exitButton.setVisible(isFullScreen);
            exitButton.setManaged(isFullScreen);

            buttonBox.setVisible(true);
            buttonBox.setManaged(true);
        });

        startAutoRecoveryMonitor();
    }

    private boolean isWindowFullScreen() {
        if (splashContent != null && splashContent.getScene() != null 
                && splashContent.getScene().getWindow() instanceof Stage stage) {
            return stage.isFullScreen();
        }
        return false;
    }

    private void startAutoRecoveryMonitor() {
        if (isMonitoringConnection) return;
        isMonitoringConnection = true;

        Thread recoveryThread = new Thread(() -> {
            boolean recovered = false;
            while (!recovered) {
                try {
                    Thread.sleep(3000);
                    if (ApiService.isBackendReachable()) {
                        recovered = true;
                        isMonitoringConnection = false;
                        Platform.runLater(this::restartAppManually);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });

        recoveryThread.setDaemon(true);
        recoveryThread.start();
    }

    private void restartAppManually() {
        loadView("/it/unical/ea_project_javafx/fxml/splash-screen.fxml");
    }

    private void loadMainApp() {
        loadView("/it/unical/ea_project_javafx/fxml/home/mainview.fxml");
    }

    private void loadView(String fxmlPath) {
        Platform.runLater(() -> SceneNavigator.getInstance().loadScene(fxmlPath, null));
    }
}