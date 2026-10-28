package it.unical.ea_project_javafx.controller.activity.photo;

import java.util.function.Consumer;
import it.unical.ea_project_javafx.util.ImageUtils;
import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import lombok.Setter;

public class PhotoItemController {

    @Setter
    private Consumer<String> onFotoClickedCallBack;

    @FXML
    private ImageView image;

    private String url;

    public void setFoto(String url) {

        this.url = ImageUtils.buildFullUrl(url);
        try {
            image.setImage(new Image(this.url, 150, 150, true, true, true));
        } catch (IllegalArgumentException invalid) {
            this.url = ImageUtils.getDefaultImageUrl();
            image.setImage(new Image(this.url, 150, 150, true, true, true));
        }

    }

    @FXML
    void handleClick(MouseEvent event) {

        if (onFotoClickedCallBack != null) {
            onFotoClickedCallBack.accept(url);
        }

    }

}
