package it.unical.ea_project_javafx.ui.payment;

import com.google.gson.JsonParser;
import it.unical.ea_project_javafx.dto.booking.BookingDTO;
import it.unical.ea_project_javafx.service.api.ApiService;
import it.unical.ea_project_javafx.ui.BookingPresentation;
import it.unical.ea_project_javafx.ui.UiText;
import it.unical.ea_project_javafx.ui.ViewSupport;
import it.unical.ea_project_javafx.ui.dialog.PopupFactory;
import java.time.LocalDateTime;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public final class MockCheckoutDialog {
    private MockCheckoutDialog() {
    }

    public static boolean eligible(BookingDTO b) {
        return b != null && b.getPrice() != null && b.getPrice().signum() > 0 && b.getBookingId() != null
                && !BookingPresentation.isCancelled(b)
                && !BookingPresentation.isHistorical(b, LocalDateTime.now())
                && !BookingPresentation.bookingClosed(b, LocalDateTime.now())
                && !"COMPLETED".equals(b.getPaymentStatus()) && !"CONFIRMED".equals(b.getPaymentStatus());
    }

    public static void show(Node anchor, BookingDTO booking, Runnable updated) {
        if (!eligible(booking))
            return;
        VBox panel = new VBox(18);
        panel.setMaxWidth(480);
        panel.setMaxHeight(Region.USE_PREF_SIZE);
        Label title = new Label("Completa il pagamento");
        title.getStyleClass().add("app-modal-title");
        Label experience = new Label(booking.getItemTitle());
        experience.setWrapText(true);
        experience.getStyleClass().add("section-header");
        Label detail = new Label(
                BookingPresentation.formatEventPeriod(booking) + " · " + booking.getSeats() + " partecipanti");
        detail.setWrapText(true);
        detail.getStyleClass().add("section-subtitle");
        Label amount = new Label(UiText.money(booking.getPrice()));
        amount.getStyleClass().add("checkout-total");
        Label notice = new Label("Pagamento di prova: non viene addebitato denaro e non servono carte o dati bancari.");
        notice.setWrapText(true);
        CheckBox consent = new CheckBox("Confermo il pagamento dimostrativo");
        Label status = new Label();
        status.setWrapText(true);
        status.setVisible(false);
        status.setManaged(false);
        Button pay = new Button("Paga in modalità demo"), cancel = new Button("Annulla");
        pay.getStyleClass().add("btn-primary");
        cancel.getStyleClass().add("btn-secondary");
        pay.setDisable(true);
        consent.selectedProperty().addListener((o, a, b) -> pay.setDisable(!b));
        HBox actions = new HBox(10, cancel, pay);
        actions.setAlignment(Pos.CENTER_RIGHT);
        panel.getChildren().addAll(title, experience, detail, amount, notice, consent, status, actions);
        var popup = PopupFactory.show(PopupFactory.resolveRoot(anchor), panel, PopupFactory.Size.COMPACT);
        cancel.setOnAction(e -> popup.close());
        pay.setOnAction(e -> ApiService.callDetailed(
                ApiService.BASE_URL + "/api/payments/mock/bookings/" + booking.getBookingId(), "{}",
                ApiService.Method.POST, r -> {
                    var receipt = JsonParser.parseString(r.body()).getAsJsonObject();
                    booking.setPaymentStatus("CONFIRMED");
                    booking.setBookingStatus("CONFIRMED");
                    popup.close();
                    updated.run();
                    PopupFactory.showMessage(PopupFactory.resolveRoot(anchor), "Pagamento registrato",
                            "Pagamento dimostrativo di " + UiText.money(receipt.get("amount").getAsBigDecimal())
                                    + " completato per “" + booking.getItemTitle() + "”.",
                            PopupFactory.Size.COMPACT);
                }, message -> ViewSupport.error(status, message), busy -> {
                    pay.setDisable(busy || !consent.isSelected());
                    consent.setDisable(busy);
                    cancel.setDisable(busy);
                }));
    }
}
