package it.unical.ea_project_javafx.controller.activity.reviews;

import it.unical.ea_project_javafx.dto.review.ResponseDTO;
import it.unical.ea_project_javafx.security.UserSession;
import it.unical.ea_project_javafx.util.DateTimeUtils;
import java.util.Objects;
import java.util.function.Consumer;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import lombok.Setter;

public class ResponseItemController {

    @FXML
    private Label lblTestoRisposta;
    @FXML
    private Label lblDataRisposta;
    @FXML
    private MenuButton btnResponseActions;

    private ResponseDTO response;

    @Setter
    private Consumer<ResponseDTO> onEdit;
    @Setter
    private Consumer<ResponseDTO> onDelete;

    public void setData(ResponseDTO response) {
        this.response = response;

        lblTestoRisposta.setText(response.getComment());

        String data = DateTimeUtils.formatDateTime(response.getCreatedAt().toString());
        if (response.getUpdatedAt() != null)
            data += " (modificata)";
        lblDataRisposta.setText(data);

        UserSession session = UserSession.getInstance();
        Long myId = session.isLoggedIn() ? session.getId() : null;
        boolean isAuthor = myId != null && Objects.equals(response.getOrganizerId(), myId);

        btnResponseActions.setVisible(isAuthor);
        btnResponseActions.setManaged(isAuthor);
    }

    @FXML
    public void handleEdit(ActionEvent event) {
        if (onEdit != null)
            onEdit.accept(response);
    }

    @FXML
    public void handleDelete(ActionEvent event) {
        if (onDelete != null)
            onDelete.accept(response);
    }
}
