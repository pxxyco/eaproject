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
import it.unical.ea_project_javafx.util.ApiService;
import com.google.gson.*;
import java.time.YearMonth;
import java.util.Map;

public class PaymentMethodsController {

    @FXML
    private ImageView bgImageView;
    @FXML
    private StackPane navbarContainer;
    @FXML
    private Label titleLabel;
    @FXML
    private Label subtitleLabel;
    @FXML
    private Label statusMessageLabel;

    @FXML
    private VBox travelerPaymentsSection;
    @FXML
    private TableView<CardRow> cardsTable;
    @FXML
    private TableColumn<CardRow, String> cardTypeColumn;
    @FXML
    private TableColumn<CardRow, String> cardNumberColumn;
    @FXML
    private TableColumn<CardRow, String> cardHolderColumn;
    @FXML
    private TableColumn<CardRow, String> expiryColumn;
    @FXML
    private TableColumn<CardRow, Void> actionColumn;

    @FXML
    private TextField cardHolderField;
    @FXML
    private TextField cardNumberField;
    @FXML
    private TextField expiryField;
    @FXML
    private PasswordField cvvField;

    @FXML
    private VBox organizerPayoutSection;
    @FXML
    private TextField payoutHolderField;
    @FXML
    private TextField ibanField;
    @FXML
    private TextField bicField;

    @FXML
    private Button addCardButton, payoutButton;
    private final UserSession userSession = UserSession.getInstance();
    private final ObservableList<CardRow> mockCards = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        BackgroundHelper.setupBackground(bgImageView);
        loadNavbar();
        setupViewByRole();
        setupCardsTable();
        if ("ORGANIZER".equals(userSession.getRole())) {
            ApiService.callDetailed(ApiService.BASE_URL + "/api/account/settings", null, ApiService.Method.GET, r -> {
                var d = JsonParser.parseString(r.body()).getAsJsonObject();
                payoutHolderField.setText(value(d, "payoutHolder"));
                ibanField.setText(value(d, "iban"));
                bicField.setText(value(d, "bic"));
            }, this::showError, null);
        } else
            loadCards();
    }

    private void loadNavbar() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/it/unical/ea_project_javafx/fxml/common/navbar.fxml"));
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
                    it.unical.ea_project_javafx.util.PopupFactory.showConfirmation(
                            it.unical.ea_project_javafx.util.PopupFactory.resolveRoot(cardsTable),
                            "Rimuovere questa carta?", "La carta di prova verrà rimossa dal tuo account.",
                            it.unical.ea_project_javafx.util.PopupFactory.Size.COMPACT,
                            () -> ApiService.callDetailed(ApiService.BASE_URL + "/api/account/cards/" + row.id, null,
                                    ApiService.Method.DELETE, r -> {
                                        mockCards.remove(row);
                                        showSuccess("Carta rimossa.");
                                    }, PaymentMethodsController.this::showError, deleteBtn::setDisable));
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty)
                    setGraphic(null);
                else
                    setGraphic(deleteBtn);
            }
        });

        cardsTable.setPlaceholder(new Label("Nessuna carta di prova salvata."));
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

        String digits = number.replaceAll("\\s", "");
        if (!digits.matches("[0-9]{13,19}") || !luhn(digits)) {
            showError("Inserisci un numero di carta di prova valido.");
            return;
        }
        if (!cvv.matches("[0-9]{3,4}") || !expiry.matches("(0[1-9]|1[0-2])/[0-9]{2}")) {
            showError("Controlla CVV e scadenza MM/AA.");
            return;
        }
        YearMonth date = YearMonth.of(2000 + Integer.parseInt(expiry.substring(3)),
                Integer.parseInt(expiry.substring(0, 2)));
        if (date.isBefore(YearMonth.now())) {
            showError("La carta è scaduta.");
            return;
        }
        String brand = digits.startsWith("4") ? "Visa"
                : digits.startsWith("34") || digits.startsWith("37") ? "Amex"
                        : digits.startsWith("5") || digits.startsWith("2") ? "Mastercard" : "Altro";
        var payload = Map.of("brand", brand, "last4", digits.substring(digits.length() - 4), "holder", holder.strip(),
                "expiryMonth", date.getMonthValue(), "expiryYear", date.getYear());
        ApiService.callDetailed(ApiService.BASE_URL + "/api/account/cards", ApiService.toJson(payload),
                ApiService.Method.POST, r -> {
                    cardHolderField.clear();
                    cardNumberField.clear();
                    expiryField.clear();
                    cvvField.clear();
                    showSuccess("Carta di prova salvata.");
                    loadCards();
                }, this::showError, addCardButton::setDisable);
    }

    private void loadCards() {
        ApiService.callDetailed(ApiService.BASE_URL + "/api/account/cards", null, ApiService.Method.GET, r -> {
            mockCards.clear();
            for (var e : JsonParser.parseString(r.body()).getAsJsonArray()) {
                var c = e.getAsJsonObject();
                CardRow row = new CardRow(value(c, "brand"), "•••• " + value(c, "last4"), value(c, "holder"), String
                        .format("%02d/%02d", c.get("expiryMonth").getAsInt(), c.get("expiryYear").getAsInt() % 100));
                row.id = c.get("id").getAsLong();
                mockCards.add(row);
            }
        }, this::showError, null);
    }

    @FXML
    private void handleSavePayout(ActionEvent event) {
        var payload = Map.of("holder", payoutHolderField.getText(), "iban", ibanField.getText(), "bic",
                bicField.getText());
        ApiService.callDetailed(ApiService.BASE_URL + "/api/account/payout", ApiService.toJson(payload),
                ApiService.Method.PUT, r -> showSuccess("Coordinate salvate nell'account dimostrativo."),
                this::showError, payoutButton::setDisable);
    }

    private static String value(JsonObject object, String key) {
        return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : "";
    }

    private boolean luhn(String digits) {
        int total = 0;
        boolean twice = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int n = digits.charAt(i) - '0';
            if (twice) {
                n *= 2;
                if (n > 9)
                    n -= 9;
            }
            total += n;
            twice = !twice;
        }
        return total % 10 == 0;
    }

    private boolean blank(String s) {
        return s == null || s.isBlank();
    }

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
        private long id;
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

        public SimpleStringProperty typeProperty() {
            return type;
        }

        public SimpleStringProperty numberProperty() {
            return number;
        }

        public SimpleStringProperty holderProperty() {
            return holder;
        }

        public SimpleStringProperty expiryProperty() {
            return expiry;
        }
    }
}