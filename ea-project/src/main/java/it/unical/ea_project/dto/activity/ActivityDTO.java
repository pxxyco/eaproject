package it.unical.ea_project.dto.activity;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityDTO {

    private Long activityId;

    private Long createdByUserId;

    private String title;

    private String description;

    // Activity.Category: EXCURSION, VISIT, TRANSPORT, HOTEL, MEAL, OTHER
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

    // Activity.Status: PUBLISHED, CANCELLED, COMPLETED
    private String status;

    private Double averageRating;

    private String notes;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;

    private List<ActivityImageDTO> images;
}
