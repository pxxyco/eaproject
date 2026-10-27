package it.unical.ea_project_javafx.controller.activity.photo;

import javafx.fxml.FXML;
import javafx.scene.input.MouseEvent;
import lombok.Setter;

public class PhotoPlaceholderController {

    @Setter
    private Runnable onPlaceholderClickedCallBack;

    @FXML
    void handleClick(MouseEvent event) {
        if (onPlaceholderClickedCallBack != null) {
            onPlaceholderClickedCallBack.run();
        }

    }

}
