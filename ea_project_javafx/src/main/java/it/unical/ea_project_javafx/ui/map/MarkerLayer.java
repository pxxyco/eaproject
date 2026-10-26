package it.unical.ea_project_javafx.ui.map;

import com.gluonhq.maps.MapLayer;
import com.gluonhq.maps.MapPoint;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

public class MarkerLayer extends MapLayer {

    private final MapPoint point;
    private final StackPane container;

    public MarkerLayer(MapPoint point, String testo) {
        this.point = point;
        Circle marker = new Circle(10, Color.BLACK);
        marker.setStroke(Color.WHITE);
        marker.setStrokeWidth(2);

        Label label = new Label(testo);
        label.setStyle(
                "-fx-background-color: transparent; -fx-padding: 2 6; -fx-background-radius: 4; -fx-text-fill: white; -fx-font-size: 11px;");

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
