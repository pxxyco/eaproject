package it.unical.ea_project_javafx.controller.activity.photo;

import java.util.function.Consumer;
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

        this.url = url;
        image.setImage(new Image(url, 150, 150, false, true));

    }

    @FXML
    void handleClick(MouseEvent event) {

        if (onFotoClickedCallBack != null) {
            onFotoClickedCallBack.accept(url);
        }

    }

}
