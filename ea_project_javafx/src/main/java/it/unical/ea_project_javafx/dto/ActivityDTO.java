package it.unical.ea_project_javafx.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityDTO {

    private Long activityId;

    private Long createdByUserId;

    private String title;

    private String description;

    private String category;

    private Double latitude;

    private Double longitude;

    private String placeName;

    private String city;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    private Integer durationMinutes;

    private Double price;

    private Integer maxSeats;

    private Integer availableSeats;

    private String status;

    private Double averageRating;

    private String notes;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;

    private List<ActivityImageDTO> images;

}
