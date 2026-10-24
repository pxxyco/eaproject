package it.unical.ea_project.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor

@Entity
@Table(name = "\"TRIP\"")
public class Trip {

    public enum TripStatus {
        DRAFT,
        PUBLISHED,
        CANCELLED,
        COMPLETED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "trip_id")
    private Long tripId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "destination_country", length = 100)
    private String destinationCountry;

    @Column(name = "destination_city", length = 100)
    private String destinationCity;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "total_price", precision = 10, scale = 2)
    private BigDecimal totalPrice;

    @Column(name = "max_seats")
    private Integer maxSeats;

    @Column(name = "available_seats")
    private Integer availableSeats;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TripStatus status;

    @Column(name = "ics_uid", length = 255)
    private String icsUid;

    @Column(name = "average_rating", precision = 3, scale = 2)
    private BigDecimal averageRating;

    @Column(name = "cover_photo_url", length = 2083)
    private String coverPhotoUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // Relazione bidirezionale con le tappe (Stage)
    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Stage> stages = new ArrayList<>();

    // Distinguish an omitted itinerary (preserve it) from an explicitly empty itinerary.
    @Transient
    @com.fasterxml.jackson.annotation.JsonIgnore
    @lombok.Setter(lombok.AccessLevel.NONE)
    private boolean stagesProvided;

    public void setStages(List<Stage> stages) {
        this.stages = stages;
        this.stagesProvided = true;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Metodi di utilità per gestire la relazione bidirezionale
    public void addStage(Stage stage) {
        stages.add(stage);
        stage.setTrip(this);
    }

    public void removeStage(Stage stage) {
        stages.remove(stage);
        stage.setTrip(null);
    }

}