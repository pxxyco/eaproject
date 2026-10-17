package it.unical.ea_project_javafx.model;

import java.util.List;

import it.unical.ea_project_javafx.dto.TripDTO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class OrganizerHomeModel {

    private ObservableList<TripDTO> organizerTrips;
    private boolean isLoading;
    private String currentFilter;

    public OrganizerHomeModel() {
        this.organizerTrips = FXCollections.observableArrayList();
        this.isLoading = false;
        this.currentFilter = "ALL";
    }

    public void setTrips(List<TripDTO> trips) {
        this.organizerTrips.setAll(trips);
    }

}