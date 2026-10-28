package it.unical.ea_project.dto.home;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivitySuggestionDTO {
    private Long activityId;
    private String title;
    private String city;
}
