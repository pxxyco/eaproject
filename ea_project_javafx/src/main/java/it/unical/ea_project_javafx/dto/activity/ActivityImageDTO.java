package it.unical.ea_project_javafx.dto.activity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityImageDTO {

    private Long imageId;

    private Long activityId;

    private String imageUrl;

    private Integer orderIndex;

}
