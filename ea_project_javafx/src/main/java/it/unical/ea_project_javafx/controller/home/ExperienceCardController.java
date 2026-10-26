package it.unical.ea_project_javafx.controller.home;

import it.unical.ea_project_javafx.controller.activity.ActivityDetailsController;
import it.unical.ea_project_javafx.dto.home.ActivityHomeDTO;
import it.unical.ea_project_javafx.dto.home.TripHomeDTO;
import it.unical.ea_project_javafx.navigation.SceneNavigator;
import it.unical.ea_project_javafx.util.ImageUtils;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

import static it.unical.ea_project_javafx.controller.activity.ActivityDetailsController.Type.ACTIVITY;

public class ExperienceCardController {

    @FXML
    private VBox cardRoot;
    @FXML
    private ImageView imageView;
    @FXML
    private Label categoryLabel;
    @FXML
    private Label ratingLabel;
    @FXML
    private Label titleLabel;
    @FXML
    private Label locationLabel;
    @FXML
    private Label dateLabel;
    @FXML
    private Label priceLabel;
    @FXML
    private ProgressIndicator imageLoader;
    @FXML
    private Button editButton;
    @FXML
    private Button deleteButton;

    private static final double IMAGE_WIDTH = 315;
    private static final double IMAGE_HEIGHT = 199;
    private static final int MAX_CACHE_SIZE = 32;

