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
public class ListActivitiesDTO {

    private Long listId;

    private Long activityId;

    private String title;

    private String city;

    private String category;

    private LocalDateTime addedAt;

}
