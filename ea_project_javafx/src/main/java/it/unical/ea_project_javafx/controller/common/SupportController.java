package it.unical.ea_project_javafx.controller.common;

import it.unical.ea_project_javafx.security.UserSession;
import it.unical.ea_project_javafx.util.BackgroundHelper;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;

public class SupportController {

    @FXML private ImageView bgImageView;
    @FXML private StackPane navbarContainer;
    @FXML private Label statusMessageLabel;

    @FXML private Label supportTitleLabel;
    @FXML private Label supportSubtitleLabel;

    @FXML private VBox travelerFaqSection;
    @FXML private VBox organizerFaqSection;

    @FXML private ComboBox<String> categoryCombo;
    @FXML private TextField subjectField;
    @FXML private TextArea messageArea;

    private final UserSession userSession = UserSession.getInstance();

    @FXML
    public void initialize() {
        BackgroundHelper.setupBackground(bgImageView);
        loadNavbar();
        setupSupportByRole();
    }

    private void loadNavbar() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/it/unical/ea_project_javafx/fxml/common/navbar.fxml")
            );
            Parent navbar = loader.load();
            navbarContainer.getChildren().setAll(navbar);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setupSupportByRole() {
        boolean isOrganizer = "ORGANIZER".equalsIgnoreCase(userSession.getRole());

        if (isOrganizer) {
            supportTitleLabel.setText("Centro Assistenza Partner & Organizzatori");
            supportSubtitleLabel.setText("Supporto dedicato alla gestione delle tue offerte, fatturazione e iscritti.");

            travelerFaqSection.setVisible(false);
            travelerFaqSection.setManaged(false);
            organizerFaqSection.setVisible(true);
            organizerFaqSection.setManaged(true);

            categoryCombo.getItems().setAll(
                    "Problema Pubblicazione Evento/Viaggio",
                    "Modifica o Cancellazione Offerta",
                    "Incassi e Payout (Stripe/IBAN)",
                    "Gestione Partecipanti & Presenze",
                    "Altra Richiesta Partner"
            );
        } else {
            supportTitleLabel.setText("Centro Assistenza Viaggiatori");
            supportSubtitleLabel.setText("Risposte alle tue domande su prenotazioni, pagamenti e itinerari.");

            organizerFaqSection.setVisible(false);
            organizerFaqSection.setManaged(false);
            travelerFaqSection.setVisible(true);
            travelerFaqSection.setManaged(true);

            categoryCombo.getItems().setAll(
                    "Info su una Prenotazione",
                    "Richiesta di Cancellazione/Rimborso",
                    "Problema con il Pagamento",
                    "Segnalazione Disservizio",
                    "Altra Richiesta"
            );
        }
        categoryCombo.getSelectionModel().selectFirst();
    }

    @FXML
    private void handleSendMessage(ActionEvent event) {
        clearMessage();
        String category = categoryCombo.getValue();
        String subject = subjectField.getText();
        String message = messageArea.getText();

        if (blank(subject) || blank(message)) {
            showError("Inserisci sia l'oggetto che il messaggio.");
            return;
        }

        subjectField.clear();
        messageArea.clear();
        showSuccess("La tua richiesta di supporto (" + category + ") è stata inviata con successo!");
    }

    private boolean blank(String s) { return s == null || s.isBlank(); }

    private void showError(String msg) {
        statusMessageLabel.setText(msg);
        statusMessageLabel.getStyleClass().setAll("status-message", "status-error");
        statusMessageLabel.setVisible(true);
        statusMessageLabel.setManaged(true);
    }

    private void showSuccess(String msg) {
        statusMessageLabel.setText(msg);
        statusMessageLabel.getStyleClass().setAll("status-message", "status-success");
        statusMessageLabel.setVisible(true);
        statusMessageLabel.setManaged(true);
    }

    private void clearMessage() {
        statusMessageLabel.setText("");
        statusMessageLabel.setVisible(false);
        statusMessageLabel.setManaged(false);
    }
}