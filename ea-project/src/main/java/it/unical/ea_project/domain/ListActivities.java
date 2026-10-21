package it.unical.ea_project.domain;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "favoriteList")
@Entity
@IdClass(ListActivities.ListActivityId.class)
@Table(name = "\"LIST_ACTIVITIES\"")
public class ListActivities {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListActivityId implements Serializable {
        private Long favoriteList;
        private Long activityId;
    }

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "list_id", nullable = false)
    private FavoriteList favoriteList;

    @Id
    @Column(name = "activity_id", nullable = false)
    private Long activityId;

    @Column(name = "added_at")
    private LocalDateTime addedAt;

}