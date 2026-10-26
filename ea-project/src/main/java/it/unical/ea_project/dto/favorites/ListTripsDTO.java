package it.unical.ea_project.dto.favorites;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
