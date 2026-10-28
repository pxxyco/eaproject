package it.unical.ea_project.domain.favorites;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

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
