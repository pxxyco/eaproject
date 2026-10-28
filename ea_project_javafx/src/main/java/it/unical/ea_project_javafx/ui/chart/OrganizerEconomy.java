package it.unical.ea_project_javafx.ui.chart;

import com.google.gson.JsonParser;
import it.unical.ea_project_javafx.service.api.ApiService;
import it.unical.ea_project_javafx.ui.UiText;
import it.unical.ea_project_javafx.ui.ViewSupport;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.List;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.StackedBarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

/**
 * One bounded monthly aggregate, shared by the organizer dashboard and finance
 * page.
 */
public final class OrganizerEconomy extends VBox implements AutoCloseable {
    private final Label status = new Label(), paid = new Label("—"), count = new Label("—"), average = new Label("—"),
            reserved = new Label("—");
    private final StackedBarChart<String, Number> chart;
    private long generation;
    private boolean closed;

    public OrganizerEconomy() {
        super(20);
        getStyleClass().add("account-panel");
        Label title = new Label("Come stanno andando le tue esperienze");
        title.getStyleClass().add("section-header");
        Label subtitle = new Label(
                "Incassi registrati negli ultimi 12 mesi, distinti fra viaggi e attività. I mesi senza pagamenti valgono zero.");
        subtitle.setWrapText(true);
        subtitle.getStyleClass().add("section-subtitle");
        FlowPane totals = new FlowPane(24, 16, metric("Incassato nel periodo", paid), metric("Pagamenti", count),
                metric("Importo medio", average), metric("Prenotazioni valide · totale", reserved));
        CategoryAxis months = new CategoryAxis();
        NumberAxis euros = new NumberAxis();
        euros.setLabel("Euro");
        euros.setForceZeroInRange(true);
        chart = new StackedBarChart<>(months, euros);
        chart.setAnimated(false);
        chart.setPrefHeight(300);
        chart.setMinHeight(230);
        chart.setCategoryGap(12);
        chart.setLegendVisible(true);
        chart.getStyleClass().add("economy-chart");
        status.setWrapText(true);
        status.setVisible(false);
        status.setManaged(false);
        getChildren().addAll(title, subtitle, totals, chart, status);
    }

    private VBox metric(String name, Label value) {
        Label caption = new Label(name);
        caption.getStyleClass().add("section-subtitle");
        value.getStyleClass().add("economy-value");
        return new VBox(7, caption, value);
    }

    public void refresh() {
        long version = ++generation;
        ApiService.callDetailed(ApiService.BASE_URL + "/api/payments/organizer/statistics", null, ApiService.Method.GET,
                r -> {
                    if (closed || version != generation)
                        return;
                    var data = JsonParser.parseString(r.body()).getAsJsonObject();
                    paid.setText(UiText.money(data.get("periodPaid").getAsBigDecimal()));
                    count.setText(data.get("paymentCount").getAsString());
                    average.setText(UiText.money(data.get("averagePayment").getAsBigDecimal()));
                    reserved.setText(UiText.money(data.get("reservedValue").getAsBigDecimal()));
                    XYChart.Series<String, Number> trips = new XYChart.Series<>(), activities = new XYChart.Series<>();
                    trips.setName("Viaggi");
                    activities.setName("Attività");
                    var formatter = DateTimeFormatter.ofPattern("MMM yy", Locale.ITALIAN);
                    for (var element : data.getAsJsonArray("months")) {
                        var month = element.getAsJsonObject();
                        String label = YearMonth.parse(month.get("month").getAsString()).format(formatter);
                        trips.getData().add(new XYChart.Data<>(label, month.get("trips").getAsBigDecimal()));
                        activities.getData().add(new XYChart.Data<>(label, month.get("activities").getAsBigDecimal()));
                    }
                    chart.getData().setAll(List.of(trips, activities));
                    ViewSupport.clear(status);
                }, message -> {
                    if (!closed && version == generation)
                        ViewSupport.error(status, message);
                }, null);
    }

    @Override
    public void close() {
        closed = true;
        generation++;
    }
}
