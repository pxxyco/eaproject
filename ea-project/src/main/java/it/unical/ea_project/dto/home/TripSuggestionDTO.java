package it.unical.ea_project.dto.home;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TripSuggestionDTO {
    private Long tripId;
    private String title;
    private String location;
}
