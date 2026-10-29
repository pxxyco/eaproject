package it.unical.ea_project.domain.review;

import it.unical.ea_project.domain.activity.Activity;
import it.unical.ea_project.domain.trip.Trip;
import it.unical.ea_project.domain.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "\"REVIEW\"")
@SQLRestriction("deleted_at = false")
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private Long reviewId;

    // Relazione con l'utente che scrive la recensione
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Relazione opzionale con Trip
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id")
    private Trip trip;

    // Relazione opzionale con Activity
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id")
    private Activity activity;

    @Column(name = "rating", nullable = false)
    private Integer rating;

    @Column(name = "comment", length = 200)
    private String comment;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "deleted_at")
    private Boolean deleted = false;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        validateTripXorActivity();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        validateTripXorActivity();
    }

    private void validateTripXorActivity() {
        // restituisce true solo se uno è null e l'altro no
        if ((this.trip == null) == (this.activity == null)) {
            throw new IllegalStateException(
                    "Una recensione deve essere associata esattamente a un Trip O a una Activity (non entrambi, non nessuno).");
        }
    }

}
