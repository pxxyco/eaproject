package it.unical.ea_project_javafx.controller.activity;

import it.unical.ea_project_javafx.dto.ActivityDTO;
import it.unical.ea_project_javafx.dto.TripDTO;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class ActivityDescriptionController implements  ActivitySectionController, TripSectionController{

    @FXML
    private Label lblDescrizione;
    @FXML
    private Label lblNota;

    @Override
    public void setData(ActivityDTO activity) {

        if(activity.getDescription() !=null){
            lblDescrizione.setText(activity.getDescription());
        }

        if(activity.getNotes() !=null){
            lblNota.setText("NOTE AGGIUNTIVE: \n\n" + activity.getNotes());
        }

    }

    @Override
    public void setData(TripDTO trip) {

        lblNota.setVisible(false);
        if(trip.getDescription() != null) {
            lblDescrizione.setText(trip.getDescription());
        }

    }

}
