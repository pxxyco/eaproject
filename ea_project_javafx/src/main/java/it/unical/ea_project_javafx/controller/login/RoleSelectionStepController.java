package it.unical.ea_project_javafx.controller.login;

import it.unical.ea_project_javafx.navigation.StepNavigator;
import javafx.fxml.FXML;
import javafx.scene.input.MouseEvent;
import lombok.Setter;

// Selezione ruolo
@Setter
public class RoleSelectionStepController {

    private StepNavigator navigator;

    @FXML
    void goToLogin() {
        navigator.goToLoginStep();
    }

    @FXML
    void selectPersonalRole(MouseEvent event) {
        navigator.goToDetailsStep("TRAVELER");
    }

    @FXML
    void selectBusinessRole(MouseEvent event) {
        navigator.goToDetailsStep("ORGANIZER");
    }

}
