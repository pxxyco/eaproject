package it.unical.ea_project.domain.booking;

import it.unical.ea_project.domain.activity.Activity;
import it.unical.ea_project.domain.trip.Trip;
import it.unical.ea_project.domain.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "\"BOOKING\"")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long bookingId;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "trip_id")
    private Trip trip;

    @ManyToOne
    @JoinColumn(name = "activity_id")
    private Activity activity;

    @Column(name = "seats")
    private Integer seats;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private BookingType bookingType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BookingStatus bookingStatus;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // -- enum per il tipo di prenotazione --
    public enum BookingType {
        TRIP, ACTIVITY
    }

    // -- enum per lo stato della prenotazione --
    public enum BookingStatus {
        PENDING, COMPLETED, CONFIRMED, CANCELLED
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        validateExclusivity();
    }

    @PreUpdate
    protected void onUpdate() {
        validateExclusivity();
    }

    protected void validateExclusivity() {

        boolean hasTrip = trip != null;
        boolean hasActivity = activity != null;

        if (hasTrip == hasActivity) {
            throw new IllegalStateException("Booking può essere o Trip o Activity, non entrambi");
        }

        if (hasTrip && bookingType != BookingType.TRIP) {
            throw new IllegalStateException("il TYPE non coincide con il campo inserito, TRIP");
        }

        if (hasActivity && bookingType != BookingType.ACTIVITY) {
            throw new IllegalStateException("il TYPE non coincide con il campo inserito, ACTIVITY");
        }
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }
}
