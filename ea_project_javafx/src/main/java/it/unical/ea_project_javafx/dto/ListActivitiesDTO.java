package it.unical.ea_project_javafx.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ListActivitiesDTO {

    private Long listId;

    private Long activityId;

    private LocalDateTime addedAt;

    private String title;

    private String city;

    private String category;

}