    private static final Map<String, Image> IMAGE_CACHE = new LinkedHashMap<>(MAX_CACHE_SIZE, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Image> eldest) {
            return size() > MAX_CACHE_SIZE;
        }
    };

    private static final DateTimeFormatter CARD_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private ActivityDetailsController.Type detailType;
    private String detailId;
    private Runnable editAction;

    @FXML
    public void initialize() {
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(imageView.fitWidthProperty());
        clip.setArcWidth(24);
        clip.setArcHeight(24);
        clip.heightProperty().bind(imageView.fitHeightProperty());
        imageView.setClip(clip);
        editButton.addEventFilter(MouseEvent.MOUSE_CLICKED, Event::consume);
        deleteButton.addEventFilter(MouseEvent.MOUSE_CLICKED, Event::consume);
    }

    public void setCardWidth(double width) {
        cardRoot.setMinWidth(width);
        cardRoot.setPrefWidth(width);
        cardRoot.setMaxWidth(width);
        imageView.setFitWidth(width - 12);
        var container = (Region) imageView.getParent();
        container.setMinWidth(0);
        container.setPrefWidth(width - 12);
        container.setMaxWidth(width - 12);
    }

    private static synchronized Image getCachedImage(String imageUrl) {
        return IMAGE_CACHE.get(imageUrl);
    }

    private static synchronized Image createAndCacheImage(String imageUrl) {
        Image cached = IMAGE_CACHE.get(imageUrl);
        if (cached != null)
            return cached;

        Image image = new Image(imageUrl, 640, 400, true, true, true);
        IMAGE_CACHE.put(imageUrl, image);
        return image;
    }

    static Image previewImage(String rawUrl) {
        return createAndCacheImage(ImageUtils.buildFullUrl(rawUrl));
    }

    static synchronized void removeCachedImage(String imageUrl, Image image) {
        Image cached = IMAGE_CACHE.get(imageUrl);
        if (cached == image) {
            IMAGE_CACHE.remove(imageUrl);
        }
    }

    private void hideImageLoader() {
        imageLoader.setVisible(false);
        imageLoader.setManaged(false);
    }

    private void showImageLoader() {
        imageLoader.setVisible(true);
        imageLoader.setManaged(true);
    }

    public void setData(String title, String location, String category, String price, String rating, String imageUrl,
            String dateInfo) {
        titleLabel.setText(title);
        locationLabel.setText(location);
        categoryLabel.setText(category);
        priceLabel.setText("da " + price);
        ratingLabel.setText("⭐ " + rating);

        dateLabel.setText(dateInfo);
        dateLabel.setVisible(dateInfo != null && !dateInfo.isBlank());
        dateLabel.setManaged(dateLabel.isVisible());

        loadImage(imageUrl);
    }

    public void setEditAction(Runnable editAction) {
        this.editAction = editAction;
        editButton.setVisible(editAction != null);
        editButton.setManaged(editAction != null);
        editButton.setOnAction(event -> {
            event.consume();
            if (this.editAction != null)
                this.editAction.run();
        });
    }

    public void setDeleteAction(Runnable deleteAction) {
        deleteButton.setVisible(deleteAction != null);
        deleteButton.setManaged(deleteAction != null);
        deleteButton.setOnAction(event -> {
            event.consume();
            if (deleteAction != null)
                deleteAction.run();
        });
    }

    public void setData(TripHomeDTO dto) {
        if (dto == null)
            return;

        detailType = ActivityDetailsController.Type.TRIP;
        detailId = dto.getTripId() != null ? dto.getTripId().toString() : "";

        String title = dto.getTitle() != null ? dto.getTitle() : "Senza Titolo";
        String location = dto.getLocation() != null ? dto.getLocation() : "";
        String category = "Viaggio";
        String price = String.format("€%.0f", dto.getTotalPrice() != null ? dto.getTotalPrice().doubleValue() : 0.0);
        String rating = dto.getAverageRating() != null ? String.format("%.1f", dto.getAverageRating().doubleValue())
                : "0,0";
        String imageUrl = dto.getCoverPhotoUrl();
        String dateInfo = formatDateRange(dto.getStartDate(), dto.getEndDate());

        cardRoot.setOnMouseClicked(event -> handleCardClick(detailType, detailId));

        setData(title, location, category, price, rating, imageUrl, dateInfo);
    }

    public void setData(ActivityHomeDTO dto) {
        if (dto == null)
            return;

        detailType = ACTIVITY;
        detailId = dto.getActivityId() != null ? dto.getActivityId().toString() : "";

        String title = dto.getTitle() != null ? dto.getTitle() : "Senza Titolo";
        String location = dto.getCity() != null ? dto.getCity() : "";
        String category = dto.getCategory() != null ? dto.getCategory() : "Attività";
        String price = String.format("€%.0f", dto.getPrice() != null ? dto.getPrice() : 0.0);
        String rating = String.format("%.1f", dto.getAverageRating() != null ? dto.getAverageRating() : 0.0);
        String imageUrl = dto.getImageUrl();
        String dateInfo = formatDateRange(dto.getStartDate(), dto.getEndDate());

        cardRoot.setOnMouseClicked(event -> handleCardClick(detailType, detailId));

        setData(title, location, category, price, rating, imageUrl, dateInfo);
    }

    private String formatDateRange(String startDate, String endDate) {
        if (startDate == null || endDate == null || startDate.isBlank() || endDate.isBlank()) {
            return "";
        }

        try {
            LocalDate start = LocalDate.parse(startDate.substring(0, 10));
            LocalDate end = LocalDate.parse(endDate.substring(0, 10));
            String formattedStart = start.format(CARD_DATE_FORMAT);

            return start.equals(end) ? formattedStart : formattedStart + " - " + end.format(CARD_DATE_FORMAT);
        } catch (DateTimeParseException | IndexOutOfBoundsException e) {
            return "";
        }
    }

    private void loadImage(String rawImageUrl) {
        String fullUrl = ImageUtils.buildFullUrl(rawImageUrl);

        showImageLoader();

        Image image = getCachedImage(fullUrl);
        if (image == null) {
            image = createAndCacheImage(fullUrl);
        }

        imageView.setImage(image);

        if (image.isError()) {
            removeCachedImage(fullUrl, image);
            handleImageError(fullUrl);
            return;
        }

        if (image.getProgress() >= 1.0) {
            hideImageLoader();
            return;
        }

        Image targetImage = image;
        final String currentUrl = fullUrl;

        ChangeListener<Number> progressListener = new ChangeListener<>() {
            @Override
            public void changed(ObservableValue<? extends Number> obs, Number oldVal,
                    Number newVal) {
                if (newVal.doubleValue() >= 1.0) {
                    hideImageLoader();
                    targetImage.progressProperty().removeListener(this);
                }
            }
        };
        image.progressProperty().addListener(progressListener);

        ChangeListener<Boolean> errorListener = new ChangeListener<>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> obs, Boolean oldVal,
                    Boolean isError) {
                if (Boolean.TRUE.equals(isError)) {
                    targetImage.errorProperty().removeListener(this);
                    removeCachedImage(currentUrl, targetImage);
                    handleImageError(currentUrl);
                }
            }
        };
        image.errorProperty().addListener(errorListener);
    }

    private void handleImageError(String failedUrl) {
        hideImageLoader();

        String defaultUrl = ImageUtils.getDefaultImageUrl();

        if (failedUrl != null && !failedUrl.equals(defaultUrl)) {
            loadImage(defaultUrl);
        } else {
            imageView.setImage(null);
        }
    }

    private void handleCardClick(ActivityDetailsController.Type type, String id) {
        if (type == null || id == null || id.isBlank())
            return;

        SceneNavigator.getInstance().loadScene("/it/unical/ea_project_javafx/fxml/activity/activity-details.fxml",
                controller -> {
                    if (controller instanceof ActivityDetailsController attivitaCtrl) {
                        attivitaCtrl.loadData(type, id);
                    }
                });
    }
}
