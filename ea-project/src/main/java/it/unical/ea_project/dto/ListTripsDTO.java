package it.unical.ea_project.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ListTripsDTO {

    private Long listId;

    private Long tripId;

    private String title;

    private String location;

    private LocalDateTime addedAt;

}
