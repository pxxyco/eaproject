package it.unical.ea_project_javafx.dto.home;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivitySuggestionDTO {

    private Long activityId;

    private String title;

    private String category;

    private String city;

}
