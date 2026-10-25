package it.unical.ea_project.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

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
        setAddedAt(LocalDateTime.now());
    }
}