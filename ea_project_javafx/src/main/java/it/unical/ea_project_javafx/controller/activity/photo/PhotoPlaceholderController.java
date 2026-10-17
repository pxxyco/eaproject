package it.unical.ea_project_javafx.controller.activity.photo;

import javafx.fxml.FXML;
import javafx.scene.input.MouseEvent;
import lombok.Setter;

public class PhotoPlaceholderController {

    @Setter
    private Runnable onPlaceholderClickedCallBack;

    @FXML
    void handleClick(MouseEvent event) {

        if (onPlaceholderClickedCallBack != null)
            onPlaceholderClickedCallBack.run();

    }

    /*
    @FXML
    void navigateToGallery(MouseEvent event) {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource(path + "photo/photo-gallery.fxml")
        );

        try{
            Scene scene = new Scene(loader.load());

            Stage stage = (Stage) ((Node) event.getSource())
                    .getScene()
                    .getWindow();

            stage.setScene(scene);
            stage.show();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

     */

}
