package it.unical.ea_project_javafx.controller.common;

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

import java.io.InputStream;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SplashScreenController {

    private static final Logger LOGGER = Logger.getLogger(SplashScreenController.class.getName());

    private static final String SPLASH_FXML = "/it/unical/ea_project_javafx/fxml/common/splash-screen.fxml";
    private static final String HOME_FXML = "/it/unical/ea_project_javafx/fxml/home/mainview.fxml";

    private static final int RECOVERY_CHECK_INTERVAL_SECONDS = 3;

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

    private ScheduledExecutorService recoveryScheduler;

    @FXML
    public void initialize() {

        ApiService.setOnNetworkFailureGlobal(this::restartAppManually);
        // Gli HTTP 429 (ERR-104) sono errori locali delle singole richieste.
        // Non vengono mai usati per riportare l'intera applicazione allo splash.
        retryButton.setOnAction(e -> checkBackendConnection());
        exitButton.setOnAction(e -> shutdownAndExit());

        checkBackendConnection();

    }

    private void checkBackendConnection() {

        stopAutoRecoveryMonitor();
        setLoadingState();

        Task<Boolean> healthCheck = createHealthCheckTask();
        
        Thread thread = new Thread(healthCheck);
        thread.setDaemon(true);
        thread.start();

    }

    private Task<Boolean> createHealthCheckTask() {

        Task<Boolean> healthCheck = new Task<>() {
            @Override
            protected Boolean call() {
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

        setVisibleAndManaged(true, progressIndicator, statusLabel);
        setVisibleAndManaged(false, errorTitleLabel, errorMessageLabel, errorImageView, buttonBox);

    }

    private void showErrorState(AppErrorCode errorCode) {

        Platform.runLater(() -> {
            hideLoadingComponents();
            showErrorImage();

            String fullErrorMessage = String.format("Codice Errore: %s. %s",
                    errorCode.getCode(),
                    errorCode.getDefaultDescription());
            errorMessageLabel.setText(fullErrorMessage);

            setVisibleAndManaged(true, errorMessageLabel, errorTitleLabel);
            retryButton.setDisable(false);
            retryButton.setText("Riprova");
            showButtons();
        });

        startAutoRecoveryMonitor();

    }

    private void hideLoadingComponents() {
        setVisibleAndManaged(false, progressIndicator, statusLabel);
    }

    private void showErrorImage() {

        String imagePath = "/it/unical/ea_project_javafx/images/sad-dino.png";
        try (InputStream is = getClass().getResourceAsStream(imagePath)) {

            if (is != null) {
                Image dinoImage = new Image(is);
                errorImageView.setImage(dinoImage);
                setVisibleAndManaged(true, errorImageView);
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Impossibile caricare l'immagine di errore: " + imagePath, e);
        }

    }

    private void showButtons() {

        boolean isFullScreen = isWindowFullScreen();
        setVisibleAndManaged(isFullScreen, exitButton);
        setVisibleAndManaged(true, buttonBox);

    }

    private boolean isWindowFullScreen() {

        return splashContent != null 
                && splashContent.getScene() != null 
                && splashContent.getScene().getWindow() instanceof Stage stage 
                && stage.isFullScreen();

    }

    private synchronized void startAutoRecoveryMonitor() {

        if (recoveryScheduler != null && !recoveryScheduler.isShutdown()) {
            return;
        }

        recoveryScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        });

        recoveryScheduler.scheduleAtFixedRate(() -> {
            if (ApiService.isBackendReachable()) {
                stopAutoRecoveryMonitor();
                Platform.runLater(this::restartAppManually);
            }
        }, RECOVERY_CHECK_INTERVAL_SECONDS, RECOVERY_CHECK_INTERVAL_SECONDS, TimeUnit.SECONDS);

    }

    private synchronized void stopAutoRecoveryMonitor() {

        if (recoveryScheduler != null && !recoveryScheduler.isShutdown()) {
            recoveryScheduler.shutdownNow();
            recoveryScheduler = null;
        }

    }

    private void shutdownAndExit() {

        stopAutoRecoveryMonitor();
        Platform.exit();

    }

    private void restartAppManually() {

        stopAutoRecoveryMonitor();
        loadView(SPLASH_FXML);

    }

    private void loadMainApp() {

        stopAutoRecoveryMonitor();
        loadView(HOME_FXML);

    }

    private void loadView(String fxmlPath) {
        Platform.runLater(() -> SceneNavigator.getInstance().loadScene(fxmlPath, null));
    }

    // Utility per ridurre il codice ripetitivo per i nodi JavaFX
    private void setVisibleAndManaged(boolean value, javafx.scene.Node... nodes) {

        for (javafx.scene.Node node : nodes) {
            if (node != null) {
                node.setVisible(value);
                node.setManaged(value);
            }
        }

    }
}