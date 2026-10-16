package it.unical.ea_project_javafx.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TripImageDTO {

    private Long imageId;

    private Long tripId;

    private String imageUrl;

    private Integer orderIndex;

}
