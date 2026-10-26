package it.unical.ea_project_javafx.controller.login;

import it.unical.ea_project_javafx.navigation.SceneNavigator;
import it.unical.ea_project_javafx.navigation.StepNavigator;
import it.unical.ea_project_javafx.ui.background.BackgroundHelper;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

public class AuthWizardController implements StepNavigator {

    @FXML
    private ImageView bgImageView;
    @FXML
    private VBox authCard;
    @FXML
    private VBox stepLogin, stepForgotPassword, stepRole, stepDetails, loadingOverlay, feedbackOverlay;

    @FXML
    private LoginStepController stepLoginController;
    @FXML
    private ForgotPasswordController stepForgotPasswordController;
    @FXML
    private RoleSelectionStepController stepRoleController;
    @FXML
    private RegistrationStepController stepDetailsController;
    @FXML
    private FeedbackOverlayController feedbackOverlayController;

    @FXML
    public void initialize() {

        BackgroundHelper.setupBackground(bgImageView);

        stepLoginController.setNavigator(this);
        stepForgotPasswordController.setNavigator(this);
        stepRoleController.setNavigator(this);
        stepDetailsController.setNavigator(this);
        feedbackOverlayController.setNavigator(this);

        goToLoginStep();
    }

    @Override
    public void goToLoginStep() {
        stepLoginController.clearError();
        showStep(stepLogin);
    }

    @Override
    public void goToForgotPasswordStep() {
        stepForgotPasswordController.clearMessage();
        showStep(stepForgotPassword);
    }

    @Override
    public void goToRoleStep() {
        showStep(stepRole);
    }

    @Override
    public void goToDetailsStep(String selectedRole) {
        stepDetailsController.clearError();
        stepDetailsController.setSelectedRole(selectedRole);
        showStep(stepDetails);
    }

    @Override
    public void goToHome() {
        SceneNavigator.getInstance().loadScene("/it/unical/ea_project_javafx/fxml/home/mainview.fxml", null);
    }

    @FXML
    void goToHomeFromButton(ActionEvent event) {
        goToHome();
    }

    @Override
    public void setLoading(boolean loading) {

        loadingOverlay.setVisible(loading);
        loadingOverlay.setManaged(loading);
        if (loading) {
            feedbackOverlay.setVisible(false);
            feedbackOverlay.setManaged(false);
        }

    }

    @Override
    public void showSuccessMessage(String message) {

        authCard.setVisible(false);
        authCard.setManaged(false);
        loadingOverlay.setVisible(false);
        loadingOverlay.setManaged(false);
        feedbackOverlayController.showSuccess(message);
        feedbackOverlay.setVisible(true);
        feedbackOverlay.setManaged(true);

    }

    @Override
    public void showErrorMessage(String message) {

        if (stepLogin.isVisible()) {
            stepLoginController.showError(message);
        } else if (stepForgotPassword.isVisible()) {
            stepForgotPasswordController.showError(message);
        } else if (stepDetails.isVisible()) {
            stepDetailsController.showError(message);
        }

    }

    private void showStep(VBox stepToShow) {

        authCard.setVisible(true);
        authCard.setManaged(true);
        stepLogin.setVisible(false);
        stepLogin.setManaged(false);
        stepForgotPassword.setVisible(false);
        stepForgotPassword.setManaged(false);
        stepRole.setVisible(false);
        stepRole.setManaged(false);
        stepDetails.setVisible(false);
        stepDetails.setManaged(false);
        loadingOverlay.setVisible(false);
        loadingOverlay.setManaged(false);
        feedbackOverlay.setVisible(false);
        feedbackOverlay.setManaged(false);
        stepToShow.setVisible(true);
        stepToShow.setManaged(true);

    }

}
