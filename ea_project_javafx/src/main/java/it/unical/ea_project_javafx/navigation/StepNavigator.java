package it.unical.ea_project_javafx.navigation;

public interface StepNavigator {

    void goToLoginStep();

    void goToForgotPasswordStep();

    void goToRoleStep();

    void goToDetailsStep(String selectedRole);

    void goToHome();

    void setLoading(boolean loading);

    void showSuccessMessage(String message);

    void showErrorMessage(String message);

}