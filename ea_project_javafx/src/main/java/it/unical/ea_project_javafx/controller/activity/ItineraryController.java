package it.unical.ea_project_javafx.controller.activity;

import com.gluonhq.maps.MapPoint;
import com.gluonhq.maps.MapView;
import it.unical.ea_project_javafx.dto.activity.ActivityDTO;
import it.unical.ea_project_javafx.dto.trip.StageDTO;
import it.unical.ea_project_javafx.dto.trip.TripDTO;
import it.unical.ea_project_javafx.ui.map.MarkerLayer;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.shape.Rectangle;
import javafx.geometry.Insets;
import java.util.List;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

public class ItineraryController implements ActivitySectionController, TripSectionController {

    @FXML
    public StackPane mapContainer;
    @FXML
    private VBox itinerarioItemsContainer;

    @FXML
    private void initialize() {
        mapContainer.setPadding(Insets.EMPTY);
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(mapContainer.widthProperty());
        clip.heightProperty().bind(mapContainer.heightProperty());
        clip.setArcWidth(44);
        clip.setArcHeight(44);
        mapContainer.setClip(clip);
    }

    private MapPoint standard = new MapPoint(41.8719, 12.5674);
    private MapView mapView;

    @Override
    public void setData(ActivityDTO activity) {
        itinerarioItemsContainer.getChildren().clear();

        setMap(activity);
        setItinerario(activity);

    }

    @Override
    public void setData(TripDTO trip) {
        itinerarioItemsContainer.getChildren().clear();

        setMap(trip);
        setItinerario(trip);

    }

    private void setItinerario(ActivityDTO activity) {

        String name = activity.getPlaceName() + ", " + activity.getCity();

        Node location = creaRigaTappa("1", name);
        location.getStyleClass().add("itinerario-box");

        location.setOnMouseClicked(mouseEvent -> {
            if (activity.getLatitude() == null || activity.getLongitude() == null)
                return;
            mapView.setZoom(mapView.getZoom() + 0.0001);
            mapView.setCenter(activity.getLatitude(), activity.getLongitude());
            mapView.setZoom(15);
        });

        itinerarioItemsContainer.getChildren().add(location);

    }

    private void setItinerario(TripDTO trip) {

        int lenght = (trip.getStages() == null ? 0 : trip.getStages().size());

        if (lenght > 0) {
            for (int i = 0; i < lenght; i++) {

                StageDTO stage = trip.getStages().get(i);
                String name = stage.getLocationName() + ", " + stage.getCity();

                Node location = creaRigaTappa(String.valueOf(i + 1), name);
                location.getStyleClass().add("itinerario-box");

                location.setOnMouseClicked(mouseEvent -> {
                    if (stage.getLatitude() == null || stage.getLongitude() == null)
                        return;
                    mapView.setZoom(mapView.getZoom() + 0.0001);
                    mapView.setCenter(stage.getLatitude(), stage.getLongitude());
                    mapView.setZoom(15);
                });
                itinerarioItemsContainer.getChildren().add(location);
            }
        } else {
            Label lbl = new Label("Nessun itinerario disponibile!");
            lbl.setStyle("-fx-text-fill: white");
            itinerarioItemsContainer.getChildren().add(lbl);
        }

    }

    private void setMap(ActivityDTO activity) {

        mapView = new MapView();
        mapView.setMinSize(0, 0);
        mapView.setPrefSize(650, 340);
        mapView.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        if (activity.getLongitude() != null && activity.getLatitude() != null) {
            MapPoint centro = new MapPoint(activity.getLatitude(), activity.getLongitude());
            mapView.setCenter(centro);

            mapView.setZoom(13);

            MarkerLayer markerLayer = new MarkerLayer(centro, "1");
            mapView.addLayer(markerLayer);
            mapView.addEventHandler(ScrollEvent.SCROLL, ScrollEvent::consume);

        } else {
            mapView.setCenter(standard);
            mapView.setZoom(4);
        }

        mapContainer.getChildren().setAll(mapView);

    }

    private void setMap(TripDTO trip) {

        mapView = new MapView();
        mapView.setMinSize(0, 0);
        mapView.setPrefSize(650, 340);
        mapView.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        var mappedStages = (trip.getStages() == null ? List.<StageDTO>of() : trip.getStages()).stream()
                .filter(stage -> stage.getLatitude() != null && stage.getLongitude() != null).toList();
        if (!mappedStages.isEmpty()) {
            MapPoint centro = new MapPoint(mappedStages.getFirst().getLatitude(),
                    mappedStages.getFirst().getLongitude());
            mapView.setCenter(centro);
            mapView.setZoom(14);

            Integer number = 1;
            for (StageDTO stage : mappedStages) {
                MapPoint point = new MapPoint(stage.getLatitude(), stage.getLongitude());
                MarkerLayer markerLayer = new MarkerLayer(point, String.valueOf(number));
                number++;
                mapView.addLayer(markerLayer);
            }
            mapView.addEventHandler(ScrollEvent.SCROLL, ScrollEvent::consume);

        } else {
            mapView.setCenter(standard);
            mapView.setZoom(4);
        }

        mapContainer.getChildren().setAll(mapView);

    }

    private Node creaRigaTappa(String numero, String nome) {

        Circle pallino = new Circle(10, Color.BLACK);
        pallino.setStroke(Color.WHITE);
        pallino.setStrokeWidth(2);

        Label numeroLbl = new Label(numero);
        numeroLbl.getStyleClass().add("number-label");
        StackPane cerchio = new StackPane(pallino, numeroLbl);

        Label nomeLbl = new Label(nome);
        nomeLbl.getStyleClass().add("location-text");

        HBox riga = new HBox(10, cerchio, nomeLbl);
        riga.setAlignment(Pos.CENTER_LEFT);
        return riga;

    }

}
