package it.unical.ea_project_javafx.controller.activity.photo;

import it.unical.ea_project_javafx.ui.dialog.FullscreenPhotoViewer;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import lombok.Setter;

public class PhotoGalleryController {

    @Setter
    private Runnable onBackLink;

    @FXML
    public StackPane rootStack;
    @FXML
    private FlowPane imgsContainer;
    @FXML
    private Button moreButton;

    private int paging = 0;
    private static final int PAGE_SIZE = 20;
    private List<String> imageUrls;
    private final String path = "/it/unical/ea_project_javafx/fxml/activity/photo";

    @FXML
    void initialize() throws IOException {
        paging = 0;
    }

    @FXML
    private void backButtonAction(ActionEvent event) throws IOException {
        if (onBackLink != null)
            onBackLink.run();
    }

    @FXML
    public void handleAddMore(ActionEvent actionEvent) {
        loadNextPage();
    }

    public void setImages(List<String> urls) {

        paging = 0;
        imgsContainer.getChildren().clear();
        imageUrls = urls == null ? List.of() : urls.stream().filter(Objects::nonNull).toList();

        loadNextPage();

    }

    private void loadNextPage() {

        int start = paging * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, imageUrls.size());

        for (int i = start; i < end; i++) {
            loadImage(imageUrls.get(i));
        }

        paging++;
        boolean more = end < imageUrls.size();
        moreButton.setVisible(more);
        moreButton.setManaged(more);

    }

    private void loadImage(String url) {

        try {

            FXMLLoader loader = new FXMLLoader(getClass().getResource(path + "/photo-item.fxml"));
            Node foto = loader.load();

            PhotoItemController controller = loader.getController();
            controller.setFoto(url);
            controller.setOnFotoClickedCallBack(s -> mostraFotoFullscreen(url));

            imgsContainer.getChildren().add(foto);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private void mostraFotoFullscreen(String url) {
        FullscreenPhotoViewer.mostra(rootStack, url);
    }
}
