package it.unical.ea_project.dto.trip;

import it.unical.ea_project.domain.trip.Stage;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StageDTO {

    private Long stageId;

    private Long tripId;

    private String title;

    private String description;

    // Stage.StageCategory: EXCURSION, VISIT, HOTEL, MEAL, TRANSPORT, OTHER
    private String category;

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

    public static StageDTO toDto(Stage stage) {
        return StageDTO.builder()
                .stageId(stage.getStageId())
                .tripId(stage.getTrip() != null ? stage.getTrip().getTripId() : null)
                .title(stage.getTitle())
                .description(stage.getDescription())
                .category(stage.getCategory() != null ? stage.getCategory().name() : null)
                .locationName(stage.getLocationName())
                .city(stage.getCity())
                .country(stage.getCountry())
                .latitude(stage.getLatitude())
                .longitude(stage.getLongitude())
                .day(stage.getDay())
                .orderInDay(stage.getOrderInDay())
                .startTime(stage.getStartTime())
                .endTime(stage.getEndTime())
                .photoUrl(stage.getPhotoUrl())
                .notes(stage.getNotes())
                .createdAt(stage.getCreatedAt())
                .updatedAt(stage.getUpdatedAt())
                .build();
    }
}
