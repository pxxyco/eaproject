package it.unical.ea_project_javafx.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TripDTO {

    private Long tripId;

    private Long createdByUserId;

    private String title;

    private String description;

    private String destinationCountry;

    private String destinationCity;

    private LocalDate startDate;

    private LocalDate endDate;

    private BigDecimal totalPrice;

    private Integer maxSeats;

    private Integer availableSeats;

    private String status;  // Trip.TripStatus: DRAFT, PUBLISHED, CANCELLED, COMPLETED

    private String icsUid;

    private BigDecimal averageRating;

    private String coverPhotoUrl;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;

    private List<StageDTO> stages;

}
