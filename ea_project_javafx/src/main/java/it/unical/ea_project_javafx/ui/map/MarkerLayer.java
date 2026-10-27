package it.unical.ea_project_javafx.ui.map;

import com.gluonhq.maps.MapLayer;
import com.gluonhq.maps.MapPoint;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;

public class MarkerLayer extends MapLayer {

    private final MapPoint point;
    private final StackPane container;

    public MarkerLayer(MapPoint point, String testo) {
        this.point = point;
        Circle marker = new Circle(10);
        marker.getStyleClass().add("map-marker");

        Label label = new Label(testo);
        label.getStyleClass().add("map-marker-label");

        this.container = new StackPane(marker, label);
        container.setAlignment(Pos.CENTER);

        getChildren().add(container);
    }

    @Override
    protected void layoutLayer() {
        var pixel = getMapPoint(point.getLatitude(), point.getLongitude());
        container.setTranslateX(pixel.getX() - container.getWidth() / 2);
        container.setTranslateY(pixel.getY() - container.getHeight());
    }
}
