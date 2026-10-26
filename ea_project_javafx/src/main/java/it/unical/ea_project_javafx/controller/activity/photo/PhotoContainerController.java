package it.unical.ea_project_javafx.controller.activity.photo;

import com.google.gson.reflect.TypeToken;
import it.unical.ea_project_javafx.controller.activity.ActivitySectionController;
import it.unical.ea_project_javafx.controller.activity.TripSectionController;
import it.unical.ea_project_javafx.dto.activity.ActivityDTO;
import it.unical.ea_project_javafx.dto.activity.ActivityImageDTO;
import it.unical.ea_project_javafx.dto.trip.TripDTO;
import it.unical.ea_project_javafx.dto.trip.TripImageDTO;
import it.unical.ea_project_javafx.service.api.ApiService;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.function.Consumer;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.FlowPane;
import lombok.Setter;

public class PhotoContainerController implements ActivitySectionController, TripSectionController {

    private static final Type TRIP_IMAGE_LIST_TYPE = new TypeToken<List<TripImageDTO>>() {
    }.getType();
    private final String path = "/it/unical/ea_project_javafx/fxml/activity/photo";

    @Setter
    private Consumer<List<String>> onGalleryRequested;

    @Setter
    private Consumer<String> onFotoRequested;

    private List<String> imageUrls;

    @FXML
    private FlowPane imgsContainer;

    @FXML
    public void initialize() {
    }

    public void setData(ActivityDTO activity) {
        loadActivityImages(activity.getImages());
    }

    public void setData(TripDTO trip) {

        ApiService.get(
                ApiService.BASE_URL + "/api/trip-images/trip/" + trip.getTripId(),
                response -> {
                    List<TripImageDTO> images = ApiService.fromJson(response.body(), TRIP_IMAGE_LIST_TYPE);
                    loadTripImages(images);
                },
                () -> {
                },
                loading -> {
                });

    }

    private void loadActivityImages(List<ActivityImageDTO> activities) {

        imageUrls = activities.stream()
                .map(a -> ApiService.BASE_URL + a.getImageUrl())
                .toList();
        int max = Math.min(imageUrls.size(), 5);
        for (int i = 0; i < max; i++) {
            loadImage(imageUrls.get(i));
        }

        if (imageUrls.size() > 5) {
            addPlaceholder();
        }
    }

    private void loadTripImages(List<TripImageDTO> images) {

        imageUrls = images.stream()
                .map(i -> ApiService.BASE_URL + i.getImageUrl())
                .toList();
        int max = Math.min(imageUrls.size(), 5);
        for (int i = 0; i < max; i++) {
            loadImage(imageUrls.get(i));
        }

        if (imageUrls.size() > 5) {
            addPlaceholder();
        }

    }

    private void loadImage(String url) {

        try {

            FXMLLoader loader = new FXMLLoader(getClass().getResource(path + "/photo-item.fxml"));
            Node foto = loader.load();

            PhotoItemController controller = loader.getController();
            controller.setFoto(url);
            controller.setOnFotoClickedCallBack(u -> {
                if (onFotoRequested != null) {
                    onFotoRequested.accept(u);
                }
            });

            imgsContainer.getChildren().add(foto);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private void addPlaceholder() {

        try {

            FXMLLoader placeholderLoader = new FXMLLoader();
            placeholderLoader.setLocation(getClass().getResource(path + "/photo-placeholder.fxml"));
            Node placeholder = placeholderLoader.load(); // load() PRIMA di getController()

            PhotoPlaceholderController controller = placeholderLoader.getController();
            controller.setOnPlaceholderClickedCallBack(() -> {
                if (onGalleryRequested != null)
                    onGalleryRequested.accept(imageUrls);
            });

            imgsContainer.getChildren().add(placeholder);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

}
