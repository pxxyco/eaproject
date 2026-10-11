package it.unical.ea_project_javafx.controller;

import it.unical.ea_project_javafx.util.ApiService;
import it.unical.ea_project_javafx.util.AppErrorCode;
import it.unical.ea_project_javafx.util.SceneNavigator;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
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
import javafx.util.Duration;

import java.util.Objects;

public class SplashScreenController {

    private static final String SPLASH_FXML = "/it/unical/ea_project_javafx/fxml/SplashScreen.fxml";
    private static final String HOME_FXML = "/it/unical/ea_project_javafx/fxml/home/mainview.fxml";

    private static final int MIN_WAIT_SECONDS = 5;
    private static final int MAX_WAIT_SECONDS = 120;

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
    private static boolean rateLimitScreenActive = false;
    private static int pendingRateLimitSeconds = 0;

    private Timeline rateLimitCountdown;
    public static void showRateLimited(int retryAfterSeconds) {
        if (rateLimitScreenActive) {
            return;
        }
        rateLimitScreenActive = true;
        pendingRateLimitSeconds = Math.clamp(retryAfterSeconds, MIN_WAIT_SECONDS, MAX_WAIT_SECONDS);
        Platform.runLater(() -> SceneNavigator.getInstance().loadScene(SPLASH_FXML));
    }

    @FXML
    public void initialize() {
        ApiService.setOnNetworkFailureGlobal(this::restartAppManually);
        ApiService.setOnRateLimitedGlobal(SplashScreenController::showRateLimited);

        retryButton.setOnAction(e -> checkBackendConnection());
        exitButton.setOnAction(e -> Platform.exit());

        if (rateLimitScreenActive) {
            showRateLimitState(pendingRateLimitSeconds);
            return;
        }

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

        progressIndicator.setVisible(true);
        progressIndicator.setManaged(true);
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);

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
            hideLoadingComponents();
            showErrorImage();

            String fullErrorMessage = String.format("Codice Errore: %s. %s",
                    errorCode.getCode(),
                    errorCode.getDefaultDescription());
            errorMessageLabel.setText(fullErrorMessage);
            errorMessageLabel.setVisible(true);
            errorMessageLabel.setManaged(true);

            errorTitleLabel.setVisible(true);
            errorTitleLabel.setManaged(true);

            showButtons();
        });

        startAutoRecoveryMonitor();
    }
    private void showRateLimitState(int seconds) {
        hideLoadingComponents();
        showErrorImage();

        errorTitleLabel.setVisible(true);
        errorTitleLabel.setManaged(true);
        errorMessageLabel.setVisible(true);
        errorMessageLabel.setManaged(true);

        updateRateLimitTexts(seconds);
        showButtons();
        retryButton.setDisable(true);

        final int[] remaining = {seconds};
        rateLimitCountdown = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            remaining[0]--;
            if (remaining[0] <= 0) {
                rateLimitCountdown.stop();
                rateLimitScreenActive = false;
                loadMainApp();
            } else {
                updateRateLimitTexts(remaining[0]);
            }
        }));
        rateLimitCountdown.setCycleCount(Timeline.INDEFINITE);
        rateLimitCountdown.play();
    }

    private void updateRateLimitTexts(int secondsLeft) {
        AppErrorCode code = AppErrorCode.ERR_RATE_LIMITED;
        errorMessageLabel.setText(String.format("Codice Errore: %s. %s Riprova tra %d s.",
                code.getCode(), code.getDefaultDescription(), secondsLeft));
        retryButton.setText("Riprova tra " + secondsLeft + " s");
    }

    private void hideLoadingComponents() {
        progressIndicator.setVisible(false);
        progressIndicator.setManaged(false);
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);
    }

    private void showErrorImage() {
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
    }

    private void showButtons() {
        boolean isFullScreen = isWindowFullScreen();
        exitButton.setVisible(isFullScreen);
        exitButton.setManaged(isFullScreen);

        buttonBox.setVisible(true);
        buttonBox.setManaged(true);
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
        loadView(SPLASH_FXML);
    }

    private void loadMainApp() {
        loadView(HOME_FXML);
    }

    private void loadView(String fxmlPath) {
        Platform.runLater(() -> SceneNavigator.getInstance().loadScene(fxmlPath));
    }
}