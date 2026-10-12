package it.unical.ea_project_javafx.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StageDTO {

    private Long stageId;

    private Long tripId;

    private String title;

    private String description;

    private String category;    // Stage.StageCategory: EXCURSION, VISIT, HOTEL, MEAL, TRANSPORT, OTHER

    private String locationName;

    private String city;

    private String country;

    private Double latitude;

    private Double longitude;

    private Integer day;

    private Integer orderInDay;

    private LocalTime startTime;

    private LocalTime endTime;

    private String photoUrl;

    private String notes;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
