package it.unical.ea_project.domain.favorites;

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
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "user")
@Entity
@Table(name = "\"FAVORITES\"",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "activity_id"}),
                @UniqueConstraint(columnNames = {"user_id", "trip_id"})
        })

public class Favorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "favorite_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "activity_id")
    private Long activityId;

    @Column(name = "trip_id")
    private Long tripId;

    @Column(name = "added_at")
    private LocalDateTime addedAt;

    @PrePersist
    @PreUpdate
    private void validateTarget() {
        if ((activityId == null) == (tripId == null)) {
            throw new IllegalStateException("Un preferito deve riferirsi a un'attività oppure a un viaggio, non a entrambi né a nessuno.");
        }
        if (addedAt == null) addedAt = LocalDateTime.now();
    }
}
