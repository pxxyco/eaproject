package it.unical.ea_project.domain;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"favoriteList", "user"})
@Entity
@IdClass(ListSharing.ListSharingId.class)
@Table(name = "\"LIST_SHARING\"")
public class ListSharing {

    /**
     * SharingPermission
     */
    public enum SharingPermission {
    
        VIEW,
        EDIT

    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListSharingId implements Serializable {
        private Long favoriteList;
        private Long user;
    }

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "list_id", nullable = false)
    private FavoriteList favoriteList;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "permission", nullable = false)
    private SharingPermission permission;

    @Column(name = "shared_at")
    private LocalDateTime sharedAt;
}