package it.unical.ea_project_javafx.dto.home;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivityHomeDTO {

    private Long activityId;

    private String title;

    private String city;

    private String category;

    private Double price;

    private Double averageRating;

    private String imageUrl;

    private String startDate;

    private String endDate;

}
