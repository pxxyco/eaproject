package it.unical.ea_project_javafx.controller.profile;

import it.unical.ea_project_javafx.security.UserSession;
import it.unical.ea_project_javafx.util.BackgroundHelper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;

public class PaymentMethodsController {

    @FXML private ImageView bgImageView;
    @FXML private StackPane navbarContainer;
    @FXML private Label titleLabel;
    @FXML private Label subtitleLabel;
    @FXML private Label statusMessageLabel;

    @FXML private VBox travelerPaymentsSection;
    @FXML private TableView<CardRow> cardsTable;
    @FXML private TableColumn<CardRow, String> cardTypeColumn;
    @FXML private TableColumn<CardRow, String> cardNumberColumn;
    @FXML private TableColumn<CardRow, String> cardHolderColumn;
    @FXML private TableColumn<CardRow, String> expiryColumn;
    @FXML private TableColumn<CardRow, Void> actionColumn;

    @FXML private TextField cardHolderField;
    @FXML private TextField cardNumberField;
    @FXML private TextField expiryField;
    @FXML private PasswordField cvvField;

    @FXML private VBox organizerPayoutSection;
    @FXML private TextField payoutHolderField;
    @FXML private TextField ibanField;
    @FXML private TextField bicField;

    private final UserSession userSession = UserSession.getInstance();
    private final ObservableList<CardRow> mockCards = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        BackgroundHelper.setupBackground(bgImageView);
        loadNavbar();
        setupViewByRole();
        setupCardsTable();
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

    private void setupViewByRole() {
        boolean isOrganizer = "ORGANIZER".equalsIgnoreCase(userSession.getRole());

        if (isOrganizer) {
            titleLabel.setText("Dati di Incasso & Payout");
            subtitleLabel.setText("Configura il conto bancario su cui ricevere gli accrediti per i tuoi viaggi.");
            travelerPaymentsSection.setVisible(false);
            travelerPaymentsSection.setManaged(false);
            organizerPayoutSection.setVisible(true);
            organizerPayoutSection.setManaged(true);
        } else {
            titleLabel.setText("Metodi di Pagamento Salvati");
            subtitleLabel.setText("Aggiungi o rimuovi le carte di credito/debito per velocizzare i tuoi acquisti.");
            travelerPaymentsSection.setVisible(true);
            travelerPaymentsSection.setManaged(true);
            organizerPayoutSection.setVisible(false);
            organizerPayoutSection.setManaged(false);
        }
    }

    private void setupCardsTable() {
        cardTypeColumn.setCellValueFactory(cell -> cell.getValue().typeProperty());
        cardNumberColumn.setCellValueFactory(cell -> cell.getValue().numberProperty());
        cardHolderColumn.setCellValueFactory(cell -> cell.getValue().holderProperty());
        expiryColumn.setCellValueFactory(cell -> cell.getValue().expiryProperty());

        actionColumn.setCellFactory(col -> new TableCell<>() {
            private final Button deleteBtn = new Button("Rimuovi");
            {
                deleteBtn.getStyleClass().add("danger-action-button");
                deleteBtn.setOnAction(e -> {
                    CardRow row = getTableView().getItems().get(getIndex());
                    mockCards.remove(row);
                    showSuccess("Carta rimossa con successo.");
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null);
                else setGraphic(deleteBtn);
            }
        });

        mockCards.add(new CardRow("Visa", "•••• •••• •••• 4242", userSession.getUsername(), "12/26"));
        cardsTable.setItems(mockCards);
    }

    @FXML
    private void handleAddCard(ActionEvent event) {
        clearMessage();
        String holder = cardHolderField.getText();
        String number = cardNumberField.getText();
        String expiry = expiryField.getText();
        String cvv = cvvField.getText();

        if (blank(holder) || blank(number) || blank(expiry) || blank(cvv)) {
            showError("Compila tutti i campi della carta.");
            return;
        }

        String masked = "•••• •••• •••• " + (number.length() >= 4 ? number.substring(number.length() - 4) : "1111");
        mockCards.add(new CardRow("Mastercard", masked, holder, expiry));

        cardHolderField.clear();
        cardNumberField.clear();
        expiryField.clear();
        cvvField.clear();

        showSuccess("Nuova carta salvata con successo (Mock).");
    }

    @FXML
    private void handleSavePayout(ActionEvent event) {
        clearMessage();
        String iban = ibanField.getText();
        String holder = payoutHolderField.getText();

        if (blank(iban) || blank(holder)) {
            showError("Inserisci almeno IBAN e Intestatario del conto.");
            return;
        }

        showSuccess("Dati di accredito IBAN salvati correttamente!");
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

    public static class CardRow {
        private final SimpleStringProperty type;
        private final SimpleStringProperty number;
        private final SimpleStringProperty holder;
        private final SimpleStringProperty expiry;

        public CardRow(String type, String number, String holder, String expiry) {
            this.type = new SimpleStringProperty(type);
            this.number = new SimpleStringProperty(number);
            this.holder = new SimpleStringProperty(holder);
            this.expiry = new SimpleStringProperty(expiry);
        }

        public SimpleStringProperty typeProperty() { return type; }
        public SimpleStringProperty numberProperty() { return number; }
        public SimpleStringProperty holderProperty() { return holder; }
        public SimpleStringProperty expiryProperty() { return expiry; }
    }
}